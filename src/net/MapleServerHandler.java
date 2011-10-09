/**
    This file is part of the CorsaireServer, a fork of OdinMS
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
            Matthias Butz <matze@odinms.de>
            Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
**/

package net;

import client.MapleClient;
import constants.ServerConstants;
import java.sql.PreparedStatement;
import net.cashshop.handler.*;
import net.channel.ChannelServer;
import net.channel.handler.*;
import net.login.handler.*;
import tools.MapleAESOFB;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import server.MapleItemInformationProvider;
import tools.DatabaseConnection;
import tools.Randomizer;
import tools.factory.CashShopFactory;
import tools.factory.EffectFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.LoginFactory;
import tools.factory.MarketFactory;

/**
 * @name        MapleServerHandler
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleServerHandler extends IoHandlerAdapter {
    private int channel = -1;
    private ServerType type = null;
    private final int version = 83;
    private final static byte key[] = {0x13, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00,
                                      (byte) 0xB4, 0x00, 0x00, 0x00, 0x1B, 0x00, 0x00, 0x00, 0x0F, 0x00, 0x00,
                                      0x00, 0x33, 0x00, 0x00, 0x00, 0x52, 0x00, 0x00, 0x00};

    public MapleServerHandler(final ServerType type) {
        this.type = type;
    }

    public MapleServerHandler(final ServerType type, final int channel) {
        this.type = type;
        this.channel = channel;
    }

    @Override
    public void messageSent(final IoSession session, final Object message) throws Exception {
        final Runnable r = ((MaplePacket) message).getOnSend();
        if (r != null) {
            r.run();
        }
        super.messageSent(session, message);
    }

    @Override
    public void exceptionCaught(final IoSession session, final Throwable cause) throws Exception {
    }

    @Override
    public void sessionOpened(final IoSession session) throws Exception {
        System.out.println("New Session : " + session.getRemoteAddress());
        if (channel > -1) {
            if (ChannelServer.getInstance(channel).isShutdown()) {
                session.close(true);
                return;
            }
        }
        final byte ivRecv[] = {70, 114, 122, (byte) Randomizer.nextInt(255)};
        final byte ivSend[] = {82, 48, 120, (byte) Randomizer.nextInt(255)};
        final MapleClient client = new MapleClient(new MapleAESOFB(key, ivSend, (short) (0xFFFF - version)), new MapleAESOFB(key, ivRecv, (short) version), session);
        client.setChannel(channel - client.getWorld() * ServerConstants.NUM_CHANNELS);
        session.write(LoginFactory.getHello((short) version, ivSend, ivRecv));
        session.setAttribute(MapleClient.CLIENT_KEY, client);
        session.getConfig().setIdleTime(IdleStatus.READER_IDLE, 30);
    }

    @Override
    public void sessionClosed(final IoSession session) throws Exception {
        final MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (client != null) {
            try {
                client.disconnect(true, type == ServerType.CASHSHOP ? true : false);
            } finally {
                session.close();
                session.removeAttribute(MapleClient.CLIENT_KEY);
            }
        }
        super.sessionClosed(session);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        byte[] content = (byte[]) message;
        SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream((byte[]) message));
        SeekableLittleEndianAccessor sleaREF = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream(content));
        short header = slea.readShort();
        for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
            if (recv.getValue() == header) {
                final MapleClient c = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
                if (recv.reqValidation() && !c.isLoggedIn()) {
                    return;
                }
                handlePacket(recv, slea, c, type);
                return;
            }
        }
        System.out.println("UNHANDLED" + sleaREF);
    }

    public static final void handlePacket(final RecvPacketOpcode header, final SeekableLittleEndianAccessor slea, final MapleClient c, final ServerType type) {
        switch(header) {
            case PONG:
                c.pongReceived();
                break;
            // LOGIN START
            case AFTER_LOGIN:
                c.announce(LoginFactory.pinOperation(0));
                break;
            case SERVERLIST_REREQUEST:
                LoginOperation.ServerListRequestHandler(c);
                break;
            case CHARLIST_REQUEST:
                LoginOperation.CharlistRequestHandler(slea, c);
                break;
            case LOGIN_PASSWORD:
                LoginOperation.LoginPasswordHandler(slea.readMapleAsciiString(), slea.readMapleAsciiString(), c);
                break;
            case RELOG:
                c.announce(LoginFactory.getRelogResponse());
                break;
            case SERVERLIST_REQUEST:
                LoginOperation.ServerListRequestHandler(c);
                break;
            case SERVERSTATUS_REQUEST:
                LoginOperation.ServerStatusRequestHandler(slea.readShort(), c);
                break;
            case CHECK_CHAR_NAME:
                LoginOperation.CheckCharNameHandler(slea.readMapleAsciiString(), c);
                break;
            case CREATE_CHAR:
                LoginOperation.CreateCharHandler(slea, c);
                break;
            case DELETE_CHAR:
                LoginOperation.DeleteCharHandler(slea, c);
                break;
            case GUEST_LOGIN:
                c.announce(LoginFactory.sendGuestTOS());
                break;
            case REGISTER_PIC:
                LoginOperation.RegisterPICHandler(slea, c);
                break;
            case PROMPT_PIC:
                LoginOperation.PromptPICHandler(slea.readMapleAsciiString(), slea.readInt(), slea.readMapleAsciiString(), c);
                break;
            case VIEW_ALL_CHAR:
                LoginOperation.ViewAllCharHandler(c);
                break;
            case VIEW_ALL_REGISTER_PIC:
                LoginOperation.ViewAllRegisterPICHandler(slea, c);
                break;
            case VIEW_ALL_PROMPT_PIC:
                LoginOperation.ViewAllPromptPICHandler(slea, c);
                break;
            case PLAYER_LOGGEDIN:
                int cid = slea.readInt();
                InterServerOperation.CatchSessionHandler(cid, c, type == ServerType.CASHSHOP);
                break;
            case CHANGE_CHANNEL:
            case ENTER_CASH_SHOP:
                InterServerOperation.ThrowSessionHandler(header == RecvPacketOpcode.ENTER_CASH_SHOP ? -10 : slea.readByte() + 1 + ServerConstants.NUM_CHANNELS * c.getPlayer().getWorld(), c);
                break;
            case CHANGE_MAP:
                if (type == ServerType.CASHSHOP) {
                    CashShopOperation.ExitHandler(c);
                } else {
                    IntraPersonalOperation.ChangeMapHandler(slea, c);
                }
                break;
            case MOVE_LIFE:
                MovementOperation.MoveLifeHandler(slea, c);
                break;
            case MOVE_PLAYER:
                MovementOperation.MovePlayerHandler(slea, c);
                break;
            case MOVE_SUMMON:
                MovementOperation.MoveSummonHandler(slea, c);
                break;
            case MOVE_PET:
                MovementOperation.MovePetHandler(slea, c);
                break;
            case GENERAL_CHAT:
                InterPersonalOperation.GeneralChatHandler(slea, c);
                break;
            case WHISPER:
                InterPersonalOperation.WhisperHandler(slea, c);
                break;
            case CHAR_INFO_REQUEST:
                InterPersonalOperation.CharInfoRequestHandler(slea, c);
                break;
            case FACE_EXPRESSION:
                InterPersonalOperation.FaceExpressionHandler(slea, c);
                break;
            case PLAYER_INTERACTION:
                InterPersonalOperation.PlayerInteractionHandler(slea, c);
                break;
            case GIVE_FAME:
                InterPersonalOperation.GiveFameHandler(slea, c);
                break;
            case PARTY_OPERATION:
                InterPersonalOperation.PartyOperationHandler(slea, c);
                break;
            case DENY_PARTY_REQUEST:
                InterPersonalOperation.DenyPartyRequestHandler(slea, c);
                break;
            case PARTYCHAT:
                InterPersonalOperation.PartyChatHandler(slea, c);
                break;
            case USE_DOOR:
                InterPersonalOperation.UseDoorHandler(slea, c);
                break;
            case BUDDYLIST_MODIFY:
                InterPersonalOperation.BuddyListModifyHandler(slea, c);
                break;
            case GUILD_OPERATION:
                InterPersonalOperation.GuildOperationHandler(slea, c);
                break;
            case DENY_GUILD_REQUEST:
                InterPersonalOperation.DenyGuildRequestHandler(slea, c);
                break;
            case BBS_OPERATION:
                InterPersonalOperation.BBSOperationHandler(slea, c);
                break;
            case MESSENGER:
                InterPersonalOperation.MessengerHandler(slea, c);
                break;
            case REPORT:
                InterPersonalOperation.ReportHandler(slea, c);
                break;
            case ADD_FAMILY:
                InterPersonalOperation.FamilyAddHandler(slea, c);
                break;
            case USE_FAMILY:
                InterPersonalOperation.FamilyOperationHandler(slea, c);
                break;
            case ACCEPT_FAMILY:
                InterPersonalOperation.FamilyAcceptHandler(slea, c);
                break;
            case ALLIANCE_OPERATION:
                InterPersonalOperation.AllianceOperationHandler(slea, c);
                break;
            case PARTY_SEARCH_START:
                InterPersonalOperation.PartySearchStartHandler(slea, c);
                break;
            case ITEM_SORT:
                InventoryOperation.ItemSortHandler(slea, c);
                break;
            case ITEM_MOVE:
                InventoryOperation.ItemMoveHandler(slea, c);
                break;
            case MESO_DROP:
                InventoryOperation.MesoDropHandler(slea, c);
                break;
            case USE_CASH_ITEM:
                InventoryOperation.UseCashItemHandler(slea, c);
                break;
            case USE_ITEM:
                InventoryOperation.UseItemHandler(slea, c);
                break;
            case USE_RETURN_SCROLL:
                InventoryOperation.UseItemHandler(slea, c);
                break;
            case USE_UPGRADE_SCROLL:
                InventoryOperation.UseScrollHandler(slea, c);
                break;
            case USE_SUMMON_BAG:
                InventoryOperation.UseSummonBagHandler(slea, c);
                break;
            case ITEM_PICKUP:
                InventoryOperation.ItemPickUpHandler(slea, c);
                break;
            case STORAGE:
                InventoryOperation.StorageHandler(slea, c);
                break;
            case USE_ITEMEFFECT:
                InventoryOperation.UseItemEffectHandler(slea, c);
                break;
            case USE_SKILL_BOOK:
                InventoryOperation.UseSkillBookHandler(slea, c);
                break;
            case USE_MOUNT_FOOD:
                InventoryOperation.UseMountFoodHandler(slea, c);
                break;
            case MAKER_SKILL:
                InventoryOperation.MakerHandler(slea, c);
                break;
            case SCRIPTED_ITEM:
                InventoryOperation.UseScriptedItemHandler(slea, c);
                break;
            case USE_SOLOMON_ITEM:
                InventoryOperation.UseSolomonItemHandler(slea, c);
                break;
            case USE_REMOTE:
                InventoryOperation.UseRemoteGachaponHandler(slea, c);
                break;
            case USE_DEATHITEM:
                InventoryOperation.UseDeathItemHandler(slea, c);
                break;
            case USE_CATCH_ITEM:
                InventoryOperation.UseCatchItemHandler(slea, c);
                break;
            case ITEM_SORT2:
                InventoryOperation.ItemSortByIdHandler(slea, c);
                break;
            case OPEN_TREASURE:
                InventoryOperation.UseTreasureHandler(slea, c);
                break;
            case USE_CHAIR:
                InventoryOperation.UseChairHandler(slea, c);
                break;
            case CANCEL_CHAIR:
                InventoryOperation.CancelChairHandler(slea, c);
                break;
            case HEAL_OVER_TIME:
                IntraPersonalOperation.HealOverTimeHandler(slea, c);
                break;
            case SPECIAL_MOVE:
                IntraPersonalOperation.SpecialMoveHandler(slea, c);
                break;
            case CANCEL_BUFF:
                IntraPersonalOperation.CancelBuffHandler(slea, c);
                break;
            case DISTRIBUTE_AP:
                IntraPersonalOperation.DistributeAPHandler(slea, c);
                break;
            case DISTRIBUTE_SP:
                IntraPersonalOperation.DistributeSPHandler(slea, c);
                break;
            case CHANGE_KEYMAP:
                IntraPersonalOperation.ChangeKeyMapHandler(slea, c);
                break;
            case DAMAGE_SUMMON:
                IntraPersonalOperation.DamageSummonHandler(slea, c);
                break;
            case SKILL_EFFECT:
                IntraPersonalOperation.SkillEffectHandler(slea, c);
                break;
            case SKILL_MACRO:
                IntraPersonalOperation.SkillMacroHandler(slea, c);
                break;
            case NOTE_DELETE:
                try {
                    PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM `notes` WHERE `to` = ?");
                    ps.setString(1, c.getPlayer().getName());
                    ps.executeUpdate();
                    ps.close();
                } catch (Exception e) {
                }
                break;
            case NOTE_ACTION:
                IntraPersonalOperation.NoteActionHandler(slea, c);
                break;
            case TROCK_ADD_MAP:
                IntraPersonalOperation.TeleportRockRecordHandler(slea, c);
                break;
            case MONSTER_BOOK_COVER:
                IntraPersonalOperation.MonsterBookCoverHandler(slea.readInt(), c);
                break;
            case AUTO_DISTRIBUTE_AP:
                IntraPersonalOperation.AutoDistributeAPHandler(slea, c);
                break;
            case BEHOLDER:
                IntraPersonalOperation.BeholderHandler(slea, c);
                break;
            case ADMIN_COMMAND:
                IntraPersonalOperation.AdminCommandHandler(slea, c);
                break;
            case QUICK_SLOT_CHANGE:
                IntraPersonalOperation.QuickSlotChange(slea, c);
                break;
            case USE_INNER_PORTAL:
                IntraPersonalOperation.UseInnerPortalHandler(slea, c);
                break;
            case CHANGE_MAP_SPECIAL:
                IntraPersonalOperation.ChangeMapSpecialHandler(slea, c);
                break;
            case CLOSE_RANGE_ATTACK:
                MobOperation.CloseRangeDamageHandler(slea, c);
                break;
            case RANGED_ATTACK:
                MobOperation.RangedAttackHandler(slea, c);
                break;
            case MAGIC_ATTACK:
                MobOperation.MagicDamageHandler(slea, c);
                break;
            case TAKE_DAMAGE:
                MobOperation.TakeDamageHandler(slea, c);
                break;
            case SUMMON_ATTACK:
                MobOperation.SummonDamageHandler(slea, c);
                break;
            case AUTO_AGGRO:
                MobOperation.AutoAggroHandler(slea.readInt(), c);
                break;
            case ENERGY_ORB_ATTACK:
                MobOperation.EnergyOrbDamageHandler(slea, c);
                break;
            case MOB_DAMAGE_MOB:
                MobOperation.MobDamageMobHandler(slea, c);
                break;
            case MOB_DAMAGE_MOB_FRIENDLY:
                MobOperation.MobDamageMobFriendlyHandler(slea, c);
                break;
            case POISON_BOMB:
                MobOperation.UsePoisonBombHandler(slea, c);
                break;
            case ARAN_COMBO:
                MobOperation.ComboCounterHandler(slea, c);
                break;
            case SPAWN_PET:
                PetOperation.PetSpawnHandler(slea, c);
                break;
            case PET_CHAT:
                PetOperation.PetChatHandler(slea, c);
                break;
            case PET_COMMAND:
                PetOperation.PetCommandHandler(slea, c);
                break;
            case PET_FOOD:
                PetOperation.PetFoodHandler(slea, c);
                break;
            case PET_LOOT:
                PetOperation.PetLootHandler(slea, c);
                break;
            case PET_AUTO_POT:
                PetOperation.PetAutoPotHandler(slea, c);
                break;
            case PET_EXCLUDE_ITEMS:
                PetOperation.PetExcludeItemHandler(slea, c);
                break;
            case BUY_CS_ITEM:
                CashShopOperation.BuyHandler(slea, c);
                break;
            case COUPON_CODE:
                CashShopOperation.CouponCodeHandler(slea, c);
                break;
            case HIRED_MERCHANT_REQUEST:
                MarketOperation.MerchantRequestHandler(slea, c);
                break;
            case REMOTE_SHOP:
                MarketOperation.MerchantRemoteAccessHandler(slea, c);
                break;
            case MINERVA_GO:
                MarketOperation.MerchantGoHandler(slea, c);
                break;
            case NPC_TALK:
                NPCOperation.NPCTalkHandler(slea, c);
                break;
            case NPC_TALK_MORE:
                NPCOperation.NPCTalkMoreHandler(slea, c);
                break;
            case QUEST_ACTION:
                NPCOperation.QuestActionHandler(slea, c);
                break;
            case NPC_SHOP:
                NPCOperation.NPCShopHandler(slea, c);
                break;
            case NPC_ACTION:
                NPCOperation.NPCActionHandler(slea, c);
                break;
            case DAMAGE_REACTOR:
                ReactorOperation.ReactorDamageHandler(slea, c);
                break;
            case TOUCHING_REACTOR:
                ReactorOperation.ReactorTouchHandler(slea, c);
                break;
            case CANCEL_ITEM_EFFECT:
                c.getPlayer().cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(-slea.readInt()), false, -1);
                break;
            case CANCEL_DEBUFF:
                c.getPlayer().dispelDebuffs();
                break;
            case CLOSE_CHALKBOARD:
                c.getPlayer().setChalkboard(null);
                c.getPlayer().getMap().broadcastMessage(EffectFactory.useChalkboard(c.getPlayer(), true));
                break;
            case USE_HAMMER:
                c.announce(EffectFactory.sendHammerMessage());
                break;
            case ADMIN_LOG:
                c.getPlayer().addCommandToList(slea.readMapleAsciiString());
                break;
            case USE_MAPLELIFE:
                c.announce(LoginFactory.charNameResponse(slea.readMapleAsciiString(), false));
                break;
            case MINERVA:
                c.announce(MarketFactory.sendMinervaList());
                break;
            case ERROR:
            case ENTER_MTS:
            case MTS_OP:
            case RING_ACTION:
            case SPOUSE_CHAT:
            case PLAYER_UPDATE:
            case UPDATE_CHAR_2:
            case DUEY_ACTION:
                c.announce(IntraPersonalFactory.enableActions());
                break;
        }
    }

    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
        final MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (client != null && (client.getLoginState() == MapleClient.LOGIN_LOGGEDIN)) {
            client.sendPing();
        }
        super.sessionIdle(session, status);
    }
}

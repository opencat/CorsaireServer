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

package net.channel.handler;

import client.BuddylistEntry;
import client.CharacterNameAndId;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.SkillFactory;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.sql.SQLException;
import net.cashshop.CashShopServer;
import net.channel.ChannelServer;
import net.world.CharacterIdChannelPair;
import net.world.CharacterTransfer;
import net.world.MapleMessengerCharacter;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.guild.MapleAlliance;
import net.world.guild.MapleGuild;
import net.world.remote.WorldChannelInterface;
import server.MapleTrade;
import server.maps.FieldLimit;
import server.quest.MapleQuest;
import server.shops.HiredMerchant;
import tools.factory.BuddyFactory;
import tools.factory.CashShopFactory;
import tools.factory.EffectFactory;
import tools.factory.FamilyFactory;
import tools.factory.GuildFactory;
import tools.factory.InterServerFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.InventoryFactory;
import tools.factory.QuestFactory;

/**
 * @name        InterServerOperation
 * @author      x711Li
 */
public class InterServerOperation {
    public static final void ThrowSessionHandler(int channel, MapleClient c) {
        if (!c.checkIPAddress()) {
            c.getSession().close();
            return;
        }
        MapleCharacter player = c.getPlayer();
        if (!player.isAlive() || (channel > -1 && c.getChannel() == channel) || FieldLimit.CHANGECHANNEL.check(player.getMap().getFieldLimit())) {
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        final ChannelServer ch = c.getChannelServer();
        String CashShopIP = null;
        try {
            CashShopIP = ch.getWorldInterface().getCashShopIP();
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
        if (channel < 0 && CashShopIP == null) { // Cash Shop not init yet
            return;
        }
        if (player.getNoPets() > 0) {
            player.unequipAllPets();
        }
        if (player.getTrade() != null) {
            MapleTrade.cancelTrade(player);
        }
        player.cancelMagicDoor();
        if (player.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
            player.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
        }
        if (player.getBuffedValue(MapleBuffStat.PUPPET) != null) {
            player.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
        }
        if (player.getBuffedValue(MapleBuffStat.COMBO) != null) {
            player.cancelEffectFromBuffStat(MapleBuffStat.COMBO);
        }
        HiredMerchant merchant = player.getHiredMerchant();
        if (merchant != null) {
            if (merchant.isOwner(player)) {
                merchant.setOpen(true);
            } else {
                merchant.removeVisitor(player);
            }
        }
        final WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
        try {
            wci.addBuffsToStorage(player.getId(), player.getAllBuffs());
            wci.addCooldownsToStorage(player.getId(), player.getAllCooldowns());
            wci.addDiseaseToStorage(player.getId(), player.getAllDiseases());
            wci.channelChange(new CharacterTransfer(player), player.getId(), channel);
        } catch (RemoteException e) {
            e.printStackTrace();
            ch.reconnectWorld();
        }
        player.saveToDB(true);
        if (player.getMessenger() != null) {
            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
            try {
                wci.silentLeaveMessenger(player.getMessenger().getId(), messengerplayer);
            } catch (RemoteException e) {
                e.printStackTrace();
                ch.reconnectWorld();
            }
        }
        player.getMap().removePlayer(player);
        ch.removePlayer(player);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        String[] socket = channel > -1 ? ch.getIP(channel).split(":") : CashShopIP.split(":");
        try {
            c.announce(InterServerFactory.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
        } catch (Exception e) {
        }
        c.setPlayer(null);
    }

    public static final void CatchSessionHandler(int id, MapleClient c, boolean cashShop) {
        MapleCharacter player = null;
        final ChannelServer cserv = c.getChannelServer();
        final CharacterTransfer transfer = cashShop ? CashShopServer.getInstance().getShopperStorage().getPendingCharacter(id) : cserv.getPlayerStorage().getPendingCharacter(id);
        if (transfer == null) {
            if (cashShop) {
                c.getSession().close();
                return;
            }
            try {
                player = MapleCharacter.loadCharFromDB(id, c, true);
            } catch (SQLException e) {
                return;
            }
        } else {
            player = MapleCharacter.loadCharFromTransfer(transfer, c, !cashShop);
        }
        c.setPlayer(player);
        c.setAccID(player.getAccountID());
        if (!c.checkIPAddress()) {
            c.getSession().close();
            return;
        }
        final int state = c.getLoginState();
        boolean allowLogin = false;
        try {
            if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
                if (cashShop ? !CashShopServer.getInstance().getCashShopInterface().isCharacterListConnected(c.loadCharacterNames(c.getWorld())) : !cserv.getWorldInterface().isCharacterListConnected(c.loadCharacterNames(c.getWorld()))) {
                    allowLogin = true;
                }
            }
        } catch (RemoteException e) {
            if (cashShop) {
                CashShopServer.getInstance().reconnectWorld();
            } else {
                cserv.reconnectWorld();
            }
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close();
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        if (cashShop) {
            CashShopServer.getInstance().getShopperStorage().registerPlayer(player);
            c.announce(CashShopFactory.warpCS(c, false));
            c.announce(CashShopFactory.enableCSUse0());
            c.announce(CashShopFactory.enableCSUse1());
            c.announce(CashShopFactory.enableCSUse2());
            c.announce(CashShopFactory.enableCSUse3());
            c.announce(CashShopFactory.showNXMapleTokens(c.getPlayer()));
            c.announce(CashShopFactory.showCashInventoryDummy(c));
            c.announce(CashShopFactory.sendWishList(c.getPlayer(), false));
            c.setInsideCashShop(true);
        } else {
            cserv.addPlayer(player);
            try {
                final WorldChannelInterface wci = ChannelServer.getInstance(c.getChannel()).getWorldInterface();
                player.silentGiveBuffs(wci.getBuffsFromStorage(player.getId()));
                player.silentGiveDebuffs(wci.getDiseaseFromStorage(player.getId()));
                player.giveCoolDowns(wci.getCooldownsFromStorage(player.getId()));
                c.announce(IntraPersonalFactory.getCharInfo(player));
                player.sendKeymap();
                player.sendMacros();
                c.announce(IntraPersonalFactory.updateMount(player.getId(), player.getMount(), false));
                player.getMap().addPlayer(player);
                player.setRates(false);
                final int buddyIds[] = player.getBuddylist().getBuddyIds();
                cserv.getWorldInterface().loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
                for (CharacterIdChannelPair onlineBuddy : cserv.getWorldInterface().multiBuddyFind(player.getId(), buddyIds)) {
                    final BuddylistEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
                    ble.setChannel(onlineBuddy.getChannel());
                    player.getBuddylist().put(ble);
                }
                c.announce(FamilyFactory.loadFamily(player));
                if (player.getFamilyId() > 0) {
                    c.announce(FamilyFactory.getFamilyInfo(player));
                }
                if (player.getGuildId() > 0) {
                    try {
                        MapleGuild playerGuild = cserv.getWorldInterface().getGuild(player.getGuildId(), player.getMGC());
                        if (playerGuild == null) {
                            player.deleteGuild(player.getGuildId());
                            player.resetMGC();
                            player.setGuildId(0);
                        } else {
                            cserv.getWorldInterface().setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                            c.announce(GuildFactory.showGuildInfo(player));
                            int allianceId = player.getGuild().getAllianceId();
                            if (allianceId > 0) {
                                MapleAlliance newAlliance = cserv.getWorldInterface().getAlliance(allianceId);
                                if (newAlliance == null) {
                                    newAlliance = MapleAlliance.loadAlliance(allianceId);
                                    if (newAlliance != null) {
                                        cserv.getWorldInterface().addAlliance(allianceId, newAlliance);
                                    } else {
                                        player.getGuild().setAllianceId(0);
                                    }
                                }
                                if (newAlliance != null) {
                                    c.announce(GuildFactory.getAllianceInfo(newAlliance));
                                    c.announce(GuildFactory.getGuildAlliances(newAlliance, c));
                                    cserv.getWorldInterface().allianceMessage(allianceId, GuildFactory.allianceMemberOnline(player, true), player.getId(), -1);
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        cserv.reconnectWorld();
                    }
                }
                player.showNote();
                if (player.getParty() != null) {
                    cserv.getWorldInterface().updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
                    player.updatePartyMemberHP();
                }
            } catch (RemoteException e) {
                cserv.reconnectWorld();
            }
            for (MapleQuestStatus status : player.getStartedQuests()) {
                if (status.hasMobKills()) {
                    c.announce(QuestFactory.updateQuestMobKills(status));
                }
            }
            for (int i = 0; i < player.getQuestInfo().size(); i++) {
                MapleQuest quest = MapleQuest.getInstance(player.getQuestInfo().get(i).getLeft().shortValue());
                if (quest != null && player.getQuest(quest).getStatus() != 2) {
                    c.announce(QuestFactory.updateQuestInfo((byte) 1, player.getQuestInfo().get(i).getLeft().shortValue(), player.getQuestInfo().get(i).getRight()));
                }
            }
            CharacterNameAndId pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
            if (pendingBuddyRequest != null) {
                player.getBuddylist().put(new BuddylistEntry(pendingBuddyRequest.getName(), "Default Group", pendingBuddyRequest.getId(), -1, false));
                c.announce(BuddyFactory.requestBuddylistAdd(pendingBuddyRequest.getId(), c.getPlayer().getId(), pendingBuddyRequest.getName(), 0, 0));
            }
            c.announce(BuddyFactory.updateBuddylist(player.getBuddylist().getBuddies()));
            player.changeSkillLevel(SkillFactory.getSkill(10000000 * player.getJobType() + 12), player.getLinkedLevel() / 10, 20);
            player.changeSkillLevel(SkillFactory.getSkill(10000000 * player.getJobType() + 1009), 1, 1);
            player.checkBerserk();
            if (player.getMap().getId() >= 140090100 && player.getMap().getId() <= 140090400 || player.getMap().getId() >= 130030001 && player.getMap().getId() <= 130030004) {
                c.announce(EffectFactory.spawnGuide(true));
            } else if (player.getDonorPts() > 0) {
                player.message("You currently have " + player.getDonorPts() + " Donor Points to use! Enjoy.");
            }
        }
    }
}
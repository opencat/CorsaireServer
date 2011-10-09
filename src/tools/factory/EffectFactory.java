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

package tools.factory;

import client.IItem;
import client.MapleCharacter;
import java.awt.Point;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import net.MaplePacket;
import net.SendPacketOpcode;
import tools.DataTool;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        EffectFactory
 * @author      x711Li
 */
public class EffectFactory {
    public static MaplePacket enableTV() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.ENABLE_TV.getValue());
        mplew.writeInt(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket removeTV() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
        mplew.writeShort(SendPacketOpcode.REMOVE_TV.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket sendTV(MapleCharacter chr, List<String> messages, int type, MapleCharacter partner) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SEND_TV.getValue());
        mplew.write(partner != null ? 3 : 1);
        mplew.write(type); //Heart = 2  Star = 1  Normal = 0
        IntraPersonalFactory.addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        if (partner != null) {
            mplew.writeMapleAsciiString(partner.getName());
        } else {
            mplew.writeShort(0);
        }
        for (int i = 0; i < messages.size(); i++) {
            if (i == 4 && messages.get(4).length() > 15) {
                mplew.writeMapleAsciiString(messages.get(4).substring(0, 15));
            } else {
                mplew.writeMapleAsciiString(messages.get(i));
            }
        }
        mplew.writeInt(1337); // 'Your thing still start in blah blah seconds'
        if (partner != null) {
            IntraPersonalFactory.addCharLook(mplew, partner, false);
        }
        return mplew.getPacket();
    }

    public static MaplePacket skillBookSuccess(MapleCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.USE_SKILL_BOOK.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.writeInt(maxlevel);
        mplew.write(canuse ? 1 : 0);
        mplew.write(success ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket doMapleLife() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAPLE_LIFE_SUBMIT.getValue());
        mplew.write(54);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket checkMapleLifeName(String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAPLE_LIFE.getValue());
        mplew.writeMapleAsciiString(name);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket sendVegaScroll(int op) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.VEGA_SCROLL.getValue());
        mplew.write(op);
        return mplew.getPacket();
    }

    public static MaplePacket sendWeddingInvite(String partnerOne, String partnerTwo, short unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WEDDING_PROPOSAL.getValue());
        mplew.write(15);
        mplew.writeMapleAsciiString(partnerOne);
        mplew.writeMapleAsciiString(partnerTwo);
        mplew.writeShort(unk);
        return mplew.getPacket();
    }

    public static MaplePacket sendZakumMessage(int mode, int minutes) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.ZAKUM_SHRINE.getValue());
        mplew.write(mode);
        mplew.writeInt(minutes);
        return mplew.getPacket();
    }

    public static MaplePacket sendHorntailMessage(int mode, int minutes) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.HT_SPAWN.getValue());
        mplew.write(mode);
        mplew.writeInt(minutes);
        return mplew.getPacket();
    }

    public static MaplePacket sendOpenTreasure(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SILVER_BOX.getValue());
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static MaplePacket removeTiger() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
        mplew.writeShort(SendPacketOpcode.REMOVE_TIGER.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket showHPQMoon() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HPQ_MOON.getValue());
        mplew.writeInt(-1);
        return mplew.getPacket();
    }

    public static MaplePacket triggerMoon(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REACTOR_HIT.getValue());
        mplew.writeInt(oid);
        mplew.write(6);//state
        mplew.writeShort(-183);
        mplew.writeShort(-433);
        mplew.writeShort(0);
        mplew.write(-1);
        mplew.write(78);
        return mplew.getPacket();
    }

    public static MaplePacket getRelayPacket(byte[] data) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(data);
        return mplew.getPacket();
    }

    public static MaplePacket displayCombo(int combo) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ARAN_COMBO_COUNTER.getValue());
        mplew.writeInt(combo);
        return mplew.getPacket();
    }

    public static MaplePacket showWZEffect(String path, int info) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT);
        if (info == -1) {
            mplew.write(0x12);
        } else {
            mplew.write(0x17);
        }
        mplew.writeMapleAsciiString(path);
        if (info > -1) {
            mplew.writeInt(info);
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateIntroState(String mode, int quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(10); //mode
        mplew.writeShort(quest);
        mplew.writeMapleAsciiString(mode);
        return mplew.getPacket();
    }

    public static MaplePacket playWZSound(String path) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(4); //mode
        mplew.writeMapleAsciiString(path);
        return mplew.getPacket();
    }

    public static MaplePacket sendDojoAnimation(byte firstByte, String animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(firstByte);
        mplew.writeMapleAsciiString(animation);
        return mplew.getPacket();
    }

    public static MaplePacket getDojoInfo(String info) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(10);
        mplew.write(HexTool.getByteArrayFromHexString("B7 04"));
        mplew.writeMapleAsciiString(info);
        return mplew.getPacket();
    }

    public static MaplePacket getDojoInfoMessage(String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(9);
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }

    public static MaplePacket blockedMessage(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_BLOCKED.getValue());
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket trembleEffect(int type, int delay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(1);
        mplew.write(type);
        mplew.writeInt(delay);
        return mplew.getPacket();
    }

    public static MaplePacket getEnergy(int level) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ENERGY.getValue());
        mplew.writeMapleAsciiString("energy");
        mplew.writeMapleAsciiString(Integer.toString(level));
        return mplew.getPacket();
    }

    public static MaplePacket dojoWarpUp() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DOJO_WARP_UP.getValue());
        mplew.write(0);
        mplew.write(6);
        return mplew.getPacket();
    }

    public static MaplePacket spawnGuide(boolean spawn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TUTORIAL_SUMMON.getValue());
        if (spawn) {
            mplew.write(1);
        } else {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket talkGuide(String talk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TUTORIAL_GUIDE.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(talk);
        mplew.write(HexTool.getByteArrayFromHexString("C8 00 00 00 A0 0F 00 00"));
        return mplew.getPacket();
    }

    public static MaplePacket talkGuide(int talk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendPacketOpcode.TUTORIAL_GUIDE.getValue());
        mplew.write(1);
        mplew.writeInt(talk);
        mplew.writeInt(10000);
        return mplew.getPacket();
    }

    public static MaplePacket sendHammerData(int hammerUsed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(0x39);
        mplew.writeInt(0);
        mplew.writeInt(hammerUsed);
        return mplew.getPacket();
    }

    public static MaplePacket sendHammerMessage() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(0x3D);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket hammerItem(IItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0); // could be from drop
        mplew.write(2); // always 2
        mplew.write(3); // quantity > 0 (?)
        mplew.write(1); // Inventory type
        mplew.write(item.getPosition()); // item slot
        mplew.writeShort(0);
        mplew.write(1);
        mplew.write(item.getPosition()); // wtf repeat
        InventoryFactory.addItemInfo(mplew, item, true, false);
        return mplew.getPacket();
    }

    public static MaplePacket showSpecialEffect(byte effect) {
        return showSpecialEffect(effect, null);
    }

    /**
     * 6 = Exp did not drop (Safety Charms)
     * 7 = Enter portal sound
     * 8 = Job change
     * 9 = Quest complete
     * 10 = damage O.O
     * 14 = Monster book pickup
     * 15 = Equipment levelup
     * 16 = Maker Skill Success
     * 19 = Exp card [500, 200, 50]
     * @param effect
     * @return
     */
    public static MaplePacket showSpecialEffect(byte effect, String path) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effect);
        if (path != null) {
            mplew.writeMapleAsciiString(path);
        }
        return mplew.getPacket();
    }

    public static MaplePacket spawnPortal(int townId, int targetId, Point pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(14);
        mplew.writeShort(SendPacketOpcode.SPAWN_PORTAL.getValue());
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        if (pos != null) {
            mplew.writeShort(pos.x);
            mplew.writeShort(pos.y);
        }
        mplew.writeLong(0);
        return mplew.getPacket();
    }

    public static MaplePacket spawnDoor(int oid, Point pos, boolean town) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendPacketOpcode.SPAWN_DOOR.getValue());
        mplew.write(town ? 1 : 0);
        mplew.writeInt(oid);
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        return mplew.getPacket();
    }

    public static MaplePacket removeDoor(int oid, boolean town) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(10);
        if (town) {
            mplew.writeShort(SendPacketOpcode.SPAWN_PORTAL.getValue());
            mplew.writeInt(999999999);
            mplew.writeInt(999999999);
        } else {
            mplew.writeShort(SendPacketOpcode.REMOVE_DOOR.getValue());
            mplew.write(0);
            mplew.writeInt(oid);
        }
        return mplew.getPacket();
    }

    public static MaplePacket partyPortal(int townId, int targetId, Point position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION);
        mplew.writeShort(0x23);
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        mplew.writeShort(position.x);
        mplew.writeShort(position.y);
        return mplew.getPacket();
    }

    public static final MaplePacket mapNameDisplay(final int mapid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(3);
        mplew.writeMapleAsciiString("maplemap/enter/" + mapid);
        return mplew.getPacket();
    }

    public static final MaplePacket effectDisplay(final String path) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(3);
        mplew.writeMapleAsciiString(path);

        return mplew.getPacket();
    }

    public static final MaplePacket aranStart() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(4);
        mplew.writeMapleAsciiString("Aran/balloon");

        return mplew.getPacket();
    }

    public static MaplePacket addTutorialStats() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_TEMP_STATS.getValue());
        mplew.writeInt(3871);
        mplew.writeShort(999);
        mplew.writeShort(999);
        mplew.writeShort(999);
        mplew.writeShort(999);
        mplew.writeShort(255);
        mplew.writeShort(999);
        mplew.writeShort(999);
        mplew.write(140);
        mplew.write(123);
        return mplew.getPacket();
    }

    public static MaplePacket removeTutorialStats() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
        mplew.writeShort(SendPacketOpcode.REMOVE_TEMP_STATS.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket getClock(int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(2); // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    public static MaplePacket getClockTime(int hour, int min, int sec) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(1); //Clock-Type
        mplew.write(hour);
        mplew.write(min);
        mplew.write(sec);
        return mplew.getPacket();
    }

    public static MaplePacket itemEffect(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_EFFECT.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static MaplePacket musicChange(String song) {
        return environmentChange(song, 6);
    }

    public static MaplePacket showEffect(String effect) {
        return environmentChange(effect, 3);
    }

    public static MaplePacket showForeignEffect(int cid, int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(effect);
        return mplew.getPacket();
    }

    public static MaplePacket playSound(String sound) {
        return environmentChange(sound, 4);
    }

    public static MaplePacket environmentChange(String env, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(env);
        return mplew.getPacket();
    }

    public static MaplePacket showMapEffect(String path) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.write(3);
        mplew.writeMapleAsciiString(path);
        return mplew.getPacket();
    }

    public static MaplePacket startMapEffect(String msg, int itemid, boolean active) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.write(active ? 0 : 1);
        mplew.writeInt(itemid);
        if (active) {
            mplew.writeMapleAsciiString(msg);
        }
        return mplew.getPacket();
    }

    public static MaplePacket removeMapEffect() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.write(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket mapEffect(String path) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.write(3);
        mplew.writeMapleAsciiString(path);
        return mplew.getPacket();
    }

    public static MaplePacket sendMapMessage(int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.MAP_MESSAGE.getValue());
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static MaplePacket sendHint(String hint, int width, int height) {
        if (width < 1) {
            width = hint.length() * 10;
            if (width < 40) {
                width = 40;
            }
        }
        if (height < 5) {
            height = 5;
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_HINT.getValue());
        mplew.writeMapleAsciiString(hint);
        mplew.writeShort(width);
        mplew.writeShort(height);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket serverMessage(String message) {
        return serverMessage(null, null, 4, 0, message, null, false);
    }

    public static MaplePacket serverNotice(int type, String message) {
        return serverMessage(null, null, type, 0, message, null, false);
    }

    public static MaplePacket serverNotice(int type, int channel, String message) {
        return serverMessage(null, null, type, channel, message, null, false);
    }

    public static MaplePacket serverNotice(int type, int channel, String message, boolean smegaEar) {
        return serverMessage(null, null, type, channel, message, null, smegaEar);
    }

    public static MaplePacket itemMegaphone(String msg, boolean whisper, int channel, IItem item) {
        return serverMessage(null, item, 8, channel, msg, null, whisper);
    }

    public static MaplePacket allianceInvitationDenied(String name) {
        return serverMessage(null, null, 9, 0, name, null, false);
    }

    public static MaplePacket getMultiMegaphone(String[] messages, int channel, boolean showEar) {
        return serverMessage(null, null, 10, channel, "", messages, showEar);
    }

    public static MaplePacket gachaponMessage(IItem item, String town, MapleCharacter player) {
        return serverMessage(player, item, 12, 0, town, null, false);
    }

    public static MaplePacket getCashShopNews(IItem item) {
        return serverMessage(null, item, 18, 0, "", null, false);
    }

    private static MaplePacket serverMessage(MapleCharacter player, IItem item, int type, int channel, String message, String[] messages, boolean megaEar) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(type);
        switch (type) {
        case 0:
        case 1:
        case 2:
        case 5:
            mplew.writeMapleAsciiString(message);
            break;
        case 3:
            mplew.writeMapleAsciiString(message);
            mplew.write(channel - 1);
            mplew.write(megaEar ? 1 : 0);
            break;
        case 4:
            mplew.write(1);
            mplew.writeMapleAsciiString(message);
            break;
        case 6:
            mplew.writeMapleAsciiString(message);
            mplew.writeInt(0);
            break;
        case 8:
            mplew.writeMapleAsciiString(message);
            mplew.write(channel - 1);
            mplew.write(megaEar ? 1 : 0);
            if (item == null) {
            mplew.write(0);
            } else {
                InventoryFactory.addItemInfo(mplew, item, false, false, true);
            }
            break;
        case 9:
            mplew.writeMapleAsciiString(message + " Guild has rejected the Guild Union invitation.");
            break;
        case 10:
            if (messages[0] != null) {
                mplew.writeMapleAsciiString(messages[0]);
            }
            mplew.write(messages.length);
            for (int i = 1; i < messages.length; i++) {
                if (messages[i] != null) {
                    mplew.writeMapleAsciiString(messages[i]);
                }
            }
            for (int i = 0; i < 10; i++) {
                mplew.write(channel - 1);
            }
            mplew.write(megaEar ? 1 : 0);
            mplew.write(1);
            break;
        case 12:
            mplew.writeMapleAsciiString(player.getName() + " : got a(n)");
            mplew.writeInt(0);
            mplew.writeMapleAsciiString(message);
            InventoryFactory.addItemInfo(mplew, item, true, true, false);
            break;
        case 7:
        case 11:
        case 13:
        case 14:
        case 15:
        case 16:
        case 17: } return mplew.getPacket();
    }

    public static MaplePacket getAvatarMega(MapleCharacter chr, String medal, int channel, int itemId, List<String> message, boolean ear) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.AVATAR_MEGA.getValue());
        mplew.writeInt(itemId);
        mplew.writeMapleAsciiString(medal + chr.getName());
        for (String s : message) {
            mplew.writeMapleAsciiString(s);
        }
        mplew.writeInt(channel - 1); // channel
        mplew.write(ear ? 1 : 0);
        IntraPersonalFactory.addCharLook(mplew, chr, true);
        return mplew.getPacket();
    }

    public static MaplePacket getChatText(int cidfrom, String text, boolean whiteBG, int show) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHATTEXT.getValue());
        mplew.writeInt(cidfrom);
        mplew.write(whiteBG ? 1 : 0);
        mplew.writeMapleAsciiString(text);
        mplew.write(show);
        return mplew.getPacket();
    }

    public static MaplePacket getShowExpGain(int gain, boolean inChat, boolean white, byte party) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(3); // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
        mplew.write(white ? 1 : 0);
        mplew.writeInt(gain);
        mplew.writeInt(inChat ? 1 : 0); //dc if it's 1.
        mplew.writeInt(0); // monsterbook
        mplew.write(0);
        mplew.write(0); // wedding bouns
        mplew.write0(18);
        if (inChat) {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket getShowFameGain(int gain) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(4);
        mplew.writeInt(gain);
        return mplew.getPacket();
    }

    public static MaplePacket getShowMesoGain(int gain) {
        return getShowMesoGain(gain, false);
    }

    public static MaplePacket getShowMesoGain(int gain, boolean inChat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        if (!inChat) {
            mplew.write(0);
            mplew.write(1);
            mplew.write(0);
        } else {
            mplew.write(5);
        }
        mplew.writeInt(gain);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket getShowItemGain(int itemId, short quantity) {
        return getShowItemGain(itemId, quantity, false);
    }

    public static MaplePacket getShowItemGain(int itemId, short quantity, boolean inChat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(20);
        if (inChat) {
            mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
            mplew.write(3);
            mplew.write(1);
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.writeShort(0);
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket dropMesoFromMapObject(int amount, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod, byte type) {
        return dropItemFromMapObjectInternal(amount, itemoid, dropperoid, ownerid, dropfrom, dropto, mod, true, type);
    }

    public static MaplePacket dropItemFromMapObject(int itemid, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod, byte type) {
        return dropItemFromMapObjectInternal(itemid, itemoid, dropperoid, ownerid, dropfrom, dropto, mod, false, type);
    }

    public static MaplePacket dropItemFromMapObjectInternal(int itemid, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod, boolean mesos, byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT);
        mplew.write(mod);
        mplew.writeInt(itemoid);
        mplew.write(mesos ? 1 : 0);
        mplew.writeInt(itemid);
        mplew.writeInt(ownerid);
        mplew.write(mesos ? 4 : type);
        mplew.writeShort(dropto.x);
        mplew.writeShort(dropto.y);
        if (mod != 2) {
            mplew.writeInt(ownerid);
            mplew.writeShort(dropfrom.x);
            mplew.writeShort(dropfrom.y);
        } else {
            mplew.writeInt(dropperoid);
        }
        mplew.write(0);
        if (mod != 2) {
            mplew.write(0);
            mplew.write(1);
        }
        if (!mesos) {
            mplew.write(DataTool.NON_EXPIRE);
        }
        return mplew.getPacket();
    }

    public static MaplePacket boatPacket(boolean type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOAT_EFFECT.getValue());
        mplew.writeShort(type ? 1 : 2);
        return mplew.getPacket();
    }

    public static MaplePacket catchMonster(int monsobid, int itemid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (itemid == 2270002) {
            mplew.writeShort(SendPacketOpcode.CATCH_ARIANT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.CATCH_MOUNT.getValue());
        }
        mplew.writeInt(monsobid);
        mplew.writeInt(itemid);
        mplew.write(success);
        return mplew.getPacket();
    }

    public static MaplePacket sendYellowTip(String tip) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.YELLOW_TIP.getValue());
        mplew.write(255);
        mplew.writeMapleAsciiString(tip);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket showNotes(ResultSet notes, int count) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_NOTES.getValue());
        mplew.write(3);
        mplew.write(count);
        for (int i = 0; i < count; i++) {
            mplew.writeInt(notes.getInt("id"));
            mplew.writeMapleAsciiString(notes.getString("from") + " ");
            mplew.writeMapleAsciiString(notes.getString("message"));
            mplew.writeLong(DataTool.getKoreanTimestamp(notes.getLong("timestamp")));
            mplew.write(0);
            notes.next();
        }
        return mplew.getPacket();
    }

    public static MaplePacket useChalkboard(MapleCharacter chr, boolean close) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHALKBOARD.getValue());
        mplew.writeInt(chr.getId());
        if (close) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getChalkboard());
        }
        return mplew.getPacket();
    }

    public static MaplePacket showOXQuiz(int questionSet, int questionId, boolean askQuestion) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendPacketOpcode.OX_QUIZ.getValue());
        mplew.write(askQuestion ? 1 : 0);
        mplew.write(questionSet);
        mplew.writeShort(questionId);
        return mplew.getPacket();
    }

    public static MaplePacket getStatusMsg(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(7);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static MaplePacket addCard(boolean full, int cardid, int level) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendPacketOpcode.MONSTERBOOK_ADD.getValue());
        mplew.write(full ? 0 : 1);
        mplew.writeInt(cardid);
        mplew.writeInt(level);
        return mplew.getPacket();
    }

    public static MaplePacket showGainCard() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(13);
        return mplew.getPacket();
    }

    public static MaplePacket showForeignCardEffect(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(id);
        mplew.write(13);
        return mplew.getPacket();
    }

    public static MaplePacket changeCover(int cardid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendPacketOpcode.MONSTER_BOOK_CHANGE_COVER.getValue());
        mplew.writeInt(cardid);
        return mplew.getPacket();
    }

    public static MaplePacket showIntro(String path) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT);
        mplew.write(0x12);
        mplew.writeMapleAsciiString(path);
        return mplew.getPacket();
    }

    public static MaplePacket showInfo(String path) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT);
        mplew.write(0x17);
        mplew.writeMapleAsciiString(path);
        mplew.writeInt(1);
        return mplew.getPacket();
    }

    /**
     * Sends a UI utility.
     * 0x01 - Equipment Inventory.
     * 0x02 - Stat Window.
     * 0x03 - Skill Window.
     * 0x05 - Keyboard Settings.
     * 0x06 - Quest window.
     * 0x09 - Monsterbook Window.
     * 0x0A - Char Info
     * 0x0B - Guild BBS
     * 0x12 - Monster Carnival Window
     * 0x16 - Party Search.
     * 0x17 - Item Creation Window.
     * 0x1A - My Ranking O.O
     * 0x1B - Family Window
     * 0x1C - Family Pedigree
     * 0x1D - GM Story Board /funny shet
     * 0x1E - Envelop saying you got mail from an admin. lmfao
     * 0x1F - Medal Window
     * 0x20 - Maple Event (???)
     * 0x21 - Invalid Pointer Crash
     *
     * @param ui
     * @return
     */
    public static MaplePacket openUI(byte ui) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.TUTORIAL_OPEN_UI.getValue());
        mplew.write(ui);
        return mplew.getPacket();
    }


    public static MaplePacket tutorialIntroLock(boolean enable) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.TUTORIAL_INTRO_LOCK.getValue());
        mplew.write(enable ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket tutorialIntroDisableUI(boolean enable) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.TUTORIAL_INTRO_DISABLE_UI.getValue());
        mplew.write(enable ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket showCygnusIntro(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x12);
        mplew.writeMapleAsciiString("Effect/Direction.img/cygnus/Scene" + id);
        return mplew.getPacket();
    }

    public static MaplePacket reportResponse(byte mode, int remainingReports) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REPORT_RESPONSE.getValue());
        mplew.write(mode);
        if (mode == 2) {
            mplew.write(1); //does this change?
            mplew.writeInt(remainingReports);
        }
        return mplew.getPacket();
    }

    public static MaplePacket getGMEffect(int type, byte mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendPacketOpcode.GM_PACKET.getValue());
        mplew.write(type);
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static MaplePacket sendMesoLimit() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0x36); //Players under level 15 can only trade 1m per day
        return mplew.getPacket();
    }

    public static MaplePacket sendEngagementRequest(String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WEDDING_ACTION.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(name);
        mplew.writeInt(10);
        return mplew.getPacket();
    }

    public static MaplePacket sendGroomWishlist() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WEDDING_ACTION.getValue());
        mplew.write(9);
        return mplew.getPacket();
    }

    public static MaplePacket removeItemFromMap(int oid, int animation, int cid) {
        return removeItemFromMap(oid, animation, cid, false, 0);
    }

    public static MaplePacket removeItemFromMap(int oid, int animation, int cid, boolean pet, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_ITEM_FROM_MAP);
        mplew.write(animation);
        mplew.writeInt(oid);
        if (animation >= 2) {
            mplew.writeInt(cid);
            if (pet) {
                mplew.write(slot);
            }
        }
        return mplew.getPacket();
    }

// FROM MOOPLE
    public static MaplePacket showHpHealed(int cid, int amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(0x0A);
        mplew.write(amount);
        return mplew.getPacket();
    }

    public static MaplePacket showWheelsLeft(int left) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x15);
        mplew.write(left);
        return mplew.getPacket();
    }

    public static MaplePacket questFailure(byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(type);//0x0B = No meso, 0x0D = Worn by character, 0x0E = Not having the item ?
        return mplew.getPacket();
    }

    public static MaplePacket disableMinimap() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GM_PACKET.getValue());
        mplew.writeShort(0x1C);
        return mplew.getPacket();
    }

    public static MaplePacket sendPolice(String text) {
    MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
    mplew.writeShort(SendPacketOpcode.MAPLE_ADMIN.getValue());
    mplew.writeMapleAsciiString(text);
    return mplew.getPacket();
    }
}
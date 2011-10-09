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

import client.MapleClient;
import constants.InventoryConstants;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.MaplePacket;
import net.SendPacketOpcode;
import server.MapleItemInformationProvider;
import server.life.MapleNPC;
import server.life.MapleNPCStats;
import server.shops.MapleShopItem;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        NPCFactory
 * @author      x711Li
 */
public class NPCFactory {
    private static int doubleToShortBits(double d) {
        return (int) (Double.doubleToLongBits(d) >> 48);
    }

    public static MaplePacket getNPCShop(MapleClient c, int sid, List<MapleShopItem> items) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_NPC_SHOP.getValue());
        mplew.writeInt(sid);
        mplew.writeShort(items.size());
        for (MapleShopItem item : items) {
            mplew.writeInt(item.getItemId());
            mplew.writeInt(item.getPrice());
            mplew.writeLong(0);
            mplew.writeInt(0);
            if (!InventoryConstants.isRechargable(item.getItemId())) {
                mplew.writeShort(1);
                mplew.writeShort(item.getBuyable());
            } else {
                mplew.writeShort(0);
                mplew.writeInt(0);
                mplew.writeShort(doubleToShortBits(ii.getPrice(item.getItemId())));
                mplew.writeShort(ii.getSlotMax(c, item.getItemId()));
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket confirmShopTransaction(byte code) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
        mplew.write(code);
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte speaker) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(msgType);
        mplew.write(speaker);
        mplew.writeMapleAsciiString(talk);
        mplew.write(HexTool.getByteArrayFromHexString(endBytes));
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkStyle(int npc, String talk, int styles[]) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(7);
        mplew.write(0);
        mplew.writeMapleAsciiString(talk);
        mplew.write(styles.length);
        for (int i = 0; i < styles.length; i++) {
            mplew.writeInt(styles[i]);
        }
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkNum(int npc, String talk, int def, int min, int max) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.writeShort(3);
        mplew.writeMapleAsciiString(talk);
        mplew.writeInt(def);
        mplew.writeInt(min);
        mplew.writeInt(max);
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkText(int npc, String talk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.writeShort(2);
        mplew.writeMapleAsciiString(talk);
        mplew.write0(12);
        return mplew.getPacket();
    }

    public static MaplePacket spawnNPC(MapleNPC life) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(24);
        mplew.writeShort(SendPacketOpcode.SPAWN_NPC);
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        if (life.getF() == 1) {
            mplew.write(0);
        } else {
            mplew.write(1);
        }
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket spawnPlayerNPC(MapleNPCStats npc, int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_NPC.getValue());
        mplew.write(0x01);
        mplew.writeInt(id);
        mplew.writeMapleAsciiString(npc.getName());
        mplew.write(0); // direction
        mplew.write(npc.getSkin());
        mplew.writeInt(npc.getFace());
        mplew.write(0);
        mplew.writeInt(npc.getHair());
        Map<Byte, Integer> equip = npc.getEquips();
        Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        for (byte position : equip.keySet()) {
            byte pos = (byte) (position * -1);
            if (pos > 100) {
                pos -= 100;
                myEquip.put(pos, equip.get(position));
            } else {
                if (myEquip.get(pos) == null) {
                    myEquip.put(pos, equip.get(position));
                }
            }
        }
        for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.writeShort(-1);
        Integer cWeapon = equip.get((short) -111);
        if (cWeapon != null) {
            mplew.writeInt(cWeapon);
        } else {
            mplew.writeInt(0);
        }
        for (int i = 0; i < 12; i++) {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket spawnNPCRequestController(MapleNPC life) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(23);
        mplew.writeShort(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        mplew.write(1);
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        if (life.getF() == 1) {
            mplew.write(0);
        } else {
            mplew.write(1);
        }
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket removeNPC(final int objectid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendPacketOpcode.REMOVE_NPC.getValue());
        mplew.writeInt(objectid);
        return mplew.getPacket();
    }

    public static MaplePacket npcAnimation(int oid, String info) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_SPECIAL_ANIMATION.getValue());
        mplew.writeInt(oid);
        mplew.writeMapleAsciiString(info);
        return mplew.getPacket();
    }
}
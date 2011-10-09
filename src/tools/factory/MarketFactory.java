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

import client.MapleCharacter;
import client.MapleClient;
import java.util.List;
import net.MaplePacket;
import net.PlayerInteraction;
import net.SendPacketOpcode;
import server.MapleMiniGame;
import server.shops.HiredMerchant;
import server.shops.MaplePlayerShopItem;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        MarketFactory
 * @author      x711Li
 */
public class MarketFactory {
    public static MaplePacket getHiredMerchant(MapleClient c, MapleMiniGame minigame, boolean owner, int piece) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("05 05 04 00 00 71 C0 4C 00"));
        mplew.writeMapleAsciiString("Hired Merchant");
        mplew.write(0xFF);
        mplew.write(0);
        mplew.write(0);
        mplew.writeMapleAsciiString(c.getPlayer().getName());
        mplew.write(HexTool.getByteArrayFromHexString("1F 7E 00 00 00 00 00 00 00 00 03 00 31 32 33 10 00 00 00 00 01 01 00 01 00 7B 00 00 00 02 52 8C 1E 00 00 00 80 05 BB 46 E6 17 02 01 00 00 00 00 00"));
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantBox() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SEND_TITLE_BOX.getValue());
        mplew.write(0x07);
        return mplew.getPacket();
    }

    public static MaplePacket getHiredMerchant(MapleCharacter chr, HiredMerchant hm, boolean firstTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue()); // header.
        mplew.write(HexTool.getByteArrayFromHexString("05 05 04"));
        mplew.write(hm.isOwner(chr) ? 0 : 1);
        mplew.write(0);
        mplew.writeInt(hm.getItemId());
        mplew.writeMapleAsciiString("Hired Merchant");
        for (int i = 0; i < 3; i++) {
            if (hm.getVisitors()[i] != null) {
                mplew.write(i + 1);
                IntraPersonalFactory.addCharLook(mplew, hm.getVisitors()[i], false);
                mplew.writeMapleAsciiString(hm.getVisitors()[i].getName());
            }
        }
        mplew.write(0xFF);
        mplew.writeShort(0); // number of chats there structure: for (int i = 0; i < num; i++) asciistring, byte slot
        mplew.writeMapleAsciiString(hm.getOwner());
        if (hm.isOwner(chr)) {
            mplew.writeInt(chr.getId());
            mplew.write(firstTime ? 1 : 0);
            mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00"));
        }
        mplew.writeMapleAsciiString(hm.getDescription());
        mplew.write(0x10);
        mplew.writeInt(0);
        mplew.write(hm.getItems().size());
        if (hm.getItems().size() == 0) {
            mplew.write(0);
        } else {
            for (MaplePlayerShopItem item : hm.getItems()) {
                mplew.writeShort(item.isExist() ? 1 : 0);
                mplew.writeShort(item.getItem().getQuantity());
                mplew.writeInt(item.getPrice());
                InventoryFactory.addItemInfo(mplew, item.getItem(), true, true);
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateHiredMerchant(HiredMerchant hm) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x19);
        mplew.writeInt(0);
        mplew.write(hm.getItems().size());
        for (MaplePlayerShopItem item : hm.getItems()) {
            mplew.writeShort(item.isExist() ? 1 : 0);
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeInt(item.getPrice());
            InventoryFactory.addItemInfo(mplew, item.getItem(), true, true);
        }
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantChat(String message, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.CHAT.getCode());
        mplew.write(8);
        mplew.write(slot); // slot
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantVisitorLeave(int slot, boolean owner) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.EXIT.getCode());
        if (!owner) {
            mplew.write(slot);
        }
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantOwnerLeave() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x2A);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantMaintenanceMessage() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.ROOM.getCode());
        mplew.write(0x00);
        mplew.write(0x12);
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantVisitorAdd(MapleCharacter chr, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.VISIT.getCode());
        mplew.write(slot);
        IntraPersonalFactory.addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        return mplew.getPacket();
    }

    public static MaplePacket spawnHiredMerchant(HiredMerchant hm) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_HIRED_MERCHANT.getValue());
        mplew.writeInt(hm.getOwnerId());
        mplew.writeInt(hm.getItemId());
        mplew.writeShort((short) hm.getPosition().getX());
        mplew.writeShort((short) hm.getPosition().getY());
        mplew.writeShort(0);
        mplew.writeMapleAsciiString(hm.getOwner());
        mplew.write(0x05);
        mplew.writeInt(hm.getObjectId());
        mplew.writeMapleAsciiString(hm.getDescription());
        mplew.write(hm.getItemId() % 10);
        mplew.write(HexTool.getByteArrayFromHexString("00 04"));
        return mplew.getPacket();
    }

    public static MaplePacket destroyHiredMerchant(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DESTROY_HIRED_MERCHANT.getValue());
        mplew.writeInt(id);
        return mplew.getPacket();
    }

    public static MaplePacket leaveHiredMerchant(int slot, int status2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.EXIT.getCode());
        mplew.write(slot);
        mplew.write(status2);
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantForceLeave2() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x0C);
        mplew.write(0);
        mplew.write(0x10);
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantForceLeave1() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x0B);
        mplew.write(0x01);
        mplew.write(0x0D);
        return mplew.getPacket();
    }

    public static MaplePacket sendMinerva(List<HiredMerchant> merchants, List<MaplePlayerShopItem> items, int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OWL_OF_MINERVA.getValue());
        mplew.write(6); // mode
        mplew.writeInt(0);
        mplew.writeInt(itemId);
        mplew.writeInt(items.size());
        for (int i = 0; i < items.size(); i++) {
            mplew.writeMapleAsciiString(merchants.get(i).getOwner());
            mplew.writeInt(merchants.get(i).getObjectId()); // merchant id maybe
            mplew.writeMapleAsciiString(merchants.get(i).getDescription());
            mplew.writeInt(items.get(i).getItem().getQuantity());
            mplew.writeInt(items.get(i).getBundles());
            mplew.writeInt(items.get(i).getPrice());
            mplew.writeInt(merchants.get(i).getOwnerId()); // wierd int
            mplew.writeShort(0); // Wierd byte
        }
        return mplew.getPacket();
    }

    public static MaplePacket sendMinervaList() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OWL_OF_MINERVA.getValue());
        mplew.write(7); // mode
        mplew.write(1); // number of items
        mplew.writeInt(2340000);
        return mplew.getPacket();
    }
}
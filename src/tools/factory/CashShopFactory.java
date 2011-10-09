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
import client.MapleClient;
import client.MapleInventoryType;
import java.util.List;
import net.MaplePacket;
import net.SendPacketOpcode;
import server.CashItemInfo;
import tools.DataTool;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        CashShopFactory
 * @author      x711Li
 */
public class CashShopFactory {
    public static MaplePacket warpCS(MapleClient c, boolean mts) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        MapleCharacter chr = c.getPlayer();
        mplew.writeShort(mts ? SendPacketOpcode.MTS_OPEN.getValue() : SendPacketOpcode.CS_OPEN.getValue());
        mplew.writeLong(-1);
        mplew.write(0);
        IntraPersonalFactory.addCharStats(mplew, chr);
        mplew.write0(2);
        InventoryFactory.addInventoryInfo(mplew, chr);
        mplew.write0(17);
        for (int i = 0; i < 15; i++) {
            mplew.write(DataTool.CHAR_INFO_MAGIC);
        }
        mplew.write0(13);
        if (!mts) {
            mplew.write(1);
        }
        mplew.writeMapleAsciiString(chr.getClient().getAccountName());
        if (mts) {
            mplew.write(HexTool.getByteArrayFromHexString("88 13 00 00 07 00 00 00 F4 01 00 00 18 00 00 00 A8 00 00 00 70 AA A7 C5 4E C1 CA 01"));
        } else {
            mplew.write0(127);
            for (int i = 1; i <= 8; i++) {
                for (int j = 0; j < 2; j++) {
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50200004);
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50200069);
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50200117);
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50100008);
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50000047);
                }
            }
            mplew.write0(7);
            mplew.writeInt(0x78);
        }
        return mplew.getPacket();
    }

    public static MaplePacket sendWishList(MapleCharacter mc, boolean update) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(43);
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        if (update) {
            mplew.write(0x55);
        } else {
            mplew.write(0x4F);
        }
        byte i = 10;
        for (int sn : mc.getWishList()) {
            mplew.writeInt(sn);
            i--;
        }
        for (byte j = 0; j < i; j++) {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket showNXMapleTokens(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_UPDATE.getValue());
        for (int i = 1; i < 5; i *= 2) {
            mplew.writeInt(chr.getCSPoints(i));
        }
        return mplew.getPacket();
    }

    public static void addCashItemInformation(MaplePacketLittleEndianWriter mplew, CashItemInfo item, int accountId) {
        mplew.writeLong(1337);//unique id
        mplew.writeInt(accountId);
        mplew.writeInt(0);
        mplew.writeInt(item.getId());
        mplew.writeInt(item.getSn());
        mplew.writeShort(item.getCount());
        mplew.write0(13);
        mplew.write(HexTool.getByteArrayFromHexString("40 F0 81 DF 08 2E 0A CB 01"));
        mplew.write0(8);
    }

    public static MaplePacket showBoughtCSItem(MapleClient c, CashItemInfo item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x57);
        addCashItemInformation(mplew, item, c.getAccID());
        return mplew.getPacket();
    }

    public static MaplePacket showBoughtCashPackage(List<CashItemInfo> cashPackage, int accountId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x89);
        mplew.write(cashPackage.size());
        for (CashItemInfo item : cashPackage) {
            addCashItemInformation(mplew, item, accountId);
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket showCashInventoryDummy(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x4B);
        mplew.writeShort(0); //TODO : cash inv here
        for (IItem item : c.getPlayer().getInventory(MapleInventoryType.CASH).list()) {
            InventoryFactory.addItemInfo(mplew, item);
        } //YEAH?
        mplew.writeShort(c.getPlayer().getStorage().getSlots());
        mplew.writeShort(c.getCharacterSlots());

        return mplew.getPacket();
    }

    public static MaplePacket wrongCouponCode() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x5C);
        mplew.write(0x9F);
        return mplew.getPacket();
    }

    public static MaplePacket sendCashShopMessage(int error) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x5C);
        mplew.write(error);
        return mplew.getPacket();
    }

    public static MaplePacket showCouponRedeemedItem(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.writeShort(0x59); //guess, 49 in v72
        mplew.writeInt(0);
        mplew.writeInt(1);
        mplew.writeShort(1);
        mplew.writeShort(0x1A);
        mplew.writeInt(itemid);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket enableCSUse0() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.write(0x12);
        mplew.write0(6);
        return mplew.getPacket();
    }

    public static MaplePacket enableCSUse1() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(9);
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x4B); //v83
        mplew.writeShort(0);
        mplew.writeShort(4);
        mplew.writeShort(5);
        return mplew.getPacket();
    }

    public static MaplePacket enableCSUse2() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.writeShort(0x4D); //v83
        mplew.write0(2);
        return mplew.getPacket();
    }

    public static MaplePacket enableCSUse3() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(43);
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x4F); //v83
        mplew.write0(40);
        return mplew.getPacket();
    }

    public static MaplePacket serverBlocked(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPEN.getValue());
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket showBoughtCSQuestItem(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("7E 01 00 00 00 01 00 19 00"));
        mplew.writeInt(itemid);//D8 82 3D 00
        return mplew.getPacket();
    }
}
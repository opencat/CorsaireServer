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

import client.IEquip;
import client.IEquip.ScrollResult;
import client.IItem;
import client.Item;
import client.MapleCharacter;
import client.MapleInventory;
import client.MapleInventoryType;
import client.MaplePet;
import constants.InventoryConstants;
import constants.ServerConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.MaplePacket;
import net.SendPacketOpcode;
import server.CashItemInfo;
import tools.DataTool;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        InventoryFactory
 * @author      x711Li
 */
public class InventoryFactory {
    public static void addInventoryInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getMeso());
        for (byte i = 1; i <= 5; i++) {
            mplew.write(chr.getInventory(MapleInventoryType.getByType(i)).getSlotLimit());
        }
        mplew.write(HexTool.getByteArrayFromHexString("00 40 E0 FD 3B 37 4F 01"));
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        Collection<IItem> equippedC = iv.list();
        List<Item> equipped = new ArrayList<Item>(equippedC.size());
        List<Item> equippedCash = new ArrayList<Item>(equippedC.size());
        for (IItem item : equippedC) {
            if (item.getPosition() <= -100) {
                equippedCash.add((Item)item);
            } else {
                equipped.add((Item)item);
            }
        }
        Collections.sort(equipped);
        for (Item item : equipped) {
            addItemInfo(mplew, item);
        }
        mplew.writeShort(0);
        for (Item item : equippedCash) {
            addItemInfo(mplew, item);
        }
        mplew.writeShort(0);
        for (IItem item : chr.getInventory(MapleInventoryType.EQUIP).list()) {
            addItemInfo(mplew, item);
        }
        mplew.writeInt(0);
        for (IItem item : chr.getInventory(MapleInventoryType.USE).list()) {
            addItemInfo(mplew, item);
        }
        mplew.write(0);
        for (IItem item : chr.getInventory(MapleInventoryType.SETUP).list()) {
            addItemInfo(mplew, item);
        }
        mplew.write(0);
        for (IItem item : chr.getInventory(MapleInventoryType.ETC).list()) {
            addItemInfo(mplew, item);
        }
        mplew.write(0);
        for (IItem item : chr.getInventory(MapleInventoryType.CASH).list()) {
            addItemInfo(mplew, item);
        }
    }

    public static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item) {
        addItemInfo(mplew, item, false, false, false);
    }

    public static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, boolean zeroPosition, boolean leaveOut) {
        addItemInfo(mplew, item, zeroPosition, leaveOut, false);
    }

    public static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, boolean zeroPosition, boolean leaveOut, boolean inTrade) {
        boolean ring = false;
        IEquip equip = null;
        if (item.getType() == IItem.EQUIP) {
            equip = (IEquip) item;
            if (equip.getRingId() > -1 && item.getPosition() < 0) {
                ring = true;
            }
        }
        short pos = item.getPosition();
        boolean masking = false;
            if (zeroPosition) {
                if (!leaveOut) {
                    mplew.write(0);
                }
            } else if (pos <= (byte) -1) {
                pos *= -1;
                if (pos > 100 || ring) {
                    masking = true;
                    mplew.writeShort(pos - 100);
                } else {
                    if ((item.getType() == IItem.EQUIP) && (!inTrade)) {
                        mplew.writeShort(pos);
                    } else {
                        mplew.write(pos);
                    }
                }
            } else {
                if ((item.getType() == IItem.EQUIP) && (!inTrade)) {
                    mplew.writeShort(item.getPosition());
                } else {
                    mplew.write(item.getPosition());
                }
            }
        if (item.getPetId() > -1) {
            mplew.write(3);
        } else {
            mplew.write(item.getType());
        }
        mplew.writeInt(item.getId());
        if (ring) {
            mplew.write(1);
            mplew.writeInt(equip.getRingId());
            mplew.writeInt(0);
        }
        if (item.getPetId() > -1) {
            MaplePet pet = MaplePet.loadFromDb(item.getId(), item.getPosition(), item.getPetId());
            String petname = pet.getName();
            mplew.write(1);
            mplew.writeInt(item.getPetId());
            mplew.writeInt(0);
            mplew.write(0);
            mplew.write(DataTool.ITEM_MAGIC);
            mplew.write(HexTool.getByteArrayFromHexString("BB 46 E6 17 02"));
            if (petname.length() > 13) {
                petname = petname.substring(0, 13);
            }
            mplew.writeAsciiString(petname);
            for (int i = petname.length(); i < 13; i++) {
                mplew.write(0);
            }
            mplew.write(pet.getLevel());
            mplew.writeShort(pet.getCloseness());
            mplew.write(pet.getFullness());
            mplew.writeLong(DataTool.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.2)));
            mplew.writeInt(0);
            mplew.write(HexTool.getByteArrayFromHexString("50 46 00 00 00 00")); //wonder what this is - WAS 50 46 00 00
            return;
        }
        if (masking && !ring) {
            mplew.write(HexTool.getByteArrayFromHexString("01 BE 50 17 00 00 00 00 00 B0 CE EB"));
            DataTool.addExpirationTime(mplew, 0, false);
        } else if (ring) {
            mplew.writeLong(DataTool.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.2)));
        } else {
            mplew.writeShort(0);
            mplew.write(DataTool.ITEM_MAGIC);
            DataTool.addExpirationTime(mplew, 0, false);
        }
        if (item.getType() == IItem.EQUIP) {
            mplew.write(equip.getUpgradeSlots());
            mplew.write(equip.getLevel());
            mplew.writeShort(equip.getStr()); // str
            mplew.writeShort(equip.getDex()); // dex
            mplew.writeShort(equip.getInt()); // int
            mplew.writeShort(equip.getLuk()); // luk
            mplew.writeShort(equip.getHp()); // hp
            mplew.writeShort(equip.getMp()); // mp
            mplew.writeShort(equip.getWatk()); // watk
            mplew.writeShort(equip.getMatk()); // matk
            mplew.writeShort(equip.getWdef()); // wdef
            mplew.writeShort(equip.getMdef()); // mdef
            mplew.writeShort(equip.getAcc()); // accuracy
            mplew.writeShort(equip.getAvoid()); // avoid
            mplew.writeShort(equip.getHands()); // hands
            mplew.writeShort(equip.getSpeed()); // speed
            mplew.writeShort(equip.getJump()); // jump
            mplew.writeMapleAsciiString(equip.getOwner());
            mplew.writeShort(equip.getFlag()); //Item Flags
            if (!masking) {
                mplew.write(0);
                mplew.write(equip.getItemLevel()); // item level
                mplew.writeShort(0);
                mplew.writeShort(1); // item exp
                mplew.writeShort(equip.getVicious());
                mplew.writeShort(0);
                mplew.writeLong(0);
            } else {
                mplew.write(HexTool.getByteArrayFromHexString("65 0A 28 27 F4 00 00 00 00 00"));
            }
            mplew.write(HexTool.getByteArrayFromHexString("00 40 E0 FD 3B 37 4F 01"));
            mplew.writeInt(-1);
        } else {
            mplew.writeShort(item.getQuantity());
            mplew.writeMapleAsciiString(item.getOwner());
            mplew.writeShort(item.getFlag());
            if (InventoryConstants.isRechargable(item.getId())) {
                mplew.write(HexTool.getByteArrayFromHexString("02 00 00 00 54 00 00 34"));
            }
        }
    }

    public static void addCharEquips(MaplePacketLittleEndianWriter mplew, MapleInventory equip) {
        Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
        for (IItem item : equip.list()) {
            byte pos = (byte) -item.getPosition();
            if (item.getId() == 1812007) {
                mplew.write(0x2E);
                mplew.writeInt(1812007);
            } else if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getId());
            } else if (pos > 100 && pos != 111) {
                pos -= 100;
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getId());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getId());
            }
        }
        for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);
        for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);
        IItem cWeapon = equip.getItem((short) -111);
        mplew.writeInt(cWeapon != null ? cWeapon.getId() : 0);
    }

    public static MaplePacket addInventorySlot(MapleInventoryType type, IItem item) {
        return addInventorySlot(type, item, false);
    }

    public static MaplePacket addInventorySlot(MapleInventoryType type, IItem item, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        if (fromDrop) {
            mplew.write(1);
        } else {
            mplew.write(0);
        }
        mplew.writeShort(1);
        mplew.write(type.getType());
        mplew.write(item.getPosition());
        addItemInfo(mplew, item, true, false);
        return mplew.getPacket();
    }

    public static MaplePacket updateInventorySlot(MapleInventoryType type, IItem item) {
        return updateInventorySlot(type, item, false);
    }

    public static MaplePacket updateInventorySlot(MapleInventoryType type, IItem item, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        if (fromDrop) {
            mplew.write(1);
        } else {
            mplew.write(0);
        }
        mplew.write(1);
        mplew.write(1);
        mplew.write(type.getType());
        mplew.writeShort(item.getPosition());
        mplew.writeShort(item.getQuantity());
        return mplew.getPacket();
    }

    public static MaplePacket moveInventoryItem(MapleInventoryType type, short src, short dst) {
        return moveInventoryItem(type, src, dst, (byte) -1);
    }

    public static MaplePacket moveInventoryItem(MapleInventoryType type, short src, short dst, byte equipIndicator) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1);
        mplew.write(1);
        mplew.write(2);
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.writeShort(dst);
        if (equipIndicator != -1) {
            mplew.write(equipIndicator);
        }
        return mplew.getPacket();
    }

    public static MaplePacket moveAndMergeInventoryItem(MapleInventoryType type, short src, short dst, short total) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1);
        mplew.write(2);
        mplew.write(3);
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.write(1);
        mplew.write(type.getType());
        mplew.writeShort(dst);
        mplew.writeShort(total);
        return mplew.getPacket();
    }

    public static MaplePacket moveAndMergeWithRestInventoryItem(MapleInventoryType type, short src, short dst, short srcQ, short dstQ) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1);
        mplew.write(2);
        mplew.write(1);
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.writeShort(srcQ);
        mplew.write(1);
        mplew.write(type.getType());
        mplew.writeShort(dst);
        mplew.writeShort(dstQ);
        return mplew.getPacket();
    }

    public static MaplePacket clearInventoryItem(MapleInventoryType type, short slot, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(fromDrop ? 1 : 0);
        mplew.write(1);
        mplew.write(3);
        mplew.write(type.getType());
        mplew.writeShort(slot);
        return mplew.getPacket();
    }

    public static MaplePacket scrolledItem(IItem scroll, IItem item, boolean destroyed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM);
        mplew.write(1);
        mplew.write(destroyed ? 2 : 3);
        mplew.write(scroll.getQuantity() > 0 ? 1 : 3);
        mplew.write(MapleInventoryType.USE.getType());
        mplew.writeShort(scroll.getPosition());
        if (scroll.getQuantity() > 0) {
            mplew.writeShort(scroll.getQuantity());
        }
        mplew.write(3);
        if (!destroyed) {
            mplew.write(MapleInventoryType.EQUIP.getType());
            mplew.writeShort(item.getPosition());
            mplew.write(0);
        }
        mplew.write(MapleInventoryType.EQUIP.getType());
        mplew.writeShort(item.getPosition());
        if (!destroyed) {
            addItemInfo(mplew, item, true, true);
        }
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket getScrollEffect(int chr, ScrollResult scrollSuccess, boolean legendarySpirit) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_SCROLL_EFFECT.getValue());
        mplew.writeInt(chr);
        switch (scrollSuccess) {
        case SUCCESS:
            mplew.writeShort(1);
            mplew.writeShort(legendarySpirit ? 1 : 0);
            break;
        case FAIL:
            mplew.writeShort(0);
            mplew.writeShort(legendarySpirit ? 1 : 0);
            break;
        case CURSE:
            mplew.write(0);
            mplew.write(1);
            mplew.writeShort(legendarySpirit ? 1 : 0);
            break;
        }
        return mplew.getPacket();
    }

    public static MaplePacket dropInventoryItem(MapleInventoryType type, short src) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01 03"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        if (src < 0) {
            mplew.write(1);
        }
        return mplew.getPacket();
    }

    public static MaplePacket dropInventoryItemUpdate(MapleInventoryType type, IItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01 01"));
        mplew.write(type.getType());
        mplew.writeShort(item.getPosition());
        mplew.writeShort(item.getQuantity());
        return mplew.getPacket();
    }

    public static MaplePacket finishedSort(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FINISH_SORT.getValue());
        mplew.write(0);
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket finishedSort2(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FINISH_SORT2.getValue());
        mplew.write(0);
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket updateSlot(IItem item) {//Just the same as merge... dst and src is the same...
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(new byte[]{0, 2, 3});
        mplew.write(item.getType());
        mplew.writeShort(item.getPosition());
        mplew.write(0);
        mplew.write(item.getType());
        mplew.writeShort(item.getPosition());
        addItemInfo(mplew, item, true, false);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket itemExpired(int Id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(2);
        mplew.writeInt(Id);
        return mplew.getPacket();
    }

    public static MaplePacket getInventoryFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getShowInventoryFull() {
        return getShowInventoryStatus(0xff);
    }

    public static MaplePacket showItemUnavailable() {
        return getShowInventoryStatus(0xfe);
    }

    public static MaplePacket getShowInventoryStatus(int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0);
        mplew.write(mode);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket getStorage(int npcId, byte slots, Collection<IItem> items, int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x16);
        mplew.writeInt(npcId);
        mplew.write(slots);
        mplew.writeShort(0x7E);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeInt(meso);
        mplew.writeShort(0);
        mplew.write((byte) items.size());
        for (IItem item : items) {
            addItemInfo(mplew, item, true, true);
        }
        mplew.writeShort(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getStorageFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x11);
        return mplew.getPacket();
    }

    public static MaplePacket mesoStorage(byte slots, int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x13);
        mplew.write(slots);
        mplew.writeShort(2);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeInt(meso);
        return mplew.getPacket();
    }

    public static MaplePacket storeStorage(byte slots, MapleInventoryType type, Collection<IItem> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0xD);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(items.size());
        for (IItem item : items) {
            addItemInfo(mplew, item, true, true);
        }
        return mplew.getPacket();
    }

    public static MaplePacket takeOutStorage(byte slots, MapleInventoryType type, Collection<IItem> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x9);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(items.size());
        for (IItem item : items) {
            addItemInfo(mplew, item, true, true);
        }
        return mplew.getPacket();
    }

    public static MaplePacket showChair(int characterid, int Id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_CHAIR.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(Id);
        return mplew.getPacket();
    }

    public static MaplePacket cancelChair(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_CHAIR.getValue());
        if (id == -1) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeShort(id);
        }
        return mplew.getPacket();
    }

    public static MaplePacket showForcedEquip() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FORCED_MAP_EQUIP.getValue());
        return mplew.getPacket();
    }
}
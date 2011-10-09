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

package server;

import java.awt.Point;
import java.util.Iterator;
import java.util.List;
import client.Equip;
import client.IItem;
import client.Item;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MapleRing;
import constants.InventoryConstants;
import tools.factory.EffectFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.InventoryFactory;

/**
 * @name        MapleInventoryManipulator
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleInventoryManipulator {
    public static boolean addRing(final MapleCharacter chr, final int Id, final int ringId) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleInventoryType type = ii.getInventoryType(Id);
        IItem nEquip = ii.getEquipById(Id, ringId);
        short newSlot = chr.getInventory(type).addItem(nEquip);
        if (newSlot == -1) {
            return false;
        }
        chr.getClient().announce(InventoryFactory.addInventorySlot(type, nEquip));
        return true;
    }

    public static boolean addById(MapleClient c, int Id, short quantity) {
        return addById(c, Id, quantity, null, -1);
    }

    public static boolean addById(MapleClient c, int Id, short quantity, String owner, int petid) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleInventoryType type = ii.getInventoryType(Id);
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(c, Id);
            final List<IItem> existing = c.getPlayer().getInventory(type).listById(Id);
            if (!InventoryConstants.isRechargable(Id)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<IItem> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = (Item) i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null)) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.announce(InventoryFactory.updateInventorySlot(type, eItem));
                            }
                        } else {
                            break;
                        }
                    }
                }
                while (quantity > 0 || InventoryConstants.isRechargable(Id)) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        Item nItem = new Item(Id, (short) 0, newQ, petid);
                        short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1) {
                            c.announce(InventoryFactory.getInventoryFull());
                            c.announce(InventoryFactory.getShowInventoryFull());
                            return false;
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        c.announce(InventoryFactory.addInventorySlot(type, nItem));
                        if ((InventoryConstants.isRechargable(Id)) && quantity == 0) {
                            break;
                        }
                    } else {
                        c.announce(IntraPersonalFactory.enableActions());
                        return false;
                    }
                }
            } else {
                final Item nItem = new Item(Id, (short) 0, quantity);
                final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.announce(InventoryFactory.getInventoryFull());
                    c.announce(InventoryFactory.getShowInventoryFull());
                    return false;
                }
                c.announce(InventoryFactory.addInventorySlot(type, nItem));
                c.announce(IntraPersonalFactory.enableActions());
            }
        } else if (quantity == 1) {
            IItem nEquip = ii.getEquipById(Id);
            if (owner != null) {
                nEquip.setOwner(owner);
            }
            short newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
            if (newSlot == -1) {
                c.announce(InventoryFactory.getInventoryFull());
                c.announce(InventoryFactory.getShowInventoryFull());
                return false;
            }
            c.announce(InventoryFactory.addInventorySlot(type, nEquip));
        } else {
            throw new RuntimeException("Trying to create equip with non-one quantity");
        }
        return true;
    }

    public static boolean addFromDrop(final MapleClient c, final Equip equip) {
        return addFromDrop(c, equip.copy(), true, true);
    }

    public static boolean addFromDrop(final MapleClient c, final Item item) {
        return addFromDrop(c, item.copy(), true, true);
    }

    public static boolean addFromDrop(final MapleClient c, final IItem item, final boolean show) {
        return addFromDrop(c, item, show, true);
    }

    public static boolean addFromDrop(final MapleClient c, final IItem item, final boolean show, final boolean showAnyMessage) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.isPickupRestricted(item.getId()) && c.getPlayer().getItemQuantity(item.getId(), true) > 0 && showAnyMessage) {
            c.announce(InventoryFactory.getInventoryFull());
            c.announce(InventoryFactory.showItemUnavailable());
            return false;
        }
        final MapleInventoryType type = ii.getInventoryType(item.getId());
        short quantity = item.getQuantity();
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(c, item.getId());
            final List<IItem> existing = c.getPlayer().getInventory(type).listById(item.getId());
            if (!InventoryConstants.isRechargable(item.getId())) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<IItem> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            final Item eItem = (Item) i.next();
                            final short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && item.getOwner().equals(eItem.getOwner())) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                short newQi = (short) Math.min(quantity, slotMax);
                                quantity -= newQi;
                                Item nItem = new Item(item.getId(), (short) 0, newQi);
                                nItem.setOwner(item.getOwner());
                                if(c.getPlayer().getInventory(type).fakeAddItem(nItem) == -1) {
                                    eItem.setQuantity(oldQ);
                                    return false;
                                }
                                quantity += newQi;
                                c.announce(InventoryFactory.updateInventorySlot(type, eItem, showAnyMessage));
                            }
                        } else {
                            break;
                        }
                    }
                }
                while (quantity > 0 || InventoryConstants.isRechargable(item.getId())) {
                    final short newQ = (short) Math.min(quantity, slotMax);
                    quantity -= newQ;
                    final Item nItem = new Item(item.getId(), (short) 0, newQ);
                    nItem.setOwner(item.getOwner());
                    final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                    if (newSlot == -1) {
                        if (showAnyMessage) {
                            c.announce(InventoryFactory.getInventoryFull());
                            c.announce(InventoryFactory.getShowInventoryFull());
                        }
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    c.announce(InventoryFactory.addInventorySlot(type, nItem, true));
                    if ((InventoryConstants.isRechargable(item.getId())) && quantity == 0) {
                        break;
                    }
                }
            } else {
                final Item nItem = new Item(item.getId(), (short) 0, quantity);
                final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    if (showAnyMessage) {
                        c.announce(InventoryFactory.getInventoryFull());
                        c.announce(InventoryFactory.getShowInventoryFull());
                    }
                    return false;
                }
                c.announce(InventoryFactory.addInventorySlot(type, nItem));
                c.announce(IntraPersonalFactory.enableActions());
            }
        } else if (quantity == 1) {
            final short newSlot = c.getPlayer().getInventory(type).addItem(item);
            if (newSlot == -1) {
                if (showAnyMessage) {
                    c.announce(InventoryFactory.getInventoryFull());
                    c.announce(InventoryFactory.getShowInventoryFull());
                }
                return false;
            }
            c.announce(InventoryFactory.addInventorySlot(type, item, true));
        } else {
            return false;
        }
        if (show) {
            c.announce(EffectFactory.getShowItemGain(item.getId(), item.getQuantity()));
        }
        return true;
    }

    public static boolean checkSpace(final MapleClient c, final int Id, int quantity, final String owner) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleInventoryType type = ii.getInventoryType(Id);
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(c, Id);
            final List<IItem> existing = c.getPlayer().getInventory(type).listById(Id);
            if (!InventoryConstants.isRechargable(Id)) {
                if (existing.size() > 0) {
                    for (IItem eItem : existing) {
                        final short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner.equals(eItem.getOwner())) {
                            final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            final int numSlotsNeeded;
            if (slotMax > 0) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else if (InventoryConstants.isRechargable(Id)) {
                numSlotsNeeded = 1;
            } else {
                numSlotsNeeded = 1;
                System.out.println("checkSpace error");
            }
            return !c.getPlayer().getInventory(type).isFull(numSlotsNeeded - 1);
        } else {
            return !c.getPlayer().getInventory(type).isFull();
        }
    }

    public static void removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot, final short quantity, final boolean fromDrop) {
        removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static void removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot, final short quantity, final boolean fromDrop, final boolean consume) {
        final IItem item = c.getPlayer().getInventory(type).getItem(slot);
        final boolean allowZero = consume && InventoryConstants.isRechargable(item.getId());
        c.getPlayer().getInventory(type).removeItem(slot, quantity, allowZero);
        if (item.getQuantity() == 0 && !allowZero) {
            c.announce(InventoryFactory.clearInventoryItem(type, item.getPosition(), fromDrop));
        } else {
            c.announce(InventoryFactory.updateInventorySlot(type, (Item) item, fromDrop));
        }
    }

    public static void removeById(final MapleClient c, final MapleInventoryType type, final int Id, final int quantity, final boolean fromDrop, final boolean consume) {
        List<IItem> items = c.getPlayer().getInventory(type).listById(Id);
        int remremove = quantity;
        for (IItem item : items) {
            if (remremove <= item.getQuantity()) {
                removeFromSlot(c, type, item.getPosition(), (short) remremove, fromDrop, consume);
                remremove = 0;
                break;
            } else {
                remremove -= item.getQuantity();
                removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume);
            }
        }
        if (remremove > 0) {
            throw new RuntimeException("[h4x] Not enough items available (" + Id + ", " + (quantity - remremove) + "/" + quantity + ")");
        }
    }

    public static void move(final MapleClient c, final MapleInventoryType type, final short src, final short dst) {
        if (src < 0 || dst < 0) {
            return;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final IItem source = c.getPlayer().getInventory(type).getItem(src);
        final IItem initialTarget = c.getPlayer().getInventory(type).getItem(dst);
        if (source == null) {
            return;
        }

        short olddstQ = -1;
        if (initialTarget != null) {
            olddstQ = initialTarget.getQuantity();
        }
        final short oldsrcQ = source.getQuantity();
        final short slotMax = ii.getSlotMax(c, source.getId());

        c.getPlayer().getInventory(type).move(src, dst, slotMax);
        if (!type.equals(MapleInventoryType.EQUIP) && initialTarget != null && initialTarget.getId() == source.getId() && !InventoryConstants.isRechargable(source.getId())) {
            if ((olddstQ + oldsrcQ) > slotMax) {
                c.announce(InventoryFactory.moveAndMergeWithRestInventoryItem(type, src, dst, (short) ((olddstQ + oldsrcQ) - slotMax), slotMax));
            } else {
                c.announce(InventoryFactory.moveAndMergeInventoryItem(type, src, dst, ((Item) c.getPlayer().getInventory(type).getItem(dst)).getQuantity()));
            }
        } else {
            c.announce(InventoryFactory.moveInventoryItem(type, src, dst));
        }
    }

    public static void equip(final MapleClient c, final short src, short dst) {
        Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(src);
        Equip target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        if (dst == -12 && target != null) {
            if (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -13) == null) {
                dst = (short) -13;
                target = null;
            }
            for (int i = 0; i < 2; i++) {
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) (i - 16)) == null) {
                    dst = (short) (i - 16);
                    target = null;
                }
            }
            if (target != null) {
                return;
            }
        }
        if (isPE(source.getId(), dst)
                || source == null
                || (MapleItemInformationProvider.getInstance().getReqLevel(source.getId()) > c.getPlayer().getLevel() && c.getPlayer().gmLevel() < 1)
                || (((source.getId() >= 1902000 && source.getId() <= 1902002) || source.getId() == 1912000) && c.getPlayer().getJobType() != 0)
                || (((source.getId() >= 1902005 && source.getId() <= 1902007) || source.getId() == 1912005) && c.getPlayer().getJobType() != 1)
                || (((source.getId() >= 1902015 && source.getId() <= 1902018) || source.getId() == 1912011) && !c.getPlayer().isAran())) {
            return;
        }
        if (MapleItemInformationProvider.getInstance().isUntradeableOnEquip(source.getId())) {
            source.setFlag((byte) InventoryConstants.UNTRADEABLE);
            c.announce(InventoryFactory.updateSlot(source));
        }
        if (dst == -6) { // unequip the overall
            final IItem top = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -5);
            if (top != null && isOverall(top.getId())) {
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.announce(InventoryFactory.getInventoryFull());
                    c.announce(InventoryFactory.getShowInventoryFull());
                    return;
                }
                unequip(c, (short) -5, c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        } else if (dst == -5) { // equip overall
            final IItem bottom = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -6);
            if (bottom != null && isOverall(source.getId())) {
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.announce(InventoryFactory.getInventoryFull());
                    c.announce(InventoryFactory.getShowInventoryFull());
                    return;
                }
                unequip(c, (short) -6, c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        } else if (dst == -10) { // check if weapon is two-handed
            final IItem weapon = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            if (weapon != null && MapleItemInformationProvider.getInstance().isTwoHanded(weapon.getId())) {
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.announce(InventoryFactory.getInventoryFull());
                    c.announce(InventoryFactory.getShowInventoryFull());
                    return;
                }
                unequip(c, (short) -11, c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        } else if (dst == -11) { // unequip shield
            final IItem shield = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            if (shield != null && MapleItemInformationProvider.getInstance().isTwoHanded(source.getId())) {
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.announce(InventoryFactory.getInventoryFull());
                    c.announce(InventoryFactory.getShowInventoryFull());
                    return;
                }
                unequip(c, (short) -10, c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        }
        if (dst == -18) {
            if (c.getPlayer().getMount() != null) {
                c.getPlayer().getMount().setId(source.getId());
            }
        }
        c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot(src);
        if (target != null) {
            c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeSlot(dst);
        }
        source.setPosition(dst);
        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getPlayer().getInventory(MapleInventoryType.EQUIP).addFromDB(target);
        }
        if (c.getPlayer().getBuffedValue(MapleBuffStat.BOOSTER) != null && isWeapon(source.getId())) {
            c.getPlayer().cancelBuffStats(MapleBuffStat.BOOSTER);
        }
        c.announce(InventoryFactory.moveInventoryItem(MapleInventoryType.EQUIP, src, dst, (byte) 2));
        c.getPlayer().equipChanged();
        if (source.getId() >= 1112001 && source.getId() <= 1112006) {
            c.getPlayer().setMarriageRing(MapleRing.loadFromDB(c.getPlayer()));
            c.announce(IntraPersonalFactory.getCharInfo(c.getPlayer()));
            c.getPlayer().getMap().removePlayer(c.getPlayer());
            c.getPlayer().getMap().addPlayer(c.getPlayer());
        }
    }

    public static void unequip(final MapleClient c, final short src, final short dst) {
        Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(src);
        Equip target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        if (dst < 0) {
            System.out.println("Unequipping to negative slot.");
        }
        if (source == null) {
            return;
        }
        if (target != null && src <= 0) {
            c.announce(InventoryFactory.getInventoryFull());
            return;
        }
        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeSlot(src);
        if (target != null) {
            c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot(dst);
        }
        source.setPosition(dst);
        c.getPlayer().getInventory(MapleInventoryType.EQUIP).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(target);
        }
        c.announce(InventoryFactory.moveInventoryItem(MapleInventoryType.EQUIP, src, dst, (byte) 1));
        c.getPlayer().equipChanged();
    }

    public static void drop(MapleClient c, MapleInventoryType type, short src, short quantity) {
        if (src < 0) {
            type = MapleInventoryType.EQUIPPED;
        }
        IItem source = c.getPlayer().getInventory(type).getItem(src);
        int Id = source.getId();
        if (c.getPlayer().getItemEffect() == Id && source.getQuantity() == 1) {
            c.getPlayer().setItemEffect(0);
            c.getPlayer().getMap().broadcastMessage(EffectFactory.itemEffect(c.getPlayer().getId(), 0));
        } else if (Id == 5370000 || Id == 5370001) { // not actually possible
            if (c.getPlayer().getItemQuantity(Id, false) == 1) {
                c.getPlayer().setChalkboard(null);
            }
        } else if ((Id >= 5000000 && Id <= 5000100) || Id == 4031284) {
            c.getPlayer().dropMessage("This item may not be dropped.");
            return;
        }
        if (c.getPlayer().getItemQuantity(Id, true) < quantity || quantity < 0 || source == null || source.getFlag() == InventoryConstants.LOCK || (quantity == 0 && !InventoryConstants.isRechargable(Id))) {
            return;
        }
        Point dropPos = new Point(c.getPlayer().getPosition());
        if (quantity < source.getQuantity() && !InventoryConstants.isRechargable(Id)) {
            IItem target = source.copy();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            c.announce(InventoryFactory.dropInventoryItemUpdate(type, source));
            dropItem(c, target, dropPos);
        } else {
            c.getPlayer().getInventory(type).removeSlot(src);
            c.announce(InventoryFactory.dropInventoryItem((src < 0 ? MapleInventoryType.EQUIP : type), src));
            if (src < 0) {
                c.getPlayer().equipChanged();
            }
            dropItem(c, source, dropPos);
        }
    }

    private static final void dropItem(MapleClient c, IItem target, Point dropPos) {
        if (MapleItemInformationProvider.getInstance().isDropRestricted(target.getId()) || c.getPlayer().gmLevel() == 1 || c.getPlayer().gmLevel() == 2) {
            c.getPlayer().getMap().disappearingItemDrop(c.getPlayer().getObjectId(), c.getPlayer().getPosition(), c.getPlayer(), target, dropPos);
        } else {
            c.getPlayer().getMap().spawnItemDrop(c.getPlayer().getObjectId(), c.getPlayer().getPosition(), c.getPlayer(), target, dropPos, true, !c.getPlayer().getMap().getEverlast());
        }
    }

    private static final boolean isOverall(final int Id) {
        return Id / 10000 == 105;
    }

    private static final boolean isWeapon(final int Id) {
        return Id >= 1302000 && Id < 1702275;
    }

    private static final boolean isPE(final int id, short dst) {
        if (dst == -14 || dst == -117 || (dst >= -48 && dst <= -20) || (dst >= -100 && dst <= -52) || (dst > -1000 && dst < -100 && MapleItemInformationProvider.getInstance().getCash(id) != 1)) {
            return true;
        }
        switch(dst) {
            case -1:    //== x - 1
            case -2:
            case -3:
            case -4:
                if (id / 10000 - 100 != -dst - 1) {
                    return true;
                }
                break;
            case -5:
                if (id / 10000 - 100 != 4 && id / 10000 - 100 != 5) {
                    return true;
                }
                break;
            case -6:    //== x
            case -7:
            case -8:
                if (id / 10000 - 100 != -dst) {
                    return true;
                }
                break;
            case -9:    //== x + 1
                if (id / 10000 != 110) {
                    return true;
                }
                break;
            case -10:   //== x - 1
                if (id / 10000 != 109 &&  id / 10000 != 134) {
                    return true;
                }
                break;
            case -11:   //weapons
                if (!isWeapon(id)) {
                    return true;
                }
                break;
            case -12:   //rings
            case -13:
            case -15:
            case -16:
                if (id / 10000 != 111) {
                    return true;
                }
                break;
            case -17:
                if (id / 10000 != 112) {
                    return true;
                }
                break;
            case -18:
                if (id / 10000 != 190) {
                    return true;
                }
                break;
            case -19:
                if (id / 10000 != 191) {
                    return true;
                }
                break;
            case -49:
                if (id / 10000 != 114) {
                    return true;
                }
                break;
            case -50:
                if (id / 10000 != 113) {
                    return true;
                }
                break;
            case -51:
                if (id / 10000 != 115) {
                    return true;
                }
                break;
            case -1000: // 194
            case -1001: // 195
            case -1002: // 196
            case -1003: // 197
                if (id / 10000 - 100 != -dst - 906) { //IF 95 == 1001 - 906
                    return true;
                }
                break;
        }
        return false;
    }
}

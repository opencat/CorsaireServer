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

package server.shops;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import client.IItem;
import client.Item;
import client.MapleClient;
import client.MapleInventoryType;
import client.MaplePet;
import constants.InventoryConstants;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.DatabaseConnection;
import tools.factory.EffectFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.InventoryFactory;
import tools.factory.NPCFactory;

/**
 * @name        MapleShop
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleShop {
    private static final Set<Integer> rechargeableItems = new LinkedHashSet<Integer>();
    private int id;
    private int npcId;
    private List<MapleShopItem> items;

    static {
        for (int i = 2070000; i < 2070017; i++) {
            rechargeableItems.add(i);
        }
        rechargeableItems.add(2331000); // Blaze Capsule
        rechargeableItems.add(2332000); // Glaze Capsule
        rechargeableItems.add(2070018);
        rechargeableItems.remove(2070014); // doesn't exist
        for (int i = 2330000; i <= 2330005; i++) {
            rechargeableItems.add(i);
        }
    }

    private MapleShop(int id, int npcId) {
        this.id = id;
        this.npcId = npcId;
        items = new ArrayList<MapleShopItem>();
    }

    private void addItem(MapleShopItem item) {
        items.add(item);
    }

    public void sendShop(MapleClient c) {
        c.getPlayer().setShop(this);
        c.announce(NPCFactory.getNPCShop(c, npcId, items));
    }

    public void buy(MapleClient c, int itemId, short quantity) {
        MapleShopItem item = (npcId >= 9209002 && npcId <= 9209006 ? findByIdDB(itemId) : findById(itemId));
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (item != null && item.getPrice() > 0) {
            if (c.getPlayer().getMeso() >= item.getPrice() * quantity) {
                if (item.getItemId() != itemId || quantity <= 0) {
                    c.getPlayer().ban(c.getPlayer().getName() + " caught packet editing through NPC store.", true);
                    return;
                }
                if (MapleInventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                    if (!InventoryConstants.isRechargable(itemId)) {
                        if (itemId >= 5000000 && itemId <= 5000100) {
                            int petId = MaplePet.createPet(itemId);
                            MapleInventoryManipulator.addById(c, itemId, quantity, null, petId);
                        } else {
                            MapleInventoryManipulator.addById(c, itemId, quantity);
                        }
                        c.getPlayer().gainMeso(-(item.getPrice() * quantity), false);
                    } else {
                        short slotMax = ii.getSlotMax(c, item.getItemId());
                        quantity = slotMax;
                        MapleInventoryManipulator.addById(c, itemId, quantity);
                        c.getPlayer().gainMeso(-item.getPrice(), false);
                    }
                    if (npcId >= 9209002 && npcId <= 9209006) {
                        try {
                            Connection con = DatabaseConnection.getConnection();
                            PreparedStatement ps = con.prepareStatement("DELETE from shopitems WHERE shopid = ? AND itemid = ? AND price = ? AND characterid = ? LIMIT 1");
                            ps.setInt(1, id);
                            ps.setInt(2, item.getItemId());
                            ps.setInt(3, item.getPrice());
                            ps.setInt(4, item.getOwnerId());
                            ps.executeUpdate();
                            ps.close();
                            ps = con.prepareStatement("UPDATE characters SET nxmesos = nxmesos + ? WHERE id = ?");
                            ps.setInt(1, item.getPrice());
                            ps.setInt(2, item.getOwnerId());
                            ps.executeUpdate();
                            ps.close();
                        } catch (SQLException ex) {
                            System.out.print(ex);
                        }
                        c.getPlayer().setNpcId(-1);
                        c.getPlayer().setShop(null);
                        c.announce(IntraPersonalFactory.getCharInfo(c.getPlayer()));
                        c.getPlayer().getMap().removePlayer(c.getPlayer());
                        c.getPlayer().getMap().addPlayer(c.getPlayer());
                    }
                } else {
                    c.announce(EffectFactory.serverNotice(1, "Your Inventory is full."));
                }
                c.announce(NPCFactory.confirmShopTransaction((byte) 0));
            }
        }
    }

    public void sell(MapleClient c, MapleInventoryType type, byte slot, short quantity) {
        if (quantity == 0xFFFF || quantity == 0) {
            quantity = 1;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem item = c.getPlayer().getInventory(type).getItem(slot);
        if (InventoryConstants.isRechargable(item.getId())) {
            quantity = item.getQuantity();
        }
        if (quantity < 0) {
            return;
        }
        short iQuant = item.getQuantity();
        if (iQuant == 0xFFFF) {
            iQuant = 1;
        }
        if (quantity <= iQuant && iQuant > 0) {
            MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
            double price;
            if (InventoryConstants.isRechargable(item.getId())) {
                price = ii.getWholePrice(item.getId()) / (double) ii.getSlotMax(c, item.getId());
            } else {
                price = ii.getPrice(item.getId());
            }
            final int recvMesos = (int) Math.max(Math.ceil(price * quantity), 0);
            if (price != -1 && recvMesos > 0) {
                c.getPlayer().gainMeso(recvMesos, false);
            }
            c.announce(NPCFactory.confirmShopTransaction((byte) 0x8));
        }
    }

    public void recharge(final MapleClient c, final byte slot) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final IItem item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (item == null || !InventoryConstants.isRechargable(item.getId())) {
            return;
        }
        short slotMax = ii.getSlotMax(c, item.getId());
        if (item.getQuantity() < 0) {
            return;
        }
        if (item.getQuantity() < slotMax) {
            final int price = (int) Math.round(ii.getPrice(item.getId()) * (slotMax - item.getQuantity()));
            if (c.getPlayer().getMeso() >= price) {
                item.setQuantity(slotMax);
                c.announce(InventoryFactory.updateInventorySlot(MapleInventoryType.USE, (Item) item));
                c.getPlayer().gainMeso(-price, false, true, false);
                c.announce(NPCFactory.confirmShopTransaction((byte) 0x8));
            } else {
                c.announce(EffectFactory.serverNotice(1, "You do not have enough mesos."));
                c.announce(IntraPersonalFactory.enableActions());
            }
        }
    }

    public void stock(int itemid, int price, int characterid) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position, characterid) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            ps.setInt(2, itemid);
            ps.setInt(3, price);
            ps.setInt(4, 0);
            ps.setInt(5, characterid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            System.out.print(ex);
        }
    }

    private MapleShopItem findById(int itemId) {
        for (MapleShopItem item : items) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    private MapleShopItem findByIdDB(int itemId) {
        for (MapleShopItem item : MapleShopFactory.getInstance().getShop(getId()).items) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public static MapleShop createFromDB(int id, boolean isShopId) {
        MapleShop ret = null;
        int shopId;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (isShopId) {
                ps = con.prepareStatement("SELECT * FROM shops WHERE shopid = ?");
            } else {
                ps = con.prepareStatement("SELECT * FROM shops WHERE npcid = ?");
            }
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                shopId = rs.getInt("shopid");
                ret = new MapleShop(shopId, rs.getInt("npcid"));
                rs.close();
                ps.close();
            } else {
                rs.close();
                ps.close();
                return null;
            }
            ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ? ORDER BY position ASC");
            ps.setInt(1, shopId);
            rs = ps.executeQuery();
            List<Integer> recharges = new ArrayList<Integer>(rechargeableItems);
            while (rs.next()) {
                if (InventoryConstants.isRechargable(rs.getInt("itemid"))) {
                    MapleShopItem starItem = new MapleShopItem((short) 1, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("characterid"));
                    ret.addItem(starItem);
                    if (rechargeableItems.contains(starItem.getItemId())) {
                        recharges.remove(Integer.valueOf(starItem.getItemId()));
                    }
                } else {
                    ret.addItem(new MapleShopItem((short) 1000, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("characterid")));
                }
            }
            for (Integer recharge : recharges) {
                ret.addItem(new MapleShopItem((short) 1000, recharge.intValue(), 0, 0));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public int getNpcId() {
        return npcId;
    }

    public int getId() {
        return id;
    }
}

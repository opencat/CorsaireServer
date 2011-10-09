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

import client.ItemFactory;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import client.IItem;
import client.MapleCharacter;
import client.MapleClient;
import java.sql.SQLException;
import net.channel.ChannelServer;
import tools.DatabaseConnection;
import net.MaplePacket;
import server.MapleInventoryManipulator;
import server.TimerManager;
import tools.Pair;
import client.MapleInventoryType;
import server.maps.AbstractMapleMapObject;
import server.maps.MapleMap;
import server.maps.MapleMapObjectType;
import tools.PrimitiveLogger;
import tools.factory.MarketFactory;

/**
 * @name        HiredMerchant
 * @author      MrXotic
 *              Modified by x711Li
 */
public class HiredMerchant extends AbstractMapleMapObject {
    private int ownerId;
    private int itemId;
    private String ownerName = "";
    private String description = "";
    private MapleCharacter[] visitors = new MapleCharacter[3];
    private final List<MaplePlayerShopItem> items = new LinkedList<MaplePlayerShopItem>();
    private boolean open;
    public ScheduledFuture<?> schedule = null;
    private MapleMap map;

    public HiredMerchant(final MapleCharacter owner, int itemId, String desc) {
        this.setPosition(owner.getPosition());
        this.ownerId = owner.getId();
        this.itemId = itemId;
        this.ownerName = owner.getName();
        this.description = desc;
        this.map = owner.getMap();
        this.schedule = TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                HiredMerchant.this.closeShop();
            }
        }, 43200000); //12 hours!
        map.getChannel().getHMRegistry().registerMerchant(this, owner);
    }

    public void broadcastToVisitors(MaplePacket packet) {
        for (MapleCharacter visitor : visitors) {
            if (visitor != null) {
                visitor.getClient().announce(packet);
            }
        }
    }

    public MapleMap getMap() {
        return map;
    }

    public void addVisitor(MapleCharacter visitor) {
        int i = this.getFreeSlot();
        if (i > -1) {
            visitors[i] = visitor;
            broadcastToVisitors(MarketFactory.hiredMerchantVisitorAdd(visitor, i + 1));
        }
    }

    public void removeVisitor(MapleCharacter visitor) {
        int slot = getVisitorSlot(visitor);
        if (visitors[slot] == visitor) {
            visitors[slot] = null;
            if (slot != 0) {
                broadcastToVisitors(MarketFactory.hiredMerchantVisitorLeave(slot + 1, false));
            }
        }
    }

    public int getVisitorSlot(MapleCharacter visitor) {
        for (int i = 0; i < 3; i++) {
            if (visitors[i] == visitor) {
                return i;
            }
        }
        return 1;
    }

    public void removeAllVisitors() {
        for (int i = 0; i < 3; i++) {
            if (visitors[i] != null) {
                visitors[i].getClient().announce(MarketFactory.leaveHiredMerchant(i + 1, 0x11));
                visitors[i].setHiredMerchant(null);
                visitors[i] = null;
            }
        }
    }

    public void buy(MapleClient c, int item, short quantity) {
        final MaplePlayerShopItem pItem = items.get(item);
        synchronized (items) {
            final IItem newItem = pItem.getItem().copy();
            newItem.setQuantity((short) (newItem.getQuantity() * quantity));
            if (newItem == null || newItem.getQuantity() * quantity < 1 || quantity < 1 || pItem.getBundles() < 1 || newItem.getQuantity() > pItem.getBundles() || !pItem.isExist()) {
                return;
            } else if (newItem.getType() == 1 && newItem.getQuantity() > 1) {
                return;
            }
            if (c.getPlayer().getMeso() >= pItem.getPrice() * quantity && pItem.getPrice() * quantity > 0) {
                if (MapleInventoryManipulator.addFromDrop(c, newItem, true)) {
                    c.getPlayer().gainMeso(-pItem.getPrice() * quantity, false);
                    pItem.setBundles((short) 0);
                    pItem.setDoesExist(false);
                    try {
                        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET merchantmesos = merchantmesos + ? WHERE id = ?");
                        ps.setInt(1, pItem.getPrice() * quantity);
                        ps.setInt(2, ownerId);
                        ps.executeUpdate();
                        ps.close();
                        this.saveItems();
                        c.getPlayer().saveToDB(true);
                        PrimitiveLogger.log("logs/merchantoperation.log", c.getPlayer().getName() + " bought " + pItem.getItem().getId() + " for " + pItem.getPrice());
                    } catch (Exception e) {
                        PrimitiveLogger.logException("logs/merchanterror.log", "Buy error", e);
                        e.printStackTrace();
                    }
                } else {
                    c.getPlayer().dropMessage(1, "Your inventory is full.");
                }
            } else {
                c.getPlayer().dropMessage(1, "You do not have enough mesos.");
            }
        }
    }

    public void closeShopAddItems(MapleClient c) {
        map.removeMapObject(this);
        map.broadcastMessage(MarketFactory.destroyHiredMerchant(ownerId));
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET hasmerchant = 0 WHERE id = ?");
            ps.setInt(1, ownerId);
            ps.executeUpdate();
            ps.close();
            for (MaplePlayerShopItem mpsi : getItems()) {
                if (mpsi.getBundles() > 2) {
                    MapleInventoryManipulator.addById(c, mpsi.getItem().getId(), mpsi.getBundles(), null, -1);
                } else if (mpsi.isExist()) {
                    MapleInventoryManipulator.addFromDrop(c, mpsi.getItem(), true);
                }
            }
        } catch (Exception e) {
        }
        items.clear();
        try {
            this.saveItems();
        } catch (Exception e) {
        }
    }

    public void closeShop() {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET hasmerchant = 0 WHERE id = ?");
            ps.setInt(1, ownerId);
            ps.executeUpdate();
            ps.close();
            this.saveItems();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        map.removeMapObject(this);
        map = null;
        schedule = null;
        map.broadcastMessage(MarketFactory.destroyHiredMerchant(ownerId));
        MapleCharacter owner = ChannelServer.getCharacterFromAllServers(this.ownerId);
        if (owner != null) {
            owner.setHasMerchant(false);
        }
        map.getChannel().getHMRegistry().deregisterMerchant(this);
    }

    public String getOwner() {
        return ownerName;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getDescription() {
        return description;
    }

    public MapleCharacter[] getVisitors() {
        return visitors;
    }

    public List<MaplePlayerShopItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(MaplePlayerShopItem item) {
        items.add(item);
        try {
            this.saveItems();
        } catch (Exception e) {
        }
    }

    public void removeFromSlot(int slot) {
        items.remove(slot);
        try {
            this.saveItems();
        } catch (Exception e) {
        } //not bothered
    }

    public int getFreeSlot() {
        for (int i = 0; i < 3; i++) {
            if (visitors[i] == null) {
                return i;
            }
        }
        return -1;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean set) {
        this.open = set;
    }

    public int getItemId() {
        return itemId;
    }

    public boolean isOwner(MapleCharacter chr) {
        return chr.getId() == ownerId;
    }

    public void saveItems() throws SQLException {
        List<Pair<IItem, MapleInventoryType>> itemsWithType = new ArrayList<Pair<IItem, MapleInventoryType>>();

        for (MaplePlayerShopItem pItems : items) {
            pItems.getItem().setQuantity(pItems.getBundles());
            if (pItems.getBundles() > 0) {
                itemsWithType.add(new Pair<IItem, MapleInventoryType>(pItems.getItem(), MapleInventoryType.getByType(pItems.getItem().getType())));
            }
        }
        ItemFactory.MERCHANT.saveItems(itemsWithType, this.ownerId);
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        return;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.HIRED_MERCHANT;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.announce(MarketFactory.spawnHiredMerchant(this));
    }
}

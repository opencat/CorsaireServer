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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import client.IItem;
import client.ItemFactory;
import client.MapleClient;
import client.MapleInventoryType;
import tools.DatabaseConnection;
import tools.Pair;
import java.util.HashMap;
import tools.factory.InventoryFactory;

/**
 * @name        MapleStorage
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleStorage implements Serializable {
    private static final long serialVersionUID = 9179541993413738569L;
    private int id, accid;
    private List<IItem> items;
    private int meso;
    private byte slots;
    private boolean open;
    private boolean changed = false;
    private Map<MapleInventoryType, List<IItem>> typeItems = new HashMap<MapleInventoryType, List<IItem>>();

    private MapleStorage(int id, byte slots, int meso) {
        this.id = id;
        this.slots = slots;
        this.items = new LinkedList<IItem>();
        this.meso = meso;
    }

    private static MapleStorage create(int id, int world) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO storages (accountid, slots, meso, world) VALUES (?, 4, 0, ?)");
            ps.setInt(1, id);
            ps.setInt(2, world);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loadOrCreateFromDB(id, world);
    }

    public static MapleStorage loadOrCreateFromDB(int id, int world) {
        MapleStorage ret = null;
        int storeId;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT storageid, slots, meso FROM storages WHERE accountid = ? AND world = ?");
            ps.setInt(1, id);
            ps.setInt(2, world);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return create(id, world);
            } else {
                storeId = rs.getInt("storageid");
                ret = new MapleStorage(storeId, (byte) rs.getInt("slots"), rs.getInt("meso"));
                rs.close();
                ps.close();
                ret.accid = id;
                for (Pair<IItem, MapleInventoryType> item : ItemFactory.STORAGE.loadItems(ret.accid * (world == 1 ? -1 : 1), false)) {
                    ret.items.add(item.getLeft());
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    public int getSlots() {
        return slots;
    }

    public void gainSlots(byte gain) {
        setSlots((byte) (gain + getSlots()));
    }

    public void setSlots(byte set) {
        this.slots = set;
    }

    public void saveToDB(int world) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE storages SET slots = ?, meso = ? WHERE storageid = ?");
            ps.setInt(1, slots);
            ps.setInt(2, meso);
            ps.setInt(3, id);
            ps.executeUpdate();
            ps.close();
            List<Pair<IItem, MapleInventoryType>> itemsWithType = new ArrayList<Pair<IItem, MapleInventoryType>>();

            for (IItem item : items) {
                itemsWithType.add(new Pair<IItem, MapleInventoryType>(item, MapleItemInformationProvider.getInstance().getInventoryType(item.getId())));
            }

            ItemFactory.STORAGE.saveItems(itemsWithType, accid * (world == 1 ? -1 : 1));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public IItem takeOut(byte slot) {
        IItem ret = items.remove(slot);
        MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(ret.getId());
        typeItems.put(type, new ArrayList<IItem>(filterItems(type)));
        return ret;
    }

    public void store(IItem item) {
        items.add(item);
        MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(item.getId());
        typeItems.put(type, new ArrayList<IItem>(filterItems(type)));
    }

    public List<IItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private List<IItem> filterItems(MapleInventoryType type) {
        List<IItem> ret = new LinkedList<IItem>();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (IItem item : items) {
            if (ii.getInventoryType(item.getId()) == type) {
                ret.add(item);
            }
        }
        return ret;
    }

    public byte getSlot(MapleInventoryType type, byte slot) {
        byte ret = 0;
        for (IItem item : items) {
            if (item == typeItems.get(type).get(slot)) {
                return ret;
            }
            ret++;
        }
        return -1;
    }

    public void sendStorage(MapleClient c, int npcId) {
        if(open) {
            return;
        } else {
            this.open = true;
        }
        
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Collections.sort(items, new Comparator<IItem>() {

            public int compare(IItem o1, IItem o2) {
                if (ii.getInventoryType(o1.getId()).getType() < ii.getInventoryType(o2.getId()).getType()) {
                    return -1;
                } else if (ii.getInventoryType(o1.getId()) == ii.getInventoryType(o2.getId())) {
                    return 0;
                }
                return 1;
            }
        });
        for (MapleInventoryType type : MapleInventoryType.values()) {
            typeItems.put(type, new ArrayList<IItem>(items));
        }
        c.announce(InventoryFactory.getStorage(npcId, slots, items, meso));
    }

    public void sendStored(MapleClient c, MapleInventoryType type) {
        c.announce(InventoryFactory.storeStorage(slots, type, typeItems.get(type)));
    }

    public void sendTakenOut(MapleClient c, MapleInventoryType type) {
        c.announce(InventoryFactory.takeOutStorage(slots, type, typeItems.get(type)));
    }

    public int getMeso() {
        return meso;
    }

    public void setMeso(int meso) {
        if (meso < 0) {
            return;
        }
        this.meso = meso;
    }

    public void sendMeso(MapleClient c) {
        c.announce(InventoryFactory.mesoStorage(slots, meso));
    }

    public boolean isFull() {
        return items.size() >= slots;
    }

    public void close() {
        typeItems.clear();
        this.open = false;
    }
}

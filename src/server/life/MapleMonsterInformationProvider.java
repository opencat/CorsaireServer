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

package server.life;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import tools.DatabaseConnection;
import java.util.HashMap;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.DropEntry;
import tools.Pair;

/**
 * @name        MapleMonsterInformationProvider
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleMonsterInformationProvider {
    private final static MapleMonsterInformationProvider instance = new MapleMonsterInformationProvider();
    private final Map<Integer, List<DropEntry>> drops = new HashMap<Integer, List<DropEntry>>();
    private final Map<Integer, Pair<Integer, Integer>> mesos = new HashMap<Integer, Pair<Integer, Integer>>();

    public final static MapleMonsterInformationProvider getInstance() {
        return instance;
    }

    public final List<DropEntry> retrieveDropChances(final int monsterId) {
        if (drops.containsKey(monsterId)) {
            return drops.get(monsterId);
        }
        final List<DropEntry> ret = new LinkedList<DropEntry>();
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT itemid, chance FROM monsterdrops WHERE monsterid = ?");
            ps.setInt(1, monsterId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(new DropEntry(rs.getInt("itemid"), rs.getInt("chance"), 0));
            }
            rs.close();
            ps.close();
            ps = DatabaseConnection.getConnection().prepareStatement("SELECT itemid, chance, quest FROM monsterquestdrops WHERE monsterid = ?");
            ps.setInt(1, monsterId);
            rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(new DropEntry(rs.getInt("itemid"), rs.getInt("chance"), rs.getInt("quest")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
        }
        drops.put(monsterId, ret);
        return ret;
    }

    public final Pair<Integer, Integer> retrieveMesoChances(final int monsterId) {
        if (mesos.containsKey(monsterId)) {
            return mesos.get(monsterId);
        }
        Pair<Integer, Integer> ret = null;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT min, max FROM monstermesodrops WHERE monsterid = ?");
            ps.setInt(1, monsterId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret = new Pair<Integer, Integer>(rs.getInt("min"), rs.getInt("max"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
        }
        mesos.put(monsterId, ret);
        return ret;
    }

    public final void clearDrops() {
        drops.clear();
    }

    public final ArrayList<Pair<Integer, String>> getListFromName(String search) {
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
        ArrayList<Pair<Integer, String>> retMobs = new ArrayList<Pair<Integer, String>>();
        MapleData data = dataProvider.getData("Mob.img");
        List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
        for (MapleData mobIdData : data.getChildren()) {
            int mobIdFromData = Integer.parseInt(mobIdData.getName());
            String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
            mobPairList.add(new Pair<Integer, String>(mobIdFromData, mobNameFromData));
        }
        for (Pair<Integer, String> mobPair : mobPairList) {
            if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                retMobs.add(mobPair);
            }
        }
        return retMobs;
    }

    public final String getNameFromId(int id) {
        try {
            return MapleLifeFactory.getInstance().getMonster(id).getName();
        } catch (Exception e) {
            return null; // nonexistant mob
        }
    }
}

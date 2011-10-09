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

package scripting.reactor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import scripting.AbstractCachedScriptManager;
import server.DropEntry;
import tools.DatabaseConnection;

/**
 * @name        ReactorScriptManager
 * @author      x711Li
 */
public class ReactorScriptManager extends AbstractCachedScriptManager {
    private static ReactorScriptManager instance = new ReactorScriptManager();
    private Map<Integer, List<DropEntry>> drops = new HashMap<Integer, List<DropEntry>>();

    private ReactorScriptManager() {
        super();
    }

    public static ReactorScriptManager getInstance() {
        return instance;
    }

    public List<DropEntry> getDrops(int rid) {
        List<DropEntry> ret = drops.get(rid);
        if (ret == null) {
            ret = new LinkedList<DropEntry>();
            try {
                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT itemid, chance FROM reactordrops WHERE reactorid = ?");
                ps.setInt(1, rid);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ret.add(new DropEntry(rs.getInt("itemid"), rs.getInt("chance"), 0));
                }
                rs.close();
                ps.close();
                ps = DatabaseConnection.getConnection().prepareStatement("SELECT itemid, chance, quest FROM reactorquestdrops WHERE reactorid = ?");
                ps.setInt(1, rid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.add(new DropEntry(rs.getInt("itemid"), rs.getInt("chance"), rs.getInt("quest")));
                }
                rs.close();
                ps.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            drops.put(rid, ret);
        }
        return ret;
    }

    public void clearDrops() {
        drops.clear();
    }
}

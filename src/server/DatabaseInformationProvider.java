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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import tools.DatabaseConnection;

/**
 * @name        DatabaseInformationProvider
 * @author      x711Li
 */
public class DatabaseInformationProvider {
    private final static DatabaseInformationProvider instance = new DatabaseInformationProvider();
    protected Set<String> forbiddenNames = new HashSet<String>();
    protected Set<Integer> scriptedQuests = new HashSet<Integer>();

    private DatabaseInformationProvider() {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT element FROM cache_forbidden");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                forbiddenNames.add(rs.getString("element"));
            }
            rs.close();
            ps.close();
            
            ps = DatabaseConnection.getConnection().prepareStatement("SELECT element FROM cache_scriptedquests");
            rs = ps.executeQuery();
            while (rs.next()) {
                scriptedQuests.add(rs.getInt("element"));
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
        }
    }

    public final static DatabaseInformationProvider getInstance() {
        return instance;
    }

    public final int getRandElement(String table) {
        int ret = -1;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `element` FROM " + table + " ORDER BY RAND() LIMIT 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret = rs.getInt("element");
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public final boolean isForbidden(final String name) {
        for (final String forbidden : forbiddenNames) {
            if (name.contains(forbidden)) {
                return true;
            }
        }
        return false;
    }

    public final boolean isScripted(final int id) {
        return scriptedQuests.contains(id);
    }
}
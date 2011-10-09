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

package server.cashshop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import tools.DatabaseConnection;
import tools.Pair;

/**
 * @name        CashDataProvider
 * @author      x711Li
 */
public class CashDataProvider {
    private static List<Pair<Integer, Byte>> customSales;

    public static List<Pair<Integer, Byte>> getCustomSales() {
        if (customSales == null) {
            customSales = new ArrayList<Pair<Integer, Byte>>();
            Connection mcdb = DatabaseConnection.getConnection();

            try {
                PreparedStatement ps = mcdb.prepareStatement("SELECT * FROM `customsales`");
                ResultSet rs = ps.executeQuery();

                while (rs.next())
                    customSales.add(new Pair<Integer, Byte>(rs.getInt("sn"), rs.getByte("sale")));

                rs.close();
                ps.close();
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }
        }

        return customSales;
    }
}

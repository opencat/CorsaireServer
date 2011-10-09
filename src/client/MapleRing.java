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

package client;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.DatabaseConnection;
import server.MapleInventoryManipulator;

/**
 * @name        MapleRing
 * @author      Leifde
 *              Modified by x711Li
 */
public class MapleRing implements Comparable<MapleRing>, Serializable {
    private MapleCharacter mc;
    private int ringId;
    private int coupleRingId;
    private int partnerId;
    private int itemId;
    private String coupleName;

    private MapleRing(int id, int id2, int partnerId, int itemid, String partnername) {
        this.ringId = id;
        this.coupleRingId = id2;
        this.partnerId = partnerId;
        this.itemId = itemid;
        this.coupleName = partnername;
    }

    public static MapleRing loadFromDB(MapleCharacter mc) {
        try {
            MapleRing ret = null;
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM rings WHERE id = (SELECT partnerRingId FROM rings WHERE partnerName = ?)");
            ps.setString(1, mc.getName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret = new MapleRing(rs.getInt("id"), rs.getInt("partnerRingId"), rs.getInt("partnerChrId"), rs.getInt("itemid"), rs.getString("partnerName"));
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static int createRing(int itemid, final MapleCharacter partner1, final MapleCharacter partner2, String message) {
        try {
            if (partner1 == null) {
                return -2;
            } else if (partner2 == null) {
                return -1;
            }
            int[] ringID = new int[2];
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO rings (`id`, itemid, partnerChrId, partnername) VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, itemid);
            ps.setInt(2, partner2.getId());
            ps.setString(3, partner2.getName());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            ringID[0] = rs.getInt(1);
            rs.close();
            ps.close();
            ps = con.prepareStatement("INSERT INTO rings (`id`, itemid, partnerRingId, partnerChrId, partnername) VALUES (DEFAULT, ?, ?, ?, ?)");
            ps.setInt(1, itemid);
            ps.setInt(2, ringID[0]);
            ps.setInt(3, partner1.getId());
            ps.setString(4, partner1.getName());
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            rs.next();
            ringID[1] = rs.getInt(1);
            rs.close();
            ps.close();
            ps = con.prepareStatement("UPDATE rings SET partnerRingId = ? WHERE id = ?");
            ps.setInt(1, ringID[1]);
            ps.setInt(2, ringID[0]);
            ps.executeUpdate();
            ps.close();
            MapleInventoryManipulator.addRing(partner1, itemid, ringID[0]);
            MapleInventoryManipulator.addRing(partner2, itemid, ringID[1]);
            return 1;
        } catch (SQLException ex) {
            return 0;
        }
    }
    
    public static int divorce(final MapleCharacter chr) {
        try {
            if (chr == null) {
                return -2;
            }
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("DELETE from rings WHERE partnerChrId = ?");
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
            ps.close();
            ps = DatabaseConnection.getConnection().prepareStatement("DELETE from rings WHERE partnerChrId = ?");
            ps.setInt(1, chr.getCoupleId());
            ps.executeUpdate();
            ps.close();
            return 1;
        } catch (SQLException ex) {
            return 0;
        }
    }

    public int getRingId() {
        return ringId;
    }

    public int getCoupleRingId() {
        return coupleRingId;
    }

    public int getCoupleId() {
        return mc.getCoupleId();
    }
    
    public String getCoupleName() {
        return coupleName;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MapleRing) {
            if (((MapleRing) o).getRingId() == getRingId()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.ringId;
        return hash;
    }

    @Override
    public int compareTo(MapleRing other) {
        if (ringId < other.getRingId()) {
            return -1;
        } else if (ringId == other.getRingId()) {
            return 0;
        }
        return 1;
    }

    public int getPartnerChrId() {
        return partnerId;
    }

    public int getId() {
        return itemId;
    }
}

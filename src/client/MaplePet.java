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

import java.awt.Point;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.Serializable;
import java.util.List;
import tools.DatabaseConnection;
import server.MapleItemInformationProvider;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;

/**
 * @name        MaplePet
 * @author      Matze
 *              Modified by x711Li
 */
public class MaplePet extends Item implements Serializable {
    private static final long serialVersionUID = 9179541993413738569L;
    private String name;
    private int uniqueid;
    private int closeness = 0;
    private int level = 1;
    private int fullness = 100;
    private int Fh;
    private Point pos;
    private int stance;

    public MaplePet(final int id, final short position, final int uniqueid) {
        super(id, position, (short) 1);
        this.uniqueid = uniqueid;
    }

    public static final MaplePet loadFromDb(final int itemid, final short position, final int petid) {
        try {
            final MaplePet ret = new MaplePet(itemid, position, petid);
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name, level, closeness, fullness FROM pets WHERE petid = ?"); // Get pet details..
            ps.setInt(1, petid);
            final ResultSet rs = ps.executeQuery();
            rs.next();
            ret.setName(rs.getString("name"));
            ret.setCloseness(Math.min(rs.getInt("closeness"), 30000));
            ret.setLevel(Math.min(rs.getInt("level"), 30));
            ret.setFullness(Math.min(rs.getInt("fullness"), 100));
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException e) {
            return null;
        }
    }

    public final void saveToDb() {
        try {
            final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ? WHERE petid = ?");
            ps.setString(1, getName());
            ps.setInt(2, getLevel());
            ps.setInt(3, getCloseness());
            ps.setInt(4, getFullness());
            ps.setInt(5, getUniqueId());
            ps.executeUpdate();
            ps.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public static final int createPet(final int itemid) {
        try {
            final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO pets (name, level, closeness, fullness) VALUES (?, 1, 0, 100)");
            ps.setString(1, MapleItemInformationProvider.getInstance().getName(itemid));
            ps.executeUpdate();
            final ResultSet rs = ps.getGeneratedKeys();
            int ret = -1;
            if (rs.next()) {
                ret = rs.getInt(1);
                rs.close();
                ps.close();
            }
            return ret;
        } catch (final SQLException e) {
            return -1;
        }
    }

    public static int createPet(int itemid, int level, int closeness, int fullness) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO pets (name, level, closeness, fullness) VALUES (?, ?, ?, ?)");
            ps.setString(1, MapleItemInformationProvider.getInstance().getName(itemid));
            ps.setInt(2, level);
            ps.setInt(3, closeness);
            ps.setInt(4, fullness);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            int ret = rs.getInt(1);
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException e) {
            return -1;
        }
    }

    public final String getName() {
        return name;
    }

    public final void setName(final String name) {
        this.name = name;
    }

    public int getUniqueId() {
        return uniqueid;
    }

    public void setUniqueId(int id) {
        this.uniqueid = id;
    }

    public final int getCloseness() {
        return closeness;
    }

    public final void setCloseness(final int closeness) {
        this.closeness = closeness;
    }

    public final void gainCloseness(final int x) {
        this.closeness += x;
    }

    public final int getLevel() {
        return level;
    }

    public final void setLevel(final int level) {
        this.level = level;
    }

    public final int getFullness() {
        return fullness;
    }

    public final void setFullness(final int fullness) {
        this.fullness = fullness;
    }

    public final int getFh() {
        return Fh;
    }

    public final void setFh(final int Fh) {
        this.Fh = Fh;
    }

    public final Point getPos() {
        return pos;
    }

    public final void setPos(final Point pos) {
        this.pos = pos;
    }

    public final int getStance() {
        return stance;
    }

    public final void setStance(final int stance) {
        this.stance = stance;
    }

    public final boolean canConsume(final int itemId) {
        final MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        for (final int petId : mii.petsCanConsume(itemId)) {
            if (petId == this.getId()) {
                return true;
            }
        }
        return false;
    }

    public final void updatePosition(final List<LifeMovementFragment> movement) {
        for (final LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    this.setPos(((LifeMovement) move).getPosition());
                }
                this.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}

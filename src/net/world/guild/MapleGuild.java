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

package net.world.guild;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import client.MapleCharacter;
import client.MapleClient;
import constants.ServerConstants;
import tools.DatabaseConnection;
import net.MaplePacket;
import net.channel.ChannelServer;
import net.world.WorldRegistryImpl;
import java.util.LinkedHashMap;
import net.channel.remote.ChannelWorldInterface;
import server.DatabaseInformationProvider;
import tools.factory.GuildFactory;
import tools.factory.PartyFactory;

/**
 * @name        MapleGuild
 * @author      StellarAshes
 *              Modified by x711Li
 */
public class MapleGuild implements java.io.Serializable {
    private static final long serialVersionUID = 4733411045302360119L;
    private enum BCOp {
        NONE, DISBAND, EMBELMCHANGE
    }
    private final List<MapleGuildCharacter> members;
    private final String rankTitles[] = new String[5];
    private String name;
    private int id;
    private int gp;
    private int logo;
    private int logoColor;
    private int leader;
    private int capacity;
    private int logoBG;
    private int logoBGColor;
    private String notice;
    private int signature;
    private final Map<Integer, List<Integer>> notifications = new LinkedHashMap<Integer, List<Integer>>();
    private boolean bDirty = true;
    private int allianceId;
    private int world;
    private ReentrantLock lock = new ReentrantLock();

    public MapleGuild(final MapleGuildCharacter init, final int world) {
        this(init);
        this.world = world;
    }

    public MapleGuild(final MapleGuildCharacter initiator) {
        int guildid = initiator.getGuildId();
        members = new CopyOnWriteArrayList<MapleGuildCharacter>();
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid = " + guildid);
            ResultSet rs = ps.executeQuery();
            if (!rs.first()) {
                id = -1;
                ps.close();
                rs.close();
                return;
            }
            id = guildid;
            name = rs.getString("name");
            gp = rs.getInt("GP");
            logo = rs.getInt("logo");
            logoColor = rs.getInt("logoColor");
            logoBG = rs.getInt("logoBG");
            logoBGColor = rs.getInt("logoBGColor");
            capacity = rs.getInt("capacity");
            for (int i = 1; i <= 5; i++) {
                rankTitles[i - 1] = rs.getString("rank" + i + "title");
            }
            leader = rs.getInt("leader");
            notice = rs.getString("notice");
            signature = rs.getInt("signature");
            allianceId = rs.getInt("allianceId");
            ps.close();
            rs.close();
            ps = con.prepareStatement("SELECT id, name, level, job, guildrank, allianceRank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC");
            ps.setInt(1, guildid);
            rs = ps.executeQuery();
            if (!rs.first()) {
                rs.close();
                ps.close();
                return;
            }
            do {
                members.add(new MapleGuildCharacter(rs.getInt("id"), rs.getInt("level"), rs.getString("name"), -1, rs.getInt("job"), rs.getInt("guildrank"), guildid, false, rs.getInt("allianceRank")));
            } while (rs.next());
            setOnline(initiator.getId(), true, initiator.getChannel());
            ps.close();
            rs.close();
        } catch (SQLException se) {
            System.out.println("unable to read guild information from sql" + se);
        }
    }

    public void buildNotifications() {
        if (!bDirty) {
            return;
        }
        Set<Integer> chs = WorldRegistryImpl.getInstance().getChannelServer(world);
        if (notifications.keySet().size() != chs.size()) {
            notifications.clear();
            for (final Integer ch : chs) {
                notifications.put(ch, new java.util.LinkedList<Integer>());
            }
        } else {
            for (List<Integer> l : notifications.values()) {
                l.clear();
            }
        }
        for (final MapleGuildCharacter mgc : members) {
            if (!mgc.isOnline()) {
                continue;
            }
            List<Integer> ch = notifications.get(mgc.getChannel() - world * ServerConstants.NUM_CHANNELS);
            if (ch == null) {
                System.out.println("Unable to connect to channel " + mgc.getChannel());
            } else {
                ch.add(mgc.getId());
            }
        }
        bDirty = false;
    }

    public void writeToDB(final boolean bDisband) {
        try {
            Connection con = DatabaseConnection.getConnection();
            if (!bDisband) {
                StringBuilder builder = new StringBuilder();
                builder.append("UPDATE guilds SET GP = ?, logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ?, ");
                for (int i = 0; i < 5; i++) {
                    builder.append("rank" + (i + 1) + "title = ?, ");
                }
                builder.append("capacity = ?, notice = ? WHERE guildid = ?");
                PreparedStatement ps = con.prepareStatement(builder.toString());
                ps.setInt(1, gp);
                ps.setInt(2, logo);
                ps.setInt(3, logoColor);
                ps.setInt(4, logoBG);
                ps.setInt(5, logoBGColor);
                for (int i = 6; i < 11; i++) {
                    ps.setString(i, rankTitles[i - 6]);
                }
                ps.setInt(11, capacity);
                ps.setString(12, notice);
                ps.setInt(13, this.id);
                ps.executeUpdate();
                ps.close();
            } else {
                PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
                ps.setInt(1, this.id);
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
                ps.setInt(1, this.id);
                ps.executeUpdate();
                ps.close();
                this.broadcast(GuildFactory.guildDisband(this.id));
            }
        } catch (SQLException se) {
        }
    }

    public final int getId() {
        return id;
    }

    public final int getLeaderId() {
        return leader;
    }

    public final int getGP() {
        return gp;
    }

    public final int getLogo() {
        return logo;
    }

    public final void setLogo(final int l) {
        logo = l;
    }

    public final int getLogoColor() {
        return logoColor;
    }

    public final void setLogoColor(final int c) {
        logoColor = c;
    }

    public final int getLogoBG() {
        return logoBG;
    }

    public final void setLogoBG(final int bg) {
        logoBG = bg;
    }

    public final int getLogoBGColor() {
        return logoBGColor;
    }

    public final void setLogoBGColor(final int c) {
        logoBGColor = c;
    }

    public final String getNotice() {
        if (notice == null) {
            return "";
        }
        return notice;
    }

    public final String getName() {
        return name;
    }

    public final java.util.Collection<MapleGuildCharacter> getMembers() {
        return java.util.Collections.unmodifiableCollection(members);
    }

    public final int getCapacity() {
        return capacity;
    }

    public final int getSignature() {
        return signature;
    }

    public final void broadcast(final MaplePacket packet) {
        broadcast(packet, -1, BCOp.NONE);
    }

    public final void broadcast(final MaplePacket packet, final int exception) {
        broadcast(packet, exception, BCOp.NONE);
    }

    public final void broadcast(final MaplePacket packet, final int exceptionId, final BCOp bcop) {
        WorldRegistryImpl wr = WorldRegistryImpl.getInstance();

        Set<Integer> chs = wr.getChannelServer(world);
        lock.lock();
        try {
            if (bDirty) {
                buildNotifications();
            }
            try {
                ChannelWorldInterface cwi;
                for (int ch : chs) {
                    cwi = wr.getChannel(ch, world);
                    if (notifications.get(ch - world * ServerConstants.NUM_CHANNELS).size() > 0) {
                        if (bcop == BCOp.DISBAND) {
                            cwi.setGuildAndRank(notifications.get(ch - world * ServerConstants.NUM_CHANNELS), 0, 5, exceptionId);
                        } else if (bcop == BCOp.EMBELMCHANGE) {
                            cwi.changeEmblem(this.id, notifications.get(ch - world * ServerConstants.NUM_CHANNELS), new MapleGuildSummary(this));
                        } else {
                            cwi.sendPacket(notifications.get(ch - world * ServerConstants.NUM_CHANNELS), packet, exceptionId);
                        }
                    }
                }
            } catch (java.rmi.RemoteException re) {
                System.out.println("Failed to contact channel(s) for broadcast.");
            }
        } finally {
            lock.unlock();
        }
    }

    public final void guildMessage(final MaplePacket serverNotice) {
        for (final MapleGuildCharacter mgc : members) {
            for (final ChannelServer cs : ChannelServer.getAllInstances()) {
                if (cs.getPlayerStorage().getCharacterById(mgc.getId()) != null) {
                    final MapleCharacter chr = cs.getPlayerStorage().getCharacterById(mgc.getId());
                    if (serverNotice != null) {
                        chr.getClient().announce(serverNotice);
                    } else {
                        chr.getMap().removePlayer(chr);
                        chr.getMap().addPlayer(chr);
                    }
                }
            }
        }
    }

    public final void setOnline(final int cid, final boolean online, final int channel) {
        if(channel < 0)
        return;
        boolean bBroadcast = true;
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getId() == cid) {
                if (mgc.isOnline() && online) {
                    bBroadcast = false;
                }
                mgc.setOnline(online);
                mgc.setChannel(channel);
                break;
            }
        }

        if (bBroadcast) {
            this.broadcast(GuildFactory.guildMemberOnline(id, cid, online), cid);
        }
        bDirty = true;
    }

    public final void guildChat(final String name, final int cid, final String msg) {
        this.broadcast(PartyFactory.multiChat(name, msg, 2), cid);
    }

    public final String getRankTitle(final int rank) {
        return rankTitles[rank - 1];
    }

    public final static int createGuild(final int leaderId, final String name) {
        if (DatabaseInformationProvider.getInstance().isForbidden(name.toLowerCase())) {
            return 0;
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.first()) {
                ps.close();
                rs.close();
                return 0;
            }
            ps.close();
            rs.close();
            ps = con.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`) VALUES (?, ?, ?)");
            ps.setInt(1, leaderId);
            ps.setString(2, name);
            ps.setInt(3, (int) System.currentTimeMillis());
            ps.execute();
            ps.close();
            ps = con.prepareStatement("SELECT guildid FROM guilds WHERE leader = ?");
            ps.setInt(1, leaderId);
            rs = ps.executeQuery();
            rs.first();
            int guildid = rs.getInt("guildid");
            rs.close();
            ps.close();
            return guildid;
        } catch (Exception e) {
            return 0;
        }
    }

    public int addGuildMember(MapleGuildCharacter mgc) {
        lock.lock();
        try {
            if (members.size() >= capacity) {
                return 0;
            }
            for (int i = members.size() - 1; i >= 0; i--) {
                if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(mgc.getName()) < 0) {
                    members.add(i + 1, mgc);
                    bDirty = true;
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
        this.broadcast(GuildFactory.newGuildMember(mgc));
        return 1;
    }

    public void leaveGuild(MapleGuildCharacter mgc) {
        this.broadcast(GuildFactory.memberLeft(mgc, false));
        lock.lock();
        try {
            members.remove(mgc);
            bDirty = true;
        } finally {
            lock.unlock();
        }
    }

    public void expelMember(MapleGuildCharacter initiator, String name, int cid) {
        for (int i = 0; i < members.size(); i++) {
            final MapleGuildCharacter mgc = members.get(i);
            if (mgc.getId() == cid && initiator.getGuildRank() < mgc.getGuildRank()) {
                this.broadcast(GuildFactory.memberLeft(mgc, true));
                bDirty = true;
                try {
                    if (mgc.isOnline()) {
                        WorldRegistryImpl.getInstance().getChannel(mgc.getChannel(), world).setGuildAndRank(cid, 0, 5);
                    } else {
                        try {
                            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)");
                            ps.setString(1, name);
                            ps.setString(2, initiator.getName());
                            ps.setString(3, "You have been expelled from the guild.");
                            ps.setLong(4, System.currentTimeMillis());
                            ps.executeUpdate();
                            ps.close();
                        } catch (SQLException e) {
                            System.out.println("expelMember - MapleGuild " + e);
                        }
                        WorldRegistryImpl.getInstance().getChannel(1, world).setOfflineGuildStatus((short) 0, (byte) 5, cid);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                return;
            }
        }
        System.out.println("Unable to find member with name " + name + " and id " + cid);
    }

    public final void changeRank(final int cid, final int newRank) {
        for (final MapleGuildCharacter mgc : members) {
            if (cid == mgc.getId()) {
                try {
                    if (mgc.isOnline()) {
                        WorldRegistryImpl.getInstance().getChannel(mgc.getChannel(), world).setGuildAndRank(cid, this.id, newRank);
                    } else {
                        WorldRegistryImpl.getInstance().getChannel(world * ServerConstants.NUM_CHANNELS + 1, world).setOfflineGuildStatus((short) this.id, (byte) newRank, cid);
                    }
                } catch (RemoteException re) {
                    re.printStackTrace();
                    return;
                }
                mgc.setGuildRank(newRank);
                this.broadcast(GuildFactory.changeRank(mgc));
                return;
            }
        }
    }

    public final void setGuildNotice(final String notice) {
        this.notice = notice;
        writeToDB(false);
        this.broadcast(GuildFactory.guildNotice(this.id, notice));
    }

    public final void memberLevelJobUpdate(final MapleGuildCharacter mgc) {
        for (MapleGuildCharacter member : members) {
            if (mgc.equals(member)) {
                member.setJobId(mgc.getJobId());
                member.setLevel(mgc.getLevel());
                this.broadcast(GuildFactory.guildMemberLevelJobUpdate(mgc));
                break;
            }
        }
    }

    @Override
    public final boolean equals(Object other) {
        if (!(other instanceof MapleGuildCharacter)) {
            return false;
        }
        MapleGuildCharacter o = (MapleGuildCharacter) other;
        return (o.getId() == id && o.getName().equals(name));
    }

    @Override
    public final int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 89 * hash + this.id;
        return hash;
    }

    public final void changeRankTitle(String[] ranks) {
        for (int i = 0; i < 5; i++) {
            rankTitles[i] = ranks[i];
        }
        this.broadcast(GuildFactory.rankTitleChange(this.id, ranks));
        this.writeToDB(false);
    }

    public final void disbandGuild() {
        this.writeToDB(true);
        this.broadcast(null, -1, BCOp.DISBAND);
    }

    public final void setGuildEmblem(final short bg, final byte bgcolor, final short logo, final byte logocolor) {
        this.logoBG = bg;
        this.logoBGColor = bgcolor;
        this.logo = logo;
        this.logoColor = logocolor;
        this.writeToDB(false);
        this.broadcast(null, -1, BCOp.EMBELMCHANGE);
    }

    public final MapleGuildCharacter getMGC(final int cid) {
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getId() == cid) {
                return mgc;
            }
        }
        return null;
    }

    public final boolean increaseCapacity() {
        if (capacity > 99) {
            return false;
        }
        capacity += 5;
        this.writeToDB(false);
        this.broadcast(GuildFactory.guildCapacityChange(this.id, this.capacity));
        return true;
    }

    public final void gainGP(final int amount) {
        this.gp += amount;
        this.writeToDB(false);
        this.guildMessage(GuildFactory.updateGP(this.id, this.gp));
    }

    public final static MapleGuildResponse sendInvite(final MapleClient c, final String targetName) {
        MapleCharacter mc = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);
        if (mc == null) {
            return MapleGuildResponse.NOT_IN_CHANNEL;
        }
        if (mc.getGuildId() > 0) {
            return MapleGuildResponse.ALREADY_IN_GUILD;
        }
        mc.getClient().announce(GuildFactory.guildInvite(c.getPlayer().getGuildId(), c.getPlayer().getName()));
        return null;
    }

    public final static void displayGuildRanks(final MapleClient c, final int npcid) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `name`, `GP`, `logoBG`, `logoBGColor`, " + "`logo`, `logoColor` FROM guilds ORDER BY `GP` DESC LIMIT 50");
            ResultSet rs = ps.executeQuery();
            c.announce(GuildFactory.showGuildRanks(npcid, rs));
            ps.close();
            rs.close();
        } catch (SQLException e) {
            System.out.println("failed to display guild ranks. " + e);
        }
    }

    public final int getAllianceId() {
        return allianceId;
    }

    public final void setAllianceId(final int aid) {
        this.allianceId = aid;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE guilds SET allianceId = ? WHERE guildid = ?");
            ps.setInt(1, aid);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
        }
    }

    public final int getIncreaseGuildCost(final int size) {
        return 500000 * (size - 6) / 6;
    }
}

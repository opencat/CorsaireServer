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

package net.login.handler;

import client.Equip;
import client.Item;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventory;
import client.MapleInventoryType;
import constants.ServerConstants;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;
import net.login.LoginServer;
import net.world.guild.MapleGuildCharacter;
import server.DatabaseInformationProvider;
import server.TimerManager;
import tools.DatabaseConnection;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.factory.LoginFactory;

/**
 * @name        LoginOperation
 * @author      x711Li
 */
public class LoginOperation {
    private static final String[] names = {"Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia", "Yellonde", "Demethos", "Elnido", "Kastia", "Judis", "Arkenia", "Plana", "Galicia", "Kalluna", "Stius", "Croa", "Zenith", "Medere"};

    public static final void ServerListRequestHandler(MapleClient c) {
        for (int i = 0; i < ServerConstants.NUM_WORLDS; i++) {//NUM_WORLDS
            try {
                c.getSession().write(LoginFactory.getServerList(i, names[i], LoginServer.getInstance().getWorldInterface().getChannelLoad(i)));
            } catch (RemoteException e) {
            }
        }
        c.getSession().write(LoginFactory.getEndOfServerList());
        c.getSession().write(LoginFactory.enableRecommendedServers());
        c.getSession().write(LoginFactory.sendRecommendedServers());
    }

    public static final void CharlistRequestHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        int server = slea.readByte();
        c.setWorld(server);
        if (!c.isLoggedIn()) {
            c.getSession().close(true);
        }
        c.setChannel(slea.readByte() + 1);
        c.sendCharList(server);
    }

    public static final void LoginPasswordHandler(String login, String pwd, MapleClient c) {
        int loginok = 0;
        c.setAccountName(login);
        final boolean isBanned = c.hasBannedIP() || c.hasBannedMac();
        loginok = c.login(login, pwd, isBanned);
        Calendar tempbannedTill = c.getTempBanCalendar();
        if (loginok == 0 && isBanned) {
            loginok = 3;
            //MapleCharacter.ban(c.getSession().getRemoteAddress().toString().split(":")[0], "Mac/IP Re-ban", false);
        } else if (loginok != 0) {
            c.announce(LoginFactory.getLoginFailed(loginok));
            return;
        }
        if (tempbannedTill.getTimeInMillis() != 0) {
            c.getSession().close(true);
            return;
        }
        if (c.finishLogin() == 0) {
            c.announce(LoginFactory.getAuthSuccessRequestPin(c, c.getAccountName()));
            final MapleClient client = c;
            c.setIdleTask(TimerManager.getInstance().schedule(new Runnable() {
                public void run() {
                    client.getSession().close(true);
                }
            }, 600000));
        } else {
            c.getSession().close(true);
        }
    }

    public static final void ServerStatusRequestHandler(int worldIndex, MapleClient c) {
        int status;
        int num = 0;
        try {
            for (int load : LoginServer.getInstance().getWorldInterface().getChannelLoad(worldIndex).keySet()) {
                num += load;
            }
        } catch (RemoteException re) {
            System.out.println("Failed to get channel load.");
        }
        if (num >= ServerConstants.CHANNEL_LOAD) {
            status = 2;
        } else if (num >= ServerConstants.CHANNEL_LOAD * .8) { // More than 80 percent o___o
            status = 1;
        } else {
            status = 0;
        }
        c.announce(LoginFactory.getServerStatus(status));
    }

    public static final boolean canCreateChar(final String name) {
        if (name.length() < 4 || name.length() > 12) {
            return false;
        }
        return filtered(name) && getIdByName(name) < 0 && Pattern.compile("[a-zA-Z0-9]{3,12}").matcher(name).matches();
    }

    private static final int getIdByName(final String name) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT id FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            int id = -1;
            if (rs.next()) {
                id = rs.getInt("id");
            }
            rs.close();
            ps.close();
            return id;
        } catch (Exception e) {
        }
        return -1;
    }

    private static final boolean filtered(final String name) {
        if (DatabaseInformationProvider.getInstance().isForbidden(name.toLowerCase())) {
            return false;
        }
        return true;
    }

    private static boolean containsInt(int[] array, int toCompare) {
        for (int i : array) {
            if (i == toCompare) {
                return true;
            }
        }
        return false;
    }

    private static final int[] allowedEquips = {1040006, 1040010, 1040002, 1060002, 1060006,
        1072005, 1072001, 1072037, 1072038, 1322005, 1312004, 1042167, 1062115, 1072383,
        1442079, 1302000, 1041002, 1041006, 1041010, 1041011, 1061002, 1061008, 1042180, 1060138, 1072418, 1061137,
        1302132, 1061160};

    public static final void CreateCharHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        String name = slea.readMapleAsciiString();
        if (!MapleCharacter.canCreateChar(name)) {
            return;
        }
        MapleCharacter newchar = MapleCharacter.getDefault(c);
        newchar.setWorld(c.getWorld());
        int job = slea.readInt();
        int face = slea.readInt();
        newchar.setFace(face);
        newchar.setHair(slea.readInt() + slea.readInt());
        newchar.setSkinColor(slea.readInt());
        int top = slea.readInt();
        int bottom = slea.readInt();
        int shoes = slea.readInt();
        int weapon = slea.readInt();
        if(!((containsInt(allowedEquips, top) && containsInt(allowedEquips, bottom) && containsInt(allowedEquips, shoes) && containsInt(allowedEquips, weapon)))) {
            c.getSession().close(true);
            return;
        }
        newchar.setGender(slea.readByte());
        newchar.setName(name);
        if (job == 0) { // Knights of Cygnus
            newchar.setJob(1000);
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161047, (byte) 1, (short) 1));
            newchar.setMap(130030000);
        } else if (job == 1) { // Adventurer
            newchar.setJob(0);
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (byte) 1, (short) 1));
            newchar.setMap(10000);
        } else if (job == 2) { // Aran
            newchar.setJob(2000);
            newchar.setMaxHp(30000);
            newchar.setHp(30000);
            newchar.setMaxMp(30000);
            newchar.setMp(30000);
            newchar.setStr(999);
            newchar.setDex(999);
            newchar.setInt(999);
            newchar.setLuk(999);
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161048, (byte) 1, (short) 1));
            newchar.setMap(914000000);
        } else {
            System.out.println("[CHAR CREATION] A new job ID has been found: " + job);
        }
        MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);
        Equip eq_top = new Equip(top, (byte) -5, -1);
        eq_top.setWdef((short) 3);
        equip.addFromDB(eq_top.copy());
        Equip eq_bottom = new Equip(bottom, (byte) -6, -1);
        eq_bottom.setWdef((short) 2);
        equip.addFromDB(eq_bottom.copy());
        Equip eq_shoes = new Equip(shoes, (byte) -7, -1);
        eq_shoes.setWdef((short) 2);
        equip.addFromDB(eq_shoes.copy());
        Equip eq_weapon = new Equip(weapon, (byte) -11, -1);
        eq_weapon.setWatk((short) 15);
        equip.addFromDB(eq_weapon.copy());
        newchar.saveToDB(false);
        c.announce(LoginFactory.addNewCharEntry(newchar));
    }

    public static final void CheckCharNameHandler(String name, MapleClient c) {
        c.announce(LoginFactory.charNameResponse(name, !canCreateChar(name)));
    }

    public static final void DeleteCharHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte state = 0;
        String pic = slea.readMapleAsciiString();
        final int cid = slea.readInt();
        state = deleteCharacter(cid, c.getWorld());
        if (c.getPIC().equals(pic) || c.getPIC().equals("0")) {
            c.announce(LoginFactory.deleteCharResponse(cid, state));
        } else {
            c.announce(LoginFactory.wrongPIC());
        }
    }

    private static byte deleteCharacter(int cid, int world) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, guildid, guildrank, name, allianceRank FROM characters WHERE id = ?");
            ps.setInt(1, cid);
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt("guildid") > 0) {
                if (rs.getInt("guildrank") == 1) {
                    rs.close();
                    ps.close();
                    return 22;
                }
                try {
                    LoginServer.getInstance().getWorldInterface().deleteGuildCharacter(new MapleGuildCharacter(cid, 0, rs.getString("name"), -1, 0, rs.getInt("guildrank"), rs.getInt("guildid"), false, rs.getInt("allianceRank")), world);
                } catch (RemoteException re) {
                    rs.close();
                    ps.close();
                    return 1;
                }
            }
            ps.close();
            rs.close();
            ps = con.prepareStatement("DELETE FROM characters WHERE id = ?");
            ps.setInt(1, cid);
            ps.executeUpdate();
            ps.close();
            String[] toDel = {"buddies", "cooldowns", "dojocounts", "extendedsp", "famelog", "family", "inventoryitems", "keymap", "questinfo", "queststatus", "savedlocations", "shopitems", "skillmacros", "skills", "telerockmaps", "trocklocations"};
            for (String s : toDel) {
                ps = con.prepareStatement("DELETE FROM `" + s + "` WHERE characterid = ?");
                ps.setInt(1, cid);
                ps.executeUpdate();
                ps.close();
            }
            return 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return 1;
        }
    }

    public static final void RegisterPICHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(5);
        String macs = slea.readMapleAsciiString();
        c.updateMacs(macs);
        if (c.hasBannedMac()) {
            c.getSession().close(true);
            return;
        }
        String pic = slea.readMapleAsciiString();
        if (pic.length() < 6 || pic.length() > 16) {
            return;
        }
        c.setPIC(pic);
    }

    public static final void PromptPICHandler(String pic, int charId, String macs, MapleClient c) {
        c.updateMacs(macs);
        if (c.hasBannedMac()) {
            c.getSession().close(true);
            return;
        }
        if (pic.equals(c.getPIC()) || c.getPIC().equals("0")) {
            try {
                if (c.getIdleTask() != null) {
                    c.getIdleTask().cancel(true);
                }
                c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
                String[] socket = LoginServer.getInstance().getIP(c.getWorld(), c.getChannel()).split(":");
                c.announce(LoginFactory.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
            } catch (UnknownHostException e) {
            }
        } else {
            c.announce(LoginFactory.wrongPIC());
        }
    }
    
    public static final void ViewAllCharHandler(MapleClient c) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT world, id FROM characters WHERE accountid = ?");
            ps.setInt(1, c.getAccID());
            int charsNum = 0;
            List<Integer> worlds = new ArrayList<Integer>();
            List<MapleCharacter> chars = new ArrayList<MapleCharacter>();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int cworld = rs.getInt("world");
                boolean inside = false;
                for (int w : worlds) {
                    if (w == cworld) {
                        inside = true;
                    }
                }
                if (!inside) {
                    worlds.add(cworld);
                }
                MapleCharacter chr = MapleCharacter.loadCharFromDB(rs.getInt("id"), c, false);
                chars.add(chr);
                charsNum++;
            }
            rs.close();
            ps.close();
            int unk = charsNum + 3 - charsNum % 3;
            c.announce(LoginFactory.showAllCharacter(charsNum, unk));
            for (int w : worlds) {
                List<MapleCharacter> chrsinworld = new ArrayList<MapleCharacter>();
                for (MapleCharacter chr : chars) {
                    if (chr.getWorld() == w) {
                        chrsinworld.add(chr);
                    }
                }
                c.announce(LoginFactory.showAllCharacterInfo(w, chrsinworld));
            }
        } catch (Exception e) {
        }
    }

    public static final void ViewAllRegisterPICHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        int charId = slea.readInt();
        int world = slea.readInt(); //world
        int channel = 0;
        String mac = slea.readMapleAsciiString();
        c.updateMacs(mac);
        if (c.hasBannedMac()) {
            c.getSession().close(true);
            return;
        }
        slea.readMapleAsciiString();
        String pic = slea.readMapleAsciiString();
        c.setPIC(pic);
        try {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
            String channelServerIP = MapleClient.getChannelServerIPFromSubnet(c.getSession().getRemoteAddress().toString().replace("/", "").split(":")[0], channel);

            if (channelServerIP.equals("0.0.0.0")) {
                String[] socket = LoginServer.getInstance().getIP(world, channel).split(":");
                c.announce(LoginFactory.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
            } else {
                String[] socket = LoginServer.getInstance().getIP(world, channel).split(":");
                c.announce(LoginFactory.getServerIP(InetAddress.getByName(channelServerIP), Integer.parseInt(socket[1]), charId));
            }
        } catch (UnknownHostException e) {
        }
    }

    public static final void ViewAllPromptPICHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        String pic = slea.readMapleAsciiString();
        int charId = slea.readInt();
        int world = slea.readInt();
        int channel = 0;
        String macs = slea.readMapleAsciiString();
        c.updateMacs(macs);
        if (c.hasBannedMac()) {
            c.getSession().close(true);
            return;
        }
        if (pic.equals(c.getPIC()) || c.getPIC().equals("0")) {
            try {
                if (c.getIdleTask() != null) {
                    c.getIdleTask().cancel(true);
                }
                c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());

                String channelServerIP = MapleClient.getChannelServerIPFromSubnet(c.getSession().getRemoteAddress().toString().replace("/", "").split(":")[0], channel);
                if(channelServerIP.equals("0.0.0.0")) {
                    String[] socket = LoginServer.getInstance().getIP(world, channel).split(":");

                    c.announce(LoginFactory.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
                } else {
                    String[] socket = LoginServer.getInstance().getIP(world, channel).split(":");
                    c.announce(LoginFactory.getServerIP(InetAddress.getByName(channelServerIP), Integer.parseInt(socket[1]), charId));
                }
            } catch (UnknownHostException e) {
            }

        } else {
            c.announce(LoginFactory.wrongPIC());
        }
    }
}
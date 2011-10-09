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
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.script.ScriptEngine;
import net.MaplePacket;
import net.cashshop.CashShopServer;
import tools.DatabaseConnection;
import net.channel.ChannelServer;
import net.login.LoginServer;
import net.world.MapleMessengerCharacter;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.remote.WorldChannelInterface;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestActionManager;
import scripting.quest.QuestScriptManager;
import server.MapleTrade;
import server.TimerManager;
import tools.MapleAESOFB;
import tools.HexTool;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.DummySession;
import tools.factory.GuildFactory;
import tools.factory.LoginFactory;

/**
 * @name        MapleClient
 * @author      Matze
 *              Modified by x711Li
 */
public final class MapleClient implements Serializable {
    private static final long serialVersionUID = 9179541993413738569L;
    public static final transient int LOGIN_NOTLOGGEDIN = 0,
        LOGIN_SERVER_TRANSITION = 1,
        LOGIN_LOGGEDIN = 2,
        LOGIN_WAITING = 3,
        CASH_SHOP_TRANSITION = 4,
        LOGIN_CS_LOGGEDIN = 5,
        CHANGE_CHANNEL = 6;
    public static final String CLIENT_KEY = "CLIENT";
    private transient MapleAESOFB send;
    private transient MapleAESOFB receive;
    private IoSession session;
    private MapleCharacter player;
    private int channel = 1;
    private int accId = -1;
    private boolean loggedIn = false;
    private boolean serverTransition = false;
    private boolean insideCashShop = false;
    private String accountName;
    private int world;
    private transient long lastPong;
    private int gmlevel;
    private String PIC;
    private transient Set<String> macs = new HashSet<String>();
    private transient Map<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();
    private transient ScheduledFuture<?> idleTask = null;
    private int characterSlots = 6;
    private transient byte loginattempt = 0;
    private Calendar tempban;
    private final transient Lock lock = new ReentrantLock(true);

    public MapleClient(final MapleAESOFB send, final MapleAESOFB receive, final IoSession session) {
        this.send = send;
        this.receive = receive;
        this.session = session;
    }

    public MapleClient() {
        this.session = new DummySession();
        this.send = null;
        this.receive = null;
    }

    public final boolean insideCashShop() {
        return insideCashShop;
    }

    public final void setInsideCashShop(final boolean insideCashShop) {
        this.insideCashShop = insideCashShop;
    }

    public final synchronized MapleAESOFB getReceiveCrypto() {
        return receive;
    }

    public final synchronized MapleAESOFB getSendCrypto() {
        return send;
    }

    public final synchronized IoSession getSession() {
        return session;
    }

    public final void announce(MaplePacket packet) {
        session.write(packet);
    }
    
    public final Lock getLock() {
    return lock;
    }

    public final MapleCharacter getPlayer() {
        return player;
    }

    public final void setPlayer(final MapleCharacter player) {
        this.player = player;
    }

    public final void sendCharList(final int server) {
        this.announce(LoginFactory.getCharList(this, server));
    }

    public final String getSessionIPAddress() {
        return session.getRemoteAddress().toString().split(":")[0];
    }

    public final boolean checkIPAddress() {
        try {
            final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT ip FROM accounts WHERE id = ?");
            ps.setInt(1, this.accId);
            final ResultSet rs = ps.executeQuery();
            boolean canlogin = false;
            if (rs.next()) {
                final String sessionIP = rs.getString("ip");
                if (sessionIP != null) { // Probably a login proced skipper?
                    canlogin = (getSessionIPAddress().equals(sessionIP.split(":")[0]) || sessionIP.equals("0"));
                }
            }
            rs.close();
            ps.close();
            return canlogin;
        } catch (final SQLException e) {
            System.out.println("Failed in checking IP address for client.");
        }
        return false;
    }

    public final List<MapleCharacter> loadCharacters(int serverId) {
        final List<MapleCharacter> chars = new ArrayList<MapleCharacter>(6);
        try {
            for (final CharNameAndId cni : loadCharactersInternal(serverId)) {
                final MapleCharacter chr = MapleCharacter.loadCharFromDB(cni.id, this, false);
                chars.add(chr);
            }
        } catch (Exception e) {
        }
        return chars;
    }

    public List<String> loadCharacterNames(int serverId) {
        List<String> chars = new ArrayList<String>(6);
        for (CharNameAndId cni : loadCharactersInternal(serverId)) {
            chars.add(cni.name);
        }
        return chars;
    }

    private List<CharNameAndId> loadCharactersInternal(int serverId) {
        PreparedStatement ps;
        List<CharNameAndId> chars = new ArrayList<CharNameAndId>(6);
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ?");
            ps.setInt(1, this.getAccID());
            ps.setInt(2, serverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                chars.add(new CharNameAndId(rs.getString("name"), rs.getInt("id")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chars;
    }

    public final boolean isLoggedIn() {
        return loggedIn;
    }

    public final boolean hasBannedIP() {
        boolean ret = false;
        try {
            final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')");
            ps.setString(1, session.getRemoteAddress().toString());
            final ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                ret = true;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
        }
        return ret;
    }

    public final boolean hasBannedMac() {
        if (macs.isEmpty()) {
            return false;
        }
        if(macs.contains("7A-79-00-00-00-02")) {
            macs.remove("7A-79-00-00-00-02");
        }
        boolean ret = false;
        int i = 0;
        try {
            final StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM macbans WHERE mac IN (");
            for (i = 0; i < macs.size(); i++) {
                sql.append("?");
                if (i != macs.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql.toString());
            i = 0;
            for (final String mac : macs) {
                i++;
                ps.setString(i, mac);
            }
            final ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                ret = true;
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
        }
        return ret;

    }

    private final void loadMacsIfNescessary() throws SQLException {
        if (macs.isEmpty()) {
            final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT macs FROM accounts WHERE id = ?");
            ps.setInt(1, accId);
            final ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                for (final String mac : rs.getString("macs").split(", ")) {
                    if (!mac.equals("")) {
                        macs.add(mac);
                    }
                }
            }
            rs.close();
            ps.close();
        }
    }

    public final void banMacs() {
        final Connection con = DatabaseConnection.getConnection();
        try {
            loadMacsIfNescessary();
            final List<String> filtered = new LinkedList<String>();
            PreparedStatement ps = con.prepareStatement("SELECT filter FROM macfilters");
            final ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                filtered.add(rs.getString("filter"));
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)");
            for (final String mac : macs) {
                boolean matched = false;
                for (String filter : filtered) {
                    if (mac.matches(filter)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    ps.setString(1, mac);
                    ps.executeUpdate();
                }
            }
            ps.close();
        } catch (SQLException e) {
        }
    }

    public int finishLogin() {
        synchronized (MapleClient.class) {
            final int state = getLoginState();
            if (state > LOGIN_NOTLOGGEDIN) {
                loggedIn = false;
                return 7;
            }
            updateLoginState(LOGIN_LOGGEDIN, null);
        }
        return 0;
    }

    public final void setPIC(final String PIC) {
        this.PIC = PIC;
        try {
            final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET PIC = ? WHERE id = ?");
            ps.setString(1, PIC);
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
        }
    }

    public final String getPIC() {
        return PIC;
    }

    public final int login(final String login, final String pwd, final boolean ipMacBanned) {
        loginattempt++;
        if (loginattempt > 6) {
            getSession().close(true);
        }
        int loginok = 5;
        final Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, password, banned, gm, PIC, tempban FROM accounts WHERE name = ?");
            ps.setString(1, login);
            final ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                final int banned = rs.getInt("banned");
                this.accId = rs.getInt("id");
                this.gmlevel = rs.getInt("gm");
                PIC = rs.getString("PIC");
                final String passhash = rs.getString("password");
                tempban = getTempBanCalendar(rs);
                ps.close();
                if (banned > 0) {
                    loginok = 3;
                } else {
                    if (banned == -1) {
                        int i;
                        try {
                            loadMacsIfNescessary();
                            final StringBuilder sql = new StringBuilder("DELETE FROM macbans WHERE mac IN (");
                            for (i = 0; i < macs.size(); i++) {
                                sql.append("?");
                                if (i != macs.size() - 1) {
                                    sql.append(", ");
                                }
                            }
                            sql.append(")");
                            ps = con.prepareStatement(sql.toString());
                            i = 0;
                            for (final String mac : macs) {
                                ps.setString(++i, mac);
                            }
                            ps.executeUpdate();
                            ps.close();
                            ps = con.prepareStatement("DELETE FROM ipbans WHERE ip LIKE CONCAT(?, '%')");
                            ps.setString(1, getSession().getRemoteAddress().toString().split(":")[0]);
                            ps.executeUpdate();
                            ps.close();
                            ps = con.prepareStatement("UPDATE accounts SET banned = 0, norankupdate = 0 WHERE id = ?");
                            ps.setInt(1, accId);
                            ps.executeUpdate();
                            ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (getLoginState() > LOGIN_NOTLOGGEDIN) {
                        loggedIn = false;
                        loginok = 7;
                    } else if (pwd.equals(passhash) || checkHash(passhash, "SHA-1", pwd)) {
                        loginok = 0;
                    } else {
                        loggedIn = false;
                        loginok = 4;
                    }
                }
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (loginok == 0) {
            loginattempt = 0;
        }
        return loginok;
    }

    private final static long dottedQuadToLong(final String dottedQuad) throws RuntimeException {
        final String[] quads = dottedQuad.split("\\.");
        if (quads.length != 4) {
            throw new RuntimeException("Invalid IP Address format.");
        }
        long ipAddress = 0;
        for (int i = 0; i < 4; i++) {
            ipAddress += (long) (Integer.parseInt(quads[i]) % 256) * (long) Math.pow(256, (double) (4 - i));
        }
        return ipAddress;
    }

    public final static String getChannelServerIPFromSubnet(final String clientIPAddress, final int channel) {
        final long ipAddress = dottedQuadToLong(clientIPAddress);
        final Properties subnetInfo = LoginServer.getInstance().getSubnetInfo();
        if (subnetInfo.contains("net.login.subnetcount")) {
            for (int i = 0; i < Integer.parseInt(subnetInfo.getProperty("net.login.subnetcount")); i++) {
                String[] connectionInfo = subnetInfo.getProperty("net.login.subnet." + i).split(":");
                if (((ipAddress & dottedQuadToLong(connectionInfo[0])) == (dottedQuadToLong(connectionInfo[1]) & dottedQuadToLong(connectionInfo[0]))) && (channel == Integer.parseInt(connectionInfo[2]))) {
                    return connectionInfo[1];
                }
            }
        }
        return "0.0.0.0";
    }

    public final void updateMacs(final String macData) {
        boolean ban = false;
        try {
            if (macs.isEmpty()) {
                final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT macs FROM accounts WHERE id = ?");
                ps.setInt(1, accId);
                final ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String s = rs.getString("macs");
                    if (s != null) {
                        for (final String mac : s.split(", ")) {
                            if (mac.length() != 0) {
                                macs.add(mac);
                            }
                        }
                    }
                }
                rs.close();
                ps.close();
            }
        } catch (SQLException se) {
        }
        macs.addAll(Arrays.asList(macData.split(", ")));
        StringBuilder newMacData = new StringBuilder(16);
        for (final String s : macs) {
            newMacData.append(s);
            if (s.equals("00-1C-C0-E8-AB-70")) { // troublesome mac, should be blacklisted everywhere. insert more here
                ban = true;
            }
            newMacData.append(", ");
        }
        newMacData.setLength(newMacData.length() - 2);
        try {
            final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET macs = ?, ip = ? WHERE id = ?");
            ps.setString(1, newMacData.toString());
            ps.setString(2, session.getRemoteAddress().toString().split(":")[0]);
            ps.setInt(3, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
        }
        if (ban) {
            this.player.ban("MAC Banned", true);
            this.disconnect(!insideCashShop, insideCashShop);
        }
    }

    public final void setAccID(final int id) {
        this.accId = id;
    }

    public final int getAccID() {
        return accId;
    }

    public final void updateLoginState(final int newstate, final String SessionID) {
        try {
            final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = ?, ip = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?");
            ps.setInt(1, newstate);
            ps.setString(2, SessionID);
            ps.setInt(3, getAccID());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (newstate == LOGIN_NOTLOGGEDIN) {
            loggedIn = false;
            serverTransition = false;
        } else {
            serverTransition = (newstate == LOGIN_SERVER_TRANSITION);
            loggedIn = !serverTransition;
        }
    }

    public final int getLoginState() {
        if(this.accId == -1) {
            return LOGIN_NOTLOGGEDIN;
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT loggedin, lastlogin FROM accounts WHERE id = ?");
            ps.setInt(1, getAccID());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("getLoginState - MapleClient");
            }
            int state = rs.getInt("loggedin");
            rs.close();
            ps.close();
            if (state == LOGIN_LOGGEDIN) {
                loggedIn = true;
            } else if (state == LOGIN_SERVER_TRANSITION) {
                ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?");
                ps.setInt(1, getAccID());
                ps.executeUpdate();
                ps.close();
            } else {
                loggedIn = false;
            }
            return state;
        } catch (SQLException e) {
            loggedIn = false;
            e.printStackTrace();
            throw new RuntimeException("login state");
        }
    }

    public final void removalTask() {
        try {
            if (!player.getAllBuffs().isEmpty()) {
                player.clearBuffs();
            }
            if (!player.getAllDiseases().isEmpty()) {
                player.clearDebuffs();
            }
            if (player.getTrade() != null) {
                MapleTrade.cancelTrade(player);
            }
            NPCScriptManager.getInstance().dispose(this);
            if (player.getEventInstance() != null) {
                player.getEventInstance().playerDisconnected(player);
            }
            if (player.getMap() != null) {
                player.getMap().removePlayer(player);
            }
        } catch (final Throwable e) {
            //FileoutputUtil.outputFileError(FileoutputUtil.Acc_Stuck, e);
        }
    }


    public final void disconnect(final boolean fromChannel, final boolean fromCashShop) {
        if (player != null && isLoggedIn()) {
            removalTask();
            player.saveToDB(true);
            final ChannelServer ch = ChannelServer.getInstance(channel);
            final WorldChannelInterface wci = ch.getWorldInterface();
            try {
                if (player.getMessenger() != null) {
                    wci.leaveMessenger(player.getMessenger().getId(), new MapleMessengerCharacter(player));
                    player.setMessenger(null);
                }
                if (player.getParty() != null) {
                    final MaplePartyCharacter chrp = new MaplePartyCharacter(player);
                    chrp.setOnline(false);
                    wci.updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, chrp);
                }
                if (!serverTransition && isLoggedIn()) {
                    wci.loggedOff(player.getName(), player.getId(), channel, player.getBuddylist().getBuddyIds());
                } else { // Change channel
                    wci.loggedOn(player.getName(), player.getId(), channel, player.getBuddylist().getBuddyIds());
                }
                if (player.getGuildId() > 0) {
                    wci.setGuildMemberOnline(player.getMGC(), false, channel);
                    int allianceId = player.getGuild().getAllianceId();
                    if (allianceId > 0) {
                        wci.allianceMessage(allianceId, GuildFactory.allianceMemberOnline(player, false), player.getId(), -1);
                    }
                }
            } catch (final RemoteException e) {
                ch.reconnectWorld();
                player.setMessenger(null);
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                if (fromChannel && ch != null) {
                    ch.removePlayer(player);
                } else if (fromCashShop && CashShopServer.getInstance() != null) {
                    CashShopServer.getInstance().getShopperStorage().deregisterPlayer(player);
                }
                player.empty();
                player = null;
            }
        }
        if (!serverTransition && isLoggedIn()) {
            updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN, null);
        }
    }

    public final int getChannel() {
        return channel;
    }

    public final ChannelServer getChannelServer() {
        return ChannelServer.getInstance(channel);
    }

    public final String getAccountName() {
        return accountName;
    }

    public final void setAccountName(final String a) {
        this.accountName = a;
    }

    public final void setChannel(final int channel) {
        this.channel = channel;
    }

    public final int getWorld() {
        return world;
    }

    public final void setWorld(final int world) {
        this.world = world;
    }

    public final void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public final void sendPing() {
        final long then = System.currentTimeMillis();
        announce(LoginFactory.getPing());
        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    if (lastPong < then) {
                        if (getSession().isConnected()) {
                            getSession().close(true);
                        }
                        if(player != null) {
                            ChannelServer.getInstance(channel).removePlayer(player);
                            player.setStuck(true);
                        }
                    }
                } catch (final NullPointerException e) {
                }
            }
        }, 15000);
    }

    public final Set<String> getMacs() {
        return Collections.unmodifiableSet(macs);
    }

    public final int gmLevel() {
        return this.gmlevel;
    }

    public final void setScriptEngine(final String name, final ScriptEngine e) {
        engines.put(name, e);
    }

    public final ScriptEngine getScriptEngine(final String name) {
        return engines.get(name);
    }

    public final void removeScriptEngine(final String name) {
        engines.remove(name);
    }

    public final ScheduledFuture<?> getIdleTask() {
        return idleTask;
    }

    public final void setIdleTask(final ScheduledFuture<?> idleTask) {
        this.idleTask = idleTask;
    }

    public final NPCConversationManager getCM() {
        return NPCScriptManager.getInstance().getCM(this);
    }

    public final QuestActionManager getQM() {
        return QuestScriptManager.getInstance().getQM(this);
    }

    private static class CharNameAndId {
        public String name;
        public int id;

        public CharNameAndId(final String name, final int id) {
            super();
            this.name = name;
            this.id = id;
        }
    }

    private final static boolean checkHash(final String hash, final String type, final String password) {
        try {
            final MessageDigest digester = MessageDigest.getInstance(type);
            digester.update(password.getBytes("UTF-8"), 0, password.length());
            return HexTool.toString(digester.digest()).replace(" ", "").toLowerCase().equals(hash);
        } catch (Exception e) {
            throw new RuntimeException("Encoding the string failed", e);
        }
    }

    public final int getCharacterSlots() {
        return characterSlots;
    }

    public final void setCharacterSlots(final int amount) {
        this.characterSlots = amount;
    }

    public final static int findAccIdForCharacterName(final String charName) {
        try {
            final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, charName);
            final ResultSet rs = ps.executeQuery();
            int ret = -1;
            if (rs.next()) {
                ret = rs.getInt("accountid");
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public final static boolean unstuck(final String character, final String name, String password) {
        final Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, character);
            ResultSet rs = ps.executeQuery();
            int accountid = -1;
            if (rs.next()) {
                accountid = rs.getInt("accountid");
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT password, id FROM accounts WHERE name = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();
            String hash = null;
            if (rs.next()) {
                if (rs.getInt("id") != accountid) {
                    rs.close();
                    ps.close();
                    return false;
                }
                hash = rs.getString("password");
            }
            rs.close();
            ps.close();
            final MessageDigest digester = MessageDigest.getInstance("SHA-1");
            digester.update(password.getBytes("UTF-8"), 0, password.length());
            if (HexTool.toString(digester.digest()).replace(" ", "").toLowerCase().equals(hash)) {
                MapleCharacter victim = null;
                for (ChannelServer cserv_ : ChannelServer.getAllInstances()) {
                    victim = cserv_.getPlayerStorage().getCharacterByName(character);
                    if (victim != null) {
                        break;
                    }
                }
                if (victim == null) {
                    return false;
                }
                victim.saveToDB(true);
                try {
                    ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?");
                    ps.setInt(1, victim.getAccountID());
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                victim.getClient().getChannelServer().removePlayer(victim);
                victim.getClient().getSession().close();
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Encoding the string failed", e);
        }
    }
    
    private final static Calendar getTempBanCalendar(final ResultSet rs) throws SQLException {
        final Calendar lTempban = Calendar.getInstance();
        final long blubb = rs.getLong("tempban");
        if (blubb == 0) {
            lTempban.setTimeInMillis(0);
            return lTempban;
        }
        final Calendar today = Calendar.getInstance();
        lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
        if (today.getTimeInMillis() < lTempban.getTimeInMillis()) {
            return lTempban;
        }

        lTempban.setTimeInMillis(0);
        return lTempban;
    }

    public final Calendar getTempBanCalendar() {
        return tempban;
    }

    public final void empty() {
        if(this.player != null) {
            this.player.getMount().empty();
            this.player.empty();
            if (this.player.getEventInstance() != null) {
                this.player.getEventInstance().removePlayer(player);
            }
            this.player.empty();
        }
        this.player = null;
        this.session = null;
        this.engines.clear();
        this.engines = null;
        this.send = null;
        this.receive = null;
        this.channel = -1;
    }
}

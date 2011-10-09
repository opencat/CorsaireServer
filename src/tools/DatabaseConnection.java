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

package tools;

import constants.ServerConstants;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.HashMap;

/**
 * @name        DatabaseConnection
 * @author      javadoc
 */
public class DatabaseConnection {
    private static final HashMap<Integer, ConWrapper> connections = new HashMap();
    private static boolean propsInited = false;
    private static long connectionTimeOut = 5 * 60 * 1000;

    private DatabaseConnection() {}

    public static Connection getConnection() {
        Thread cThread = Thread.currentThread();
        int threadID = (int) cThread.getId();
        ConWrapper ret = connections.get(threadID);

        if (ret == null) {
            Connection retCon = connectToDB();
            ret = new ConWrapper(retCon);
            ret.id = threadID;
            connections.put(threadID, ret);
        }

        return ret.getConnection();
    }

    private static long getWaitTimeout(Connection con) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery("SHOW VARIABLES LIKE 'wait_timeout'");
            if (rs.next()) {
                return Math.max(1000, rs.getInt(2) * 1000 - 1000);
            } else {
                return -1;
            }
        } catch (SQLException ex) {
            return -1;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                } finally {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (SQLException ex1) {}
                    }
                }
            }
        }
    }

    private static Connection connectToDB() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Connection con = DriverManager.getConnection(ServerConstants.DATABASE_URL, ServerConstants.DATABASE_USER, ServerConstants.DATABASE_PASS);
            if (!propsInited) {
                long timeout = getWaitTimeout(con);
                if (timeout != -1) {
                    connectionTimeOut = timeout;
                }
                propsInited = true;

            }
            return con;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    static class ConWrapper {
        private long lastAccessTime = 0;
        private Connection connection;
        private int id;

        public ConWrapper(Connection con) {
            this.connection = con;
        }

        public Connection getConnection() {
            if (expiredConnection()) {
                try {
                    connection.close();
                } catch (Throwable err) {
                }
                this.connection = connectToDB();
            }

            lastAccessTime = System.currentTimeMillis();
            return this.connection;
        }

        public boolean expiredConnection() {
            if (lastAccessTime == 0) {
                return false;
            }
            try {
                return System.currentTimeMillis() - lastAccessTime >= connectionTimeOut || connection.isClosed();
            } catch (Throwable ex) {
                return true;
            }
        }
    }

    public static void closeAll() throws SQLException {
        for (ConWrapper con : connections.values()) {
            con.connection.close();
        }
        connections.clear();
    }
}
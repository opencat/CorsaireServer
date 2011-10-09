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

package net.login;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import tools.DatabaseConnection;
import java.util.HashMap;
import net.MapleServerHandler;
import net.ServerMode;
import net.ServerMode.Mode;
import net.ServerType;
import net.cashshop.CashShopServer;
import net.mina.MapleCodecFactory;
import net.world.remote.WorldLoginInterface;
import net.world.remote.WorldRegistry;
import server.TimerManager;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * @name        LoginServer
 * @author      Matze
 *              Modified by x711Li
 */
public class LoginServer implements Runnable {
    private IoAcceptor acceptor;
    private static WorldRegistry worldRegistry = null;
    private static Map<Integer, Map<Integer, String>> channelServer = new HashMap<Integer, Map<Integer, String>>();
    private LoginWorldInterface lwi;
    private WorldLoginInterface wli;
    private Boolean worldReady = Boolean.TRUE;
    private Properties subnetInfo = new Properties();
    private static LoginServer instance = new LoginServer();

    public static LoginServer getInstance() {
        return instance;
    }

    public void addChannel(int world, int channel, String ip) {
        if (!channelServer.containsKey(world)) {
            channelServer.put(world, new HashMap<Integer, String>());
        }
        channelServer.get(world).put(channel, ip);
    }

    public void removeChannel(int channel) {
        channelServer.remove(channel);
    }

    @Override
    public void run() {
        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
            worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
            lwi = new LoginWorldInterfaceImpl();
            wli = worldRegistry.registerLoginServer("releaselogin", lwi);
            Statement stmt = DatabaseConnection.getConnection().createStatement();
            stmt.addBatch("UPDATE accounts SET loggedin = 0");
            stmt.addBatch("UPDATE characters SET hasmerchant = 0");
            stmt.executeBatch();
            stmt.close();
        } catch (RemoteException e) {
            throw new RuntimeException("Could not connect to world server.", e);
        } catch (NotBoundException ex) {
        } catch (SQLException sql) {
        }
        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());
        acceptor = new NioSocketAcceptor();
        ((SocketSessionConfig) acceptor.getSessionConfig()).setTcpNoDelay(true);
        acceptor.getFilterChain().addLast("codec", (IoFilter) new ProtocolCodecFilter(new MapleCodecFactory()));
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
        acceptor.setHandler(new MapleServerHandler(ServerType.LOGIN));
        try {
            acceptor.bind(new InetSocketAddress(8484));
            System.out.println("Login: Listening on port 8484");
        } catch (IOException ex) {
        }
    }

    public void shutdown() {
        try {
            worldRegistry.deregisterLoginServer(lwi);
        } catch (RemoteException e) {
        }
        TimerManager.getInstance().stop();
        System.out.println("Login Server offline.");
        System.exit(0);
    }

    public WorldLoginInterface getWorldInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return wli;
    }

    public static void main(String args[]) {
        ServerMode.setServerMode(Mode.LOGIN);
        LoginServer.getInstance().run();
        CashShopServer.getInstance().run();
    }

    public Properties getSubnetInfo() {
        return subnetInfo;
    }

    public String getIP(int world, int channel) {
        return channelServer.get(world).get(channel);
    }
}

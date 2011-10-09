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

package net.cashshop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import net.MapleServerHandler;
import net.ServerType;
import tools.DatabaseConnection;
import net.mina.MapleCodecFactory;
import net.world.remote.WorldRegistry;
import net.cashshop.remote.CashShopWorldInterface;
import net.channel.PlayerStorage;
import net.world.remote.WorldCashShopInterface;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import server.TimerManager;
import server.MapleItemInformationProvider;
import server.CashItemFactory;

/**
 * @name        CashShopServer
 * @author      x711Li
 */
public class CashShopServer {
    private String ip;
    private final int port = 6767;
    private IoAcceptor acceptor;
    private CashShopWorldInterface lwi;
    private WorldCashShopInterface wli;
    private Boolean worldReady = Boolean.TRUE;
    private WorldRegistry worldRegistry = null;
    private PlayerStorage shoppers;
    private static final CashShopServer instance = new CashShopServer();

    public static final CashShopServer getInstance() {
        return instance;
    }

    public final void register() {
        try {
            ip = "127.0.0.1";
            final Registry registry = LocateRegistry.getRegistry(ip, Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
            ip += ":" + port;
            worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
            lwi = new CashShopWorldInterfaceImpl(this);
            wli = worldRegistry.registerCashShopServer("release1", ip, lwi);
            DatabaseConnection.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not connect to world server.", e);
        }
    }

    public final void reconnectWorld() {
        try {
            wli.isAvailable();
        } catch (RemoteException ex) {
            synchronized (worldReady) {
                worldReady = Boolean.FALSE;
            }
            synchronized (lwi) {
                synchronized (worldReady) {
                    if (worldReady) {
                        return;
                    }
                }
                System.out.println("Reconnecting to world server");
                synchronized (wli) {
                    register();
                    worldReady = Boolean.TRUE;
                }
            }
            synchronized (worldReady) {
                worldReady.notifyAll();
            }
        }
    }

    public final void run() {
        register();
        TimerManager.getInstance().start();
        CashItemFactory.getInstance();
        MapleItemInformationProvider.getInstance();
        shoppers = new PlayerStorage();
        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());
        acceptor = new NioSocketAcceptor();
        ((SocketSessionConfig) acceptor.getSessionConfig()).setTcpNoDelay(true);
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
        acceptor.setHandler(new MapleServerHandler(ServerType.CASHSHOP));
        try {
            acceptor.bind(new InetSocketAddress(port));
            System.out.println("CashShop: Listening on port " + port);
        } catch (final IOException e) {
            System.err.println("Binding to port " + port + " failed" + e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutDownListener()));
    }

    public static void start() {
        try {
            CashShopServer.instance.run();
        } catch (final Exception ex) {
            System.err.println("Error initializing Cash Shop server" + ex);
        }
    }

    public final String getIP() {
        return ip;
    }

    public final PlayerStorage getShopperStorage() {
        return shoppers;
    }

    public final WorldCashShopInterface getCashShopInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (final InterruptedException e) {
                }
            }
        }
        return wli;
    }

    public final void deregister() {
        System.out.println("Shutting down...");
        try {
            worldRegistry.deregisterCashShopServer();
        } catch (final RemoteException e) {
        }
        System.exit(0);
    }

    public final void shutdown() {
        System.out.println("Saving all connected clients...");
        shoppers.disconnectAll();
        System.exit(0);
    }

    private final class ShutDownListener implements Runnable {
        @Override
        public void run() {
            System.out.println("Saving all connected clients...");
            shoppers.disconnectAll();
        }
    }
}

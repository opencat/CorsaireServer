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

package net.channel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MalformedObjectNameException;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import client.MapleCharacter;
import constants.ServerConstants;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map.Entry;
import net.world.MapleParty;
import tools.DatabaseConnection;
import java.util.HashMap;
import net.MaplePacket;
import net.MapleServerHandler;
import net.ServerMode;
import net.ServerMode.Mode;
import net.ServerType;
import net.channel.remote.ChannelWorldInterface;
import net.mina.MapleCodecFactory;
import net.world.MaplePartyCharacter;
import net.world.guild.MapleGuild;
import net.world.guild.MapleGuildCharacter;
import net.world.guild.MapleGuildSummary;
import net.world.remote.WorldChannelInterface;
import net.world.remote.WorldRegistry;
import scripting.event.EventScriptManager;
import server.ShutdownServer;
import server.TimerManager;
import server.maps.MapleMapFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import server.MakerItemFactory;
import server.MapleItemInformationProvider;
import server.maps.MapleMap;
import tools.factory.EffectFactory;

/**
 * @name        ChannelServer
 * @author      Matze
 *              Modified by x711Li
 */
public class ChannelServer implements Runnable {
    private int port = 7575;
    private static WorldRegistry worldRegistry;
    private PlayerStorage players;
    private int channel;
    private String key;
    private ChannelWorldInterface cwi;
    private WorldChannelInterface wci = null;
    private IoAcceptor acceptor;
    private String ip;
    private boolean shutdown = false;
    private boolean shuttingdown = false;
    private boolean finishedShutdown = false;
    private final MapleMapFactory mapFactory = new MapleMapFactory();
    private EventScriptManager eventSM;
    private static final Map<Integer, ChannelServer> instances = new HashMap<Integer, ChannelServer>();
    private static final Map<String, ChannelServer> pendingInstances = new HashMap<String, ChannelServer>();
    private Map<Integer, MapleGuildSummary> gsStore = new HashMap<Integer, MapleGuildSummary>();
    private Boolean worldReady = true;
    private int instanceId = 0;
    private static int world = 0;
    private HiredMerchantRegistry HMRegistry = new HiredMerchantRegistry(channel);

    private ChannelServer(final String key) {
        this.key = key;
    }

    public static final WorldRegistry getWorldRegistry() {
        return worldRegistry;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public void addInstanceId() {
        instanceId++;
    }

    public void setShuttingDown() {
        shuttingdown = true;
    }

    public boolean isShuttingDown() {
        return shuttingdown;
    }

    public final void reconnectWorld() {
        reconnectWorld(false);
    }
    
    public final void reconnectWorld(boolean force) {
        if(!force) {
            try {
            wci.isAvailable();
            } catch (RemoteException ex) {
                synchronized (worldReady) {
                    worldReady = false;
                }
            }
            synchronized (cwi) {
                synchronized (worldReady) {
                    if (worldReady && !force) {
                        return;
                    }
                }
                System.out.println("Reconnecting to world server");
                synchronized (wci) {
                    try {
                        final Registry registry = LocateRegistry.getRegistry(ServerConstants.HOST, Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
                        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
                        cwi = new ChannelWorldInterfaceImpl(this, world);
                        wci = worldRegistry.registerChannelServer(key, cwi, world);
                        DatabaseConnection.getConnection();
                        wci.serverReady(world);
                    } catch (Exception e) {
                    }
                    worldReady = true;
                }
            }
            synchronized (worldReady) {
                worldReady.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        try {
            cwi = new ChannelWorldInterfaceImpl(this, world);
            wci = worldRegistry.registerChannelServer(key, cwi, world);
            eventSM = new EventScriptManager(this, ServerConstants.EVENTS.split(" "));
            port = 7575 + this.world * 100 + this.channel - 1;
            ip = ServerConstants.HOST + ":" + port;
            IoBuffer.setUseDirectBuffer(false);
            IoBuffer.setAllocator(new SimpleBufferAllocator());
            TimerManager tMan = TimerManager.getInstance();
            tMan.start();
            tMan.register(new Runnable() {
                @Override
                public void run() {
                    TimerManager.getInstance().purgeTM();
                }
            }, 100000);
            final ChannelServer cserv = this;
            tMan.register(new Runnable() {
                @Override
                public void run() {
                    cserv.yellowWorldMessage("[MapleTip] " + ServerConstants.MAPLE_TIPS[(int) Math.floor(Math.random() * ServerConstants.MAPLE_TIPS.length)]);
                }
            }, 300000);
            MakerItemFactory.getInstance();
            MapleItemInformationProvider.getInstance();
            players = new PlayerStorage();
            acceptor = new NioSocketAcceptor();
            ((SocketSessionConfig) acceptor.getSessionConfig()).setTcpNoDelay(true);
            acceptor.getFilterChain().addLast("codec", (IoFilter) new ProtocolCodecFilter(new MapleCodecFactory()));
            acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
            acceptor.setHandler(new MapleServerHandler(ServerType.CHANNEL, channel));
            try {
                acceptor.bind(new InetSocketAddress(port));
                System.out.println("Channel " + getChannel() + ": Listening on port " + port);
            } catch (IOException ex) {
            }
            wci.serverReady(world);
            eventSM.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final void shutdown() {
        shutdown = true;
        boolean error = true;
        while (error) {
            try {
                for (MapleCharacter chr : players.getAllCharacters()) {
                    synchronized (chr) {
                        if (chr.getHiredMerchant().isOpen()) {
                            chr.getHiredMerchant().saveItems();
                        }
                        chr.getClient().disconnect(true, false);
                    }
                    error = false;
                }
            } catch (Exception e) {
                error = true;
            }
        }
        finishedShutdown = true;
        wci = null;
        cwi = null;
    }

    public final void unbind() {
        acceptor.unbind();
    }

    public final boolean hasFinishedShutdown() {
        return finishedShutdown;
    }

    public final MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    private static final ChannelServer newInstance(String key, int world) throws InstanceAlreadyExistsException, MalformedObjectNameException {
        ChannelServer instance = new ChannelServer(key);
        instance.world = world;
        pendingInstances.put(key, instance);
        return instance;
    }

    public static final ChannelServer getInstance(int channel) {
        return instances.get(channel);
    }

    public final void addPlayer(MapleCharacter chr) {
        players.registerPlayer(chr);
        chr.getClient().announce(EffectFactory.serverMessage(ServerConstants.SERVER_MESSAGE));
        try {
            if (worldRegistry.getEvent() != null) {
                chr.dropMessage(worldRegistry.getEvent().getHost() + "'s Event, " + worldRegistry.getEvent().getDescription() + ", is running now! Type @event to attend.");
            }
        } catch (Exception e) {
            reconnectWorld();
            e.printStackTrace();
        }
    }

    public final PlayerStorage getPlayerStorage() {
        return players;
    }

    public final void removePlayer(MapleCharacter chr) {
        players.deregisterPlayer(chr);
    }

    public final int getConnectedClients() {
        return players.getAllCharacters().size();
    }

    public void setServerMessage(String newMessage) {
        ServerConstants.SERVER_MESSAGE = newMessage;
        broadcastPacket(EffectFactory.serverMessage(ServerConstants.SERVER_MESSAGE));
    }

    public final void broadcastPacket(final MaplePacket data) {
        players.broadcastPacket(data);
    }

    public final void broadcastGMPacket(final MaplePacket data) {
        players.broadcastGMPacket(data);
    }

    public final void broadcastNONGMPacket(final MaplePacket data) {
        players.broadcastNONGMPacket(data);
    }

    public final void broadcastAnnouncementPacket(final MaplePacket data) {
    players.broadcastAnnouncementPacket(data);
    }

    public final int getChannel() {
        return channel;
    }

    public final void setChannel(int channel) {
        if (pendingInstances.containsKey(key)) {
            pendingInstances.remove(key);
        }
        if (instances.containsKey(channel)) {
            instances.remove(channel);
        }
        instances.put(channel, this);
        this.channel = channel;
        this.mapFactory.setChannel(channel);
    }

    public static final Collection<ChannelServer> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public final String getIP() {
        return ip;
    }

    public final String getIP(final int channel) {
        try {
            return getWorldInterface().getIP(channel);
        } catch (RemoteException e) {
            System.out.println("Lost connection to world server " + e);
            reconnectWorld();
            throw new RuntimeException("Lost connection to world server");
        }
    }

    public final WorldChannelInterface getWorldInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return wci;
    }
    
    public final boolean isShutdown() {
        return shutdown;
    }

    public final void shutdown(int time) {
        TimerManager.getInstance().schedule(new ShutdownServer(getChannel(), getWorld()), time);
    }

    public EventScriptManager getEventSM() {
        return eventSM;
    }

    public final MapleGuild getGuild(final MapleGuildCharacter mgc) {
        final int gid = mgc.getGuildId();
        MapleGuild g = null;
        try {
            g = this.getWorldInterface().getGuild(gid, mgc);
        } catch (RemoteException re) {
            System.out.println("RemoteException while fetching MapleGuild. " + re);
            reconnectWorld();
            re.printStackTrace();
            return null;
        }
        if (gsStore.get(gid) == null) {
            gsStore.put(gid, new MapleGuildSummary(g));
        }
        return g;
    }

    public final MapleGuildSummary getGuildSummary(final int gid) {
        if (gsStore.containsKey(gid)) {
            return gsStore.get(gid);
        } else {
            try {
                final MapleGuild g = this.getWorldInterface().getGuild(gid, null);
                if (g != null) {
                    gsStore.put(gid, new MapleGuildSummary(g));
                }
                return gsStore.get(gid);
            } catch (RemoteException re) {
                System.out.println("RemoteException while fetching GuildSummary. " + re);
                return null;
            }
        }
    }

    public final void updateGuildSummary(final int gid, final MapleGuildSummary mgs) {
        gsStore.put(gid, mgs);
    }

    public void reloadGuildSummary() {
        try {
            MapleGuild g;
            for (int i : gsStore.keySet()) {
                g = this.getWorldInterface().getGuild(i, null);
                if (g != null) {
                    gsStore.put(i, new MapleGuildSummary(g));
                } else {
                    gsStore.remove(i);
                }
            }
        } catch (RemoteException re) {
            System.out.println("RemoteException while reloading GuildSummary." + re);
            this.reconnectWorld();
        }
    }

    public static void main(String args[]) throws FileNotFoundException, IOException, NotBoundException, InstanceAlreadyExistsException, MalformedObjectNameException {
        ServerMode.setServerMode(Mode.CHANNEL);
        final Registry registry = LocateRegistry.getRegistry("localhost", Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
        for (int i = 0; i < ServerConstants.NUM_WORLDS; i++) {
            for (int j = 0; j < ServerConstants.NUM_CHANNELS; j++) {
                newInstance("release" + (j + 1 + i * ServerConstants.NUM_CHANNELS), i).run();
            }
        }
        DatabaseConnection.getConnection();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (ChannelServer channel : getAllInstances()) {
                    for (MapleCharacter mc : channel.getPlayerStorage().getAllCharacters()) {
                        mc.saveToDB(true);
                        if (mc.getHiredMerchant() != null) {
                            if (mc.getHiredMerchant().isOpen()) {
                                try {
                                    mc.getHiredMerchant().saveItems();
                                } catch (SQLException e) {
                                    System.out.println(mc.getName() + "'s merchant failed to save.");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    public final void yellowWorldMessage(final String msg) {
        for (MapleCharacter mc : getPlayerStorage().getAllCharacters()) {
            mc.getClient().announce(EffectFactory.sendYellowTip(msg));
        }
    }

    public final void worldMessage(final String msg) {
        for (MapleCharacter mc : getPlayerStorage().getAllCharacters()) {
            mc.dropMessage(msg);
        }
    }

    public List<MapleCharacter> getPartyMembers(final MapleParty party, int map) {
        List<MapleCharacter> partym = new ArrayList<MapleCharacter>(6);
        for (final MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getChannel()) {
                if (map > 0 && partychar.getMapid() != map) {
                    continue;
                }
                MapleCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;
    }

    public final List<MapleCharacter> getPartyMembers(final MapleParty party) {
        List<MapleCharacter> partym = new LinkedList<MapleCharacter>();
        for (final MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getChannel()) {
                MapleCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;
    }

    public class respawnMaps implements Runnable {
        @Override
        public void run() {
            for (Entry<Integer, MapleMap> map : mapFactory.getMaps().entrySet()) {
                map.getValue().respawn(false);
            }
        }
    }

    public static MapleCharacter getCharacterFromAllServers(int id) {
        for (ChannelServer cserv_ : ChannelServer.getAllInstances()) {
            MapleCharacter ret = cserv_.getPlayerStorage().getCharacterById(id);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public int getWorld() {
        return world;
    }

    public final void reloadEvents() {
        eventSM.cancel();
        eventSM = new EventScriptManager(this, ServerConstants.EVENTS.split(" "));
        eventSM.init();
    }

    public HiredMerchantRegistry getHMRegistry() {
        return this.HMRegistry;
    }
}

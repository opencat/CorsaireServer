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

package net.world;

import constants.ServerConstants;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import net.MaplePacket;
import net.login.LoginWorldInterface;
import net.world.guild.MapleAlliance;
import net.world.guild.MapleGuild;
import net.world.guild.MapleGuildCharacter;
import net.world.remote.WorldChannelInterface;
import net.world.remote.WorldLocation;
import java.util.HashMap;
import net.channel.remote.ChannelWorldInterface;

/**
 * @name        WorldChannelInterfaceImpl
 * @author      Matze
 *              Modified by x711Li
 */
public class WorldChannelInterfaceImpl extends UnicastRemoteObject implements WorldChannelInterface {
    private static final long serialVersionUID = -5948361439191412565L;
    private ChannelWorldInterface cb;
    private int world;

    public WorldChannelInterfaceImpl(ChannelWorldInterface cb, int dbId, int world) throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        this.cb = cb;
        this.world = world;
    }

    public void serverReady(int world) throws RemoteException {
        for (LoginWorldInterface wli : WorldRegistryImpl.getInstance().getLoginServer()) {
            try {
                wli.channelOnline(world, cb.getChannelId(), cb.getIP());
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterLoginServer(wli);
            } catch (Exception e) {
            }
        }
        System.out.println("World: " + world + " Channel: " + cb.getChannelId() + " is online");
    }

    public String getIP(int channel) throws RemoteException {
        final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(channel, world);
        if (cwi == null) {
            return "0.0.0.0:0";
        } else {
            try {
                return cwi.getIP();
            } catch (RemoteException e) {
                //WorldRegistryImpl.getInstance().deregisterChannelServer(channel, world);
                return "0.0.0.0:0";
            }
        }
    }

    public int getWorld() throws RemoteException {
        return world;
    }

    public void whisper(String sender, String target, int channel, String message) throws RemoteException {
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    cwi.whisper(sender, target, channel, message);
                } catch (RemoteException e) {
                }
            }
        }
    }
    
    public boolean isConnected(String charName) throws RemoteException {
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    if (cwi.isConnected(charName)) {
                        return true;
                    }
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
        return false;
    }
    
    public void broadcastMessage(String sender, byte[] message) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.broadcastMessage(sender, message);
            } catch (RemoteException e) {
                //WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public void broadcastMessageEx(String sender, byte[] message) throws RemoteException {
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    cwi.broadcastMessage(sender, message);
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
    }

    public void broadcastAnnouncement(byte[] message) throws RemoteException {
    for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
        final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
        try {
        cwi.broadcastAnnouncement(message);
        } catch (RemoteException e) {
        //WorldRegistryImpl.getInstance().deregisterChannelServer(i);
        }
    }
    }

    public void broadcastGMMessage(String sender, byte[] message) throws RemoteException {
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    cwi.broadcastGMMessage(sender, message);
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
    }

    public int find(String charName, boolean display) throws RemoteException {
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    if (cwi.isConnected(charName)) {
                        int ret = cwi.getChannelId();
                        if(display) {
                            ret -= j * ServerConstants.NUM_CHANNELS;//ServerConstants.NUM_CHANNELS;
                        }
                        return ret;
                    }
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
        return -1;
    }

    // can we generify this
    @Override
    public int find(int characterId, boolean display) throws RemoteException {
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    if (cwi.isConnected(characterId)) {
                        int ret = cwi.getChannelId();
                        if(display) {
                            ret -= j * ServerConstants.NUM_CHANNELS; //ServerConstants.NUM_CHANNELS;
                        }
                        return ret;
                    }
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
        return -1;
    }

    public void shutdown(int time) throws RemoteException {
        for (LoginWorldInterface lwi : WorldRegistryImpl.getInstance().getLoginServer()) {
            try {
                lwi.shutdown();
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterLoginServer(lwi);
            }
        }
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.shutdown(time);
            } catch (RemoteException e) {
                //WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public Map<Integer, Integer> getConnected() throws RemoteException {
        Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
        int total = 0;

        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                int curConnected = cwi.getConnected();
                ret.put(i, curConnected);
                total += curConnected;
            } catch (RemoteException e) {
                //WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
        ret.put(0, total);
        return ret;
    }

    public void loggedOn(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.loggedOn(name, characterId, channel, buddies);
            } catch (RemoteException e) {
                //WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    @Override
    public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.loggedOff(name, characterId, channel, buddies);
            } catch (RemoteException e) {
                //WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }
    public void updateParty(int partyid, PartyOperation operation, MaplePartyCharacter target) throws RemoteException {
        MapleParty party = WorldRegistryImpl.getInstance().getParty(partyid);
        if (party == null) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        switch (operation) {
        case JOIN:
            party.addMember(target);
            break;
        case EXPEL:
        case LEAVE:
            party.removeMember(target);
            break;
        case DISBAND:
            WorldRegistryImpl.getInstance().disbandParty(partyid);
            break;
        case SILENT_UPDATE:
        case LOG_ONOFF:
            party.updateMember(target);
            break;
        case CHANGE_LEADER:
            party.setLeader(target);
            break;
        default:
            throw new RuntimeException("Unhandeled updateParty operation " + operation.name());
        }
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    cwi.updateParty(party, operation, target);
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
    }

    public String getCashShopIP() throws RemoteException {
        return WorldRegistryImpl.getInstance().getCashShopIP();
    }

    public MapleParty createParty(MaplePartyCharacter chrfor) throws RemoteException {
        return WorldRegistryImpl.getInstance().createParty(chrfor);
    }

    public MapleParty getParty(int partyid) throws RemoteException {
        return WorldRegistryImpl.getInstance().getParty(partyid);
    }

    @Override
    public void partyChat(int partyid, String chattext, String namefrom) throws RemoteException {
        MapleParty party = WorldRegistryImpl.getInstance().getParty(partyid);
        if (party == null) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    cwi.partyChat(party, chattext, namefrom);
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
    }

    public boolean isAvailable() throws RemoteException {
        return true;
    }

    public WorldLocation getLocation(String charName) throws RemoteException {
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    if (cwi.isConnected(charName)) {
                        return new WorldLocation(cwi.getLocation(charName), cwi.getChannelId());
                    }
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
        return null;
    }

    @Override
    public ChannelWorldInterface getChannelInterface(int channel) {
        ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(channel, world);
        return cwi;
    }

    @Override
    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException {
        for (ChannelWorldInterface cwi : WorldRegistryImpl.getInstance().getAllChannelServers(world)) {
            cwi.buddyChat(recipientCharacterIds, cidFrom, nameFrom, chattext);
        }
    }

    @Override
    public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException {
        List<CharacterIdChannelPair> foundsChars = new ArrayList<CharacterIdChannelPair>(characterIds.length);
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            for (int charid : cwi.multiBuddyFind(charIdFrom, characterIds)) {
                foundsChars.add(new CharacterIdChannelPair(charid, i));
            }
        }
        return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
    }

    @Override
    public MapleGuild getGuild(int id, MapleGuildCharacter mgc) throws RemoteException {
        return WorldRegistryImpl.getInstance().getGuild(id, mgc, world);
    }

    @Override
    public void setGuildMemberOnline(MapleGuildCharacter mgc, boolean bOnline, int channel) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildMemberOnline(mgc, bOnline, channel, world);
    }

    @Override
    public int addGuildMember(MapleGuildCharacter mgc) throws RemoteException {
        return WorldRegistryImpl.getInstance().addGuildMember(mgc);
    }

    @Override
    public void guildChat(int gid, String name, int cid, String msg)
    throws RemoteException {
        WorldRegistryImpl.getInstance().guildChat(gid, name, cid, msg);
    }

    @Override
    public void leaveGuild(MapleGuildCharacter mgc) throws RemoteException {
        WorldRegistryImpl.getInstance().leaveGuild(mgc);
    }

    @Override
    public void changeRank(int gid, int cid, int newRank) throws RemoteException {
        WorldRegistryImpl.getInstance().changeRank(gid, cid, newRank);
    }

    @Override
    public void expelMember(MapleGuildCharacter initiator, String name, int cid) throws RemoteException {
        WorldRegistryImpl.getInstance().expelMember(initiator, name, cid);
    }

    @Override
    public void setGuildNotice(int gid, String notice) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildNotice(gid, notice);
    }

    @Override
    public void memberLevelJobUpdate(MapleGuildCharacter mgc) throws RemoteException {
        WorldRegistryImpl.getInstance().memberLevelJobUpdate(mgc);
    }

    @Override
    public void changeRankTitle(int gid, String[] ranks) throws RemoteException {
        WorldRegistryImpl.getInstance().changeRankTitle(gid, ranks);
    }

    @Override
    public int createGuild(int leaderId, String name) throws RemoteException {
        return WorldRegistryImpl.getInstance().createGuild(leaderId, name);
    }

    @Override
    public void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildEmblem(gid, bg, bgcolor, logo, logocolor);
    }

    @Override
    public void disbandGuild(int gid) throws RemoteException {
        WorldRegistryImpl.getInstance().disbandGuild(gid);
    }

    @Override
    public boolean increaseGuildCapacity(int gid) throws RemoteException {
        return WorldRegistryImpl.getInstance().increaseGuildCapacity(gid);
    }

    @Override
    public void gainGP(int gid, int amount) throws RemoteException {
        WorldRegistryImpl.getInstance().gainGP(gid, amount);
    }

    public MapleMessenger createMessenger(MapleMessengerCharacter chrfor) throws RemoteException {
        return WorldRegistryImpl.getInstance().createMessenger(chrfor);
    }

    public MapleMessenger getMessenger(int messengerid) throws RemoteException {
        return WorldRegistryImpl.getInstance().getMessenger(messengerid);
    }

    public void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException {
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    cwi.messengerInvite(sender, messengerid, target, fromchannel);
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
    }

    public void leaveMessenger(int messengerid, MapleMessengerCharacter target) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        int position = messenger.getPositionByName(target.getName());
        messenger.removeMember(target);
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    cwi.removeMessengerPlayer(messenger, position);
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
    }

    public void joinMessenger(int messengerid, MapleMessengerCharacter target, String from, int fromchannel) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target);

        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    cwi.addMessengerPlayer(messenger, from, fromchannel, target.getPosition());
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
    }

    public void messengerChat(int messengerid, String chattext, String namefrom) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    cwi.messengerChat(messenger, chattext, namefrom);
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
    }

    public void declineChat(String target, String namefrom) throws RemoteException {
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    cwi.declineChat(target, namefrom);
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
    }

    public void updateMessenger(int messengerid, String namefrom, int fromchannel) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        int position = messenger.getPositionByName(namefrom);
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    cwi.updateMessenger(messenger, namefrom, position, fromchannel);
                } catch (RemoteException e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
    }

    public void silentLeaveMessenger(int messengerid, MapleMessengerCharacter target) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentRemoveMember(target);
    }

    public void silentJoinMessenger(int messengerid, MapleMessengerCharacter target, int position) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentAddMember(target, position);
    }

    public void addBuffsToStorage(int chrid, List<PlayerBuffValueHolder> toStore) throws RemoteException {
        WorldRegistryImpl.getInstance().getPlayerBuffStorage().addBuffsToStorage(chrid, toStore);
    }

    public void addCooldownsToStorage(int chrid, List<PlayerCoolDownValueHolder> toStore) throws RemoteException {
        WorldRegistryImpl.getInstance().getPlayerBuffStorage().addCooldownsToStorage(chrid, toStore);
    }

    public void addDiseaseToStorage(int chrid, List<PlayerDiseaseValueHolder> toStore) throws RemoteException {
        WorldRegistryImpl.getInstance().getPlayerBuffStorage().addDiseaseToStorage(chrid, toStore);
    }

    public void sendSpouseChat(String sender, String target, String message) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.sendSpouseChat(sender, target, message);
            } catch (Exception e) {
                //WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    @Override
    public MapleAlliance getAlliance(int id) throws RemoteException {
        return WorldRegistryImpl.getInstance().getAlliance(id);
    }

    @Override
    public void addAlliance(int id, MapleAlliance alliance) throws RemoteException {
        WorldRegistryImpl.getInstance().addAlliance(id, alliance);
    }

    @Override
    public void disbandAlliance(int id) throws RemoteException {
        WorldRegistryImpl.getInstance().disbandAlliance(id);
    }

    @Override
    public void allianceMessage(int id, MaplePacket packet, int exception, int guildex) throws RemoteException {
        WorldRegistryImpl.getInstance().allianceMessage(id, packet, exception, guildex);
    }

    @Override
    public boolean setAllianceNotice(int aId, String notice) throws RemoteException {
        return WorldRegistryImpl.getInstance().setAllianceNotice(aId, notice);
    }

    @Override
    public boolean setAllianceRanks(int aId, String[] ranks) throws RemoteException {
        return WorldRegistryImpl.getInstance().setAllianceRanks(aId, ranks);
    }

    @Override
    public boolean removeGuildFromAlliance(int aId, int guildId) throws RemoteException {
        return WorldRegistryImpl.getInstance().removeGuildFromAlliance(aId, guildId);
    }

    @Override
    public boolean addGuildtoAlliance(int aId, int guildId) throws RemoteException {
        return WorldRegistryImpl.getInstance().addGuildtoAlliance(aId, guildId);
    }

    @Override
    public boolean setGuildAllianceId(int gId, int aId) throws RemoteException {
        return WorldRegistryImpl.getInstance().setGuildAllianceId(gId, aId);
    }

    @Override
    public boolean increaseAllianceCapacity(int aId, int inc) throws RemoteException {
        return WorldRegistryImpl.getInstance().increaseAllianceCapacity(aId, inc);
    }

    @Override
    public void channelChange(CharacterTransfer Data, int characterid, int toChannel) throws RemoteException {
        if (toChannel != -10) {
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
                if (i == toChannel) {
                    final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
                    try {
                        cwi.channelChange(Data, characterid);
                    } catch (RemoteException e) {
                        //WorldRegistryImpl.getInstance().deregisterChannelServer(i);
                    }
                }
            }
        } else {
            WorldRegistryImpl.getInstance().getCashShopServer().channelChange(Data, characterid);
        }
    }

    public List<PlayerBuffValueHolder> getBuffsFromStorage(int chrid) throws RemoteException {
        return WorldRegistryImpl.getInstance().getPlayerBuffStorage().getBuffsFromStorage(chrid);
    }

    public List<PlayerCoolDownValueHolder> getCooldownsFromStorage(int chrid) throws RemoteException {
        return WorldRegistryImpl.getInstance().getPlayerBuffStorage().getCooldownsFromStorage(chrid);
    }

    public List<PlayerDiseaseValueHolder> getDiseaseFromStorage(int chrid) throws RemoteException {
        return WorldRegistryImpl.getInstance().getPlayerBuffStorage().getDiseaseFromStorage(chrid);
    }
    
    public String getAllPlayerNames(int world) throws RemoteException {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                try {
                    final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                    sb.append(cwi.getAllPlayerNames());
                } catch (Exception e) {
                    //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
        return sb.toString();
    }

    public String getAllScania() throws RemoteException {
        StringBuilder sb = new StringBuilder();
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(0)) {
            try {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, 0);
                sb.append(cwi.getAllPlayerNames());
            } catch (Exception e) {
                //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
            }
        }
        return sb.toString();
    }

    public String getAllBera() throws RemoteException {
        StringBuilder sb = new StringBuilder();
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(1)) {
            try {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, 1);
                sb.append(cwi.getAllPlayerNames());
            } catch (Exception e) {
                //WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
            }
        }
        return sb.toString();
    }

    public boolean isCharacterListConnected(List<String> charName) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                if (cwi.isCharacterListConnected(charName)) {
                    return true;
                }
            } catch (RemoteException e) {
                //WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
        return false;
    }
}

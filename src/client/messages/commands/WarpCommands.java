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

package client.messages.commands;

import client.MapleCharacter;
import client.MapleClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import constants.ServerConstants;
import java.net.InetAddress;
import java.rmi.RemoteException;
import net.MaplePacket;
import net.channel.ChannelServer;
import net.world.CharacterTransfer;
import net.world.remote.WorldLocation;
import server.MaplePortal;
import server.maps.MapleMap;
import tools.factory.InterServerFactory;

/**
 * @name        WarpCommands
 * @author      Matze
 *              Modified by x711Li
 */
public class WarpCommands implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
    IllegalCommandSyntaxException {
        ChannelServer cserv = c.getChannelServer();
        MapleCharacter player = c.getPlayer();
         if (splitted[0].equals("!warp")) {
            player.setBanCount(0);
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                if (splitted.length == 2) {
                    MapleMap target = victim.getMap();
                    player.changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
                }
            } else {
                try {
                    WorldLocation loc = null;
                    victim = player;
                    for (ChannelServer instance : ChannelServer.getAllInstances()) {
                        if(loc == null)
                        loc = instance.getWorldInterface().getLocation(splitted[1]);
                    }
                    if (loc != null) {
                        mc.dropMessage("You will be cross-channel warped. This may take a few seconds.");
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(loc.map);
                        String ip = "0.0.0.0:0";
                        if(ChannelServer.getInstance(ServerConstants.NUM_CHANNELS + 1) != null) {
                            ip = ChannelServer.getInstance(ServerConstants.NUM_CHANNELS + 1).getIP(loc.channel);
                        }
                        if(ip.equals("0.0.0.0:0")) {
                            ip = ChannelServer.getInstance(1).getIP(loc.channel);
                            if (ip.equals("0.0.0.0:0")) {
                                return;
                            }
                        }
                        try {
                            c.getChannelServer().getWorldInterface().channelChange(new CharacterTransfer(player), player.getId(), loc.channel);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            c.getChannelServer().reconnectWorld();
                        }
                        player.saveToDB(true);
                        player.getMap().removePlayer(player);
                        victim.setMap(target);
                        String[] socket = ip.split(":");
                        ChannelServer.getInstance(c.getChannel()).removePlayer(player);
                        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
                        try {
                            MaplePacket packet = InterServerFactory.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]));
                            c.announce(packet);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        int map = Integer.parseInt(splitted[1]);
                        MapleMap target = cserv.getMapFactory().getMap(map);
                        player.changeMap(target, target.getPortal(0));
                    }
                } catch (Exception e) {
                    mc.dropMessage("Something went wrong " + e.getMessage());
                }
            }
        } else if (splitted[0].equals("!warphere")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.changeMap(player.getMap(), player.getMap().findClosestSpawnpoint(player.getPosition()));
            }
        } else if (splitted[0].equals("!jail")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int mapid = 200090300; // mulung ride
            if (splitted.length > 2 && splitted[1].equals("2")) {
                mapid = 980000404; // exit for CPQ; not used
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[2]);
            }
            if (victim != null) {
                MapleMap target = cserv.getMapFactory().getMap(mapid);
                MaplePortal targetPortal = target.getPortal(0);
                victim.changeMap(target, targetPortal);
                mc.dropMessage(victim.getName() + " was jailed!");
            } else {
                mc.dropMessage(splitted[1] + " not found!");
            }
        } else if (splitted[0].equals("!warpmap")) {
            if((player.getMap().getId() == 280010010 || player.getMap().getId() == 280010020 || player.getMap().getId() == 280010030 || Integer.valueOf(splitted[1]) == 180000000 || (player.getMap().getId() >= 914000000 && player.getMap().getId() <= 914000500))) {
                mc.dropMessage("Restricted use of !warphere. Stored in GM Log.");
            } else {
                for (MapleCharacter chr : player.getMap().getCharacters()) {
                    chr.changeMap(c.getChannelServer().getMapFactory().getMap(Integer.valueOf(splitted[1])));
                }
            }
        } else {
            mc.dropMessage("GM Command " + splitted[0] + " does not exist");
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("warp", "<id/name>", "Warps to ID or player.", 1),
            new CommandDefinition("warphere", "<name>", "Warps player to you.", 2),
            new CommandDefinition("jail", "<name>", "Jails player.", 1),
            new CommandDefinition("warpmap", "<id>", "Warps map to ID.", 2)
        };
    }

}

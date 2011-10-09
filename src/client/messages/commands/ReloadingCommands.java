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

import client.MapleClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import net.channel.ChannelServer;
import server.maps.MapleMap;
import client.MapleCharacter;
import scripting.map.MapScriptManager;
import scripting.portal.PortalScriptManager;
import scripting.reactor.ReactorScriptManager;
import scripting.timer.TimerScriptManager;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.shops.MapleShopFactory;

/**
 * @name        ReloadCommands
 * @author      Various
 *              Modified by x711Li
 */
public class ReloadingCommands implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        if (splitted[0].equals("!reloadmap")) {
            if(c.getPlayer().gmLevel() < 3 && (c.getPlayer().getMap().getId() >= 910000001 && c.getPlayer().getMap().getId() <= 910000022)) {
                mc.dropMessage("Illegal use of !reloadmap logged.");
                return;
            }
            MapleMap oldMap = c.getPlayer().getMap();
            MapleMap newMap = c.getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId(), true, true, true, true, true);
            for (MapleCharacter ch : oldMap.getCharacters()) {
                ch.changeMap(newMap);
            }
            oldMap.empty();
            oldMap = null;
            c.getPlayer().getMap().respawn(false);
        } else if (splitted[0].equals("!reloadallmaps")) {
            for (MapleMap map : c.getChannelServer().getMapFactory().getMaps().values()) {
                MapleMap newMap = c.getChannelServer().getMapFactory().getMap(map.getId(), true, true, true, true, true);
                for (MapleCharacter ch : map.getCharacters()) {
                    ch.changeMap(newMap);
                }
                newMap.respawn(false);
                map.empty();
                map = null;
            }
        } else if (splitted[0].equals("!clearportalscripts")) {
            PortalScriptManager.getInstance().clearScripts();
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!clearmonsterdrops")) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!clearmonsterstats")) {
            MapleLifeFactory.getInstance().clearMonsterStats();
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!clearreactordrops")) {
            ReactorScriptManager.getInstance().clearDrops();
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!clearmapscripts")) {
            MapScriptManager.getInstance().clearScripts();
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!cleartimerscripts")) {
            TimerScriptManager.getInstance().clearScripts();
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!clearreactorscripts")) {
            ReactorScriptManager.getInstance().clearScripts();
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!clearshops")) {
            MapleShopFactory.getInstance().clear();
        } else if (splitted[0].equals("!clearmonster")) {
            MapleLifeFactory.getInstance().clear();
        } else if (splitted[0].equals("!clearitems")) {
            MapleItemInformationProvider.getInstance().clear();
        } else if (splitted[0].equals("!clearevents")) {
            for (ChannelServer instance : ChannelServer.getAllInstances()) {
                instance.reloadEvents();
            }
            mc.dropMessage("Done.");
        }
    }
    
    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("reloadmap", "", "Reload map.", 2),
            new CommandDefinition("reloadallmaps", "", "Reload all maps on channel.", 3),
            new CommandDefinition("clearportalscripts", "", "Clear portal script cache.", 3),
            new CommandDefinition("clearmonsterdrops", "", "Clear monster drop cache.", 3),
            new CommandDefinition("clearmonsterstats", "", "Clear monster stats cache.", 3),
            new CommandDefinition("clearreactordrops", "", "Clear reactor drop cache.", 3),
            new CommandDefinition("clearmapscripts", "", "Clear map script cache.", 3),
            new CommandDefinition("cleartimerscripts", "", "Clear timer script cache.", 3),
            new CommandDefinition("clearreactorscripts", "", "Clear reactor script cache.", 3),
            new CommandDefinition("clearshops", "", "Clear shop cache.", 3),
            new CommandDefinition("clearmonsters", "", "Clear monster stat cache.", 3),
            new CommandDefinition("clearitems", "", "Clear item stat cache.", 3),
            new CommandDefinition("clearevents", "", "Clear event cache.", 3)
        };
    }
}

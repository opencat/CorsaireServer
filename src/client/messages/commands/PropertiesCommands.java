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

import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.MessageCallback;
import client.MapleClient;
import client.messages.IllegalCommandSyntaxException;
import java.util.ArrayList;
import net.channel.ChannelServer;
import client.MapleCharacter;
import java.rmi.RemoteException;
import server.PropertiesTable;

/**
 * @name        PropertiesCommands
 * @author      Simon
 *              Modified by x711Li
 */
public class PropertiesCommands implements Command {

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        ChannelServer cserv = c.getChannelServer();
        if (splitted[0].equals("!set")) {
            ArrayList<String> propNames;
            if(splitted.length != 4 || !(splitted[3].equals("on") || splitted[3].equals("off")) || !(splitted[1].equals("world") || splitted[1].equals("map"))) {
                mc.dropMessage("Syntax helper: !set <map / world> <property> on / off");
                return;
            } else {
                boolean world = splitted[1].equals("world");
                try {
                    if (world) {
                        propNames = c.getChannelServer().getWorldRegistry().getPropertyNames();
                    } else {
                        propNames = c.getPlayer().getMap().getProperties().getPropertyNames();
                    }
                    if(propNames.contains(splitted[2])) {
                        if (world) {
                        c.getChannelServer().getWorldRegistry().setProperty(splitted[2], Boolean.valueOf(splitted[3].equals("on")));
                        } else {
                        c.getPlayer().getMap().getProperties().setProperty(splitted[2], Boolean.valueOf(splitted[3].equals("on")));
                        }
                        mc.dropMessage("Property " + splitted[2] + " now changed to: " + splitted[3]);
                    } else {
                        mc.dropMessage("Incorrect parameter. Current properties: ");
                        for(String s : propNames) {
                            mc.dropMessage(s);
                        }
                    }
                } catch (RemoteException re) {
                    cserv.reconnectWorld();
                }
            }
        } else if(splitted[0].equals("!get")) {
            if(splitted.length != 3 || !(splitted[1].equals("world") || splitted[1].equals("map"))) {
                mc.dropMessage("Syntax helper: !get <map / world> <property>");
                return;
            }
            boolean world = splitted[1].equals("world");
            try {
                ArrayList<String> propNames;
                Object value;
                if (world) {
                    propNames = c.getChannelServer().getWorldRegistry().getPropertyNames();
                    value = c.getChannelServer().getWorldRegistry().getProperty(splitted[2]);
                } else {
                    propNames = c.getPlayer().getMap().getProperties().getPropertyNames();
                    value = c.getPlayer().getMap().getProperties().getProperty(splitted[2]);
                }
                if(propNames.contains(splitted[2])) {
                    mc.dropMessage("Property " + splitted[2] + " has value: " + (value.equals(Boolean.TRUE) ? "on" : "off"));
                } else {
                    mc.dropMessage("Property not found, please try again or use !listproperties.");
                }
            } catch (RemoteException re) {
                re.printStackTrace();
                cserv.reconnectWorld();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(splitted[0].equals("!listproperties")) {
            boolean world = splitted[1].equals("world");
            if(splitted.length != 2 || !(splitted[1].equals("world") || splitted[1].equals("map"))) {
                mc.dropMessage("Syntax helper: !listproperties <map / world>");
                return;
            }
            try {
                ArrayList<String> propNames;
                Object value;
                if (world) {
                    propNames = c.getChannelServer().getWorldRegistry().getPropertyNames();
                } else {
                    propNames = c.getPlayer().getMap().getProperties().getPropertyNames();
                }
                for(String s : propNames) {
                    value = world ? c.getChannelServer().getWorldRegistry().getProperty(s) : c.getPlayer().getMap().getProperties().getProperty(s);
                    mc.dropMessage("Property " + s + " has value " + (value.equals(Boolean.TRUE) ? "on" : "off"));
                }
            } catch (RemoteException re) {
                cserv.reconnectWorld();
            }
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("set", "<map/world> <propertyname> <value>", "Sets the value of the specified property.", 3),
            new CommandDefinition("get", "<map/world> <propertyname>", "Gets the value of the specified property.", 1),
            new CommandDefinition("listproperties", "<map/world>", "Lists the available properties and their current values in the specified scope.", 1),
        };
    }
}

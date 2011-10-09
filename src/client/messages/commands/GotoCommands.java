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
import client.messages.CommandDefinition;
import client.messages.Command;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import server.MaplePortal;
import server.maps.MapleMap;
import client.MapleClient;
import net.channel.ChannelServer;

/**
 * @name        GotoCommands
 * @author      x711Li
 */
public class GotoCommands implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
    IllegalCommandSyntaxException {
        if (splitted[0].equals("!goto")) {
            // Map name array
            int[] gotomapid = { 180000000, 60000, 1010000, 100000000, 101000000, 102000000, 103000000, 103050000, 104000000, 105040300, 110000000, 200000000, 209000000, 211000000, 220000000, 230000000, 240000000, 250000000, 251000000, 221000000, 222000000, 600000000, 990000000, 801000000, 200000301, 800000000, 910000000, 260000100, 540010000, 610010004 };
            String[] gotomapname = { "gmmap", "southperry", "amherst", "henesys", "ellinia", "perion", "kerning", "secret", "lith", "sleepywood", "florina", "orbis", "happy", "elnath", "ludi", "aqua", "leafre", "mulung", "herb", "omega", "korean", "nlc", "excavation", "showa", "guild", "shrine", "fm", "ariant", "singapore", "crimson" };
            // Function
            if (splitted.length < 2) { // If no arguments, list options.
                StringBuilder builder = new StringBuilder("Syntax: !goto <mapname> where mapname is one of: ");
                for (int i = 0; i < gotomapname.length; i++) {
                    builder.append(gotomapname[i]);
                    builder.append(", ");
                }
                builder.setLength(builder.length() - 2);
                mc.dropMessage(builder.toString());
            } else {
                String mapname = splitted[1];
                for (int i = 0; gotomapid[i] != 0 && gotomapname[i] != null; ++i) { // for every array which isn't empty
                    if (mapname.equals(gotomapname[i])) { // If argument equals name
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(gotomapid[i]);
                        c.getPlayer().changeMap(target, target.getPortal(0));
                        break;
                    }
                }     
            }

        }
    }
    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("goto", "<name>", "Go to map. Type !goto only for a list.", 1)
        };
    }
}

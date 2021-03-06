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

import java.awt.Point;
import java.util.Arrays;
import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import server.life.MapleLifeFactory;
import server.life.MapleNPC;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.factory.NPCFactory;

/**
 * @name        NPCSpawningCommands
 * @author      x711Li
 */
public class NPCSpawningCommands implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
    IllegalCommandSyntaxException {
        if (splitted[0].equals("!npc")) {
            MapleNPC npc = MapleLifeFactory.getInstance().getNPC(Integer.parseInt(splitted[1]));
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(c.getPlayer().getPosition().y);
                npc.setRx0(c.getPlayer().getPosition().x + 50);
                npc.setRx1(c.getPlayer().getPosition().x - 50);
                npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(NPCFactory.spawnNPC(npc));
            } else {
                mc.dropMessage("You entered an invalid NPC id.");
            }
        } else if (splitted[0].equals("!removenpcs")) {
            MapleCharacter player = c.getPlayer();
            List<MapleMapObject> npcs = player.getMap().getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.NPC));
            for (MapleMapObject npcmo : npcs) {
                MapleNPC npc = (MapleNPC) npcmo;
                if (npc.isCustom()) {
                    c.getPlayer().getMap().broadcastMessage(NPCFactory.removeNPC(npc.getObjectId()));
                    player.getMap().removeMapObject(npc.getObjectId());
                }
            }
        } else if (splitted[0].equals("!mynpcpos")) {
            Point pos = c.getPlayer().getPosition();
            StringBuilder info = new StringBuilder("CY: ");
            info.append(pos.y);
            info.append(" | RX0: ");
            info.append(pos.x + 50);
            info.append(" | R: ");
            info.append(pos.x);
            info.append(" | RX1: ");
            info.append(pos.x - 50);
            info.append(" | FH: ");
            info.append(c.getPlayer().getMap().getFootholds().findBelow(pos).getId());
            mc.dropMessage(info.toString());
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("npc", "<id>", "Spawns NPC.", 3),
            new CommandDefinition("removenpcs", "", "Removes all custom NPCs.", 3),
            new CommandDefinition("mynpcpos", "", "Retrieves position information.", 1),
        };
    }

}

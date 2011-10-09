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

import static client.messages.CommandProcessor.getOptionalIntArg;
import client.messages.CommandDefinition;
import client.messages.Command;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import client.MapleClient;
import java.rmi.RemoteException;
import server.MapleEvent;
import server.life.MapleLifeFactory;
import tools.StringUtil;
import tools.factory.EffectFactory;

/**
 * @name        EventCommands
 * @author      Simon
 *              Modified by x711Li
 */
public class EventCommands implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
    IllegalCommandSyntaxException {
        if (splitted[0].equals("!zakum")){
            c.getPlayer().getMap().spawnFakeMonsterOnGroundBelow(MapleLifeFactory.getInstance().getMonster(8800000), c.getPlayer().getPosition());
            for (int x = 8800003; x < 8800011; x++) {
                c.getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getInstance().getMonster(x), c.getPlayer().getPosition());
            }
            c.getPlayer().getMap().broadcastMessage(EffectFactory.serverNotice(0, "The almighty Zakum has awakened!"));
        } else if (splitted[0].equals("!chaoszakum")){
            c.getPlayer().getMap().spawnFakeMonsterOnGroundBelow(MapleLifeFactory.getInstance().getMonster(8800100), c.getPlayer().getPosition());
            for (int x = 8800103; x < 8800111; x++) {
                c.getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getInstance().getMonster(x), c.getPlayer().getPosition());
            }
            c.getPlayer().getMap().broadcastMessage(EffectFactory.serverNotice(0, "The almighty Chaos Zakum has awakened!"));
        } else if (splitted[0].equals("!horntail")){
            for (int x = 8810002; x < 8810010; x++) {
                c.getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getInstance().getMonster(x), c.getPlayer().getPosition());
            }
            c.getPlayer().getMap().broadcastMessage(EffectFactory.serverNotice(0, "Look out, here comes Horntail!"));
        } else if (splitted[0].equals("!chaoshorntail")){
            for (int x = 8810102; x < 8810110; x++) {
                c.getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getInstance().getMonster(x), c.getPlayer().getPosition());
            }
            c.getPlayer().getMap().broadcastMessage(EffectFactory.serverNotice(0, "Look out, here comes Chaos Horntail!"));
        } else if (splitted[0].equals("!clock")) {
            c.getPlayer().getMap().broadcastMessage(EffectFactory.getClock(getOptionalIntArg(splitted, 1, 60)));
        } else if (splitted[0].equals("!startevent")) {
            if(splitted.length < 2) {
                mc.dropMessage("Syntax Helper: !startevent <description>");
            } else {
                try {
                    c.getChannelServer().getWorldRegistry().setEvent(new MapleEvent(c.getPlayer().getMapId(), c.getChannel(), c.getPlayer().getName(), StringUtil.joinStringFrom(splitted, 1)));
                } catch (RemoteException re) {
                    c.getChannelServer().reconnectWorld();
                }
            }
        } else if(splitted[0].equals("!endevent")) {
            try {
                c.getChannelServer().getWorldRegistry().setEvent(null);
            } catch (RemoteException re) {
                c.getChannelServer().reconnectWorld();
            }
        }
    }
    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("zakum", "", "Summon Zakum.", 2),
            new CommandDefinition("chaoszakum", "", "Summon Chaos Zakum.", 2),
            new CommandDefinition("horntail", "", "Summon Horntail.", 2),
            new CommandDefinition("chaoshorntail", "", "Summon Chaos Horntail.", 2),
            new CommandDefinition("clock", "<seconds>", "Display clock.", 2),
            new CommandDefinition("startevent", "<description>", "Start event. Description should be a name (Hide and Seek, Boss Event, etc.)", 2),
            new CommandDefinition("endevent", "", "End event.", 2)
        };
    }

}
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

/**
 * @name        CharInfoCommand
 * @author      Matze
 *              Modified by x711Li
 */
public class CharInfoCommand implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splittedLine) throws Exception,
    IllegalCommandSyntaxException {
        MapleCharacter victim = null;
        victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splittedLine[1]);
        if (victim == null) {
            return;
        }
        StringBuilder builder = new StringBuilder("Spy Info on ");
        builder.append(victim.getName());
        builder.append(" Position : ");
        builder.append(victim.getPosition().x);
        builder.append(", ");
        builder.append(victim.getPosition().y);
        builder.append(" STR : ");
        builder.append(victim.getTotalStr());
        builder.append(" DEX : ");
        builder.append(victim.getTotalDex());
        builder.append(" INT : ");
        builder.append(victim.getInt());
        builder.append(" LUK : ");
        builder.append(victim.getTotalLuk());
        builder.append(" WA : ");
        builder.append(victim.getTotalWatk());
        builder.append(" MA : ");
        builder.append(victim.getTotalMagic());
        builder.append(" ACC : ");
        builder.append(victim.getTotalAcc());
        builder.append(" IP : ");
        builder.append(victim.getClient().getSession().getRemoteAddress());
        builder.append(" Mesos : ");
        builder.append(victim.getMeso());
        builder.append(" Reborns : ");
        builder.append(victim.getReborns());
        builder.append(" Donor/GM : ");
        builder.append((victim.gmLevel() == 0 ? "False" : "True"));
        mc.dropMessage(builder.toString());
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("charinfo", "<name>", "Displays information of player.", 1),
        };
    }
}

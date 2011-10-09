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
import client.SkillFactory;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import client.messages.ServernoticeMapleClientMessageCallback;
import java.util.List;
import server.MapleStatEffect;

/**
 * @name        CheaterHuntingCommands
 * @author      Matze
 *              Modified by x711Li
 */
public class CheaterHuntingCommands implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
    IllegalCommandSyntaxException {
        if (splitted[0].equals("!whosthere")) {
            MessageCallback callback = new ServernoticeMapleClientMessageCallback(c);
            StringBuilder builder = new StringBuilder("Players on Map: ");
            for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                if (builder.length() > 150) {
                    builder.setLength(builder.length() - 2);
                    callback.dropMessage(builder.toString());
                    builder = new StringBuilder();
                }
                builder.append(MapleCharacter.makeMapleReadable(chr.getName()));
                builder.append(", ");
            }
            builder.setLength(builder.length() - 2);
            mc.dropMessage(builder.toString());
        } else if (splitted[0].equals("!hide")) {
            SkillFactory.getSkill(9001004).getEffect(1).applyTo(c.getPlayer());
        } else if (splitted[0].equals("!buffs")) {
            MapleCharacter victim = null;
            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                return;
            }
            StringBuilder builder = new StringBuilder("Buff Info on ");
            builder.append(victim.getName());
            List<MapleStatEffect> lmse = victim.getBuffEffects();
            builder.append(" Item Buffs : ");
            for (MapleStatEffect mse : lmse) {
                if (!mse.isSkill()) {
                    builder.append(mse.getSourceId());
                    builder.append(", ");
                }
            }
            builder.setLength(builder.length() - 2);
            builder.append(" Skill Buffs : ");
            for (MapleStatEffect mse : lmse) {
                if (mse.isSkill()) {
                    builder.append(mse.getSourceId());
                    builder.append(", ");
                }
            }
            builder.setLength(builder.length() - 2);
            mc.dropMessage(builder.toString());
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("whosthere", "", "Outputs list of characters on map.", 1),
            new CommandDefinition("hide", "", "Activates hide.", 1),
            new CommandDefinition("buffs", "<name>", "Displays buffs.", 1)
        };
    }
}

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

import static client.messages.CommandProcessor.getNamedDoubleArg;
import static client.messages.CommandProcessor.getNamedIntArg;
import static client.messages.CommandProcessor.getOptionalIntArg;
import client.MapleClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;

/**
 * @name        SpawnMonsterCommand
 * @author      x711Li
 */
public class SpawnMonsterCommand implements Command {
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SpawnMonsterCommand.class);

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
    IllegalCommandSyntaxException {
        int mid = Integer.parseInt(splitted[1]);
        int num = Math.min(getOptionalIntArg(splitted, 2, 1), 500);
        MapleMonster onemob = MapleLifeFactory.getInstance().getMonster(mid);
        int newhp = 0;
        newhp = onemob.getMaxHp();
        if (newhp < 1) {
            newhp = 1;
        }
        MapleMonsterStats overrideStats = new MapleMonsterStats();
        overrideStats.setHp(newhp);
        overrideStats.setExp(0);
        overrideStats.setMp(onemob.getMaxMp());
        for (int i = 0; i < num; i++) {
            MapleMonster mob = MapleLifeFactory.getInstance().getMonster(mid);
            mob.setHp(newhp);
            mob.setOverrideStats(overrideStats);
            c.getPlayer().getMap().spawnMonsterOnGroudBelow(mob, c.getPlayer().getPosition());
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("spawn", "<id>", "Spawns monster.", 2),
        };
    }

}

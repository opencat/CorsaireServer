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
import tools.StringUtil;
import java.util.Collection;

/**
 * @name        ServerMessageCommand
 * @author      x711Li
 */
public class ServerMessageCommand implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splittedLine) throws Exception, IllegalCommandSyntaxException {
        Collection<ChannelServer> cservs = ChannelServer.getAllInstances();
        String outputMessage = StringUtil.joinStringFrom(splittedLine, 1);
        for (ChannelServer cserv : cservs) {
            cserv.setServerMessage(outputMessage);
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("servermessage", "<message>", "Changes the servermessage.", 3),
        };
    }
}

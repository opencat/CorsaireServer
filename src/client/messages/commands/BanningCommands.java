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

import java.rmi.RemoteException;
import static client.messages.CommandProcessor.getNamedIntArg;
import static client.messages.CommandProcessor.joinAfterString;

import java.text.DateFormat;
import java.util.Calendar;

import client.MapleCharacter;
import client.MapleClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import java.sql.PreparedStatement;
import net.channel.ChannelServer;
import tools.DatabaseConnection;
import tools.StringUtil;
import tools.factory.EffectFactory;

/**
 * @name        BanningCommands
 * @author      Matze
 *              Modified by x711Li
 */
public class BanningCommands implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception {
        ChannelServer cserv = c.getChannelServer();
        MapleCharacter player = c.getPlayer();
        if (splitted[0].equals("!ban")) {
            if (splitted.length < 3) {
                mc.dropMessage("Syntax for !ban: !ban user reason");
                return;
            }
            String originalReason = StringUtil.joinStringFrom(splitted, 2);
            String reason = c.getPlayer().getName() + " banned " + splitted[1] + ": " + originalReason;
            MapleCharacter target = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (target != null) {
                if (target.gmLevel() > player.gmLevel()) {
                    player.ban(player.getName() + " banned - Trying to ban " + target.getName(), true);
                    return;
                }
                String ip = target.getClient().getSession().getRemoteAddress().toString().split(":")[0];
                reason += " (IP: " + ip + ")";
                target.ban(reason, true);
                player.setBanCount(player.getBanCount() + 1);
            } else {
                if (MapleCharacter.offlineBan(splitted[1], reason)) {
                    mc.dropMessage("Offline Banned " + splitted[1]);
                    player.setBanCount(player.getBanCount() + 1);
                } else {
                    mc.dropMessage("Failed to ban " + splitted[1]);
                }
            }
            if(player.getBanCount() > 2 && player.gmLevel() < 3) {
                player.ban(player.getName() + " banned - Ban Abuse", true);
            }
        } else if (splitted[0].equals("!unban")) {
            try {
                PreparedStatement p = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET banned = 0 WHERE id = (SELECT accountid FROM characters WHERE name = ?)");
                p.setString(1, splitted[1]);
                p.executeUpdate();
                p.close();
            } catch (Exception e) {
                mc.dropMessage("Failed to unban " + splitted[1]);
                return;
            }
            mc.dropMessage("Unbanned " + splitted[1]);
        } else if (splitted[0].equals("!dc")) {
            if (splitted.length < 3) {
                mc.dropMessage("Syntax for !dc: !dc user reason");
                return;
            }
            String originalReason = StringUtil.joinStringFrom(splitted, 2);
            if(player.getBanCount() > 10) {
                player.ban(player.getName() + " banned - DC Abuse", true);
            }
            MapleCharacter victim = null;
            for (ChannelServer cserv_ : ChannelServer.getAllInstances()) {
                victim = cserv_.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    break;
                }
            }
            if (victim == null) {
                return;
            }
            victim.getClient().getSession().close();
            player.setBanCount(player.getBanCount() + 1);
            for (ChannelServer cserv_ : ChannelServer.getAllInstances()) {
                cserv_.broadcastGMPacket(EffectFactory.serverNotice(6, "[D/C Report] " + MapleCharacter.makeMapleReadable(player.getName() + " disconnected " + victim.getName() + " for " + originalReason)));
            }
        } else if (splitted[0].equals("!mute")) {
            if (splitted.length < 3) {
                mc.dropMessage("Syntax for !mute: !mute user reason");
                return;
            }
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                return;
            }
            if(victim.gmLevel() > c.getPlayer().gmLevel()) {
                mc.dropMessage(victim.getName() + " cannot be muted!");
                return;
            }
            victim.setMuted(System.currentTimeMillis());
            victim.dropMessage("You have been muted for 90 seconds. If you continue to spam, you will be temporarily banned.");
            mc.dropMessage(victim.getName() + " muted for 90 seconds.");
            for (ChannelServer cserv_ : ChannelServer.getAllInstances()) {
                cserv_.broadcastGMPacket(EffectFactory.serverNotice(6, "[Mute Report] " + MapleCharacter.makeMapleReadable(player.getName() + " muted " + victim.getName() + " for " + StringUtil.joinStringFrom(splitted, 2))));
            }
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("ban", "<player>", "Bans player.", 1),
            new CommandDefinition("unban", "<player>", "Unbans player.", 2),
            new CommandDefinition("dc", "<player>", "Disconnects player.", 1),
            new CommandDefinition("mute", "<player> <seconds>", "Mutes player for 90 seconds.", 1)
        };
    }

}

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

import client.Equip;
import tools.factory.EffectFactory;
import client.MapleCharacter;
import client.messages.CommandDefinition;
import client.messages.Command;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import client.MapleClient;
import client.MapleStat;
import net.MaplePacket;
import tools.StringUtil;
import server.life.MapleMonster;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.shops.HiredMerchant;
import net.channel.ChannelServer;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.life.MapleMonsterStats;

/**
 * @name        CustomCommands
 * @author      Various
 *              Modified by x711Li
 */
public class CustomCommands implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
    IllegalCommandSyntaxException {
        MapleCharacter player = c.getPlayer();
        ChannelServer cserv = c.getChannelServer();
        if (splitted[0].equals("!heal")) {
            player.setHp(player.getCurrentMaxHp());
            player.updateSingleStat(MapleStat.HP, player.getCurrentMaxHp());
            player.setMp(player.getCurrentMaxMp());
            player.updateSingleStat(MapleStat.MP, player.getCurrentMaxMp());
        } else if (splitted[0].equals("!kill")) {
            MapleCharacter victim1 = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            victim1.setHp(0);
            victim1.setMp(0);
            victim1.updateSingleStat(MapleStat.HP, 0);
            victim1.updateSingleStat(MapleStat.MP, 0);
        } else if (splitted[0].equals("!killmap")) {
            for (MapleCharacter mch : c.getPlayer().getMap().getCharacters()) {
                if (mch != null) {
                    mch.setHp(0);
                    mch.setMp(0);
                    mch.updateSingleStat(MapleStat.HP, 0);
                    mch.updateSingleStat(MapleStat.MP, 0);
                }
            }
        } else if (splitted[0].equals("!dcall")) {
            for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()){
                mch.getClient().getSession().close();
                mch.getClient().disconnect(true, false);
            }
        } else if (splitted[0].equals("!mesos")){
            c.getPlayer().gainMeso(Integer.parseInt(splitted[1]), true); 
        } else if (splitted[0].equals("!chattype")) {
            player.toggleGMChat();
            mc.dropMessage("You now chat in " + (player.getGMChat() ? "white." : "black."));
        } else if (splitted[0].equals("!cleardrops")) {
            player.getMap().clearDrops(player, true);
        } else if (splitted[0].equals("!speak")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                mc.dropMessage("unable to find '" + splitted[1] + "'");
            } else {
                victim.getMap().broadcastMessage(EffectFactory.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 2), victim.isGM(), 0));
            }
        }  else if (splitted[0].equals("!smega")) {
            if (splitted.length == 1) {
                mc.dropMessage("Usage: !smega [name] [type] [message], where [type] is love, cloud, or diablo.");
            }
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            String type = splitted[2];
            int channel = victim.getClient().getChannel();
            String text = StringUtil.joinStringFrom(splitted, 3);
            int itemID = 0;
            if (type.equals("love")) {
                itemID = 5390002;
            } else if (type.equals("cloud")) {
                itemID = 5390001;
            } else if (type.equals("diablo")) {
                itemID = 5390000;
            } else {
                mc.dropMessage("Invalid type (use love, cloud, or diablo)");
                return;
            }
            String[] lines = {"", "", "", ""};
            if (text.length() > 30) {
                lines[0] = text.substring(0, 10);
                lines[1] = text.substring(10, 20);
                lines[2] = text.substring(20, 30);
                lines[3] = text.substring(30);
            } else if (text.length() > 20) {
                lines[0] = text.substring(0, 10);
                lines[1] = text.substring(10, 20);
                lines[2] = text.substring(20);
            } else if (text.length() > 10) {
                lines[0] = text.substring(0, 10);
                lines[1] = text.substring(10);
            } else if (text.length() <= 10) {
                lines[0] = text;
            }
            LinkedList list = new LinkedList();
            list.add(lines[0]);
            list.add(lines[1]);
            list.add(lines[2]);
            list.add(lines[3]);
            try {
                MaplePacket mp = EffectFactory.getAvatarMega(victim, "", channel, itemID, list, true);
                victim.getClient().getChannelServer().getWorldInterface().broadcastMessage(null, mp.getBytes());
            } catch (Exception e) {
            }
        } else if (splitted[0].equals("!killall")) {
            List<MapleMapObject> monsters = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
            for (MapleMapObject monstermo : monsters) {
                MapleMonster monster = (MapleMonster) monstermo;
                MapleMonsterStats overrideStats = new MapleMonsterStats();
                overrideStats.setExp(0);
                monster.setOverrideStats(overrideStats);
                player.getMap().killMonster(monster, player, false);
            }
            mc.dropMessage("Killed " + monsters.size() + " monsters.");
        } else if (splitted[0].equals("!killtoleft") || splitted[0].equals("!killtoright")) {
            boolean left = splitted[0].equals("!killtoleft");
            boolean kill = false;
            if (c.getPlayer().getMap().getId() != 109020001) {
                mc.dropMessage("You may only use this command in the OX Quiz Map (109020001).");
                return;
            }
            for (MapleCharacter mch : c.getPlayer().getMap().getCharacters()) {
                if ((mch.getPosition().getX() < c.getPlayer().getPosition().getX()) && left) {
                    kill = true;
                } else if ((mch.getPosition().getX() > c.getPlayer().getPosition().getX()) && !left) {
                    kill = true;
                }
                if (kill) {
                    mch.setHp(0);
                    mch.setMp(0);
                    mch.updateSingleStat(MapleStat.HP, 0);
                    mch.updateSingleStat(MapleStat.MP, 0);
                }
            }
        } else if (splitted[0].equals("!closeallmerchants")) {
            for(ChannelServer cserver : ChannelServer.getAllInstances()) {
                cserver.getHMRegistry().closeAndDeregisterAll();
            }
        } else if (splitted[0].equals("!closemerchant")) {
            if(splitted.length != 2) {
                mc.dropMessage("Syntax helper: !closemerchant <name>");
            }
            HiredMerchant victimMerch = c.getChannelServer().getHMRegistry().getMerchantForPlayer(splitted[1]);
            if(victimMerch != null) {
            victimMerch.closeShop();
            } else {
                mc.dropMessage("The specified player is either not online or does not have a merchant.");
            }
        } else if (splitted[0].equals("!staffpackage")) {
            final int packageitems[] = {1442057, 1002800, 1102174, 1082245, 1052167, 1072368, 1022004 };
            for(int i = 0; i < packageitems.length; i++) {
                Equip toDrop = (Equip) MapleItemInformationProvider.getInstance().getEquipById(packageitems[i]);
                toDrop.setStr((short) 999);
                toDrop.setInt((short) 999);
                toDrop.setDex((short) 999);
                toDrop.setLuk((short) 999);
                toDrop.setWatk((short) 999);
                toDrop.setMatk((short) 999);
                toDrop.setHp((short) 999);
                toDrop.setMp((short) 999);
                toDrop.setSpeed((short) 99);
                toDrop.setJump((short) 9);
                toDrop.setLevel((byte) 99);
                toDrop.setOwner(player.getName());
                MapleInventoryManipulator.addFromDrop(c, toDrop, true);
            }
        } else if (splitted[0].equals("!nx")) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                return;
            }
            victim.modifyCSPoints(1, victim.getCSPoints(1) + Integer.parseInt(splitted[2]));
            mc.dropMessage("Done");
        } else if (splitted[0].equals("!dp")) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                return;
            }
            victim.setDonorPts(victim.getDonorPts() + Integer.parseInt(splitted[2]));
            mc.dropMessage("Done");
        } else if (splitted[0].equals("!setdonor")) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                return;
            }
            victim.setGM(-1);
            victim.setRates(false);
            mc.dropMessage("Done");
        } else if (splitted[0].equals("!smegaoff")) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                return;
            }
            victim.setCanSmega(false);
            victim.dropMessage("Your megaphone privileges have been disabled by a GM. If you continue to spam you will be temporarily banned.");
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!smegaon")) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                return;
            }
            victim.setCanSmega(true);
            victim.dropMessage("Your megaphone privileges have been enabled by a GM. Please remember not to spam.");
            mc.dropMessage("Done.");
        }
    }
    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("heal", "", "Set to max HP/MP.", 1),
            new CommandDefinition("kill", "<name>", "Kills player.", 3),
            new CommandDefinition("killmap", "", "Kills players on map.", 3),
            new CommandDefinition("dcall", "", "Disconnects all players.", 3),
            new CommandDefinition("mesos", "", "Sets mesos.", 3),
            new CommandDefinition("chattype", "", "Toggle chat.", 1),
            new CommandDefinition("cleardrops", "", "Clear drops.", 2),
            new CommandDefinition("speak", "", "Player pseudo-speaks.", 3),
            new CommandDefinition("smega", "", "Player pseudo-smegas.", 3),
            new CommandDefinition("killall", "", "Kill all monsters.", 2),
            new CommandDefinition("killtoleft", "", "Kill all players to your left. Only works at OX Quiz.", 2),
            new CommandDefinition("killtoright", "", "Kill all players to your right. Only works at OX Quiz.", 2),
            new CommandDefinition("closeallmerchants", "", "Close all merchants.", 3),
            new CommandDefinition("closemerchant", "<name>", "Close merchant.", 2),
            new CommandDefinition("staffpackage", "", "Distributes Staff Equipment.", 1),
            new CommandDefinition("nx", "<name> <amount>", "Hands NX to player.", 3),
            new CommandDefinition("dp", "<name> <amount>", "Hands Donor Points to player.", 3),
            new CommandDefinition("setdonor", "<name>", "Sets player as donor.", 3),
            new CommandDefinition("smegaoff", "<name>", "Turns smega privileges off.", 1),
            new CommandDefinition("smegaon", "<name>", "Turns smega privileges on.", 1)
        };
    }
}
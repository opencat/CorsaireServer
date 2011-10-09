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
import client.MapleStat;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import constants.ServerConstants;
import net.channel.ChannelServer;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;
import server.maps.MapleMap;
import server.shops.MapleShopFactory;
import tools.StringUtil;
import tools.factory.EffectFactory;
import tools.factory.IntraPersonalFactory;

/**
 * @name        PlayerCommands
 * @author      Matze
 *              Modified by x711Li
 */
public class PlayerCommands implements Command {
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
    IllegalCommandSyntaxException {
        MapleCharacter player = c.getPlayer();
        if (splitted[0].equals("@str") || splitted[0].equals("@int") || splitted[0].equals("@luk") || splitted[0].equals("@dex")) {
            int amount = Integer.parseInt(splitted[1]);
            boolean str = splitted[0].equals("@str");
            boolean Int = splitted[0].equals("@int");
            boolean luk = splitted[0].equals("@luk");
            boolean dex = splitted[0].equals("@dex");
            if (amount > 0 && amount <= player.getRemainingAp() && amount <= 32763 || amount < 0 && amount >= -32763 && Math.abs(amount) + player.getRemainingAp() <= 32767) {
                if (str && amount + player.getStr() <= 32767 && amount + player.getStr() >= 4) {
                    player.setStr(player.getStr() + amount);
                    player.updateSingleStat(MapleStat.STR, player.getStr());
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                } else if (Int && amount + player.getInt() <= 32767 && amount + player.getInt() >= 4) {
                    player.setInt(player.getInt() + amount);
                    player.updateSingleStat(MapleStat.INT, player.getInt());
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                } else if (luk && amount + player.getLuk() <= 32767 && amount + player.getLuk() >= 4) {
                    player.setLuk(player.getLuk() + amount);
                    player.updateSingleStat(MapleStat.LUK, player.getLuk());
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                } else if (dex && amount + player.getDex() <= 32767 && amount + player.getDex() >= 4) {
                    player.setDex(player.getDex() + amount);
                    player.updateSingleStat(MapleStat.DEX, player.getDex());
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                } else {
                    mc.dropMessage("Please make sure the stat you are trying to raise is not over 32,767 or under 4.");
                }
            } else {
                mc.dropMessage("Please make sure your AP is not over 32,767 and you have enough to distribute.");
            }
        } else if (splitted[0].equals("@dispose")) {
            NPCScriptManager.getInstance().dispose(c);
            QuestScriptManager.getInstance().dispose(c);
            c.announce(IntraPersonalFactory.enableActions());
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("@save")) {
            player.saveToDB(true);
            mc.dropMessage("Save Complete.");
        } else if (splitted[0].equals("@say") && player.gmLevel() == -1) {
            if (System.currentTimeMillis() - player.getLastAnnounced() <= 30000) {
                mc.dropMessage("Please wait " +
            ((30000 - (System.currentTimeMillis() - player.getLastAnnounced())) / 1000) + " seconds before using @say again.");
            } else {
                player.setLastAnnounced(System.currentTimeMillis());
                try {
                    c.getChannelServer().getWorldInterface().broadcastAnnouncement(EffectFactory.itemMegaphone("[Donor] " + player.getName() + " : " + StringUtil.joinStringFrom(splitted, 1), true, c.getChannel() - c.getPlayer().getWorld() * ServerConstants.NUM_CHANNELS, null).getBytes());
                } catch (Exception e) {
                    c.getChannelServer().reconnectWorld();
                }
            }
        } else if (splitted[0].equals("@world") && !(player.getMap().getId() >= 914000000 && player.getMap().getId() <= 914000500)) {
            if (c.getPlayer().gmLevel() == 0) {
                mc.dropMessage("You do not have the privileges for this command.");
                return;
            }
            c.getPlayer().changeMap(c.getChannelServer().getMapFactory().getMap(103000007));
        } else if (splitted[0].equals("@notify")) {
            mc.dropMessage("Type @faq to see if your question is answered in there. A Gamemaster will be with you shortly.");
            for (ChannelServer cserv_ : ChannelServer.getAllInstances()) {
                cserv_.broadcastGMPacket(EffectFactory.serverNotice(6, player.getName() + " needs your help!"));
            }
        } else if (splitted[0].equals("@ask")) {
            mc.dropMessage("Type @faq to see if your question is answered in there. A Gamemaster will be with you shortly.");
            for (ChannelServer cserv_ : ChannelServer.getAllInstances()) {
                cserv_.broadcastGMPacket(EffectFactory.serverNotice(6, player.getName() + " asks " + StringUtil.joinStringFrom(splitted, 1)));
            }
        } else if (splitted[0].equals("@panel")) {
            NPCScriptManager.getInstance().start(c, 9010000, 1);
        } else if (splitted[0].equals("@faq")) {
            NPCScriptManager.getInstance().start(c, 9010000, 2);
        } else if (splitted[0].equals("@droplist")) {
            NPCScriptManager.getInstance().start(c, 9010000, 3);
        } else if (splitted[0].equals("@go") && ServerConstants.WARPER) {
            NPCScriptManager.getInstance().start(c, 9010000, 4);
        } else if (splitted[0].equals("@autojob") && ServerConstants.AUTO_JOB) {
            NPCScriptManager.getInstance().start(c, 9010000, 5);
        } else if (splitted[0].equals("@jukebox")) {
            NPCScriptManager.getInstance().start(c, 9010000, 6);
        } else if (splitted[0].equals("@donate")) {
            NPCScriptManager.getInstance().start(c, 9010000, 7);
        } else if (splitted[0].equals("@monsterdrops")) {
            NPCScriptManager.getInstance().start(c, 9010000, 8);
        } else if (splitted[0].equals("@togglemegaphones")) {
            player.toggleDeaf();
        } else if (splitted[0].equals("@toggleexperience")) {
            player.toggleExperience();
        } else if (splitted[0].equals("@storage")) {
            if (c.getPlayer().gmLevel() == 0) {
                mc.dropMessage("You do not have the privileges for this command.");
                return;
            }
            c.getPlayer().getStorage().sendStorage(c, 9010000);
        } else if (splitted[0].equals("@shop")) {
            if (c.getPlayer().gmLevel() == 0) {
                mc.dropMessage("You do not have the privileges for this command.");
                return;
            }
            MapleShopFactory.getInstance().getShop(10000002).sendShop(c);
        } else if (splitted[0].equals("@aio") && ServerConstants.ALL_IN_ONE) {
            NPCScriptManager.getInstance().start(c, 9010000, 6);
        /*} else if (splitted[0].equals("@whatdrops") || splitted[0].equals("@droplist")) {
            String searchString = StringUtil.joinStringFrom(splitted, 1);
            boolean itemSearch = splitted[0].equals("@whatdrops");
            int limit = 5;
            ArrayList<Pair<Integer, String>> searchList;
            if(itemSearch) {
                searchList = MapleItemInformationProvider.getInstance().getItemDataByName(searchString);
            } else {
                searchList = MapleMonsterInformationProvider.getInstance().getListFromName(searchString);
            }
            Iterator<Pair<Integer, String>> listIterator = searchList.iterator();
            for (int i = 0; i < limit; i++) {
                if(listIterator.hasNext()) {
                    Pair<Integer, String> data = listIterator.next();
                    if(itemSearch) {
                        mc.dropMessage("Item " + data.getRight() + " dropped by:");
                    } else {
                        mc.dropMessage("Mob " + data.getRight() + " drops:");
                    }
                    try {
                        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM monsterdrops WHERE " + (itemSearch ? "itemid" : "monsterid") + " = ? LIMIT 50");
                        ps.setInt(1, data.getLeft());
                        ResultSet rs = ps.executeQuery();
                        while(rs.next()) {
                            String resultName;
                            if(itemSearch) {
                                resultName = MapleMonsterInformationProvider.getInstance().getNameFromId(rs.getInt("monsterid"));
                            } else {
                                resultName = MapleItemInformationProvider.getInstance().getName(rs.getInt("itemid"));
                            }
                            if(resultName != null) {
                                mc.dropMessage(resultName);
                            }
                        }
                        rs.close();
                        ps.close();
                    } catch (Exception e) {
                        mc.dropMessage("There was a problem retreiving the required data. Please try again.");
                        e.printStackTrace();
                        return;
                    }
                } else {
                    break;
                }
            }*/
        } else if (splitted[0].equals("@commands") || splitted[0].equals("@help")) {
            mc.dropMessage(ServerConstants.SERVER_NAME + " Player Commands");
            mc.dropMessage("@str - <amount> - Add to STR.");
            mc.dropMessage("@int - <amount> - Add to INT.");
            mc.dropMessage("@luk - <amount> - Add to LUK.");
            mc.dropMessage("@dex - <amount> - Add to DEX.");
            mc.dropMessage("@ask - <question> - Notify GM with question.");
            mc.dropMessage("@dispose - Use when NPCs/portals/inventories don't work.");
            mc.dropMessage("@event - Go to event.");
            mc.dropMessage("@save - Save character.");
            if (ServerConstants.AUTO_JOB) {
                mc.dropMessage("@autojob - Launches auto-job advancer.");
            }
            mc.dropMessage("@panel - Launches custom management panel.");
            if (ServerConstants.WARPER) {
                mc.dropMessage("@go - Launches warper.");
            }
            mc.dropMessage("@faq - Launches FAQ.");
            if (ServerConstants.ALL_IN_ONE) {
                mc.dropMessage("@aio - Launches All-In-One Shop.");
            }
            mc.dropMessage("@notify - Notify GM that you have a problem.");
            mc.dropMessage("@droplist - Launches drop list for monsters.");
            mc.dropMessage("@commands - Displays commands.");
            mc.dropMessage("@say - [Donor] Display message to server.");
            mc.dropMessage("@world - [Donor] Warp to Donor World.");
            mc.dropMessage("@shop - [Donor] Opens Donor Shop.");
            mc.dropMessage("@storage - [Donor] Accesses storage.");
        } else {
            mc.dropMessage("Command " + splitted[0] + " does not exist. Use @commands for a list of working commands.");
        }             
    }
    
    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("str", "", "", 0),
            new CommandDefinition("int", "", "", 0),
            new CommandDefinition("luk", "", "", 0),
            new CommandDefinition("dex", "", "", 0),
            new CommandDefinition("dispose", "", "", 0),
            new CommandDefinition("save", "", "", 0),
            new CommandDefinition("say", "", "", 0),
            new CommandDefinition("world", "", "", 0),
            new CommandDefinition("autojob", "", "", 0),
            new CommandDefinition("panel", "", "", 0),
            new CommandDefinition("jukebox", "", "", 0),
            new CommandDefinition("donate", "", "", 0),
            new CommandDefinition("monsterdrops", "", "", 0),
            new CommandDefinition("togglemegaphones", "", "", 0),
            new CommandDefinition("toggleexperience", "", "", 0),
            new CommandDefinition("go", "", "", 0),
            new CommandDefinition("notify", "", "", 0),
            new CommandDefinition("ask", "", "", 0),
            new CommandDefinition("faq", "", "", 0),
            new CommandDefinition("aio", "", "", 0),
            new CommandDefinition("shop", "", "", 0),
            new CommandDefinition("storage", "", "", 0),
            new CommandDefinition("droplist", "", "", 0),
            new CommandDefinition("commands", "", "", 0)
        };
    }

}
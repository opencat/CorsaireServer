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
import client.IItem;
import client.Item;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MaplePet;
import client.MapleStat;
import client.SkillFactory;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import client.messages.ServernoticeMapleClientMessageCallback;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.shops.MapleShop;
import server.shops.MapleShopFactory;
import net.channel.ChannelServer;
import java.awt.Point;
import provider.MapleDataProviderFactory;
import provider.MapleData;
import client.ISkill;
import java.io.File;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import tools.DatabaseConnection;

/**
 * @name        CharCommands
 * @author      Matze
 *              Modified by x711Li
 */
public class CharCommands implements Command {
    @SuppressWarnings("static-access")
    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
    IllegalCommandSyntaxException {
        MapleCharacter player = c.getPlayer();
        if (splitted[0].equals("!skill")) {
            int skill = Integer.parseInt(splitted[1]);
            int level = getOptionalIntArg(splitted, 2, 1);
            int masterlevel = getOptionalIntArg(splitted, 3, 1);
            c.getPlayer().changeSkillLevel(SkillFactory.getSkill(skill), level, masterlevel);
        } else if (splitted[0].equals("!ap")) {
            player.setRemainingAp(Integer.parseInt(splitted[1]));
            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
        } else if (splitted[0].equals("!sp")) {
            player.setRemainingSp(Integer.parseInt(splitted[1]));
            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
        } else if (splitted[0].equals("!job")) {
            c.getPlayer().changeJob(Integer.parseInt(splitted[1]));
        } else if (splitted[0].equals("!whereami")) {
            new ServernoticeMapleClientMessageCallback(c).dropMessage("You are on map " + c.getPlayer().getMap().getId());
        } else if (splitted[0].equals("!shop")) {
            MapleShopFactory sfact = MapleShopFactory.getInstance();
            MapleShop shop = sfact.getShop(getOptionalIntArg(splitted, 1, 1));
            shop.sendShop(c);
        } else if (splitted[0].equals("!levelup")) {
            c.getPlayer().levelUp(true);
            int newexp = c.getPlayer().getExp();
            if (newexp < 0) {
                c.getPlayer().gainExp(-newexp, false, false);
            }
        } else if (splitted[0].equals("!setall")) {
            final int x = Short.parseShort(splitted[1]);
            setAllStats(player, (short) x);
        } else if (splitted[0].equals("!maxstats")) {
            setAllStats(player, Short.MAX_VALUE);
            player.setLevel(255);
            player.setFame(999);
            player.setMaxHp(30000);
            player.setMaxMp(30000);
            player.updateSingleStat(MapleStat.LEVEL, 255);
            player.updateSingleStat(MapleStat.FAME, 999);
            player.updateSingleStat(MapleStat.MAXHP, 30000);
            player.updateSingleStat(MapleStat.MAXMP, 30000);
        } else if (splitted[0].equals("!maxskills")) {
            for (MapleData skill_ : MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren())
            try {
                ISkill skill = SkillFactory.getSkill(Integer.parseInt(skill_.getName()));
                if ((skill.getId() < 1009 || skill.getId() > 1011));
                player.changeSkillLevel(skill, skill.getMaxLevel(), skill.getMaxLevel());
            } catch (NumberFormatException nfe) {
                break;
            } catch (NullPointerException npe) {
                continue;
            }
        } else if (splitted[0].equals("!item")) {
            int itemid = Integer.parseInt(splitted[1]);
            short quantity = (short) getOptionalIntArg(splitted, 2, 1);
            if (MapleItemInformationProvider.getInstance().getEquipById(itemid) == null) {
                return;
            } else if (itemid >= 5000000 && itemid <= 5000100) {
                if (quantity > 1) {
                    quantity = 1;
                }
                int petId = MaplePet.createPet(itemid);
                MapleInventoryManipulator.addById(c, itemid, quantity, player.getName(), petId);
                return;
            }
            MapleInventoryManipulator.addById(c, itemid, quantity);
        } else if (splitted[0].equals("!drop")) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            int itemId = Integer.parseInt(splitted[1]);
            byte toFlag = (byte) getOptionalIntArg(splitted, 2, 0);
            IItem toDrop;
            if (ii.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                toDrop = ii.getEquipById(itemId);
                if (toFlag > 0) {
                    byte flag = toDrop.getFlag();
                    flag |= toFlag;
                    toDrop.setFlag(flag);
                }
            } else {
                toDrop = new Item(itemId, (byte) 0, (short) 1);
            }
            if (c.getPlayer().gmLevel() < 3) {
                toDrop.setOwner(player.getName());
            }
            final int playerId = c.getPlayer().getId();
            final Point playerPos = c.getPlayer().getPosition();
            c.getPlayer().getMap().spawnItemDrop(playerId, playerPos, c.getPlayer(), toDrop, playerPos, true, true);
        } else if (splitted[0].equals("!level")) {
            int quantity = Integer.parseInt(splitted[1]);
            c.getPlayer().setLevel(quantity - 1);
            c.getPlayer().levelUp(true);
            c.getPlayer().gainExp(-c.getPlayer().getExp(), false, false);
        } else if (splitted[0].equals("!online")) {
            String playerStr = "";
            try {
                playerStr = c.getChannelServer().getWorldInterface().getAllPlayerNames(player.getWorld());
            } catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }
            int onlinePlayers = playerStr.split(", ").length;
            mc.dropMessage("Online players: " + onlinePlayers);
            mc.dropMessage(playerStr);
        } else if (splitted[0].equals("!unstuck")) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                return;
            }
            victim.saveToDB(true);
            try {
                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?");
                ps.setInt(1, victim.getAccountID());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            c.getChannelServer().removePlayer(victim);
            victim.getClient().getSession().close();
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!saveall")) {
            Collection<ChannelServer> cservs = ChannelServer.getAllInstances();
            for (ChannelServer cserv : cservs) {
                try {
                    for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                        synchronized (chr) {
                            if (chr.getHiredMerchant().isOpen()) {
                                chr.getHiredMerchant().saveItems();
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
            for (ChannelServer cserv : cservs) {
                try {
                    for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                        synchronized (chr) {
                            chr.saveToDB(true);
                        }
                    }
                } catch (Exception e) {
                }
            }
            for (ChannelServer cserv : cservs) {
                try {
                    for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                        synchronized (chr) {
                            chr.getClient().disconnect(true, false);
                        }
                    }
                } catch (Exception e) {
                }
            }
            mc.dropMessage("All characters saved.");
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] {
            new CommandDefinition("skill", "<skillid> <level> <masterlevel>", "Teaches skill.", 1),
            new CommandDefinition("ap", "<amount>", "Sets AP.", 1),
            new CommandDefinition("sp", "<amount>", "Sets SP.", 1),
            new CommandDefinition("job", "<id>", "Sets job.", 1),
            new CommandDefinition("whereami", "", "Outputs map ID.", 1),
            new CommandDefinition("shop", "<id>", "Opens shop by ID", 3),
            new CommandDefinition("levelup", "", "Levels up.", 1),
            new CommandDefinition("setall", "<amount>", "Set all stats.", 1),
            new CommandDefinition("maxstats", "", "Maxes stats.", 1),
            new CommandDefinition("maxskills", "", "Maxes skills.", 1),
            new CommandDefinition("item", "<id>", "Creates item.", 2),
            new CommandDefinition("drop", "<id>", "Drops item.", 3),
            new CommandDefinition("level", "<amount>", "Levels character.", 1),
            new CommandDefinition("online", "", "Outputs online players.", 1),
            new CommandDefinition("unstuck", "<name>", "Unstucks players.", 1),
            new CommandDefinition("saveall", "", "Saves all characters.", 3)
        };
    }

    private void setAllStats(MapleCharacter player, short value) {
        short x = value;
        player.setStr(x);
        player.setDex(x);
        player.setInt(x);
        player.setLuk(x);
        player.updateSingleStat(MapleStat.STR, x);
        player.updateSingleStat(MapleStat.DEX, x);
        player.updateSingleStat(MapleStat.INT, x);
        player.updateSingleStat(MapleStat.LUK, x);
    }

}

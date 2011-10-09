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

package scripting.npc;

import client.Equip;
import client.IItem;
import client.ISkill;
import client.ItemFactory;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import constants.ExpTable;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MaplePet;
import client.SkillEntry;
import client.MapleStat;
import client.SkillFactory;
import tools.Randomizer;
import java.io.File;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import tools.DatabaseConnection;
import net.world.guild.MapleAlliance;
import net.world.guild.MapleGuild;
import net.world.remote.WorldChannelInterface;
import provider.MapleData;
import provider.MapleDataProviderFactory;
import scripting.AbstractPlayerInteraction;
import scripting.event.EventManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.shops.MapleShopFactory;
import server.MapleStatEffect;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;
import tools.Pair;
import tools.factory.EffectFactory;
import tools.factory.GuildFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.InventoryFactory;
import tools.factory.NPCFactory;
import tools.factory.PetFactory;

/**
 * @name        NPCConversationManager
 * @author      Matze
 *              Modified by x711Li
 */
public class NPCConversationManager extends AbstractPlayerInteraction {

    private int npc;
    private String getText;
    private MapleMapFactory mapFactory = new MapleMapFactory();
    private int questid;

    public NPCConversationManager(MapleClient c, int npc, int questid) {
        super(c);
        this.npc = npc;
        this.questid = questid;
        mapFactory.setChannel(c.getChannelServer().getChannel());
    }

    public int getNpc() {
        return npc;
    }

    public int getQuest() {
        return questid;
    }

    public void dispose() {
        NPCScriptManager.getInstance().dispose(this);
    }

    public void sendNext(String text) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 0, text, "00 01", (byte) 0));
    }

    public void sendNextS(String text, byte type) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 0, text, "00 01", type));
    }

    public void sendPrev(String text) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 0, text, "01 00", (byte) 0));
    }

    public void sendPrevS(String text, byte type) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 0, text, "01 00", type));
    }

    public void sendNextPrev(String text) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 0, text, "01 01", (byte) 0));
    }

    public void sendNextPrevS(String text, byte type) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 0, text, "01 01", type));
    }

    public void sendOk(String text) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) 0));
    }

    public void sendOkS(String text, byte type) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 0, text, "00 00", type));
    }

    public void sendYesNo(String text) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 1, text, "", (byte) 0));
    }

    public void sendYesNoS(String text, byte type) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 1, text, "", type));
    }

    public void sendAcceptDecline(String text) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 0x0C, text, "", (byte) 0));
    }

    public void sendSimple(String text) {
        c.announce(NPCFactory.getNPCTalk(npc, (byte) 4, text, "", (byte) 0));
    }

    public void askAcceptDecline(String text) {
        sendAcceptDecline(text);
    }

    public void clearSkills() {
        Map<ISkill, SkillEntry> skills = getPlayer().getSkills();
        for (Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
            getPlayer().changeSkillLevel(skill.getKey(), 0, 0);
        }
        getPlayer().setSaveSkills();
    }

    public void sendStyle(String text, int styles[]) {
        getClient().announce(NPCFactory.getNPCTalkStyle(npc, text, styles));
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        getClient().announce(NPCFactory.getNPCTalkNum(npc, text, def, min, max));
    }

    public void sendGetText(String text) {
        getClient().announce(NPCFactory.getNPCTalkText(npc, text));
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return this.getText;
    }

    public int getJobId() {
        return getPlayer().getJob();
    }

    public void startQuest(int id) {
        try {
            MapleQuest.getInstance(id).forceStart(getPlayer(), npc);
        } catch (NullPointerException ex) {
        }
    }

    public void completeQuest(int id) {
        try {
            MapleQuest.getInstance(id).forceComplete(getPlayer(), npc);
        } catch (NullPointerException ex) {
        }
    }

    public void forfeitQuest(int id) {
        try {
            MapleQuest.getInstance(id).forfeit(getPlayer());
        } catch (NullPointerException ex) {
        }
    }

    public void forceStartQuest(int id) {
        MapleQuest.getInstance(id).forceStart(getPlayer(), getNpc());
    }

    public void forceStartQuest(int id, String customData) {
    MapleQuest.getInstance(id).forceStart(getPlayer(), getNpc(), customData);
    }

    public final void forceCompleteQuest(final int id) {
        MapleQuest.getInstance(id).forceComplete(getPlayer(), 0);
    }

    public int getMeso() {
        return getPlayer().getMeso();
    }

    public void gainMeso(int gain) {
        getPlayer().gainMeso(gain, true, false, true);
    }

    public void gainPet(int pet) {
        final int petId = MaplePet.createPet(pet);
        MapleInventoryManipulator.addById(c, pet, (short) 1, null, petId);
        c.announce(EffectFactory.getShowItemGain(petId, (short) 1, true));
    }

    public void gainExp(int gain) {
        getPlayer().gainExp(gain, true, true);
    }

    public int getLevel() {
        return getPlayer().getLevel();
    }

    public EventManager getEventManager(String event) {
        return getClient().getChannelServer().getEventSM().getEventManager(event);
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public void showEffect(String effect) {
        getPlayer().getMap().broadcastMessage(EffectFactory.environmentChange(effect, 3));
    }

    public void playSound(String sound) {
        getPlayer().getMap().broadcastMessage(EffectFactory.environmentChange(sound, 4));
    }

    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(MapleStat.HAIR, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(MapleStat.FACE, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor(color);
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }

    public int itemQuantity(int Id) {
        return getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(Id)).countById(Id);
    }

    public void displayGuildRanks() {
        MapleGuild.displayGuildRanks(getClient(), npc);
    }

    public void environmentChange(String env, int mode) {
        getPlayer().getMap().broadcastMessage(EffectFactory.environmentChange(env, mode));
    }

    public void gainCloseness(int closeness) {
        for (MaplePet pet : getPlayer().getPets()) {
            if (pet.getCloseness() > 30000) {
                pet.setCloseness(30000);
                return;
            }
            pet.gainCloseness(closeness);
            while (pet.getCloseness() > ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                pet.setLevel(pet.getLevel() + 1);
                getClient().announce(PetFactory.showOwnPetLevelUp(getPlayer().getPetIndex(pet)));
            }
            getPlayer().getClient().announce(InventoryFactory.updateSlot(pet));
        }
    }

    public String getName() {
        return getPlayer().getName();
    }

    public int getGender() {
        return getPlayer().getGender();
    }

    public int getHiredMerchantMesos(boolean zero) {
        int mesos = 0;
        PreparedStatement ps = null;
        Connection con = DatabaseConnection.getConnection();
        try {
            ps = con.prepareStatement("SELECT merchantmesos FROM characters WHERE id = ?");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                mesos = rs.getInt("merchantmesos");
            }
            rs.close();
            ps.close();
            if (zero) {
                ps = con.prepareStatement("UPDATE characters SET merchantmesos = 0 WHERE id = ?");
                ps.setInt(1, getPlayer().getId());
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        }
        return mesos;
    }

    public int getNXShopMesos(boolean zero) {
        int mesos = 0;
        PreparedStatement ps = null;
        Connection con = DatabaseConnection.getConnection();
        try {
            ps = con.prepareStatement("SELECT nxmesos FROM characters WHERE id = ?");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                mesos = rs.getInt("nxmesos");
            }
            rs.close();
            ps.close();
            if (zero) {
                ps = con.prepareStatement("UPDATE characters SET nxmesos = 0 WHERE id = ?");
                ps.setInt(1, getPlayer().getId());
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        }
        return mesos;
    }

    public void changeJobById(int a) {
        getPlayer().changeJob(a);
    }

    public void addRandomItem(int id) {
        MapleItemInformationProvider i = MapleItemInformationProvider.getInstance();
        MapleInventoryManipulator.addFromDrop(getClient(), i.randomizeStats((Equip) i.getEquipById(id)), true);
    }

    public boolean isQuestCompleted(int quest) {
        try {
            return getPlayer().getQuest(MapleQuest.getInstance(quest)).getStatus() == 2;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean isQuestStarted(int quest) {
        try {
            return getPlayer().getQuest(MapleQuest.getInstance(quest)).getStatus() == 1;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public MapleStatEffect getItemEffect(int Id) {
        return MapleItemInformationProvider.getInstance().getItemEffect(Id);
    }

    public void resetStats() {
        getPlayer().resetStats();
    }

    public void resetStats(final int str, final int dex, final int int_, final int luk) {
    List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>(2);
    final MapleCharacter chr = c.getPlayer();
    int total = chr.getStr() + chr.getDex() + chr.getLuk() + chr.getInt() + chr.getRemainingAp();

    total -= str;
    chr.setStr(str);

    total -= dex;
    chr.setDex(dex);

    total -= int_;
    chr.setInt(int_);

    total -= luk;
    chr.setLuk(luk);

    chr.setRemainingAp(total);

    stats.add(new Pair<MapleStat, Integer>(MapleStat.STR, str));
    stats.add(new Pair<MapleStat, Integer>(MapleStat.DEX, dex));
    stats.add(new Pair<MapleStat, Integer>(MapleStat.INT, int_));
    stats.add(new Pair<MapleStat, Integer>(MapleStat.LUK, luk));
    stats.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLEAP, total));
    c.announce(IntraPersonalFactory.updatePlayerStats(stats, false));
    }

    public void levelUp() {
        getPlayer().gainExp(ExpTable.getExpNeededForLevel(getPlayer().getLevel()) - getPlayer().getExp(), false, false);
    }

    public void levelUp(int times) {
        for (int i = 0; i < times; i++)
        getPlayer().gainExp(ExpTable.getExpNeededForLevel(getPlayer().getLevel()) - getPlayer().getExp(), false, false);
    }

    public void expandInventory(byte type, int amt) {
        c.getPlayer().getInventory(MapleInventoryType.getByType(type)).addSlot((byte) 4);
    }

    public void maxMastery() {
        for (MapleData skill_ : MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren()) {
            try {
                ISkill skill = SkillFactory.getSkill(Integer.parseInt(skill_.getName()));
                if ((skill.getId() / 10000 % 10 == 2 || (getClient().getPlayer().isCygnus() && skill.getId() / 10000 % 10 == 1)) && getPlayer().getSkillLevel(skill) < 1) {
                    getPlayer().changeSkillLevel(skill, 0, skill.getMaxLevel());
                }
            } catch (NumberFormatException nfe) {
                break;
            } catch (NullPointerException npe) {
                continue;
            }
        }
    }

    public void processGachapon(int[] id, boolean remote) {
        int[] gacMap = {100000000, 101000000, 102000000, 103000000, 105040300, 800000000, 809000101, 809000201, 600000000, 120000000};
        int Id = id[Randomizer.getInstance().nextInt(id.length)];
        addRandomItem(Id);
        if (!remote) {
            gainItem(5220000, (short) -1);
        }
        sendNext("You have obtained a #b#t" + Id + "##k.");
    }

    public void disbandAlliance(MapleClient c, int allianceId) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM `alliance` WHERE id = ?");
            ps.setInt(1, allianceId);
            ps.executeUpdate();
            ps.close();
            c.getChannelServer().getWorldInterface().allianceMessage(c.getPlayer().getGuild().getAllianceId(), GuildFactory.disbandAlliance(allianceId), -1, -1);
            c.getChannelServer().getWorldInterface().disbandAlliance(allianceId);
        } catch (RemoteException r) {
            c.getChannelServer().reconnectWorld();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        }
    }

    public boolean canBeUsedAllianceName(String name) {
        if (name.contains(" ") || name.length() > 12) {
            return false;
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name FROM alliance WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ps.close();
                rs.close();
                return false;
            }
            ps.close();
            rs.close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static MapleAlliance createAlliance(MapleCharacter chr1, MapleCharacter chr2, String name) {
        int id = 0;
        int guild1 = chr1.getGuildId();
        int guild2 = chr2.getGuildId();
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO `alliance` (`name`, `guild1`, `guild2`) VALUES (?, ?, ?)");
            ps.setString(1, name);
            ps.setInt(2, guild1);
            ps.setInt(3, guild2);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            id = rs.getInt(1);
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        MapleAlliance alliance = new MapleAlliance(name, id, guild1, guild2);
        try {
            chr1.setAllianceRank(1);
            chr1.saveGuildStatus();
            chr2.setAllianceRank(2);
            chr2.saveGuildStatus();
            WorldChannelInterface wci = chr1.getClient().getChannelServer().getWorldInterface();
            wci.setGuildAllianceId(guild1, id);
            wci.setGuildAllianceId(guild2, id);
            wci.addAlliance(id, alliance);
            wci.allianceMessage(id, GuildFactory.makeNewAlliance(alliance, chr1.getClient()), -1, -1);
        } catch (RemoteException e) {
            chr1.getClient().getChannelServer().reconnectWorld();
            chr2.getClient().getChannelServer().reconnectWorld();
            return null;
        }
        return alliance;
    }

    public static int updateAlliance(MapleCharacter initiator, MapleCharacter member) {
        int gid = member.getGuild().getId();
        int aid = initiator.getGuild().getAllianceId();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM `alliance` WHERE `id` = ?");
            ps.setInt(1, aid);
            ps.executeUpdate();
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                for (int i = 1; i <= 5; i++) {
                    if (rs.getInt("guild" + i) == -1) {
                        rs.close();
                        ps.close();
                        ps = con.prepareStatement("UPDATE `alliance` SET guild" + i + " = ? WHERE `id` = ?");
                        ps.setInt(1, member.getGuildId());
                        ps.setInt(2, aid);
                        ps.executeUpdate();
                        ps.close();
                        WorldChannelInterface wci = member.getClient().getChannelServer().getWorldInterface();
                        wci.setGuildAllianceId(gid, aid);
                        wci.addGuildtoAlliance(aid, gid);
                        member.setAllianceRank(2);
                        member.saveGuildStatus();
                        wci.allianceMessage(aid, GuildFactory.makeNewAlliance(initiator.getClient().getChannelServer().getWorldInterface().getAlliance(aid), initiator.getClient()), -1, -1);
                        return 1;
                    }
                }
            }
            rs.close();
            ps.close();
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -2;
        }
    }

    public void addRemGuildFromDB(int gid, boolean add) throws RemoteException {
        c.getChannelServer().getWorldInterface().getAlliance(c.getPlayer().getGuild().getAllianceId()).addRemGuildFromDB(gid, add);
    }

    public void removeHiredMerchantItem(int id, boolean save) {
        try {
            List<Pair<IItem, MapleInventoryType>> workingList = getHiredMerchantItems();
            for (Pair<IItem, MapleInventoryType> p : workingList) {
                if (p.getLeft().getDBID() == id) {
                    workingList.remove(p);
                    break;
                }
            }
            if(save) {
                ItemFactory.MERCHANT.saveItems(workingList, c.getPlayer().getId());
            }
        } catch (Exception e) {
        }
    }

    public void saveHiredMerchantItems() {
        try {
            ItemFactory.MERCHANT.saveItems(getHiredMerchantItems(), c.getPlayer().getId());
        } catch (Exception e) {
        }
    }

    public List<Pair<IItem, MapleInventoryType>> getHiredMerchantItems() {
        try {
            return ItemFactory.MERCHANT.loadItems(c.getPlayer().getId(), false);
        } catch (Exception e) {
            System.out.println("Error loading merchant items:");
            e.printStackTrace();
        }
        return null;
    }

    public void getAndRemoveAllHiredMerchantItems() {
        List<Pair<IItem, MapleInventoryType>> items = null;
        List<Pair<IItem, MapleInventoryType>> toRemove = new LinkedList<Pair<IItem, MapleInventoryType>>();

        try {
            items = ItemFactory.MERCHANT.loadItems(c.getPlayer().getId(), false);
        } catch (Exception e) {
            System.out.println("Error loading merchant items:");
            e.printStackTrace();
        }
        int iterations = items.size();
        for(int id = 0; id < iterations; id++) {
            if(!canHold(items.get(id).getLeft().getId())) {
                sendOk("Make some space in your inventory for the item please.");
                for(Pair<IItem, MapleInventoryType> p : toRemove) {
                    items.remove(p);
                }
                try {
                    ItemFactory.MERCHANT.saveItems(items, c.getPlayer().getId());
                } catch (Exception e) {
                    System.out.println("Error saving merchant items:");
                    e.printStackTrace();
                }
                dispose();
                return;
            } else {
                if(items.get(id).getLeft().getId() < 2000000)
                MapleInventoryManipulator.addFromDrop(getClient(), (Equip) items.get(id).getLeft().copy());
                else
                MapleInventoryManipulator.addById(getClient(), items.get(id).getLeft().getId(), items.get(id).getLeft().getQuantity());
                for (Pair<IItem, MapleInventoryType> p : items) {
                    if (p.getLeft().getDBID() == items.get(id).getLeft().getDBID()) {
                        toRemove.add(p);
                        break;
                    }
                }
            }
        }
        for(Pair<IItem, MapleInventoryType> p : toRemove) {
            items.remove(p);
        }
        try {
            ItemFactory.MERCHANT.saveItems(items, c.getPlayer().getId());
        } catch (Exception e) {
            System.out.println("Error saving merchant items:");
            e.printStackTrace();
        }
        sendOk("Thank you for using our services, you should find all items added to your character.\r\n\r\nHave fun on #bCorsaireMS#k!");
        dispose();
    }

    public int partyMembersInMap() {
        int inMap = 0;
        for (MapleCharacter char2 : getPlayer().getMap().getCharacters()) {
            if (char2.getParty() == getPlayer().getParty()) {
                inMap++;
            }
        }
        return inMap;
    }

    public int getAverageLevel(MapleParty mp) {
        int total = 0;
        for (MaplePartyCharacter mpc : mp.getMembers()) {
            total += mpc.getLevel();
        }
        return total / mp.getMembers().size();
    }

    public void removeAranSkills() {
        for (Entry<ISkill, SkillEntry> skill : getPlayer().getSkills().entrySet()) {
            if(skill.getValue().skillevel >= 20000014 && skill.getValue().skillevel <= 20000018)
            getPlayer().getSkills().remove(skill.getKey());
        }
        getPlayer().getClient().announce(IntraPersonalFactory.getCharInfo(getPlayer()));
        getPlayer().setSaveSkills();
    }

    public void sendShop(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(c);
    }

    public void addNXShopItem(int shopid, int itemid, int price) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO `shopitems` (`shopid`, `itemid`, `price`, `position`, `characterid`) VALUES (?, ?, ?, 0, ?)");
            ps.setInt(1, shopid);
            ps.setInt(2, itemid);
            ps.setInt(3, price);
            ps.setInt(4, getPlayer().getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeNXShopItem(int itemid, int price) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("DELETE from shopitems WHERE itemid = ? AND price = ? AND characterid = ? LIMIT 1");
            ps.setInt(1, itemid);
            ps.setInt(2, price);
            ps.setInt(3, getPlayer().getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Pair<Integer, Integer>> getNXShopItems() {
        List<Pair<Integer, Integer>> items = new ArrayList<Pair<Integer, Integer>>();
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT itemid, price FROM `shopitems` WHERE characterid = ?");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(new Pair<Integer, Integer>(rs.getInt("itemid"), rs.getInt("price")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public void getAndRemoveAllNXShopItems() { //DISGUSTING CODE IS DISGUSTING
        List<Integer> items = new ArrayList<Integer>();
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT itemid FROM `shopitems` WHERE characterid = ?");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(rs.getInt("itemid"));
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            System.out.println("Error loading merchant items:");
            e.printStackTrace();
        }
        int iterations = items.size();
        for(int id = 0; id < iterations; id++) {
            if(!canHold(items.get(id))) {
                sendOk("Make some space in your inventory for the item please.");
                dispose();
                return;
            } else {
                try {
                    PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("DELETE from shopitems WHERE itemid = ? AND characterid = ? LIMIT 1");
                    ps.setInt(1, items.get(id));
                    ps.setInt(2, getPlayer().getId());
                    ps.executeUpdate();
                    ps.close();
                } catch (Exception e) {
                    System.out.println("Error deleting merchant items:");
                    e.printStackTrace();
                }
                MapleInventoryManipulator.addById(getClient(), items.get(id), (short) 1);
            }
        }
        sendOk("Thank you for using our services, you should find all items added to your character.\r\n\r\nHave fun on #bCorsaireMS#k!");
        dispose();
    }
}

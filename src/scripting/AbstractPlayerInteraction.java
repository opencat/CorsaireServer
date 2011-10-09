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

package scripting;

import java.util.Arrays;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import tools.DatabaseConnection;
import client.Equip;
import client.IItem;
import client.Item;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MaplePet;
import client.SkillFactory;
import constants.InventoryConstants;
import java.awt.Point;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.LinkedList;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import net.world.guild.MapleGuild;
import scripting.npc.NPCScriptManager;
import scripting.reactor.ReactorScriptManager;
import server.DatabaseInformationProvider;
import server.DropEntry;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.TimerManager;
import server.life.MapleLifeFactory;
import server.maps.MapMonitor;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleReactor;
import server.maps.SavedLocationType;
import server.quest.MapleQuest;
import tools.factory.BuffFactory;
import tools.factory.EffectFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.QuestFactory;

/**
 * @name        AbstractPlayerInteraction
 * @author      Matze
 *              Modified by x711Li
 */
public class AbstractPlayerInteraction {
    public MapleClient c;
    private MapleReactor reactor;

    public AbstractPlayerInteraction(MapleClient c) {
        this.c = c;
    }

    public AbstractPlayerInteraction(MapleClient c, MapleReactor reactor) {
        this.c = c;
        this.reactor = reactor;
    }

    public MapleClient getClient() {
        return c;
    }

    public MapleCharacter getPlayer() {
        return c.getPlayer();
    }

    public void warp(int map) {
        getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(0));
    }

    public void warp(int map, int portal) {
        getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(portal));
    }

    public void warp(int map, String portal) {
        getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(portal));
    }

    public void warpMap(int map) {
        for (MapleCharacter mc : getPlayer().getMap().getCharacters()) {
            mc.changeMap(getWarpMap(map), getWarpMap(map).getPortal(0));
        }
    }

    public final boolean isPQReqMet(int amt, int min, int max) {
        int x = 0;
        for (final MapleCharacter partymem : c.getChannelServer().getPartyMembers(c.getPlayer().getParty())) {
            x++;
            if (!(partymem.getLevel() >= min && partymem.getLevel() <= max)) {
                return false;
            }
        }
        if(x != amt) {
            return false;
        }
        return true;
    }
    
    public final boolean allMembersHere() {
        boolean allHere = true;
        for (final MapleCharacter partymem : c.getChannelServer().getPartyMembers(c.getPlayer().getParty())) {
            if (partymem.getMapId() != c.getPlayer().getMapId()) {
                allHere = false;
                break;
            }
        }
        return allHere;
    }

    protected MapleMap getWarpMap(int map) {
        MapleMap target;
        if (getPlayer().getEventInstance() == null) {
            target = c.getChannelServer().getMapFactory().getMap(map);
        } else {
            target = getPlayer().getEventInstance().getMapInstance(map);
        }
        return target;
    }

    public MapleMap getMap(int map) {
        return getWarpMap(map);
    }

    public boolean haveItem(int Id) {
        return haveItem(Id, 1);
    }

    public boolean haveItem(int Id, int quantity) {
        return c.getPlayer().getItemQuantity(Id, false) >= quantity;
    }

    public boolean canHold(int Id) {
        return c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(Id)).getNextFreeSlot() > -1;
    }

    public boolean canHold(MapleClient c, int Id) {
        return c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(Id)).getNextFreeSlot() > -1;
    }

    public int getQuestStatus(int id) {
        return c.getPlayer().getQuest(MapleQuest.getInstance(id)).getStatus();
    }

    public final void gainItem(MapleClient c, int id, short quantity, boolean randomStats) {
        gainItem(id, quantity, randomStats, -1);
    }

    public final void gainItem(int id, short quantity) {
        gainItem(id, quantity, false, -1);
    }

    public final void gainItem(int id) {
        gainItem(id, (short) 1, false, -1);
    }

    public final void gainItem(int id, short quantity, boolean randomStats, boolean flag) {
        gainItem(id, quantity, randomStats, -1);
    }

    public final void gainItem(int id, short quantity, boolean randomStats, int appliedFlag) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem item = ii.getEquipById(id);
        if (item == null) {
            return;
        } else if (quantity >= 0) {
            if (id >= 5000000 && id <= 5000100) {
                MapleInventoryManipulator.addById(c, id, (short) 1, null, MaplePet.createPet(id));
            }
            if (!MapleInventoryManipulator.checkSpace(c, id, quantity, "")) {
                c.getPlayer().dropMessage(1, "Your inventory is full. Please remove an item from your " + ii.getInventoryType(id).name() + " inventory.");
                return;
            }
            if (appliedFlag > 0) {
                item.setOwner(c.getPlayer().getName());
                byte flag = item.getFlag();
                flag |= 0x01;
                item.setFlag(flag);
            }
            if (ii.getInventoryType(id).equals(MapleInventoryType.EQUIP) && !InventoryConstants.isRechargable(item.getId())) {
                if (randomStats) {
                    MapleInventoryManipulator.addFromDrop(c, ii.randomizeStats((Equip) item), false);
                } else {
                    MapleInventoryManipulator.addFromDrop(c, (Equip) item, false);
                }
            } else {
                MapleInventoryManipulator.addById(c, id, quantity);
            }
        } else {
            MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
        }
        c.announce(EffectFactory.getShowItemGain(id, quantity, true));
    }

    public void changeMusic(String songName) {
        getPlayer().getMap().broadcastMessage(EffectFactory.musicChange(songName));
    }

    public void playerMessage(int type, String message) {
        c.announce(EffectFactory.serverNotice(type, message));
    }

    public void mapMessage(int type, String message) {
        getPlayer().getMap().broadcastMessage(EffectFactory.serverNotice(type, message));
    }

    public void guildMessage(int type, String message) {
        if (getGuild() != null) {
            getGuild().guildMessage(EffectFactory.serverNotice(type, message));
        }
    }

    public MapleGuild getGuild() {
        try {
            return c.getChannelServer().getWorldInterface().getGuild(getPlayer().getGuildId(), null);
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
        return null;
    }

    public MapleParty getParty() {
        return getPlayer().getParty();
    }

    public boolean isLeader() {
        if (c.getPlayer().getParty() != null) {
            return getParty().getLeader().equals(new MaplePartyCharacter(c.getPlayer()));
        }
        return false;
    }

    public void givePartyItems(int id, short quantity, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            MapleClient cl = chr.getClient();
            if (quantity >= 0) {
                MapleInventoryManipulator.addById(cl, id, quantity);
            } else {
                MapleInventoryManipulator.removeById(cl, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
            }
            cl.announce(EffectFactory.getShowItemGain(id, quantity, true));
        }
    }

    public void givePartyExp(int amount) {
        for (MapleCharacter mc : getPartyMembers()) {
            mc.gainExp((int) (amount), true, true);
        }
    }

    public void givePartyExpEx(int amount, int min, int max) {
        for (MapleCharacter mc : getPartyMembers()) {
            if(mc.getMap().getId() >= min && mc.getMap().getId() <= max && mc.isAlive())
            mc.gainExp((int) (amount), true, true);
        }
    }

    public void gainExp(int gain) {
        c.getPlayer().gainExp(gain, true, true, true);
    }

    public void partySetPQI(String item) {
        for (MapleCharacter mc : getPartyMembers()) {
            mc.setPartyQuestItemObtained(item);
        }
    }

    public void partyRemovePQI(String item) {
        for (MapleCharacter mc : getPartyMembers()) {
            mc.removePartyQuestItem(item);
        }
    }

    public void removeFromParty(int id) {
        for (MapleCharacter mc : getPartyMembers()) {
            removeAll(id, mc.getClient());
        }
    }

    public void removeAll(int id) {
        removeAll(id, c);
    }

    public void removeAll(int id, MapleClient cl) {
        int possessed = cl.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(id)).countById(id);
        if (possessed > 0) {
            MapleInventoryManipulator.removeById(cl, MapleItemInformationProvider.getInstance().getInventoryType(id), id, possessed, true, false);
            cl.announce(EffectFactory.getShowItemGain(id, (short) -possessed, true));
        }
    }
    
    public int getMapId() {
        return c.getPlayer().getMap().getId();
    }

    public MapleMap getMap() {
        return c.getPlayer().getMap();
    }

    public List<MapleCharacter> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<MapleCharacter> chars = new LinkedList<MapleCharacter>();
        for (MapleCharacter chr : getClient().getChannelServer().getPartyMembers(getPlayer().getParty(), -1)) {
            if (chr != null) {
                chars.add(chr);
            }
        }
        return chars;
    }

    public void warpParty(int id) {
        for (MapleCharacter mc : getPartyMembers()) {
            mc.changeMap(getWarpMap(id));
        }
    }

    public void warpPartyEx(int id, int min, int max) {
        for (MapleCharacter mc : getPartyMembers()) {
            if(mc.getMap().getId() >= min && mc.getMap().getId() <= max)
            mc.changeMap(getWarpMap(id));
        }
    }

    public void reloadMap(int map) {
        MapleMap newMap = c.getChannelServer().getMapFactory().getMap(map, true, true, true, true, true);
        for (MapleCharacter ch : c.getPlayer().getMap().getCharacters()) {
            ch.changeMap(newMap);
        }
        //newMap.empty();
        newMap = null;
        c.getPlayer().getMap().respawn(false);
    }

    public void giveMapExp(int amount) {
        for (MapleCharacter mc : c.getPlayer().getMap().getCharacters()) {
            mc.gainExp((int) (amount), true, true);
        }
    }

    public void gainItemMap(int id, short quantity) {
        for (MapleCharacter mc : c.getPlayer().getMap().getCharacters()) {
            if(mc.isAlive() && !mc.isGM())
            gainItem(mc.getClient(), id, quantity, false);
        }
    }

    public void reloadWarp(int map) {
        MapleMap newMap = c.getChannelServer().getMapFactory().getMap(map, true, true, true, true, true);
        c.getPlayer().changeMap(newMap);
        //newMap.empty();
        newMap = null;
        c.getPlayer().getMap().respawn(false);
    }

    public void reloadParty(int map) {
        MapleMap newMap = c.getChannelServer().getMapFactory().getMap(map, true, true, true, true, true);
        for (MapleCharacter ch : getPartyMembers()) {
            ch.changeMap(newMap);
        }
        //newMap.empty();
        newMap = null;
        c.getPlayer().getMap().respawn(false);
    }

    public void reloadPartyEx(int map, int min, int max) {
        MapleMap newMap = c.getChannelServer().getMapFactory().getMap(map, true, true, true, true, true);
        for (MapleCharacter ch : getPartyMembers()) {
            if(ch.getMap().getId() >= min && ch.getMap().getId() <= max && ch.isAlive())
            ch.changeMap(newMap);
        }
        //newMap.empty();
        newMap = null;
        c.getPlayer().getMap().respawn(false);
    }

    public int getPlayerCount(int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getCharacters().size();
    }

    public void showInstruction(String msg, int width, int height) {
        c.announce(EffectFactory.sendHint(msg, width, height));
        c.announce(IntraPersonalFactory.enableActions());
    }

    public void resetMap(int mapid) {
        getMap(mapid).resetReactors();
        getMap(mapid).killAllMonsters();
        for (MapleMapObject i : getMap(mapid).getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM))) {
            getMap(mapid).removeMapObject(i);
            getMap(mapid).broadcastMessage(EffectFactory.removeItemFromMap(i.getObjectId(), 0, c.getPlayer().getId()));
        }
    }

    public void sendClock(MapleClient d, int time) {
        d.announce(EffectFactory.getClock((int) (time - System.currentTimeMillis()) / 1000));
    }

    public void useItem(int id) {
        MapleItemInformationProvider.getInstance().getItemEffect(id).applyTo(c.getPlayer());
        c.announce(EffectFactory.getStatusMsg(id));
    }

    public void mapEffect(String path) {
        c.announce(EffectFactory.showMapEffect(path));
    }

    public final int getSavedLocation(final String loc) {
        final Integer ret = c.getPlayer().getSavedLocation(loc);
        if (ret == null || ret == -1) {
            return 102000000;
        }
        return ret;
    }

    public final boolean isServerShuttingDown() {
        return c.getChannelServer().isShuttingDown();
    }

    public final void saveLocation(final String loc) {
        c.getPlayer().saveLocation(loc);
    }

    public final void clearSavedLocation(final String loc) {
        c.getPlayer().clearSavedLocation(SavedLocationType.fromString(loc));
    }

    public final void summonMsg(final String msg) {
        c.announce(EffectFactory.talkGuide(msg));
    }

    public final void summonMsg(final int type) {
        c.announce(EffectFactory.talkGuide(type));
    }

    public final void playerSummonHint(final boolean summon) {
        c.announce(EffectFactory.spawnGuide(summon));
    }

    public final void introLock(final boolean enabled) {
        c.announce(EffectFactory.tutorialIntroDisableUI(enabled));
        c.announce(EffectFactory.tutorialIntroLock(enabled));
    }

    public final void showInfo(final String data) {
        c.announce(EffectFactory.showInfo(data));
    }

    public final void showWZEffect(final String data) {
        c.announce(EffectFactory.showWZEffect(data, -1));
    }

    public final int getMonsterCount(final int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getAllObjects(MapleMapObjectType.MONSTER).size();
    }

    public final void openNpc(final int id) {
        NPCScriptManager.getInstance().start(getClient(), id);
    }

    public final void playPortalSE() {
        c.announce(BuffFactory.showOwnBuffEffect(0, 7));
    }

    public final void teachSkill(final int id, final byte level, final byte masterlevel) {
        getPlayer().changeSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public final void playSlots() {
        playSlots(-1, -1, true);
    }

    public final void playSlots(final int level, final int job, final boolean show) { //TODO TIMING
        int result[] = { (int) (Math.random() * 5), 0, 0 };
        int typeFactor = (int) (Math.random() * 15);
        int levelFactor = (int) (Math.random() * 21); //o_O? 0 - 20? really?
        if (typeFactor >= 10) {
            result[1] = 3; //Shoe
        } else if (typeFactor >= 6) {
            result[1] = 2; //Glove
        } else if (typeFactor >= 3) {
            result[1] = 0; //Cap
        } else if (typeFactor >= 1) {
            result[1] = 1; //Top
        } else if (typeFactor == 0) {
            result[1] = 4; //Weapon
        }
        if (levelFactor >= 15) {
            result[2] = 0; //30
        } else if (levelFactor >= 10) {
            result[2] = 1; //40
        } else if (levelFactor >= 6) {
            result[2] = 2; //50
        } else if (levelFactor >= 3) {
            result[2] = 3; //60
        } else if (levelFactor >= 1) {
            result[2] = 4; //70
        } else if (levelFactor == 0) {
            result[2] = 5; //80
        }
        if (job >= 0) {
            result[0] = job;
        }
        if (level >= 0) {
            result[2] = level;
        }
        if (show) {
            getClient().announce(EffectFactory.effectDisplay("miro/frame"));
            for(int i = 1; i <= 3; i++) {
                getClient().announce(EffectFactory.effectDisplay("miro/RR" + i + "/" + result[i - 1]));
            }
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `element` FROM `" + "cache_slots" + result[0] + "" + result[2] + "` WHERE ROUND(element" + (result[1] == 4 ? ") >= 1302008" : result[1] == 1 ? " / 10000) = 104 OR ROUND(element / 10000) = 105" : " / 10000) = ?") + " ORDER BY RAND() LIMIT 1");
            if (result[1] != 4 && result[1] != 1) {
                switch(result[1]) {
                    case 0:
                        ps.setInt(1, 100);
                        break;
                    case 2:
                        ps.setInt(1, 108);
                        break;
                    case 3:
                        ps.setInt(1, 107);
                        break;
                    default:
                        break;
                }
            }
            final ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                final int element = rs.getInt("element");
                if (show) {
                    TimerManager.getInstance().schedule(new Runnable() {
                        public void run() {
                            gainItem(element);
                        }
                    }, 3500);
                } else {
                    gainItem(element);
                }
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final int getRandElement(String table) {
        return DatabaseInformationProvider.getInstance().getRandElement(table);
    }

    public void dropItems() {
        dropItems(false, 0, 0, 0, 0);
    }

    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso) {
        dropItems(meso, mesoChance, minMeso, maxMeso, 0);
    }

    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        if(c.getPlayer().gmLevel() == 1 || c.getPlayer().gmLevel() == 2) {
            return;
        }
        List<DropEntry> chances = ReactorScriptManager.getInstance().getDrops(reactor.getId());
        List<DropEntry> items = new LinkedList<DropEntry>();
        int numItems = 0;
        if (meso && Math.random() < (1 / (double) mesoChance)) {
            items.add(new DropEntry(0, mesoChance, 0));
        }
        Iterator<DropEntry> iter = chances.iterator();
        while (iter.hasNext()) {
            DropEntry d = iter.next();
            if (d.getQuest() > 0 && c.getPlayer().getQuest(MapleQuest.getInstance(d.getQuest())).getStatus() != 1) {
                continue;
            }
            if (Math.random() < (1 / (double) d.getChance())) {
                numItems++;
                items.add(d);
            }
        }
        while (items.size() < minItems) {
            items.add(new DropEntry(0, mesoChance, 0));
            numItems++;
        }
        java.util.Collections.shuffle(items);
        final Point dropPos = reactor.getPosition();
        dropPos.x -= (12 * numItems);
        for (DropEntry d : items) {
            if (d.getId() == 0) {
                int range = maxMeso - minMeso;
                int displayDrop = (int) (Math.random() * range) + minMeso;
                int mesoDrop = (displayDrop);
                final int dropperId = reactor.getId();
                final Point dropperPos = reactor.getPosition();
                reactor.getMap().spawnMesoDrop(mesoDrop, displayDrop, dropPos, dropperId, dropperPos, getPlayer(), false);
            } else {
                IItem drop;
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (ii.getInventoryType(d.getId()) != MapleInventoryType.EQUIP) {
                    drop = new Item(d.getId(), (byte) 0, (short) 1);
                } else {
                    drop = ii.randomizeStats((Equip) ii.getEquipById(d.getId()));
                }
                reactor.getMap().spawnItemDrop(reactor.getObjectId(), reactor.getPosition(), getPlayer(), drop, dropPos, false, true);
            }
            dropPos.x += 25;
        }
    }

    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPosition());
    }

    public void spawnMonster(int id, int x, int y) {
        spawnMonster(id, 1, new Point(x, y));
    }

    public void createMapMonitor(String portalName, int portalMapId, String portalReactorName) {
        new MapMonitor(c.getChannelServer().getMapFactory(), reactor.getMap(), portalName, portalMapId, portalReactorName);
    }
    public void spawnMonster(int id, int qty) {
        spawnMonster(id, qty, getPosition());
    }

    public void spawnMonster(int id, int qty, int x, int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    private void spawnMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            reactor.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getInstance().getMonster(id), pos);
        }
    }

    public Point getPosition() {
        Point pos = reactor.getPosition();
        pos.y -= 10;
        return pos;
    }

    public void spawnNpc(int npcId) {
        spawnNpc(npcId, getPosition());
    }

    public void spawnNpc(int npcId, Point pos) {
        reactor.getMap().spawnNpc(npcId, pos);
    }

    public MapleReactor getReactor() {
        return reactor;
    }

    public void spawnFakeMonster(int id) {
        reactor.getMap().spawnFakeMonsterOnGroundBelow(MapleLifeFactory.getInstance().getMonster(id), getPosition());
    }

    public void addQuestInfo(short id, String info) {
        getClient().announce(QuestFactory.updateQuestInfo((byte) 1, id, info));
        getPlayer().addQuestInfo(id, info);
    }

    public final void inFreeMarket() {
    if (getMapId() != 910000000) {
        if (getPlayer().getLevel() > 10) {
        saveLocation("FREE_MARKET");
        playPortalSE();
        warp(910000000, "st00");
        } else {
        playerMessage(5, "You must be over level 10 to enter here.");
        }
    }
    }
}
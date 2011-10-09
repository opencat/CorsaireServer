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

package server.maps;

import server.shops.HiredMerchant;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import client.Equip;
import client.IEquip;
import client.IItem;
import client.Item;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MaplePet;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.InventoryConstants;
import constants.ServerConstants;
import java.io.File;
import tools.Randomizer;
import scripting.map.MapScriptManager;
import net.MaplePacket;
import net.channel.ChannelServer;
import net.world.MaplePartyCharacter;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleStatEffect;
import server.TimerManager;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.life.MapleNPC;
import server.life.SpawnPoint;
import server.quest.MapleQuest;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.HashMap;
import scripting.timer.TimerScriptManager;
import server.DropEntry;
import server.MapleInventoryManipulator;
import server.PropertiesTable;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import tools.Pair;
import tools.factory.BuffFactory;
import tools.factory.EffectFactory;
import tools.factory.InterPersonalFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.MobFactory;
import tools.factory.NPCFactory;
import tools.factory.PetFactory;
import tools.factory.QuestFactory;
import tools.factory.ReactorFactory;
import tools.factory.SummonFactory;

/**
 * @name        MapleMap
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleMap {
    private static final List<MapleMapObjectType> rangedMapobjectTypes = Arrays.asList(MapleMapObjectType.SHOP, MapleMapObjectType.ITEM, MapleMapObjectType.NPC, MapleMapObjectType.MONSTER, MapleMapObjectType.DOOR, MapleMapObjectType.SUMMON, MapleMapObjectType.REACTOR);
    private final Map<Integer, MapleMapObject> mapobjects = new HashMap<Integer, MapleMapObject>();
    private final Collection<SpawnPoint> monsterSpawn = new LinkedList<SpawnPoint>();
    private final AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private final List<MapleCharacter> characters = new ArrayList<MapleCharacter>();
    private final Map<Integer, MaplePortal> portals = new HashMap<Integer, MaplePortal>();
    private final List<Rectangle> areas = new ArrayList<Rectangle>();
    private MapleFootholdTree footholds = null;
    private float monsterRate;
    private MapleMapEffect mapEffect = null;
    private int channel, runningOid = 30000, forcedReturnMap = 999999999, decHP = 0, protectItem = 0, timeLimit = 0, mapid, returnMapId, fieldLimit = 0, dp = 0;
    private boolean town = false, everlast = false, clock = false;
    private String mapName, streetName, bgm, onFirstUserEnter, onUserEnter;
    private long timerStarted = 0, timerTime = 0;
    private PropertiesTable properties;
    public ScheduledFuture respawnTask = null;
    public ScheduledFuture<?> scriptTask = null;
    private ReentrantReadWriteLock objectlock = new ReentrantReadWriteLock(true);
    private ReentrantReadWriteLock characterlock = new ReentrantReadWriteLock(true);

    public MapleMap(final int mapid, final int channel, final int returnMapId, final float monsterRate) {
        this.mapid = mapid;
        this.channel = (short) channel;
        this.returnMapId = returnMapId;
        this.monsterRate = monsterRate;
        this.properties = new PropertiesTable();
        properties.setProperty("mute", Boolean.FALSE);
        properties.setProperty("aoe", Boolean.TRUE);
        properties.setProperty("drops", Boolean.TRUE);
        properties.setProperty("respawn", Boolean.TRUE);
        properties.setProperty("skills", Boolean.TRUE);
        if(this.mapid == 200090300 || mapid == 980000404) {
            properties.setProperty("jail", Boolean.TRUE);
        } else {
            this.properties.setProperty("jail", Boolean.FALSE);
        }
        initiateRespawnTask();
    }

    public final void toggleDrops() {
        this.properties.setProperty("drops", properties.getProperty("drops").equals(Boolean.TRUE) ? Boolean.FALSE: Boolean.TRUE);
    }

    public final int getId() {
        return mapid;
    }

    public final MapleMap getReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(returnMapId);
    }

    public final int getReturnMapId() {
        return returnMapId;
    }

    public final void setReactorState() {
        objectlock.readLock().lock();
        try {
            for (final MapleMapObject o : mapobjects.values()) {
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    ((MapleReactor) o).setState((byte) 1);
                    broadcastMessage(ReactorFactory.triggerReactor((MapleReactor) o, 1));
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
    }

    public final int getForcedReturnId() {
        return forcedReturnMap;
    }

    public final MapleMap getForcedReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(forcedReturnMap);
    }

    public final void setForcedReturnMap(final int map) {
        this.forcedReturnMap = map;
    }

    public final int getCurrentPartyId() {
        characterlock.writeLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (chr.getPartyId() != -1) {
                    return chr.getPartyId();
                }
            }
        } finally {
            characterlock.writeLock().unlock();
        }
        return -1;
    }

    public final void addMapObject(final MapleMapObject mapobject) {
        objectlock.writeLock().lock();
        try {
            mapobject.setObjectId(runningOid);
            this.mapobjects.put(Integer.valueOf(runningOid), mapobject);
            incrementRunningOid();
        } finally {
            objectlock.writeLock().unlock();
        }
    }

    private final void spawnAndAddRangedMapObject(final MapleMapObject mapobject, final DelayedPacketCreation packetbakery) {
        spawnAndAddRangedMapObject(mapobject, packetbakery, null);
    }

    private final void spawnAndAddRangedMapObject(final MapleMapObject mapobject, final DelayedPacketCreation packetbakery, final SpawnCondition condition) {
        objectlock.writeLock().lock();
        characterlock.readLock().lock();
        try {
            mapobject.setObjectId(runningOid);
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (condition == null || condition.canSpawn(chr)) {
                    if (chr.getPosition().distanceSq(mapobject.getPosition()) <= 722500) {
                        packetbakery.sendPackets(chr.getClient());
                        chr.addVisibleMapObject(mapobject);
                    }
                }
            }
            this.mapobjects.put(Integer.valueOf(runningOid), mapobject);
            incrementRunningOid();
        } finally {
            characterlock.readLock().unlock();
            objectlock.writeLock().unlock();
        }
    }

    private final void incrementRunningOid() {
        runningOid++;
    }

    public final void removeMapObject(final int num) {
        objectlock.writeLock().lock();
        try {
            this.mapobjects.remove(Integer.valueOf(num));
        } finally {
            objectlock.writeLock().unlock();
        }
    }

    public final void removeMapObject(final MapleMapObject obj) {
        removeMapObject(obj.getObjectId());
    }

    private final Point calcPointBelow(final Point initial) {
        MapleFoothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            final double s1 = Math.abs(fh.getY2() - fh.getY1());
            final double s2 = Math.abs(fh.getX2() - fh.getX1());
            final double s5 = Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2)));
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) s5;
            } else {
                dropY = fh.getY1() + (int) s5;
            }
        }
        return new Point(initial.x, dropY);
    }

    private final Point calcDropPos(final Point initial, final Point fallback) {
        Point ret = calcPointBelow(new Point(initial.x, initial.y - 99));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    public void dropFromMonster(final MapleCharacter chr, final MapleMonster mob) {
        if (mob.dropsDisabled() || this.properties.getProperty("drops").equals(Boolean.FALSE) || chr.gmLevel() == 1 || chr.gmLevel() == 2) {
            return;
        }
        final byte droptype = (byte) (mob.isBoss() ? 3 : chr.getParty() != null ? 1 : 0);
        final int mobpos = mob.getPosition().x;
        IItem idrop;
        byte d = 1;
        List<DropEntry> toDrop = MapleMonsterInformationProvider.getInstance().retrieveDropChances(mob.getId());
        if (Randomizer.nextInt(10) < 4) {
            Pair<Integer, Integer> mesoLimits = MapleMonsterInformationProvider.getInstance().retrieveMesoChances(mob.getId());
            if (mesoLimits != null) {
                int mesos = (Randomizer.nextInt(mesoLimits.getRight() - mesoLimits.getLeft()) + mesoLimits.getLeft()) * ServerConstants.MESO_RATE;
                if (chr.getBuffedValue(MapleBuffStat.MESOUP) != null) {
                    mesos = (int) (mesos * chr.getBuffedValue(MapleBuffStat.MESOUP).doubleValue() / 100.0);
                }
                final int finalmesos = mesos;
                final Point pos = new Point(
                        (droptype == 3 ? (int) (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2)))) : (int) (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))))),
                        mob.getPosition().y);
                TimerManager.getInstance().schedule(new Runnable() {
                    public void run() {
                        spawnMesoDrop(finalmesos, finalmesos, pos, mob.getObjectId(), mob.getPosition(), chr, mob.isBoss());
                    }
                }, mob.getAnimationTime("die1"));
                d++;
            }
        }
        for (final DropEntry de : toDrop) {
            if (de.getQuest() > 0 && chr.getQuest(MapleQuest.getInstance(de.getQuest())).getStatus() != 1) {
                continue;
            } else if (Randomizer.nextInt(999999) < de.getChance() * ServerConstants.DROP_RATE) {
                final Point pos = new Point(
                        (droptype == 3 ? (int) (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2)))) : (int) (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))))),
                        mob.getPosition().y);
                if (de.getId() / 1000000 == MapleInventoryType.EQUIP.getType()) {
                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    idrop = ii.randomizeStats((Equip) ii.getEquipById(de.getId()));
                } else {
                    idrop = new Item(de.getId(), (byte) 0, (short) (InventoryConstants.isRechargable(de.getId()) ? Randomizer.nextInt(90) + 1 : 1));
                }
                final MapleMapItem mdrop = new MapleMapItem(idrop, pos, mob.getId(), mob.getPosition(), chr);
                TimerManager.getInstance().schedule(new Runnable() {
                    public void run() {
                        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
                            @Override
                            public void sendPackets(MapleClient c) {
                                c.getSession().write(EffectFactory.dropItemFromMapObject(de.getId(), mdrop.getObjectId(), mob.getObjectId(), 0, mob.getPosition(), pos, (byte) 1, (byte) (mob.isBoss() ? 3 : 0)));
                            }
                        });
                    }
                }, mob.getAnimationTime("die1"));
                activateItemReactors(mdrop, chr.getClient());
                d++;
            }
        }
        if(getMapObjectsInRange(chr.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM)).size() > 200 && !(mapid >= 209000001 && mapid <= 209000015)) {
            clearMesos(chr);
        }
    }

    public final MapleMonster getMonsterById(final int id) {
        objectlock.readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.values()) {
                if (obj.getType() == MapleMapObjectType.MONSTER) {
                    if (((MapleMonster) obj).getId() == id) {
                        return (MapleMonster) obj;
                    }
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
        return null;
    }

    public final int countMonster(final int id) {
        int count = 0;
        for (MapleMapObject m : getAllObjects(MapleMapObjectType.MONSTER)) {
            if (((MapleMonster) m).getId() == id) {
                count++;
            }
        }
        return count;
    }

    public final List<MapleMapObject> getAllObjects(MapleMapObjectType type) {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(type));
    }

    public final boolean damageMonster(final MapleCharacter chr, final MapleMonster monster, final int damage) {
        return damageMonster(chr, monster, damage, -1);
    }

    public final List<HiredMerchant> getHiredMerchants() {
        final List<HiredMerchant> ret = new LinkedList<HiredMerchant>();
        final Point from = new Point(0, 0);
        final double rangeSq = Double.POSITIVE_INFINITY;

        objectlock.writeLock().lock();
        try {
            final Iterator<MapleMapObject> ltr = mapobjects.values().iterator();
            MapleMapObject obj;
            while (ltr.hasNext()) {
                obj = ltr.next();
                if (obj.getType() == MapleMapObjectType.HIRED_MERCHANT) {
                    if (from.distanceSq(obj.getPosition()) <= rangeSq) {
                        ret.add((HiredMerchant) obj);
                    }
                }
            }
        } finally {
            objectlock.writeLock().unlock();
        }
        return ret;
    }

    public final boolean damageMonster(final MapleCharacter chr, MapleMonster monster, final int damage, final int skill) {
        if (monster.getId() == 8800000) {
            for (MapleMapObject object : chr.getMap().getMapObjects()) {
                MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                if (mons != null) {
                    if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                        return true;
                    }
                }
            }
        }
        if (monster.isAlive()) {
            boolean killMonster_bool = false;
            monster.monsterLock.lock();
            try {
                if (damage > 0) {
                    int monsterhp = monster.getHp();
                    monster.damage(chr, damage, true, skill);
                    if (!monster.isAlive()) {
                        killMonster_bool = true;
                        if (monster.getId() >= 8810002 && monster.getId() <= 8810009) {
                            for (MapleMapObject object : chr.getMap().getMapObjects()) {
                                MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                                if (mons != null && (mons.getId() == 8810018 || mons.getId() == 8810026)) {
                                    damageMonster(chr, mons, monsterhp);
                                }
                            }
                        }
                    } else if (monster.getId() >= 8810002 && monster.getId() <= 8810009) {
                        for (MapleMapObject object : chr.getMap().getMapObjects()) {
                            MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                            if (mons != null) {
                                if (mons.getId() == 8810018 || mons.getId() == 8810026) {
                                    damageMonster(chr, mons, damage);
                                }
                            }
                        }
                    }
                }
            } finally {
                monster.monsterLock.unlock();
            }
            if (killMonster_bool && monster != null) {
                killMonster(monster, chr, true);
                removeFromAllCharsVisible(monster);
                if(mapid >= 970030100 && mapid <= 970032711 && getAllObjects(MapleMapObjectType.MONSTER).isEmpty()) {
                    broadcastMessage(EffectFactory.showEffect("praid/clear"));
                    broadcastMessage(EffectFactory.playSound("Party1/Clear"));
                } else if(mapid >= 925020100 && mapid <= 925023814 && getAllObjects(MapleMapObjectType.MONSTER).isEmpty()) {
                    broadcastMessage(EffectFactory.showEffect("dojang/end/clear"));
                    broadcastMessage(EffectFactory.playSound("Dojang/clear"));
                    chr.addDojoBossCount(monster.getId() - 9300184);
                    chr.setSaveDojoCount();
                    if (chr.getDojoBossCount(monster.getId() - 9300184) >= 100 && !(chr.haveItem(1142033 + monster.getId() - 9300184) || chr.haveItemEquipped(1142033 + monster.getId() - 9300184))) {
                        chr.dropMessage("You have slayed " + monster.getName() + " 100 times! You've been awarded the " + MapleItemInformationProvider.getInstance().getName(1142033 + monster.getId() - 9300184) + " Medal!");
                        MapleInventoryManipulator.addById(chr.getClient(), 1142033 + monster.getId() - 9300184, (short) 1);
                    }
                }
            }
            return true;
        }
        return false;
    }

    public final void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops) {
        killMonster(monster, chr, withDrops, false, 1);
    }

    public final void killMonster(MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean secondTime, final int animation) {
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        broadcastMessage(MobFactory.killMonster(monster.getObjectId(), animation), monster.getPosition());
        removeMapObject(monster);
        if (monster.getId() >= 8800003 && monster.getId() <= 8800010) {
            boolean makeZakReal = true;
            Collection<MapleMapObject> objects = getMapObjects();
            objectlock.readLock().lock(); // threadsafeness
            try {
                for (MapleMapObject object : objects) {
                    MapleMonster mons = getMonsterByOid(object.getObjectId());
                    if (mons != null) {
                        if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                            makeZakReal = false;
                            break;
                        }
                    }
                }
            } finally {
                objectlock.readLock().unlock();
            }
            if (makeZakReal) {
                objectlock.readLock().lock();
                try {
                    for (MapleMapObject object : objects) {
                        MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                        if (mons != null) {
                            if (mons.getId() == 8800000) {
                                makeMonsterReal(mons);
                                updateMonsterController(mons);
                                break;
                            }
                        }
                    }
                } finally {
                    objectlock.readLock().unlock();
                }
            }
        }
        MapleCharacter dropOwner = monster.killBy(chr);
        if (withDrops && !monster.dropsDisabled()) {
            if (dropOwner == null) {
                dropOwner = chr;
            }
            dropFromMonster(dropOwner, monster);
        }
        if(mapid == 910600000 && monster.getId() == 9300387) {
            spawnNpc(1013201, monster.getPosition());
        } else if(monster.getId() == 8810018) {
            for (MapleMapObject monstermo : getMapObjectsInRange(chr.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER))) {
                MapleMonster monster_ = (MapleMonster) monstermo;
                killMonster(monster_, chr, false);
            }
            for (MapleMapObject monstermo : getMapObjectsInRange(chr.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER))) {
                MapleMonster monster_ = (MapleMonster) monstermo;
                killMonster(monster_, chr, false);
            }
            String notice = "[Notice] Congratulations to ";
            for (MapleMapObject mmo : this.getAllObjects(MapleMapObjectType.PLAYER)) {
                MapleCharacter character = (MapleCharacter) mmo;
                notice += character.getName() + ", ";
            }
            notice += "for slaying <Horntail>!";
            try {
                chr.getClient().getChannelServer().getWorldInterface().broadcastMessage(null, EffectFactory.serverNotice(6, notice).getBytes());
            } catch (RemoteException re) {
                chr.getClient().getChannelServer().reconnectWorld();
            }
        }
    }

    public final void killMonster(final int monsId) {
        for (final MapleMapObject mmo : getMapObjects()) {
            if (mmo instanceof MapleMonster) {
                if (((MapleMonster) mmo).getId() == monsId) {
                    this.killMonster((MapleMonster) mmo, (MapleCharacter) getAllObjects(MapleMapObjectType.PLAYER).get(0), false);
                }
            }
        }
    }

    public final void killAllMonsters() {
        for (final MapleMapObject monstermo : getAllObjects(MapleMapObjectType.MONSTER)) {
            final MapleMonster monster = (MapleMonster) monstermo;
            MapleMonsterStats overrideStats = new MapleMonsterStats();
            overrideStats.setExp(0);
            monster.setOverrideStats(overrideStats);
            killMonster(monster, (MapleCharacter) getAllObjects(MapleMapObjectType.PLAYER).get(0), false);
        }
    }

    public final void destroyReactor(final int oid) {
        final MapleReactor reactor = getReactorByOid(oid);
        TimerManager tMan = TimerManager.getInstance();
        broadcastMessage(ReactorFactory.destroyReactor(reactor));
        reactor.setAlive(false);
        removeMapObject(reactor);
        reactor.setTimerActive(false);
        final int x_ = reactor.getPosition().x;
        final int y_ = reactor.getPosition().y;
        if (reactor.getDelay() > 0) {
            tMan.schedule(new Runnable() {
                @Override
                public final void run() {
                    respawnReactor(reactor, x_, y_);
                }
            }, reactor.getDelay());
        }
    }

    public final void resetReactors() {
        objectlock.readLock().lock();
        try {
            for (final MapleMapObject o : mapobjects.values()) {
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    ((MapleReactor) o).setState((byte) 0);
                    ((MapleReactor) o).setTimerActive(false);
                    broadcastMessage(ReactorFactory.triggerReactor((MapleReactor) o, 0));
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
    }

    public final void shuffleReactors() {
        List<Point> points = new ArrayList<Point>();
        objectlock.readLock().lock();
        try {
            for (final MapleMapObject o : mapobjects.values()) {
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    points.add(((MapleReactor) o).getPosition());
                }
            }
            Collections.shuffle(points);
            for (final MapleMapObject o : mapobjects.values()) {
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    ((MapleReactor) o).setPosition(points.remove(points.size() - 1));
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
    }

    public final MapleReactor getReactorById(final int Id) {
        objectlock.readLock().lock();
        try {
            for (final MapleMapObject obj : mapobjects.values()) {
                if (obj.getType() == MapleMapObjectType.REACTOR) {
                    if (((MapleReactor) obj).getId() == Id) {
                        return (MapleReactor) obj;
                    }
                }
            }
            return null;
        } finally {
            objectlock.readLock().unlock();
        }
    }

    public final void updateMonsterController(final MapleMonster monster) {
        if (monster == null)
            return;
        synchronized(monster) {
            if (!monster.isAlive()) {
                return;
            }
            if (monster.getController() != null) {
                if (monster.getController().getMap() != this) {
                    monster.getController().stopControllingMonster(monster);
                } else {
                    return;
                }
            }
            int mincontrolled = -1;
            MapleCharacter newController = null;
            characterlock.readLock().lock();
            try {
                final Iterator<MapleCharacter> ltr = characters.iterator();
                MapleCharacter chr;
                while (ltr.hasNext()) {
                    chr = ltr.next();
                    if (!chr.isHidden() && (chr.getControlledMonsters().size() < mincontrolled || mincontrolled == -1)) {
                        mincontrolled = chr.getControlledMonsters().size();
                        newController = chr;
                    }
                }
            } finally {
                characterlock.readLock().unlock();
            }
            if (newController != null) {
                if (monster.isFirstAttack()) {
                    newController.controlMonster(monster, true);
                    monster.setControllerHasAggro(true);
                    monster.setControllerKnowsAboutAggro(true);
                } else {
                    newController.controlMonster(monster, false);
                }
            }
        }
    }

    public final Collection<MapleMapObject> getMapObjects() {
        return Collections.unmodifiableCollection(mapobjects.values());
    }

    public final boolean containsNPC(final int npcid) {
        objectlock.readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.values()) {
                if (obj.getType() == MapleMapObjectType.NPC) {
                    if (((MapleNPC) obj).getId() == npcid) {
                        return true;
                    }
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
        return false;
    }

    public final MapleMapObject getMapObject(final int oid) {
        return mapobjects.get(oid);
    }

    public final MapleMonster getMonsterByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid);
        if (mmo == null) {
            return null;
        }
        if (mmo.getType() == MapleMapObjectType.MONSTER) {
            return (MapleMonster) mmo;
        }
        return null;
    }

    public final MapleReactor getReactorByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid);
        if (mmo == null) {
            return null;
        }
        return mmo.getType() == MapleMapObjectType.REACTOR ? (MapleReactor) mmo : null;
    }

    public final MapleReactor getReactorByName(final String name) {
        objectlock.readLock().lock();
        try {
            for (final MapleMapObject obj : mapobjects.values()) {
                if (obj.getType() == MapleMapObjectType.REACTOR) {
                    if (((MapleReactor) obj).getName().equals(name)) {
                        return (MapleReactor) obj;
                    }
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
        return null;
    }

    public final void spawnMonsterOnGroudBelow(final MapleMonster mob, final Point pos) {
        spawnMonsterOnGroundBelow(mob, pos);
    }

    public final void spawnMonsterOnGroundBelow(final MapleMonster mob, final Point pos) {
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        spos.y--;
        mob.setPosition(spos);
        spawnMonster(mob);
    }

    public final void spawnFakeMonsterOnGroundBelow(final MapleMonster mob, final Point pos) {
        Point spos = getGroundBelow(pos);
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    public final void spawnNpc(final int id, final Point pos) {
        final MapleNPC npc = MapleLifeFactory.getInstance().getNPC(id);
        npc.setPosition(pos);
        npc.setCy(pos.y);
        npc.setRx0(pos.x + 50);
        npc.setRx1(pos.x - 50);
        npc.setFh(getFootholds().findBelow(pos).getId());
        addMapObject(npc);
        broadcastMessage(NPCFactory.spawnNPC(npc));
    }

    public final void removeNpc(final int id) {
        final List<MapleMapObject> npcs = getAllObjects(MapleMapObjectType.NPC);
        for (final MapleMapObject npcmo : npcs) {
            final MapleNPC npc = (MapleNPC) npcmo;
            if (npc.getId() == id) {
                broadcastMessage(NPCFactory.removeNPC(npc.getObjectId()));
                removeMapObject(npc.getObjectId());
            }
        }
    }

    public final Point getGroundBelow(final Point pos) {
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        spos.y--;
        return spos;
    }

    public final void spawnRevives(final MapleMonster monster) {
        monster.setMap(this);
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            public final void sendPackets(MapleClient c) {
                c.announce(MobFactory.spawnMonster(monster, false));
            }
        });
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public final void spawnMonster(final MapleMonster monster) {
        monster.setMap(this);
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            public final void sendPackets(MapleClient c) {
                c.announce(MobFactory.spawnMonster(monster, true));
            }
        });
        if (monster.hasBossHPBar()) {
            broadcastMessage(monster.makeBossHPBarPacket(), monster.getPosition());
        } else if (monster.getRemoveAfter() == 5) {
            killMonster(monster, characters.get(0), true);
        }
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public final void spawnMonsterWithEffect(final MapleMonster monster, final int effect, Point pos) {
        monster.setMap(this);
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        spos.y--;
        monster.setPosition(spos);
        if (mapid < 925020000 || mapid > 925030000) {
            monster.disableDrops();
        }
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            public final void sendPackets(MapleClient c) {
                c.announce(MobFactory.spawnMonster(monster, true, effect));
            }
        });
        if (monster.hasBossHPBar()) {
            broadcastMessage(monster.makeBossHPBarPacket(), monster.getPosition());
        }
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public final void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            public final void sendPackets(MapleClient c) {
                c.announce(MobFactory.spawnFakeMonster(monster, 0));
            }
        });
        spawnedMonstersOnMap.incrementAndGet();
    }

    public final void makeMonsterReal(final MapleMonster monster) {
        monster.setFake(false);
        broadcastMessage(MobFactory.makeMonsterReal(monster));
        updateMonsterController(monster);
    }

    public final void spawnReactor(final MapleReactor reactor) {
        reactor.setMap(this);
        spawnAndAddRangedMapObject(reactor, new DelayedPacketCreation() {
            public final void sendPackets(MapleClient c) {
                c.announce(ReactorFactory.spawnReactor(reactor));
            }
        });
    }

    private final void respawnReactor(final MapleReactor reactor, final int x_, final int y_) {
        reactor.setState((byte) 0);
        reactor.setAlive(true);
        reactor.setPosition(new Point(x_, y_));
        spawnReactor(reactor);
    }

    public final void spawnDoor(final MapleDoor door) {
        spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {
            public final void sendPackets(MapleClient c) {
                c.announce(EffectFactory.spawnDoor(door.getOwner().getId(), door.getTargetPosition(), false));
                if (door.getOwner().getParty() != null && (door.getOwner() == c.getPlayer() || door.getOwner().getParty().containsMembers(new MaplePartyCharacter(c.getPlayer())))) {
                    c.announce(EffectFactory.partyPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                }
                c.announce(EffectFactory.spawnPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                c.announce(IntraPersonalFactory.enableActions());
            }
        }, new SpawnCondition() {
            public boolean canSpawn(MapleCharacter chr) {
                return chr.getMapId() == door.getTarget().getId() || chr == door.getOwner() && chr.getParty() == null;
            }
        });
    }

    public final List<MapleCharacter> getPlayersInRange(Rectangle box, List<MapleCharacter> chr) {
        List<MapleCharacter> character = new LinkedList<MapleCharacter>();
        characterlock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter a;
            while (ltr.hasNext()) {
                a = ltr.next();
                if (chr.contains(a.getClient().getPlayer())) {
                    if (box.contains(a.getPosition())) {
                        character.add(a);
                    }
                }
            }
            return character;
        } finally {
            characterlock.readLock().unlock();
        }
    }

    public final void spawnSummon(final MapleSummon summon) {
        spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {
            public final void sendPackets(MapleClient c) {
                if (summon != null) {
                    c.announce(SummonFactory.spawnSpecialMapObject(summon, summon.getSkillLevel(), true));
                }
            }
        }, null);
    }

    public void spawnMist(final MapleMist mist, final int duration, boolean poison, boolean fake) {
        addMapObject(mist);
        broadcastMessage(fake ? mist.makeFakeSpawnData(30) : mist.makeSpawnData());
        TimerManager tMan = TimerManager.getInstance();
        final ScheduledFuture<?> mistSchedule;
        if (mist.getMistType() == MapleMistType.POISON) {
            Runnable mistTask = new Runnable() {
                @Override
                public void run() {
                    if(mist.getMistType() == MapleMistType.POISON) {
                        for (MapleMapObject mo : getMapObjectsInBox(mist.getBox(), Collections.singletonList(MapleMapObjectType.MONSTER))) {
                            if (mist.makeChanceResult()) {
                                ((MapleMonster) mo).applyStatus(mist.getOwner(), new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), mist.getSourceSkill(), false), true, duration);
                            }
                        }
                    }
                }
            };
            mistSchedule = tMan.register(mistTask, 2000, 2500);
        } else {
            mistSchedule = null;
        }
        tMan.schedule(new Runnable() {
            @Override
            public void run() {
                removeMapObject(mist);
                if (mistSchedule != null) {
                    mistSchedule.cancel(false);
                }
                broadcastMessage(mist.makeDestroyData());
            }
        }, duration);
    }

    public final void disappearingItemDrop(final int dropperId, final Point dropperPosition, final MapleCharacter owner, final IItem item, Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropperId, dropperPosition, owner);
        broadcastMessage(EffectFactory.dropItemFromMapObject(item.getId(), drop.getObjectId(), 0, 0, dropperPosition, droppos, (byte) 3, (byte) (0)), drop.getPosition());
    }

    public final void spawnItemDrop(final int dropperId, final Point dropperPos, final MapleCharacter owner, final IItem item, Point pos, final boolean ffaDrop, final boolean expire) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropperId, dropperPos, owner);
        spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {
            public void sendPackets(MapleClient c) {
                c.announce(EffectFactory.dropItemFromMapObject(item.getId(), drop.getObjectId(), 0, 0, dropperPos, droppos, (byte) 1, (byte) (0)));
            }
        });
        broadcastMessage(EffectFactory.dropItemFromMapObject(item.getId(), drop.getObjectId(), 0, 0, dropperPos, droppos, (byte) 0, (byte) (0)), drop.getPosition());
        activateItemReactors(drop, owner.getClient());
    }

    private final void activateItemReactors(final MapleMapItem drop, final MapleClient c) {
        IItem item = drop.getItem();
        boolean horntailPQ = (mapid >= 240050101 && mapid <= 240050105 && item.getId() == mapid - 240050101 + 4001087);
        final TimerManager tMan = TimerManager.getInstance();
        for (final MapleMapObject o : mapobjects.values()) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                if (((MapleReactor) o).getReactorType() == 100) {
                    if (horntailPQ || (((MapleReactor) o).getReactItem().getLeft() == item.getId() && ((MapleReactor) o).getReactItem().getRight() <= item.getQuantity())) {
                        Rectangle area = ((MapleReactor) o).getArea();
                        if (area.contains(drop.getPosition())) {
                            MapleReactor reactor = (MapleReactor) o;
                            if (!reactor.isTimerActive()) {
                                tMan.schedule(new ActivateItemReactor(drop, reactor, c), 5000);
                                reactor.setTimerActive(true);
                            }
                        }
                    }
                }
            }
        }
    }

    public final void spawnMesoDrop(final int meso, final int displayMeso, Point position, final int dropperId, final Point dropperPos, final MapleCharacter owner, final boolean ffaLoot) {
        if(owner.gmLevel() == 1 || owner.gmLevel() == 2) {
            return;
        }
        final Point droppos = calcDropPos(position, position);
        final MapleMapItem mdrop = new MapleMapItem(meso, displayMeso, droppos, dropperId, dropperPos, owner);
        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            public void sendPackets(MapleClient c) {
                c.announce(EffectFactory.dropMesoFromMapObject(displayMeso, mdrop.getObjectId(), dropperId, ffaLoot ? 0 : owner.getId(), dropperPos, droppos, (byte) 1, (byte) (ffaLoot ? 3 : 0)));
            }
        });
    }

    public final void startMapEffect(final String msg, final int itemId) {
        startMapEffect(msg, itemId, 30000);
    }

    public final void startMapEffect(final String msg, final int itemId, final long time) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new MapleMapEffect(msg, itemId);
        broadcastMessage(mapEffect.makeStartData());
        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                broadcastMessage(mapEffect.makeDestroyData());
                mapEffect = null;
            }
        }, time);
    }

    private final void handlePets(final MapleCharacter chr) {
        broadcastMessage(chr, InterPersonalFactory.spawnPlayerMapobject(chr), false);
        MaplePet[] pets = chr.getPets();
        for (int i = 0; i < chr.getPets().length; i++) {
            try {
                if (pets[i] != null && chr.getPosition() != null) {
                    pets[i].setPos(getGroundBelow(chr.getPosition()));
                    broadcastMessage(chr, PetFactory.showPet(chr, pets[i], false, false), false);
                } else {
                    break;
                }
            } catch (NullPointerException npe) {
            }
        }
    }

    public final void addPlayer(final MapleCharacter chr) {
        characterlock.writeLock().lock();
        try {
            this.characters.add(chr);
        } finally {
            characterlock.writeLock().unlock();
        }
        if (!onFirstUserEnter.equals("") && !chr.hasEntered(onFirstUserEnter, mapid) && new File("scripts/map/onFirstUserEnter/" + onFirstUserEnter + ".js").exists()) {
            if (getAllObjects(MapleMapObjectType.PLAYER).size() <= 1) {
                chr.enteredScript(onFirstUserEnter, mapid);
                MapScriptManager.getInstance().start(chr.getClient(), null, "map/onFirstUserEnter/" + onFirstUserEnter);
            }
        }
        if (!onUserEnter.equals("") && new File("scripts/map/onUserEnter/" + onUserEnter + ".js").exists()) {
            MapScriptManager.getInstance().start(chr.getClient(), null, "map/onUserEnter/" + onUserEnter);
        }
        if (FieldLimit.CANNOTUSEMOUNTS.check(fieldLimit) && chr.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            chr.cancelBuffStats(MapleBuffStat.MONSTER_RIDING);
        }
        handlePets(chr);
        sendObjectPlacement(chr.getClient());
        MaplePet[] pets = chr.getPets();
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                pets[i].setPos(getGroundBelow(chr.getPosition()));
                chr.getClient().announce(PetFactory.showPet(chr, pets[i], false, false));
            } else {
                break;
            }
        }
        this.objectlock.writeLock().lock();
        try {
            this.mapobjects.put(Integer.valueOf(chr.getObjectId()), chr);
        } finally {
            this.objectlock.writeLock().unlock();
        }
        if (!chr.getSummons().isEmpty()) {
            for(final MapleSummon summon : chr.getSummons().values()) {
                summon.setPosition(chr.getPosition());
                chr.getMap().spawnSummon(summon);
                updateMapObjectVisibility(chr, summon);
            }
        }
        if (mapEffect != null) {
            mapEffect.sendStartData(chr.getClient());
        }
        if (chr.getEnergyBar() >= 10000) {
            broadcastMessage(chr, (BuffFactory.giveForeignEnergyCharge(chr.getId(), 10000)));
        }
        if (timeLimit > 0 && getForcedReturnMap() != null) {
            chr.getClient().announce(EffectFactory.getClock(timeLimit));
            chr.startMapTimeLimitTask(this, this.getForcedReturnMap());
        }
        if (hasTimer()) {
            chr.getClient().announce(EffectFactory.getClock(getTimerTimeLeft()));
        }
        if (clock) {
            Calendar cal = Calendar.getInstance();
            chr.getClient().announce((EffectFactory.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));
        }
        chr.receivePartyMemberHP();
        chr.getClient().announce(EffectFactory.musicChange((String) this.getProperties().getProperty("bgm")));
    }

    public final MaplePortal findClosestPortal(final Point from) {
        MaplePortal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public final MaplePortal getRandomSpawnpoint() {
        List<MaplePortal> spawnPoints = new ArrayList<MaplePortal>();
        for (MaplePortal portal : portals.values()) {
            if (portal.getType() >= 0 && portal.getType() <= 2) {
                spawnPoints.add(portal);
            }
        }
        MaplePortal portal = spawnPoints.get(new Random().nextInt(spawnPoints.size()));
        return portal != null ? portal : getPortal(0);
    }

    public final void removePlayer(final MapleCharacter chr) {
        characterlock.writeLock().lock();
        try {
            characters.remove(chr);
        } finally {
            characterlock.writeLock().unlock();
        }
        removeMapObject(Integer.valueOf(chr.getObjectId()));
        broadcastMessage(InterPersonalFactory.removePlayerFromMap(chr.getId()));
        for (MapleMonster monster : chr.getControlledMonsters()) {
            monster.setController(null);
            monster.setControllerHasAggro(false);
            monster.setControllerKnowsAboutAggro(false);
            updateMonsterController(monster);
        }
        chr.leaveMap();
        for (MapleSummon summon : chr.getSummons().values()) {
            if (summon.isStationary()) {
                chr.cancelBuffStats(MapleBuffStat.PUPPET);
            } else {
                removeMapObject(summon);
            }
        }
    }

    public final void broadcastMessage(final MaplePacket packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public final void broadcastMessage(final MapleCharacter source, final MaplePacket packet) {
        broadcastMessage(source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    public final void broadcastMessage(final MapleCharacter source, final MaplePacket packet, final boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }
    public final void broadcastMessage(final MapleCharacter source, final MaplePacket packet, final boolean repeatToSource, final boolean ranged) {
        broadcastMessage(repeatToSource ? null : source, packet, ranged ? 722500 : Double.POSITIVE_INFINITY, source.getPosition());
    }

    public final void broadcastMessage(final MaplePacket packet, final Point rangedFrom) {
        broadcastMessage(null, packet, 722500, rangedFrom);
    }

    public final void broadcastMessage(final MapleCharacter source, final MaplePacket packet, final Point rangedFrom) {
        broadcastMessage(source, packet, 722500, rangedFrom);
    }

    private final void broadcastMessage(final MapleCharacter source, final MaplePacket packet, final double rangeSq, final Point rangedFrom) {
        this.characterlock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (chr != source) {
                    if (chr == null || chr.getClient() == null || chr.getClient().getSession() == null) {
                        this.characterlock.readLock().unlock();
                        removePlayer(chr);
                        return;
                    }
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                            chr.getClient().announce(packet);
                        }
                    } else {
                        chr.getClient().announce(packet);
                    }
                }
            }
        } finally {
            this.characterlock.readLock().unlock();
        }
    }

    private final boolean isNonRangedType(final MapleMapObjectType type) {
        switch (type) {
        case NPC:
        case PLAYER:
        case HIRED_MERCHANT:
        case PLAYER_NPC:
        case MIST:
            return true;
        }
        return false;
    }

    private void sendObjectPlacement(MapleClient mapleClient) { // once again this really should be synchronised
        objectlock.readLock().lock();
        try {
            for (MapleMapObject o : mapobjects.values()) {
                if(o == null)
                    continue;
                if (isNonRangedType(o.getType())) {
                    o.sendSpawnData(mapleClient);
                } else if (o.getType() == MapleMapObjectType.MONSTER) {
                    updateMonsterController((MapleMonster) o);
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
        MapleCharacter chr = mapleClient.getPlayer();
        if (chr != null) {
            for (MapleMapObject o : getMapObjectsInRange(chr.getPosition(), 722500, rangedMapobjectTypes)) {
                if(o == null)
                    continue;
                try
                {
                    if (o.getType() == MapleMapObjectType.REACTOR) {
                        if (((MapleReactor) o).isAlive()) {
                            o.sendSpawnData(chr.getClient());
                            chr.addVisibleMapObject(o);
                        }
                    } else {
                        o.sendSpawnData(chr.getClient());
                        chr.addVisibleMapObject(o);
                    }
                } catch (NullPointerException npe)
                {
                    this.removeMapObject(o);
                }
            }
        }
    }
    
    public final Collection<MapleCharacter> getCharacters() {
        final List<MapleCharacter> chars = new ArrayList<MapleCharacter>();
        characterlock.writeLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            while (ltr.hasNext()) {
                chars.add(ltr.next());
            }
        } finally {
            characterlock.writeLock().unlock();
        }
        return chars;
    }

    public final MapleCharacter getCharacterById(int id) {
        characterlock.writeLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter c;
            while (ltr.hasNext()) {
                c = ltr.next();
                if (c.getId() == id) {
                    return c;
                }
            }
        } finally {
            characterlock.writeLock().unlock();
        }
        return null;
    }

    public final List<MapleMapObject> getMapObjectsInRange(final Point from, final double rangeSq, final List<MapleMapObjectType> MapObject_types) {
        final List<MapleMapObject> ret = new LinkedList<MapleMapObject>();

        objectlock.writeLock().lock();
        try {
            final Iterator<MapleMapObject> ltr = mapobjects.values().iterator();
            MapleMapObject obj;
            while (ltr.hasNext()) {
            obj = ltr.next();
                if (MapObject_types.contains(obj.getType())) {
                    if (from.distanceSq(obj.getPosition()) <= rangeSq) {
                        ret.add(obj);
                    }
                }
            }
        } finally {
            objectlock.writeLock().unlock();
        }
        return ret;
    }
    
    public final List<MapleMapObject> getMapObjectsInBox(final Rectangle box, final List<MapleMapObjectType> MapObject_types) {
        final List<MapleMapObject> ret = new LinkedList<MapleMapObject>();

        objectlock.readLock().lock();
        try {
            final Iterator<MapleMapObject> ltr = mapobjects.values().iterator();
            MapleMapObject obj;
            while (ltr.hasNext()) {
                obj = ltr.next();
                if (MapObject_types.contains(obj.getType())) {
                    if (box.contains(obj.getPosition())) {
                        ret.add(obj);
                    }
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
        return ret;
    }
    
    public final void addPortal(final MaplePortal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public MaplePortal getPortal(String portalname) {
        for (MaplePortal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public final MaplePortal getPortal(final int portalid) {
        return portals.get(portalid);
    }

    public final void addMapleArea(final Rectangle rec) {
        areas.add(rec);
    }

    public final List<Rectangle> getAreas() {
        return new ArrayList<Rectangle>(areas);
    }

    public final Rectangle getArea(final int index) {
        return areas.get(index);
    }

    public final void setFootholds(final MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public final MapleFootholdTree getFootholds() {
        return footholds;
    }

    public final void addMonsterSpawn(final MapleMonster mob, final int mobTime) {
        addMonsterSpawn(mob.getId(), mobTime, mob.getPosition(), mob.isMobile());
    }

    public final void addMonsterSpawn(final int monsterId, final int mobTime, final Point position, final boolean mobile) {
        Point newpos = calcPointBelow(position);
        newpos.y -= 1;
        SpawnPoint sp = new SpawnPoint(monsterId, newpos, mobTime, mobile);
        monsterSpawn.add(sp);
        if (sp.shouldSpawn() || mobTime == -1) {
            sp.spawnMonster(this);
        }
    }

    public final float getMonsterRate() {
        return monsterRate;
    }

    public final void setMonsterRate(final float newRate) {
        monsterRate = newRate;
    }

    private final void updateMapObjectVisibility(final MapleCharacter chr, MapleMapObject mo) {
        if (mo == null) {
            return;
        }
        if (mo.getPosition() == null) {
            chr.removeVisibleMapObject(mo);
            mo = null;
            return;
        }
        if (!chr.isMapObjectVisible(mo)) { // monster entered view range
            if (mo.getType() == MapleMapObjectType.SUMMON || mo.getPosition().distanceSq(chr.getPosition()) <= 722500) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else if (mo.getType() != MapleMapObjectType.SUMMON && mo.getPosition().distanceSq(chr.getPosition()) > 722500) {
            chr.removeVisibleMapObject(mo);
            mo.sendDestroyData(chr.getClient());
        }
    }

    public final void moveMonster(final MapleMonster monster, final Point reportedPos) {
        monster.setPosition(reportedPos);
        characterlock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            while (ltr.hasNext()) {
                updateMapObjectVisibility(ltr.next(), monster);
            }
        } finally {
            characterlock.readLock().unlock();
        }
    }

    public final void movePlayer(final MapleCharacter player, final Point newPosition) {
        player.setPosition(newPosition);
        final Collection<MapleMapObject> visibleObjects = player.getVisibleMapObjects();
        final MapleMapObject[] visibleObjectsNow = visibleObjects.toArray(new MapleMapObject[visibleObjects.size()]);
        try {
            for (MapleMapObject mo : visibleObjectsNow) {
                if (mo != null) {
                    if (mapobjects.get(mo.getObjectId()) == mo) {
                        updateMapObjectVisibility(player, mo);
                    } else {
                        player.removeVisibleMapObject(mo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (MapleMapObject mo : getMapObjectsInRange(player.getPosition(), 722500, rangedMapobjectTypes)) {
            if (!player.isMapObjectVisible(mo)) {
                mo.sendSpawnData(player.getClient());
                player.addVisibleMapObject(mo);
            }
        }
    }

    public final MaplePortal findClosestSpawnpoint(final Point from) {
        MaplePortal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public final Collection<MaplePortal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public final String getMapName() {
        return mapName;
    }

    public final void setMapName(final String mapName) {
        this.mapName = mapName;
    }

    public final String getStreetName() {
        return streetName;
    }

    public void setClock(boolean hasClock) {
        this.clock = hasClock;
    }

    public final void setTimeLimit(final int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public final int getTimeLimit() {
        return timeLimit;
    }

    public final void setTown(final boolean isTown) {
        this.town = isTown;
    }

    public final boolean isTown() {
        return town;
    }

    public final void setStreetName(final String streetName) {
        this.streetName = streetName;
    }

    public final void setEverlast(final boolean everlast) {
        this.everlast = everlast;
    }

    public final boolean getEverlast() {
        return everlast;
    }

    public final int getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap.get();
    }

    private class ActivateItemReactor implements Runnable {
        private MapleMapItem mapitem;
        private MapleReactor reactor;
        private MapleClient c;

        public ActivateItemReactor(MapleMapItem mapitem, MapleReactor reactor, MapleClient c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
                mapitem.itemLock.lock();
                try {
                    TimerManager tMan = TimerManager.getInstance();
                    if (mapitem.isPickedUp()) {
                        return;
                    }
                    MapleMap.this.broadcastMessage(EffectFactory.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());
                    MapleMap.this.removeMapObject(mapitem);
                    reactor.hitReactor(c);
                    reactor.setTimerActive(false);
                    if (reactor.getDelay() > 0) {
                        tMan.schedule(new Runnable() {
                            @Override
                            public void run() {
                                reactor.setState((byte) 0);
                                broadcastMessage(ReactorFactory.triggerReactor(reactor, 0));
                            }
                        }, reactor.getDelay());
                    }
                } finally {
                    mapitem.itemLock.unlock();
                }
            }
        }
    }

    public final void respawn(final boolean force) {
        if (force) {
            for (SpawnPoint spawnPoint : monsterSpawn) {
                spawnPoint.spawnMonster(MapleMap.this);
            }
        } else {
            if (characters.size() == 0 || this.properties.getProperty("respawn").equals(Boolean.FALSE)) {
                return;
            }
            final int numShouldSpawn = (monsterSpawn.size() - spawnedMonstersOnMap.get()) * Math.round(monsterRate);
            if (numShouldSpawn > 0) {
                final List<SpawnPoint> randomSpawn = new ArrayList<SpawnPoint>(monsterSpawn);
                Collections.shuffle(randomSpawn);
                int spawned = 0;
                for (SpawnPoint spawnPoint : randomSpawn) {
                    if (spawnPoint.shouldSpawn()) {
                        spawnPoint.spawnMonster(MapleMap.this);
                        spawned++;
                    }
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        }
    }

    private static interface DelayedPacketCreation {
        void sendPackets(MapleClient c);
    }

    private static interface SpawnCondition {
        boolean canSpawn(MapleCharacter chr);
    }

    public final int getHPDec() {
        return decHP;
    }

    public final void setHPDec(final int delta) {
        decHP = delta;
    }

    public final int getHPDecProtect() {
        return protectItem;
    }

    public final void setHPDecProtect(final int delta) {
        this.protectItem = delta;
    }

    public final void broadcastGMMessage(final MapleCharacter source, final MaplePacket packet, final boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    private final void broadcastGMMessage(final MapleCharacter source, final MaplePacket packet, final double rangeSq, final Point rangedFrom) {
        characterlock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (chr != source && chr.isGM()) {
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                            chr.getClient().announce(packet);
                        }
                    } else {
                        chr.getClient().announce(packet);
                    }
                }
            }
        } finally {
            characterlock.readLock().unlock();
        }
    }

    public final void broadcastNONGMMessage(final MapleCharacter source, final MaplePacket packet, final boolean repeatToSource) {
        characterlock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (chr != source && !chr.isGM()) {
                    chr.getClient().announce(packet);
                }
            }
        } finally {
            characterlock.readLock().unlock();
        }
    }

    public final void setOnUserEnter(final String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public final String getOnUserEnter() {
        return onUserEnter;
    }

    public final void setOnFirstUserEnter(final String onFirstUserEnter) {
        this.onFirstUserEnter = onFirstUserEnter;
    }

    public final String getOnFirstUserEnter() {
        return onFirstUserEnter;
    }

    public final void clearDrops(final MapleCharacter player, final boolean command) {
        List<MapleMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
        for (final MapleMapObject i : items) {
            player.getMap().removeMapObject(i);
            player.getMap().broadcastMessage(EffectFactory.removeItemFromMap(i.getObjectId(), 0, player.getId()));
        }
        if (command) {
            player.message("Items Destroyed: " + items.size());
        }
    }

    public final void clearMesos(final MapleCharacter player) {
        List<MapleMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
        for (final MapleMapObject i : items) {
            MapleMapItem j = (MapleMapItem) i;
            if(j.getItem() == null || j.getItem().getId() >= 3010000) {
                player.getMap().removeMapObject(i);
                player.getMap().broadcastMessage(EffectFactory.removeItemFromMap(i.getObjectId(), 0, player.getId()));
            }
        }
    }

    public final void setFieldLimit(final int fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public final int getFieldLimit() {
        return fieldLimit;
    }

    public final void setDP(final int dp) {
        this.dp = dp;
    }

    public final int getDP() {
        return dp;
    }

    public final void initiateRespawnTask() {
        respawnTask = TimerManager.getInstance().register(new Runnable() {

            @Override
            public void run() {
                respawn(false);
            }
        }, 10000);
    }

    private final void removeFromAllCharsVisible(final MapleMapObject mmo) {
        this.characterlock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter ch;
            while (ltr.hasNext()) {
                ch = ltr.next();
                try {
                    ch.removeVisibleMapObject(mmo);
                } catch (Exception e) {
                }
            }
        } finally {
            this.characterlock.readLock().unlock();
        }
    }

    private final void cancelRespawnTask() {
        if (this.respawnTask != null) {
            respawnTask.cancel(false);
        }
    }

    public final void cancelScriptTask() {
        if (this.scriptTask != null) {
            scriptTask.cancel(false);
        }
    }

    public final void empty() {
        this.areas.clear();
        this.characters.clear();
        this.mapobjects.clear();
        this.cancelRespawnTask();
        this.cancelScriptTask();
        this.footholds = null;
    }

    public final ChannelServer getChannel() {
        return ChannelServer.getInstance(channel);
    }

    public void startScriptTask(final MapleClient client, int interval, final boolean recurring) {
        TimerScriptManager.getInstance().start(client, null, "timer/" + mapid);
        Runnable event = new Runnable() {
            @Override
            public void run() {
                TimerScriptManager.getInstance().end(client, "timer/" + mapid);
            }
        };
        if (recurring) {
            scriptTask = TimerManager.getInstance().register(event, interval);
        } else {
            scriptTask = TimerManager.getInstance().schedule(event, interval);
        }
    }

    private boolean hasTimer() {
        if ((timerTime - (System.currentTimeMillis() - timerStarted)) / 1000 <= 0) {
            timerStarted = 0;
            timerTime = 0;
            return false;
        }
        return timerTime > 0 && timerStarted > 0;
    }

    private int getTimerTimeLeft() {
        if ((timerTime - (System.currentTimeMillis() - timerStarted)) / 1000 <= 0) {
            timerStarted = 0;
            timerTime = 0;
            return 0;
        }
        return (int) (timerTime - (System.currentTimeMillis() - timerStarted)) / 1000;
    }

    public void setTimerStarted(long timerStarted) {
        this.timerStarted = timerStarted;
    }

    public void setTimerTime(long timerTime) {
        this.timerTime = timerTime;
    }

    public ScheduledFuture getScriptTask() {
        return scriptTask;
    }

    public PropertiesTable getProperties() {
        return this.properties;
    }

    public String getDefaultBGM() {
        return this.bgm;
    }

    public void setDefaultBGM(String bgm) {
        this.bgm = bgm;
        properties.setProperty("bgm", bgm);
    }
}
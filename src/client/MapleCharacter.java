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

package client;

import constants.ExpTable;
import constants.ServerConstants;
import constants.skills.Bishop;
import constants.skills.BlazeWizard;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.Corsair;
import constants.skills.Crusader;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.FPArchMage;
import constants.skills.GM;
import constants.skills.Hermit;
import constants.skills.ILArchMage;
import constants.skills.Magician;
import constants.skills.Marauder;
import constants.skills.Marksman;
import constants.skills.Priest;
import constants.skills.Ranger;
import constants.skills.Sniper;
import constants.skills.Spearman;
import constants.skills.SuperGM;
import constants.skills.Swordsman;
import constants.skills.ThunderBreaker;
import constants.skills.WhiteKnight;
import java.awt.Point;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Formatter;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import net.MaplePacket;
import net.channel.ChannelServer;
import net.world.CharacterTransfer;
import net.world.MapleMessenger;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.PlayerBuffValueHolder;
import net.world.PlayerCoolDownValueHolder;
import net.world.PlayerDiseaseValueHolder;
import net.world.WorldRegistryImpl;
import net.world.guild.MapleGuild;
import net.world.guild.MapleGuildCharacter;
import net.world.remote.WorldChannelInterface;
import scripting.event.EventInstanceManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleMiniGame;
import server.MaplePortal;
import server.shops.MapleShop;
import server.MapleStatEffect;
import server.MapleStorage;
import server.MapleTrade;
import server.TimerManager;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.maps.AbstractAnimatedMapleMapObject;
import server.shops.HiredMerchant;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleSummon;
import server.maps.SavedLocation;
import server.maps.SavedLocationType;
import server.quest.MapleQuest;
import tools.DatabaseConnection;
import java.util.LinkedHashMap;
import tools.Pair;
import tools.Randomizer;
import tools.factory.BuddyFactory;
import tools.factory.EffectFactory;
import tools.factory.InterPersonalFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.PartyFactory;
import tools.factory.PetFactory;
import tools.factory.BuffFactory;
import tools.factory.GuildFactory;
import tools.factory.InventoryFactory;
import tools.factory.MobFactory;
import tools.factory.QuestFactory;
import tools.factory.SummonFactory;

/**
 * @name        MapleCharacter
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleCharacter extends AbstractAnimatedMapleMapObject implements Serializable {
    private final static int[] key = {18, 65, 2, 23, 3, 4, 5, 6, 16, 17, 19, 25, 26, 27, 31, 34, 35, 37, 38, 40, 43, 44, 45, 46, 50, 56, 59, 60, 61, 62, 63, 64, 57, 48, 29, 7, 24, 33, 41};
    private final static int[] type = {4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 5, 6, 6, 6, 6, 6, 6, 5, 4, 5, 4, 4, 4, 4};
    private final static int[] action = {0, 106, 10, 1, 12, 13, 18, 24, 8, 5, 4, 19, 14, 15, 2, 17, 11, 3, 20, 16, 9, 50, 51, 6, 7, 53, 100, 101, 102, 103, 104, 105, 54, 22, 52, 21, 25, 26, 23};
    private int world, accountid, rank, rankMove, jobRank, jobRankMove;
    private int id, level, str, dex, luk, int_, hp, maxhp, mp, maxmp, hpApUsed, mpApUsed, hair,
                    face, remainingAp, remainingSp, fame, gender;
    private int initialSpawnPoint, mapid;
    private int chair, itemEffect, nx;
    private int guildid, guildrank, alliancerank;
    private int energybar = 0, dojoenergy = 0, battleshipHp = 0; //TODO FIX BATTLESHIP
    private int gmLevel;
    private String linkedName;
    private int linkedLevel;
    private int expRate = 1;
    private int donorpts = 0;
    private int dojopts = 0;
    private int carnivalpts = 0;
    private int reborns;
    private int rebornPts;
    private int fallcounter;
    private int npcId = -1;
    private int aranCombo;
    private int slot;
    private long lastHealed;
    private long lastAnnounced;
    private long lastTalked;
    private long lastFameTime;
    private long muted;
    private String wordsTalked = "";
    private int spamCheck;
    private long megaLimit = 0;
    private transient int localmaxhp;
    private transient int localmaxmp;
    private transient int localstr;
    private transient int localdex;
    private transient int localluk;
    private transient int localint_;
    private transient int localacc;
    private transient int magic;
    private transient int watk;
    private boolean saveBook;
    private boolean saveKeyMap;
    private boolean saveMacros;
    private boolean saveTeleportMaps;
    private boolean saveSkills;
    private boolean saveLocations;
    private boolean saveBuddies;
    private boolean saveQuests;
    private boolean saveQuestInfo;
    private boolean saveAccount;
    private boolean saveStorage;
    private boolean saveWishList;
    private boolean saveDojoCount;
    private boolean hasBeacon = false; //FIX HOMING BEACON
    private int beaconOid = -1;
    private int projectile = 0;
    private boolean hidden;
    private boolean canDoor = true;
    private boolean whitechat = true;
    private boolean berserk;
    private boolean hasMerchant;
    private boolean allowMapChange = true;
    private boolean stuck = false;
    private boolean deaf = false;
    private String name;
    private String chalktext = "";
    private String search = null;
    private String partyquestitems = "";
    private AtomicInteger exp = new AtomicInteger();
    private AtomicInteger meso = new AtomicInteger();
    private BuddyList buddylist;
    private HiredMerchant hiredMerchant = null;
    private MapleClient client;
    private MapleGuildCharacter mgc = null;
    private MapleInventory[] inventory;
    private int job = 0;
    private int skinColor = 0;
    private transient EventInstanceManager eventInstance = null;
    private transient MapleMap map;
    private transient MapleShop shop = null;
    private transient MapleTrade trade = null;
    private MapleMessenger messenger = null;
    private MapleMiniGame miniGame;
    private MapleMount mount;
    private MapleParty party;
    private MaplePet[] pets = new MaplePet[3];
    private MapleRing marriageRing;
    private MapleStorage storage = null;
    private SavedLocation savedLocations[];
    private SkillMacro[] skillMacros = new SkillMacro[5];
    private Map<MapleQuest, MapleQuestStatus> quests = new LinkedHashMap<MapleQuest, MapleQuestStatus>();
    private List<Pair<Integer, String>> questinfo = new ArrayList<Pair<Integer, String>>();
    private int[] dojoBossCount = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private transient Set<MapleMonster> controlled = new LinkedHashSet<MapleMonster>();
    private transient Set<MapleMapObject> visibleMapObjects = new LinkedHashSet<MapleMapObject>();
    private Map<Integer, String> entered = new LinkedHashMap<Integer, String>();
    private Map<ISkill, SkillEntry> skills = new LinkedHashMap<ISkill, SkillEntry>();
    private transient Map<MapleBuffStat, MapleBuffStatValueHolder> effects = Collections.synchronizedMap(new EnumMap<MapleBuffStat, MapleBuffStatValueHolder>(MapleBuffStat.class));
    private transient Map<MapleDisease, MapleDiseaseValueHolder> diseases = new EnumMap<MapleDisease, MapleDiseaseValueHolder>(MapleDisease.class);
    private transient Map<Integer, MapleCoolDownValueHolder> coolDowns = new LinkedHashMap<Integer, MapleCoolDownValueHolder>(50);
    private transient Map<Integer, MapleSummon> summons = new LinkedHashMap<Integer, MapleSummon>();
    private Map<Integer, MapleKeyBinding> keymap = new LinkedHashMap<Integer, MapleKeyBinding>();
    private List<Integer> lastMonthFameIds;
    private List<MapleDoor> doors = new ArrayList<MapleDoor>();
    private List<Integer> vipRockMaps = new LinkedList<Integer>();
    private List<Integer> rockMaps = new LinkedList<Integer>();
    private transient ScheduledFuture<?> mapTimeLimitTask;
    private transient ScheduledFuture<?> hpDecreaseTask;
    private transient ScheduledFuture<?> poisonTask; //TODO POISONTASK
    private transient ScheduledFuture<?> dragonBloodSchedule;
    private transient ScheduledFuture<?> beholderHealingSchedule;
    private transient ScheduledFuture<?> beholderBuffSchedule;
    private transient ScheduledFuture<?> berserkSchedule;
    private ArrayList<String> commands = new ArrayList<String>();
    private ArrayList<Integer> excluded = new ArrayList<Integer>();
    private MonsterBook monsterBook;
    private int bookCover;
    private List<Integer> wishList = new ArrayList<Integer>();
    private boolean canSmega;
    public long latestUse = 0;
    private int omokWins;
    private int omokTies;
    private int omokLosses;
    private int coupleId;
    private int banCount;
    private int savedDamage;
    private int sameDamageCounter;
    private int familyId;
    private int foo;
    private int[] quickslots = {42, 82, 71, 73, 29, 83, 79, 81};

    private MapleCharacter() {
        canSmega = true;
        setStance(0);
        inventory = new MapleInventory[MapleInventoryType.values().length];
        savedLocations = new SavedLocation[SavedLocationType.values().length];
        for (final MapleInventoryType type : MapleInventoryType.values()) {
            inventory[type.ordinal()] = new MapleInventory(type);
        }
        for (int i = 0; i < SavedLocationType.values().length; i++) {
            savedLocations[i] = null;
        }
        setPosition(new Point(0, 0));
    }

    public static MapleCharacter getDefault(final MapleClient c) {
        MapleCharacter ret = new MapleCharacter();
        ret.client = c;
        ret.gmLevel = c.gmLevel();
        ret.hp = 50;
        ret.maxhp = 50;
        ret.mp = 50;
        ret.maxmp = 50;
        ret.remainingAp = 0;
        ret.str = 12;
        ret.dex = 5;
        ret.int_ = 4;
        ret.luk = 4;
        ret.map = null;
        ret.mapid = -1;
        ret.job = 0;
        ret.level = 1;
        ret.accountid = c.getAccID();
        ret.buddylist = new BuddyList(20);
        ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(48);
        ret.getInventory(MapleInventoryType.USE).setSlotLimit(48);
        ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(48);
        ret.getInventory(MapleInventoryType.ETC).setSlotLimit(48);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name, nx, gm FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret.client.setAccountName(rs.getString("name"));
                ret.nx = rs.getInt("nx");
                ret.gmLevel = rs.getInt("gm");
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
        }
        ret.mount = null;
        for (int i = 0; i < key.length; i++) {
            ret.keymap.put(key[i], new MapleKeyBinding(type[i], action[i]));
        }
        return ret;
    }

    public void reborn(int mode) {
        reborns += 1;
        rebornPts += 1;
        level = 1;
        hp = 50;
        mp = 50;
        maxhp = 50;
        maxmp = 50;
        str = 12;
        dex = 5;
        luk = 4;
        int_ = 4;
        remainingAp = 0;
        remainingSp = 0;
        job = 1000 * mode;
        updateSingleStat(MapleStat.LEVEL, 1);
        updateSingleStat(MapleStat.HP, 50);
        updateSingleStat(MapleStat.MP, 50);
        updateSingleStat(MapleStat.MAXHP, 50);
        updateSingleStat(MapleStat.MAXMP, 50);
        updateSingleStat(MapleStat.STR, 4);
        updateSingleStat(MapleStat.DEX, 4);
        updateSingleStat(MapleStat.LUK, 4);
        updateSingleStat(MapleStat.INT, 4);
        updateSingleStat(MapleStat.AVAILABLEAP, 9);
        updateSingleStat(MapleStat.AVAILABLESP, 0);
        setRates(false);
    }

    public void addCooldown(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
        if (this.coolDowns.containsKey(Integer.valueOf(skillId))) {
            this.coolDowns.remove(skillId);
        }
        this.coolDowns.put(Integer.valueOf(skillId), new MapleCoolDownValueHolder(skillId, startTime, length, timer));
    }

    public void addCommandToList(String command) {
        commands.add(command);
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    public void addExcluded(int x) {
        excluded.add(x);
    }

    public void setSavedDamage(int savedDamage) {
        this.savedDamage = savedDamage;
    }

    public void setSameDamageCounter(int sameDamageCounter) {
        this.sameDamageCounter = sameDamageCounter;
    }

    public int getSavedDamage() {
        return savedDamage;
    }

    public int getSameDamageCounter() {
        return sameDamageCounter;
    }

    public List<Pair<IItem, MapleInventoryType>> getHiredMerchantItems() {
        try {
            return ItemFactory.MERCHANT.loadItems(id, false);
        } catch (Exception e) {
            System.out.println("Error loading merchant items:");
            e.printStackTrace();
        }
        return null;
    }

    public void addHP(int delta) {
        setHp(hp + delta);
        updateSingleStat(MapleStat.HP, hp);
    }

    public boolean getCanSmega() {
        return canSmega;
    }

    public void setCanSmega(boolean setTo) {
        canSmega = setTo;
    }

    public void addMP(int delta) {
        setMp(mp + delta);
        updateSingleStat(MapleStat.MP, mp);
    }

    public void addMPHP(int hpDiff, int mpDiff) {
        setHp(hp + hpDiff);
        setMp(mp + mpDiff);
        updateSingleStat(MapleStat.HP, hp);
        updateSingleStat(MapleStat.MP, mp);
    }

    public void addPet(MaplePet pet) {
        for (int i = 0; i < 3; i++) {
            if (pets[i] == null) {
                pets[i] = pet;
                return;
            }
        }
    }

    public void addStat(int type, int up) {
        if (type == 1) {
            this.str += up;
            updateSingleStat(MapleStat.STR, str);
        } else if (type == 2) {
            this.dex += up;
            updateSingleStat(MapleStat.DEX, dex);
        } else if (type == 3) {
            this.int_ += up;
            updateSingleStat(MapleStat.INT, int_);
        } else if (type == 4) {
            this.luk += up;
            updateSingleStat(MapleStat.LUK, luk);
        }
    }

    public void addSummon(int id, MapleSummon summon) {
        summons.put(id, summon);
    }

    public void addTeleportRockMap(Integer mapId, int type) {
        if (type == 0 && rockMaps.size() < 5 && !rockMaps.contains(mapId)) {
            rockMaps.add(mapId);
        } else if (vipRockMaps.size() < 10 && !vipRockMaps.contains(mapId)) {
            vipRockMaps.add(mapId);
        }
    }

    public void addToWishList(int sn) {
        wishList.add(sn);
        saveWishList = true;
    }

    public void addVisibleMapObject(MapleMapObject mo) {
        visibleMapObjects.add(mo);
    }

    public void ban(String reason, boolean dc) {
        try {
            client.banMacs();
            Connection con = DatabaseConnection.getConnection();
            String ip = client.getSession().getRemoteAddress().toString().split(":")[0];
            PreparedStatement ps = con.prepareStatement("SELECT ip FROM ipbans WHERE ip = ?");
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                if (!ip.startsWith("/220.255")) { // sg ip
                    ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                    ps.setString(1, ip);
                    ps.executeUpdate();
                    ps.close();
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
            ps.setString(1, reason);
            ps.setInt(2, accountid);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
        }
        for (final ChannelServer cserv : ChannelServer.getAllInstances()) {
            cserv.broadcastGMPacket(EffectFactory.serverNotice(6, "[Ban Report] " + MapleCharacter.makeMapleReadable(reason)));
        }
        if (dc) {
            client.disconnect(!client.insideCashShop(), client.insideCashShop());
        }
    }

    public static boolean offlineBan(String name, String reason) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
            ps.setString(1, reason);
            ps.setInt(2, MapleClient.findAccIdForCharacterName(name));
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
            } catch (SQLException e) {
            }
        }
        return false;
    }

    public void resetStats() {
        int totAp = getStr() + getDex() + getLuk() + getInt() + getRemainingAp();
        setStr(4);
        setDex(4);
        setLuk(4);
        setInt(4);
        setRemainingAp(totAp - 16);
        updateSingleStat(MapleStat.STR, 4);
        updateSingleStat(MapleStat.DEX, 4);
        updateSingleStat(MapleStat.LUK, 4);
        updateSingleStat(MapleStat.INT, 4);
        updateSingleStat(MapleStat.AVAILABLEAP, totAp - 16);
    }

    public int calculateMaxBaseDamageForHH(int watk) {
        if (watk == 0) {
            return 1;
        } else {
            IItem weapon_item = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            if (weapon_item != null) {
                return (int) (((MapleItemInformationProvider.getInstance().getWeaponType(weapon_item.getId()).getMaxDamageMultiplier() * localstr + localdex) / 100.0) * watk);
            } else {
                return 0;
            }
        }
    }

    public void cancelAllBuffs() {
        for (final MapleBuffStatValueHolder mbsvh : new ArrayList<MapleBuffStatValueHolder>(effects.values())) {
            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
        }
    }

    public void clearBuffs() {
        effects.clear();
    }

    public void clearDebuffs() {
        diseases.clear();
    }

    public void cancelBuffStats(MapleBuffStat stat) {
        List<MapleBuffStat> buffStatList = Arrays.asList(stat);
        deregisterBuffStats(buffStatList);
        cancelPlayerBuffs(buffStatList);
    }

    public void cancelEffect(MapleStatEffect effect, boolean overwrite, long startTime) {
        List<MapleBuffStat> buffstats;
        if (!overwrite) {
            buffstats = getBuffStats(effect, startTime);
        } else {
            List<Pair<MapleBuffStat, Integer>> statups = effect.getStatups();
            buffstats = new ArrayList<MapleBuffStat>(statups.size());
            for (final Pair<MapleBuffStat, Integer> statup : statups) {
                buffstats.add(statup.getLeft());
            }
        }
        deregisterBuffStats(buffstats);
        if (effect.isMagicDoor()) {
            if (!getDoors().isEmpty()) {
                MapleDoor door = getDoors().iterator().next();
                for (final MapleCharacter chr : door.getTarget().getCharacters()) {
                    door.sendDestroyData(chr.client);
                }
                for (final MapleCharacter chr : door.getTown().getCharacters()) {
                    door.sendDestroyData(chr.client);
                }
                for (final MapleDoor destroyDoor : getDoors()) {
                    door.getTarget().removeMapObject(destroyDoor);
                    door.getTown().removeMapObject(destroyDoor);
                }
                clearDoors();
                silentPartyUpdate();
            }
        } else if (effect.getSourceId() == Spearman.HYPER_BODY || effect.getSourceId() == GM.HYPER_BODY || effect.getSourceId() == SuperGM.HYPER_BODY) {
            List<Pair<MapleStat, Integer>> statup = new ArrayList<Pair<MapleStat, Integer>>(4);
            statup.add(new Pair<MapleStat, Integer>(MapleStat.HP, Math.min(hp, maxhp)));
            statup.add(new Pair<MapleStat, Integer>(MapleStat.MP, Math.min(mp, maxhp)));
            statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, maxhp));
            statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, maxmp));
            client.announce(IntraPersonalFactory.updatePlayerStats(statup));
        } else if (effect.isMonsterRiding()) {
            if (effect.getSourceId() != Corsair.BATTLE_SHIP) {
                mount.cancelSchedule();
            }
        }
        if (!overwrite) {
            cancelPlayerBuffs(buffstats);
            if (effect.isHide() && (MapleCharacter) getMap().getMapObject(getObjectId()) != null) {
                this.hidden = false;
                getMap().broadcastNONGMMessage(this, InterPersonalFactory.spawnPlayerMapobject(this), false);
                for (int i = 0; pets[i] != null; i++) {
                    getMap().broadcastNONGMMessage(this, PetFactory.showPet(this, pets[i], false, false), false);
                }
            }
        }
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        cancelEffect(effects.get(stat).effect, false, -1);
    }

    public void cancelMagicDoor() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMagicDoor()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
        }
    }

    private void cancelPlayerBuffs(List<MapleBuffStat> buffstats) {
        if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
            recalcLocalStats();
            enforceMaxHpMp();
            client.announce(BuffFactory.cancelBuff(buffstats));
            if (buffstats.size() > 0 && !buffstats.get(0).equals(MapleBuffStat.HOMING_BEACON)) {
                getMap().broadcastMessage(this, BuffFactory.cancelForeignBuff(getId(), buffstats), false);
            }
        }
    }

    public static boolean canCreateChar(String name) {
        if (name.length() < 4 || name.length() > 12) {
            return false;
        }
        return getByName(name, "id") < 0 && Pattern.compile("[a-zA-Z0-9_-]{3,12}").matcher(name).matches();
    }

    public void toggleDeaf() {
        if (!deaf) {
            deaf = true;
            dropMessage(5, "You have turned off Megaphones.");
        } else {
            deaf = false;
            dropMessage(5, "You have turned on Megaphones.");
        }
    }

    public void toggleExperience() {
        if (expRate == 0) {
            expRate = 1;
            setRates(false);
            dropMessage(5, "You have turned on Experience Gain.");
        } else {
            expRate = 0;
            dropMessage(5, "You have turned off Experience Gain.");
        }
    }

    public boolean isDeaf() {
        return deaf;
    }
    
    public boolean canDoor() {
        return canDoor;
    }

    public void changeJob(int newJob) {
        this.job = newJob;
        this.remainingSp++;
        if (newJob % 10 == 2) {
            this.remainingSp += 2;
        }
        int job_ = job % 1000;
        if (job_ == 100) {
            maxhp += rand(200, 250);
        } else if (job_ == 200) {
            maxmp += rand(100, 150);
        } else if (job_ % 100 == 0) {
            maxhp += rand(100, 150);
            maxhp += rand(25, 50);
        } else if (job_ > 0 && job_ < 200) {
            maxhp += rand(300, 350);
        } else if (job_ < 300) {
            maxmp += rand(250, 300);
        } else if (job_ > 0 && job_ != 1000) {
            maxhp += rand(300, 350);
            maxmp += rand(150, 200);
        }
        if (maxhp >= 30000) {
            maxhp = 30000;
        }
        if (maxmp >= 30000) {
            maxmp = 30000;
        }
        if(world == 1) {
            try {
                if(job % 10 == 1 && gmLevel < 1) {
                    client.getChannelServer().getWorldInterface().broadcastMessage(null, EffectFactory.serverNotice(6, "[Notice] Congratulations to " + name + " for successfully achieving their Third Job Advancement!").getBytes());
                } else if(job % 10 == 2 && gmLevel < 1) {
                    client.getChannelServer().getWorldInterface().broadcastMessage(null, EffectFactory.serverNotice(6, "[Notice] Congratulations to " + name + " for successfully achieving their Fourth Job Advancement!").getBytes());
                }
            } catch (RemoteException re) {
                client.getChannelServer().reconnectWorld();
            }
        }

        List<Pair<MapleStat, Integer>> statup = new ArrayList<Pair<MapleStat, Integer>>(5);
        statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, Integer.valueOf(maxhp)));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, Integer.valueOf(maxmp)));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLEAP, remainingAp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLESP, remainingSp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.JOB, Integer.valueOf(job)));
        recalcLocalStats();
        client.announce(IntraPersonalFactory.updatePlayerStats(statup, false));
        silentPartyUpdate();
        guildUpdate();
        getMap().broadcastMessage(this, EffectFactory.showForeignEffect(getId(), 8), false);
    }

    public void changeKeybinding(int key, MapleKeyBinding keybinding) {
        if (keybinding.getType() != 0) {
            keymap.put(Integer.valueOf(key), keybinding);
        } else {
            keymap.remove(Integer.valueOf(key));
        }
    }

    public void changeMap(final int mapid) {
        changeMap(client.getChannelServer().getMapFactory().getMap(mapid));
    }

    public void changeMap(final MapleMap to) {
        changeMap(to, to.getPortal(0));
    }

    public void changeMap(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), IntraPersonalFactory.getWarpToMap(to, pto.getId(), this));
    }

    public void changeMap(final MapleMap to, final Point pos) {
        changeMapInternal(to, pos, IntraPersonalFactory.getWarpToMap(to, 0x80, this));
    }

    public void changeMapBanish(final int mapid, final String portal, final String msg) {
        dropMessage(5, msg);
        final MapleMap map_ = ChannelServer.getInstance(client.getChannel()).getMapFactory().getMap(mapid);
        changeMap(map_, map_.getPortal(portal));
    }

    private void changeMapInternal(final MapleMap to, final Point pos, MaplePacket warpPacket) {
        if ((map.getCharacters().size() == 1 || (map.getMapObjectsInRange(getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM)).size() > 200)) && !(map.getId() >= 209000001 && map.getId() <= 209000015)) {
            map.clearDrops(MapleCharacter.this, false);
        }
        warpPacket.setOnSend(new Runnable() {
            @Override
            public void run() {
                map.removePlayer(MapleCharacter.this);
                if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
                    map = to;
                    setPosition(pos);
                    map.addPlayer(MapleCharacter.this);
                    if (party != null) {
                        silentPartyUpdate();
                        client.announce(PartyFactory.updateParty(client.getChannel(), party, PartyOperation.SILENT_UPDATE, null));
                        updatePartyMemberHP();
                    }
                    if (getMap().getHPDec() > 0) {
                        hpDecreaseTask = TimerManager.getInstance().schedule(new Runnable() {
                            @Override
                            public void run() {
                                doHurtHp();
                            }
                        }, 10000);
                    }
                }
            }
        });
        client.announce(warpPacket);
    }

    public void changeSkillLevel(final ISkill skill, int newLevel, int newMasterlevel) {
        skills.put(skill, new SkillEntry(newLevel, newMasterlevel));
        if(!(skill.getId() % 10000000 == 1009)) {
            this.client.announce(BuffFactory.updateSkill(skill.getId(), newLevel, newMasterlevel));
        }
        saveSkills = true;
    }

    public void checkBerserk() {
        if (berserkSchedule != null) {
            berserkSchedule.cancel(false);
        }
        final MapleCharacter chr = this;
        if (job == DarkKnight.ID) {
            ISkill berserkX = SkillFactory.getSkill(DarkKnight.BERSERK);
            final int skilllevel = getSkillLevel(berserkX);
            if (skilllevel > 0) {
                berserk = chr.getHp() * 100 / chr.getMaxHp() < berserkX.getEffect(skilllevel).getX();
                berserkSchedule = TimerManager.getInstance().register(new Runnable() {

                    @Override
                    public void run() {
                        client.announce(BuffFactory.showOwnBerserk(skilllevel, berserk));
                        getMap().broadcastMessage(MapleCharacter.this, BuffFactory.showBerserk(getId(), skilllevel, berserk), false);
                    }
                }, 5000, 3000);
            }
        }
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (!monster.isControllerHasAggro()) {
            if (monster.getController() == this) {
                monster.setControllerHasAggro(true);
            } else {
                monster.switchController(this, true);
            }
        }
    }

    public void clearDoors() {
        doors.clear();
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = null;
        saveLocations = true;
    }

    public void clearWishList() {
        wishList.clear();
        saveWishList = true;
    }

    public void controlMonster(MapleMonster monster, boolean aggro) {
        monster.setController(this);
        controlled.add(monster);
        client.announce(MobFactory.controlMonster(monster, false, aggro));
    }

    public int countItem(int itemid) {
        return inventory[MapleItemInformationProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
    }

    public void decreaseBattleshipHp(int decrease) {
        this.battleshipHp -= decrease;
        if (battleshipHp <= 0) {
            this.battleshipHp = 0;
            ISkill battleship = SkillFactory.getSkill(Corsair.BATTLE_SHIP);
            int cooldown = battleship.getEffect(getSkillLevel(battleship)).getCooldown();
            getClient().announce(BuffFactory.skillCooldown(Corsair.BATTLE_SHIP, cooldown));
            addCooldown(Corsair.BATTLE_SHIP, System.currentTimeMillis(), cooldown * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(this, Corsair.BATTLE_SHIP), cooldown * 1000));
            cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            cancelBuffStats(MapleBuffStat.MONSTER_RIDING);
            resetBattleshipHp();
        }
    }

    public void deleteGuild(int guildId) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
            ps.setInt(1, guildId);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            System.out.print("Error deleting guild: " + ex);
        }
    }

    public void deleteTeleportRockMap(Integer mapId, int type) {
        if (type == 0) {
            rockMaps.remove(mapId);
        } else {
            vipRockMaps.remove(mapId);
        }
    }

    private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
    }

    private void deregisterBuffStats(final List<MapleBuffStat> stats) {
        synchronized (stats) {
            final List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<MapleBuffStatValueHolder>(stats.size());
            for (MapleBuffStat stat : stats) {
                MapleBuffStatValueHolder mbsvh = effects.get(stat);
                if (mbsvh != null) {
                    effects.remove(stat);
                    boolean addMbsvh = true;
                    for (MapleBuffStatValueHolder contained : effectsToCancel) {
                        if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
                            addMbsvh = false;
                        }
                    }
                    if (addMbsvh) {
                        effectsToCancel.add(mbsvh);
                    }
                    if (stat == MapleBuffStat.SUMMON || stat == MapleBuffStat.PUPPET) {
                        final int summonId = mbsvh.effect.getSourceId();
                        final MapleSummon summon = summons.get(summonId);
                        if (summon != null) {
                            getMap().broadcastMessage(SummonFactory.removeSpecialMapObject(summon, true), summon.getPosition());
                            getMap().removeMapObject(summon);
                            removeVisibleMapObject(summon);
                            summons.remove(summonId);
                        }
                        if (summon.getSkill() == DarkKnight.BEHOLDER) {
                            if (beholderHealingSchedule != null) {
                                beholderHealingSchedule.cancel(false);
                                beholderHealingSchedule = null;
                            }
                            if (beholderBuffSchedule != null) {
                                beholderBuffSchedule.cancel(false);
                                beholderBuffSchedule = null;
                            }
                        }
                    } else if (stat == MapleBuffStat.DRAGONBLOOD) {
                        dragonBloodSchedule.cancel(false);
                        dragonBloodSchedule = null;
                    }
                }
                stat = null;
            }
            for (MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
                if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).size() == 0) {
                    if (!cancelEffectCancelTasks.effect.isHomingBeacon()) {
                        cancelEffectCancelTasks.schedule.cancel(false);
                    }
                }
            }
        }
    }

    public void disableDoor() {
        canDoor = false;
        TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                canDoor = true;
            }
        }, 5000);
    }

    public void disbandGuild() {
        if (guildid < 1 || guildrank != 1) {
            return;
        }
        try {
            client.getChannelServer().getWorldInterface().disbandGuild(guildid);
        } catch (Exception e) {
        }
    }

    public void dispel() {
        for (MapleBuffStatValueHolder mbsvh : new ArrayList<MapleBuffStatValueHolder>(effects.values())) {
            if (mbsvh.effect.isSkill()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public void dispelDebuffs() {
        dispelDebuff(MapleDisease.CURSE);
        dispelDebuff(MapleDisease.DARKNESS);
        dispelDebuff(MapleDisease.POISON);
        dispelDebuff(MapleDisease.SEAL);
        dispelDebuff(MapleDisease.WEAKEN);
    }

    public void dispelDebuff(MapleDisease debuff) {
        if (hasDisease(debuff)) {
            long mask = debuff.getValue();
            client.announce(BuffFactory.cancelDebuff(mask));
            map.broadcastMessage(this, BuffFactory.cancelForeignDebuff(id, mask), false);
            diseases.remove(debuff);
        }
    }

    public void dispelSkill(int skillid) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (skillid == 0) {
                if (mbsvh.effect.isSkill() && dispelSkills(mbsvh.effect.getSourceId())) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            } else if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    private boolean dispelSkills(int skillid) {
        switch (skillid) {
        case DarkKnight.BEHOLDER:
        case FPArchMage.ELQUINES:
        case ILArchMage.IFRIT:
        case Priest.SUMMON_DRAGON:
        case Bishop.BAHAMUT:
        case Ranger.PUPPET:
        case Ranger.SILVER_HAWK:
        case Sniper.PUPPET:
        case Sniper.GOLDEN_EAGLE:
        case Hermit.SHADOW_PARTNER:
            return true;
        default:
            return false;
        }
    }

    public void doHurtHp() {
        if (this.getInventory(MapleInventoryType.EQUIPPED).findById(getMap().getHPDecProtect()) != null) {
            return;
        }
        addHP(-getMap().getHPDec());
        hpDecreaseTask = TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                doHurtHp();
            }
        }, 10000);
    }

    public void dropMessage(String message) {
        dropMessage(5, message);
    }

    public void dropMessage(int type, String message) {
        client.announce(EffectFactory.serverNotice(type, message));
    }

    private void enforceMaxHpMp() {
        List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>(2);
        if (getMp() > getCurrentMaxMp()) {
            setMp(getMp());
            stats.add(new Pair<MapleStat, Integer>(MapleStat.MP, Integer.valueOf(getMp())));
        }
        if (getHp() > getCurrentMaxHp()) {
            setHp(getHp());
            stats.add(new Pair<MapleStat, Integer>(MapleStat.HP, Integer.valueOf(getHp())));
        }
        if (stats.size() > 0) {
            client.announce(IntraPersonalFactory.updatePlayerStats(stats));
        }
    }

    public void enteredScript(String script, int mapid) {
        if (!entered.containsKey(mapid)) {
            entered.put(mapid, script);
        }
    }

    public void addDojoBossCount(int index) {
        dojoBossCount[index]++;
    }

    public void addQuestInfo(int id, String info) {
        saveQuestInfo = true;
        questinfo.add(new Pair<Integer, String>(id, info));
    }

    public List<Pair<Integer, String>> getQuestInfo() {
        return questinfo;
    }

    public final void equipChanged() {
        getMap().broadcastMessage(this, IntraPersonalFactory.updateCharLook(this), false);
        recalcLocalStats();
        enforceMaxHpMp();
        if (getMessenger() != null) {
            WorldChannelInterface wci = client.getChannelServer().getWorldInterface();
            try {
                wci.updateMessenger(getMessenger().getId(), getName(), client.getChannel());
            } catch (final RemoteException e) {
                client.getChannelServer().reconnectWorld();
            }
        }
    }

    public void expirationTask() {
        long expiration, currenttime = System.currentTimeMillis();
        List<IItem> toberemove = new ArrayList<IItem>();
        for (MapleInventory inv : inventory) {
            for (IItem item : inv.list()) {
                expiration = item.getExpiration();
                if (expiration != -1) {
                    if (currenttime < expiration) {
                        client.announce(InventoryFactory.itemExpired(item.getId()));
                        toberemove.add(item);
                    }
                }
            }
            for (IItem item : toberemove) {
                MapleInventoryManipulator.removeFromSlot(client, inv.getType(), item.getPosition(), item.getQuantity(), true);
            }
            toberemove.clear();
        }
    }

    public enum FameStatus {
        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public void gainExp(final int gain, final boolean show, final boolean inChat) {
        gainExp(gain, show, inChat, true);
    }

    public void gainExp(final int gain, final boolean show, final boolean inChat, final boolean white) {
        gainExp(gain, show, inChat, white, 0);
    }

    public void gainExp(int gain, final boolean show, final boolean inChat, final boolean white, final int party) {
        if (level < getMaxLevel()) {
            int total = gain;
            if (party > 1) {
                total += party * gain / 20;
            }
            total /= reborns + 1;
            if ((long) this.exp.get() + (long) gain > (long) Integer.MAX_VALUE) {
                final int gainFirst = ExpTable.getExpNeededForLevel(level) - this.exp.get();
                gain -= gainFirst + 1;
                this.gainExp(gainFirst + 1, false, inChat, white);
            }
            updateSingleStat(MapleStat.EXP, this.exp.addAndGet(gain));
            if (show && gain != 0) {
                client.announce(EffectFactory.getShowExpGain(gain, inChat, white, (byte) (total != gain ? party - 1 : 0)));
            }
            if (exp.get() >= ExpTable.getExpNeededForLevel(level)) {
                levelUp(true);
                final int need = ExpTable.getExpNeededForLevel(level);
                if (exp.get() >= need) {
                    setExp(need - 1);
                    updateSingleStat(MapleStat.EXP, need);
                }
            }
        }
    }

    public void gainFame(int delta) {
        this.addFame(delta);
        this.updateSingleStat(MapleStat.FAME, this.fame);
    }

    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false, false);
    }

    public void gainMeso(int gain, boolean show, boolean enableActions, boolean inChat) {
        boolean noOp = false;
        if (((long) (meso.get()) + gain) >= 2147483647L) {
            noOp = true;
            client.announce(IntraPersonalFactory.enableActions());
            return;
        } else {
            updateSingleStat(MapleStat.MESO, meso.addAndGet(gain), enableActions);
        }
        if (show && !noOp) {
            client.announce(EffectFactory.getShowMesoGain(gain, inChat));
        }
    }

    public void genericGuildMessage(int code) {
        this.client.announce(GuildFactory.genericGuildMessage((byte) code));
    }

    public int getAccountID() {
        return accountid;
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        List<PlayerBuffValueHolder> ret = new ArrayList<PlayerBuffValueHolder>();
        for (MapleBuffStatValueHolder mbsvh : effects.values()) {
            ret.add(new PlayerBuffValueHolder(mbsvh.startTime, mbsvh.effect));
        }
        return ret;
    }

    public List<PlayerCoolDownValueHolder> getAllCooldowns() {
        List<PlayerCoolDownValueHolder> ret = new ArrayList<PlayerCoolDownValueHolder>();
        for (MapleCoolDownValueHolder mcdvh : coolDowns.values()) {
            ret.add(new PlayerCoolDownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
        }
        return ret;
    }

    public final List<PlayerDiseaseValueHolder> getAllDiseases() {
        final List<PlayerDiseaseValueHolder> ret = new ArrayList<PlayerDiseaseValueHolder>(5);

        MapleDiseaseValueHolder vh;
        for (Entry<MapleDisease, MapleDiseaseValueHolder> disease : diseases.entrySet()) {
            vh = disease.getValue();
            ret.add(new PlayerDiseaseValueHolder(disease.getKey(), vh.startTime, vh.length));
        }
        return ret;
    }

    public int getBattleshipHp() {
        return battleshipHp;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public Long getBuffedStarttime(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return Long.valueOf(mbsvh.startTime);
    }

    public Integer getBuffedValue(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return Integer.valueOf(mbsvh.value);
    }

    public ArrayList<MapleStatEffect> getBuffEffects() {
        ArrayList<MapleStatEffect> almseret = new ArrayList<MapleStatEffect>();
        HashSet<Integer> hs = new HashSet<Integer>();
        for (MapleBuffStatValueHolder mbsvh : effects.values()) {
            if (mbsvh != null && mbsvh.effect != null) {
                Integer nid = Integer.valueOf(mbsvh.effect.isSkill() ? mbsvh.effect.getSourceId() : -mbsvh.effect.getSourceId());
                if (!hs.contains(nid)) {
                    almseret.add(mbsvh.effect);
                    hs.add(nid);
                }
            }
        }
        return almseret;
    }

    public int getBuffSource(MapleBuffStat stat) {
        MapleBuffStatValueHolder mbsvh = effects.get(stat);
        if (mbsvh == null) {
            return -1;
        }
        return mbsvh.effect.getSourceId();
    }

    private List<MapleBuffStat> getBuffStats(final MapleStatEffect effect, final long startTime) {
        final List<MapleBuffStat> stats = new ArrayList<MapleBuffStat>();
        for (Entry<MapleBuffStat, MapleBuffStatValueHolder> stateffect : effects.entrySet()) {
            final MapleBuffStatValueHolder mbsvh = stateffect.getValue();
            if (mbsvh.effect.sameSource(effect) && (startTime == -1 || startTime == mbsvh.startTime)) {
                stats.add(stateffect.getKey());
            }
        }
        return stats;
    }

    public int getChair() {
        return chair;
    }

    public String getChalkboard() {
        return this.chalktext;
    }

    public final MapleClient getClient() {
        return client;
    }

    public final List<MapleQuestStatus> getCompletedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2) {
                ret.add(q);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public Collection<MapleMonster> getControlledMonsters() {
        return Collections.unmodifiableCollection(controlled);
    }

    public int getCSPoints(int type) {
        if (type == 1) {
            return nx;
        }
        return 0;
    }

    public int getCurrentMaxHp() {
        return localmaxhp;
    }

    public int getCurrentMaxMp() {
        return localmaxmp;
    }

    public int getDex() {
        return dex;
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<MapleDoor>(doors);
    }

    public int getEnergyBar() {
        return energybar;
    }

    public int getDojoEnergy() {
        return dojoenergy;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public ArrayList<Integer> getExcluded() {
        return excluded;
    }

    public int getExp() {
        return exp.get();
    }

    public int getExpRate() {
        return expRate;
    }

    public int getFace() {
        return face;
    }

    public int getFallCounter() {
        return fallcounter;
    }

    public int getCoupleId() {
        return coupleId;
    }

    public int getBanCount() {
        return banCount;
    }

    public int getFame() {
        return fame;
    }

    public MapleFamilyEntry getFamily() {
        return MapleFamily.getMapleFamily(this).getMember(getId());
    }

    public int getGender() {
        return gender;
    }

    public int getDonorPts() {
        return donorpts;
    }

    public int getDojoPts() {
        return dojopts;
    }

    public int getCarnivalPts() {
        return carnivalpts;
    }

    public int getFamilyId() {
        return familyId;
    }

    public int getReborns() {
        return reborns;
    }

    public int getRebornPts() {
        return rebornPts;
    }

    public int getNpcId() {
        return npcId;
    }

    public void setNpcId(int npcId) {
        this.npcId = npcId;
    }

    public boolean getGMChat() {
        return whitechat;
    }

    public MapleGuild getGuild() {
        try {
            return client.getChannelServer().getWorldInterface().getGuild(getGuildId(), null);
        } catch (Exception ex) {
            return null;
        }
    }

    public int getGuildId() {
        return guildid;
    }

    public int getGuildRank() {
        return guildrank;
    }

    public int getHair() {
        return hair;
    }

    public HiredMerchant getHiredMerchant() {
        return hiredMerchant;
    }

    public int getHp() {
        return hp;
    }

    public int getHpApUsed() {
        return hpApUsed;
    }

    public int getMpApUsed() {
        return mpApUsed;
    }

    public int getId() {
        return id;
    }

    public static int getByName(String name, String data) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT ? FROM characters WHERE name = ?");
            ps.setString(1, data);
            ps.setString(2, name);
            ResultSet rs = ps.executeQuery();
            int id = -1;
            if (rs.next()) {
                id = rs.getInt(data);
            }
            rs.close();
            ps.close();
            return id;
        } catch (Exception e) {
        }
        return -1;
    }

    public int getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public int getInt() {
        return int_;
    }

    public final MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public final MapleInventory[] getInventories() {
        return inventory;
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int possesed = inventory[MapleItemInformationProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    public int getJob() {
        return job;
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

    public int getJobType() {
        return job / 1000;
    }

    public Map<Integer, MapleKeyBinding> getKeymap() {
        return keymap;
    }

    public long getLastHealed() {
        return lastHealed;
    }

    public long getLastAnnounced() {
        return lastAnnounced;
    }

    public long getMuted() {
        return muted;
    }

    public long getLastTalked() {
        return lastTalked;
    }

    public String getWordsTalked() {
        return wordsTalked;
    }

    public int getSpamCheck() {
        return spamCheck;
    }

    public int getLevel() {
        return level;
    }

    public String getLinkedName() {
        return linkedName;
    }

    public int getLinkedLevel() {
        return linkedLevel;
    }

    public boolean isLinked() {
        return linkedName != null;
    }

    public int getLuk() {
        return luk;
    }

    public MapleMap getMap() {
        return map;
    }

    public MapleRing getMarriageRing() {
        return marriageRing;
    }

    public void setMarriageRing(MapleRing marriageRing) {
        this.marriageRing = marriageRing;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public int getMasterLevel(final ISkill skill) {
        final SkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.masterlevel;
    }

    public int getMaxHp() {
        return maxhp;
    }

    public int getMaxLevel() {
        if(world == 1) {
        return isCygnus() ? 120 : 200;
        }
        return 200;
    }

    public int getMaxMp() {
        return maxmp;
    }

    public int getMeso() {
        return meso.get();
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    public MapleMiniGame getMiniGame() {
        return miniGame;
    }

    public int getMiniGamePoints(String type, boolean omok) {
        if (type.equals("wins")) {
            return omokWins;
        } else if (type.equals("losses")) {
            return omokLosses;
        } else {
            return omokTies;
        }
    }

    public MonsterBook getMonsterBook() {
        return monsterBook;
    }

    public int getMonsterBookCover() {
        return bookCover;
    }

    public MapleMount getMount() {
        return mount;
    }

    public int getMp() {
        return mp;
    }

    public MapleMessenger getMessenger() {
        return messenger;
    }

    public String getName() {
        return name;
    }

    public int getNextEmptyPetIndex() {
        for (int i = 0; i < 3; i++) {
            if (pets[i] == null) {
                return i;
            }
        }
        return 3;
    }

    public int getNoPets() {
        int ret = 0;
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                ret++;
            }
        }
        return ret;
    }

    public int getNumControlledMonsters() {
        return controlled.size();
    }

    public MapleParty getParty() {
        return party;
    }

    public int getPartyId() {
        return (party != null ? party.getId() : -1);
    }

    public final MaplePet getPet(final int index) {
        return pets[index];
    }

    public int getPetIndex(final int petId) {
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == petId) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int getPetIndex(final MaplePet pet) {
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == pet.getUniqueId()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public MaplePet[] getPets() {
        return pets;
    }

    public MapleQuestStatus getQuest(MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            return new MapleQuestStatus(quest, 0);
        }
        return quests.get(quest);
    }

    public final Map<MapleQuest, MapleQuestStatus> getQuests() {
        return quests;
    }

    public final int getDojoBossCount(int index) {
        return dojoBossCount[index];
    }

    public final int[] getDojoBossCounts() {
        return dojoBossCount;
    }

    public int[] getQuickSlot() {
        return quickslots;
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getRemainingAp() {
        return remainingAp;
    }

    public int getRemainingSp() {
        return remainingSp;
    }

    public int getSavedLocation(String type) {
        SavedLocation sl = savedLocations[SavedLocationType.fromString(type).ordinal()];
        if(sl == null) {
            return -1;
        }
        int m = sl.getMapId();
        if (!SavedLocationType.fromString(type).equals(SavedLocationType.WORLDTOUR)) {
            clearSavedLocation(SavedLocationType.fromString(type));
        }
        return m;
    }

    public final SavedLocation[] getSavedLocations() {
        return savedLocations;
    }

    public String getSearch() {
        return search;
    }

    public MapleShop getShop() {
        return shop;
    }

    public Map<ISkill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public int getSkillLevel(int skill) {
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return 0;
        }
        return ret.skillevel;
    }

    public int getSkillLevel(final ISkill skill) {
        final SkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.skillevel;
    }

    public int getSkinColor() {
        return skinColor;
    }

    public final List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 1) {
                ret.add(q);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect;
    }

    public MapleStorage getStorage() {
        return storage;
    }

    public int getStr() {
        return str;
    }

    public Map<Integer, MapleSummon> getSummons() {
        return summons;
    }

    public List<Integer> getTeleportRockMaps(int type) {
        if (type == 0) {
            return rockMaps;
        } else {
            return vipRockMaps;
        }
    }

    public int getTotalStr() {
        return localstr;
    }

    public int getTotalLuk() {
        return localluk;
    }

    public int getTotalDex() {
        return localdex;
    }

    public int getTotalAcc() {
        return localacc;
    }

    public int getTotalMagic() {
        return magic;
    }

    public int getTotalWatk() {
        return watk;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public Collection<MapleMapObject> getVisibleMapObjects() {
        return Collections.unmodifiableCollection(visibleMapObjects);
    }

    public List<Integer> getWishList() {
        return wishList;
    }

    public int getWorld() {
        return world;
    }

    public void giveCoolDown(final int skillid, long starttime, long length) {
        int time = (int) ((length + starttime) - System.currentTimeMillis());
        addCooldown(skillid, System.currentTimeMillis(), time, TimerManager.getInstance().schedule(new CancelCooldownAction(this, skillid), time));
    }

    public void giveCoolDowns(final List<PlayerCoolDownValueHolder> cooldowns) {
        int time;
        if (cooldowns != null) {
            for (PlayerCoolDownValueHolder cooldown : cooldowns) {
                time = (int) ((cooldown.length + cooldown.startTime) - System.currentTimeMillis());
                ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(this, cooldown.skillId), time);
                addCooldown(cooldown.skillId, System.currentTimeMillis(), time, timer);
            }
        } else {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT skillid, starttime, length FROM cooldowns WHERE characterid = ?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    final long length = rs.getLong("length"), startTime = rs.getLong("starttime");
                    if (length + startTime > System.currentTimeMillis()) {
                        giveCoolDown(rs.getInt("skillid"), startTime, length);
                    }
                }
                rs.close();
                ps.close();
                deleteWhereCharacterId(con, "DELETE FROM cooldowns WHERE characterid = ?");
                rs = null;
                ps = null;
            } catch (SQLException e) {
                System.err.println("Error while retriving cooldown from SQL storage");
            }
        }
    }

    public final boolean hasDisease(final MapleDisease dis) {
        for (final MapleDisease disease : diseases.keySet()) {
            if (disease == dis) {
                return true;
            }
        }
        return false;
    }

    public void giveDebuff(final MapleDisease disease, final MobSkill skill) {
        final List<Pair<MapleDisease, Integer>> debuff = Collections.singletonList(new Pair<MapleDisease, Integer>(disease, Integer.valueOf(skill.getX())));
        if (!hasDisease(disease) && diseases.size() < 2) {
            if (!(disease == MapleDisease.SEDUCE || disease == MapleDisease.STUN)) {
                if (isActiveBuffedValue(2321005)) {
                    return;
                } else if (disease == MapleDisease.POISON) {
                    poisonTask = TimerManager.getInstance().register(new Runnable() {
                        @Override
                        public void run() {
                            if (!hasDisease(MapleDisease.POISON)) {
                                poisonTask.cancel(false);
                            }
                            addHP(skill.getX());
                        }
                    }, 900, 900);
                }
            }
            TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    dispelDebuff(disease);
                }
            }, skill.getDuration());

            diseases.put(disease, new MapleDiseaseValueHolder(System.currentTimeMillis(), skill.getDuration()));
            client.announce(BuffFactory.giveDebuff(debuff, skill));
            map.broadcastMessage(this, BuffFactory.giveForeignDebuff(id, debuff, skill), false);
        }
    }

    public final void silentGiveDebuffs(final List<PlayerDiseaseValueHolder> ld) {
        if (ld != null) {
            for (final PlayerDiseaseValueHolder disease : ld) {
                TimerManager.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        dispelDebuff(disease.disease);
                    }
                }, (disease.length + disease.startTime) - System.currentTimeMillis());
                diseases.put(disease.disease, new MapleDiseaseValueHolder(disease.startTime, disease.length));
            }
        }
    }

    public int gmLevel() {
        return gmLevel;
    }

    public boolean gotPartyQuestItem(String partyquestchar) {
        return partyquestitems.contains(partyquestchar);
    }

    private void guildUpdate() {
        if (this.guildid < 1) {
            return;
        }
        mgc.setLevel(level);
        mgc.setJobId(job);
        try {
            this.client.getChannelServer().getWorldInterface().memberLevelJobUpdate(this.mgc);
            int allianceId = getGuild().getAllianceId();
            if (allianceId > 0) {
                client.getChannelServer().getWorldInterface().allianceMessage(allianceId, GuildFactory.updateAllianceJobLevel(this), getId(), -1);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public final void handleEnergyChargeGain() { // to get here energychargelevel has to be > 0
        final ISkill energycharge = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.ENERGY_CHARGE) : SkillFactory.getSkill(Marauder.ENERGY_CHARGE);
        final MapleStatEffect ceffect = energycharge.getEffect(getSkillLevel(energycharge));
        TimerManager tMan = TimerManager.getInstance();
        if (energybar < 10000) {
            energybar += 92 + getSkillLevel(energycharge) * 4;
            if (energybar > 10000) {
                energybar = 10000;
            }
            client.announce(BuffFactory.giveEnergyCharge(energybar));
            client.announce(BuffFactory.showOwnBuffEffect(energycharge.getId(), 2));
            getMap().broadcastMessage(this, BuffFactory.showBuffEffect(this, id, energycharge.getId(), 2));
            if (energybar == 10000) {
                getMap().broadcastMessage(this, BuffFactory.giveForeignEnergyCharge(id, energybar));
            }
        }
        if (energybar >= 10000 && energybar < 11000) {
            energybar = 15000;
            final MapleCharacter chr = this;
            tMan.schedule(new Runnable() {
                @Override
                public void run() {
                    client.announce(BuffFactory.giveEnergyCharge(0));
                    getMap().broadcastMessage(chr, BuffFactory.giveForeignEnergyCharge(id, energybar));
                    energybar = 0;
                }
            }, ceffect.getDuration());
        }
    }

    public void handleDojoEnergyGain(boolean fastattack) {
        int increment = fastattack ? 25 : 250;
        if (dojoenergy + increment > 10000) {
            dojoenergy = 10000;
        } else {
            dojoenergy += increment;
        }
        client.announce(EffectFactory.getEnergy(dojoenergy));
    }

    public void handleOrbConsume() {
        int skillid = isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
        ISkill combo = SkillFactory.getSkill(skillid);
        List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO, 1));
        setBuffedValue(MapleBuffStat.COMBO, 1);
        client.announce(BuffFactory.giveBuff(skillid, combo.getEffect(getSkillLevel(combo)).getDuration() + (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis())), stat, false));
        getMap().broadcastMessage(this, BuffFactory.giveForeignBuff(getId(), stat, false), false);
    }

    public boolean hasEntered(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEntered(String script, int mapId) {
        if (entered.containsKey(mapId)) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMerchant() {
        return hasMerchant;
    }

    public final boolean haveItem(int itemid) {
        return getItemQuantity(itemid, false) > 0;
    }

    public final boolean haveItem(int itemid, int quantity) {
        return getItemQuantity(itemid, false) >= quantity;
    }

    public boolean haveItemEquipped(int itemid) {
        if (getInventory(MapleInventoryType.EQUIPPED).findById(itemid) != null) {
            return true;
        }
        return false;
    }

    public void increaseGuildCapacity() {
        if (getMeso() < 500000) {
            dropMessage(1, "You don't have enough mesos.");
            return;
        }
        try {
            client.getChannelServer().getWorldInterface().increaseGuildCapacity(guildid);
        } catch (RemoteException e) {
            client.getChannelServer().reconnectWorld();
            return;
        }
        gainMeso(-500000, true, false, false);
    }

    public boolean isActiveBuffedValue(int skillid) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                return true;
            }
        }
        return false;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public boolean isBuffFrom(MapleBuffStat stat, ISkill skill) {
        MapleBuffStatValueHolder mbsvh = effects.get(stat);
        if (mbsvh == null) {
            return false;
        }
        return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
    }

    public boolean isCygnus() {
        return getJobType() == 1;
    }

    public boolean isAran() {
        return job == 2000 || job / 100 == 21;
    }

    public boolean isA(int baseJob) {
        return job >= baseJob && job / 100 == baseJob / 100;
    }

    public boolean isGM() {
        return gmLevel > 0;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isMapObjectVisible(MapleMapObject mo) {
        return visibleMapObjects.contains(mo);
    }

    public boolean isPartyLeader() {
        return party.getLeader() == party.getMemberById(getId());
    }

    public void leaveMap() {
        controlled.clear();
        visibleMapObjects.clear();
        if (chair != 0) {
            chair = 0;
        }
        if (hpDecreaseTask != null) {
            hpDecreaseTask.cancel(false);
        }
    }

    public void setReborns(int reborns) {
        this.reborns = reborns;
    }

    public void setRebornPts(int rebornPts) {
        this.rebornPts = rebornPts;
    }

    public void levelUp(boolean takeexp) {
        int improvingMaxHPLevel = 0;
        int improvingMaxMPLevel = 0;
        if (job / 100 % 10 > 0 || level > 10) {
            remainingAp += 5 + (reborns * 2);
        } else {
            this.str += 5;
            this.updateSingleStat(MapleStat.STR, str);
        }
        if (isCygnus()) {
            if(level < 70)
            remainingAp++;
            else if(level > 120 && level <= 190)
            remainingAp--;
        }
        int jobtype = job / 100 % 10;
        if (jobtype == 0) {
            maxhp += rand(8, 12);
            maxmp += rand(6, 8);
        } else if (jobtype == 1) {
            if (isAran()) {
                maxhp += Randomizer.rand(Math.min(50, (10 + level)), Math.min(50, (10 + level)) + 4); //SPECIAL
            } else {
                final ISkill improvingMaxHP = isCygnus() ? SkillFactory.getSkill(DawnWarrior.MAX_HP_INCREASE) : SkillFactory.getSkill(Swordsman.IMPROVED_MAX_HP_INCREASE);
                final ISkill improvingMaxMP = isCygnus() ? SkillFactory.getSkill(DawnWarrior.INCREASED_MP_RECOVERY) : job == Crusader.ID ? SkillFactory.getSkill(Crusader.IMPROVING_MP_RECOVERY) : SkillFactory.getSkill(WhiteKnight.IMPROVING_MP_RECOVERY);
                improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
                if (improvingMaxHPLevel > 0) {
                    maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
                }
                improvingMaxMPLevel = getSkillLevel(improvingMaxMP);
                if (improvingMaxMPLevel > 0) {
                    maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
                }
                maxhp += rand(20, 24);
            }
            maxmp += Randomizer.rand(4, 6); //TOO EASY
        } else if (jobtype == 2) {
            final ISkill improvingMaxMP = isCygnus() ? SkillFactory.getSkill(BlazeWizard.INCREASING_MAX_MP) : SkillFactory.getSkill(Magician.IMPROVED_MAX_MP_INCREASE);
            improvingMaxMPLevel = getSkillLevel(improvingMaxMP);
            if (improvingMaxMPLevel > 0) {
                maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
            }
            maxhp += rand(6, 10);
            maxmp += rand(18, 20);
        } else if (jobtype <= 4) {
            maxhp += rand(16, 20);
            maxmp += rand(10, 12);
        } else if (jobtype == 5) {
            final ISkill improvingMaxHP = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.IMPROVE_MAX_HP) : SkillFactory.getSkill(Brawler.IMPROVE_MAX_HP);
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            if (improvingMaxHPLevel > 0) {
                maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
            }
            maxhp += rand(18, 22);
            maxmp += rand(14, 16);
        } else if (jobtype == 9) {
            maxhp = 30000;
            maxmp = 30000;
        }
        maxmp += localint_ / 10;
        if (takeexp) {
            exp.addAndGet(-ExpTable.getExpNeededForLevel(level));
            if (exp.get() < 0) {
                exp.set(0);
            }
        }
        level++;
        if (level >= getMaxLevel()) {
            exp.set(0);
        }
        maxhp = Math.min(30000, maxhp);
        maxmp = Math.min(30000, maxmp);

        hp = maxhp;
        mp = maxmp;
        recalcLocalStats();
        final List<Pair<MapleStat, Integer>> statup = new ArrayList<Pair<MapleStat, Integer>>(8);
        statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLEAP, remainingAp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.HP, localmaxhp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.MP, localmaxmp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.EXP, exp.get()));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.LEVEL, level));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, maxhp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, maxmp));
        if (jobtype > 0) {
            remainingSp += 3;
            statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLESP, remainingSp));
        }
        client.announce(IntraPersonalFactory.updatePlayerStats(statup, false));
        getMap().broadcastMessage(this, EffectFactory.showForeignEffect(getId(), 0), false);
        if (level == 8 && getJobType() == 0 || level == 10 || level == 30 || level == 70 || level == 120) {
            dropMessage(5, "Type @toggleexperience to turn on/off experience gain to prevent over-leveling.");
        } else if(level == 200 && gmLevel < 1) {
            try {
                client.getChannelServer().getWorldInterface().broadcastMessage(null, EffectFactory.serverNotice(6, "[Notice] " + name + " has reached Level 200!").getBytes());
            } catch (RemoteException re) {
                client.getChannelServer().reconnectWorld();
            }
        }
        client.announce(IntraPersonalFactory.updatePlayerStats(statup, false));
        getMap().broadcastMessage(this, EffectFactory.showForeignEffect(getId(), 0), false);
        recalcLocalStats();
        silentPartyUpdate();
        guildUpdate();
        if (this.guildid > 0) {
            this.getGuild().broadcast(EffectFactory.serverNotice(5, "[Guild] " + name + " has reached Lv. " + level + "."));
        }
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver) throws SQLException {
        try {
            final MapleCharacter ret = new MapleCharacter();
            ret.client = client;
            ret.id = charid;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT `name`, `level`, `str`, `dex`, `int`, `luk`, `exp`, `hp`, `maxhp`, `mp`, `maxmp`, `hpapused`, `mpapused`, `sp`, `ap`, `meso`, `gm`, `skincolor`, `gender`, `job`, `hair`, `face`, `accountid`, `map`, `spawnpoint`, `world`, `coupleid`, `reborns`, `rebornpts`, `family`, `mountexp`, `mountlevel`, `mounttiredness`, `omokwins`, `omoklosses`, `omokties`, `guildid`, `guildrank`, `alliancerank`, `hasmerchant`, `equipslots`, `useslots`, `setupslots`, `etcslots`, `buddyslots`, `partyquestitems`, `fame`, `monsterbookcover`, `party`, `donorpts`, `dojopts`, `carnivalpts` FROM characters WHERE id = ?");
            ps.setInt(1, charid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("Loading char failed (not found)");
            }
            ret.name = rs.getString("name");
            ret.level = rs.getInt("level");
            ret.donorpts = rs.getInt("donorpts");
            ret.dojopts = rs.getInt("dojopts");
            ret.carnivalpts = rs.getInt("carnivalpts");
            ret.fame = rs.getInt("fame");
            ret.str = rs.getInt("str");
            ret.dex = rs.getInt("dex");
            ret.int_ = rs.getInt("int");
            ret.luk = rs.getInt("luk");
            ret.exp.set(rs.getInt("exp"));
            ret.hp = rs.getInt("hp");
            ret.maxhp = rs.getInt("maxhp");
            ret.mp = rs.getInt("mp");
            ret.maxmp = rs.getInt("maxmp");
            ret.hpApUsed = rs.getInt("hpapused");
            ret.mpApUsed = rs.getInt("mpapused");
            ret.remainingSp = rs.getInt("sp");
            ret.remainingAp = rs.getInt("ap");
            ret.meso.set(rs.getInt("meso"));
            ret.gmLevel = rs.getInt("gm");
            ret.skinColor = rs.getInt("skincolor");
            ret.gender = rs.getInt("gender");
            ret.job = rs.getInt("job");
            ret.hair = rs.getInt("hair");
            ret.face = rs.getInt("face");
            ret.accountid = rs.getInt("accountid");
            ret.mapid = rs.getInt("map");
            ret.initialSpawnPoint = rs.getInt("spawnpoint");
            ret.world = rs.getInt("world");
            ret.coupleId = rs.getInt("coupleid");
            ret.reborns = rs.getInt("reborns");
            ret.rebornPts = rs.getInt("rebornpts");
            int mountexp = rs.getInt("mountexp");
            int mountlevel = rs.getInt("mountlevel");
            int mounttiredness = rs.getInt("mounttiredness");
            ret.omokWins = rs.getInt("omokwins");
            ret.omokLosses = rs.getInt("omoklosses");
            ret.omokTies = rs.getInt("omokties");
            ret.guildid = rs.getInt("guildid");
            ret.guildrank = rs.getInt("guildrank");
            ret.alliancerank = rs.getInt("alliancerank");
            ret.bookCover = rs.getInt("monsterbookcover");
            ret.monsterBook = new MonsterBook();
            ret.monsterBook.loadCards(charid);
            ret.whitechat = ret.gmLevel > 0;
            ret.hasMerchant = rs.getInt("hasmerchant") == 1;
            ret.familyId = rs.getInt("family");
            ret.partyquestitems = rs.getString("partyquestitems");
            if (ret.guildid > 0) {
                ret.mgc = new MapleGuildCharacter(ret);
            }
            ret.buddylist = new BuddyList(rs.getInt("buddyslots"));
            for (byte i = 1; i <= 4; i++) {
                MapleInventoryType type = MapleInventoryType.getByType(i);
                ret.getInventory(type).setSlotLimit(rs.getInt(type.name().toLowerCase() + "slots"));
            }
            for (Pair<IItem, MapleInventoryType> item : ItemFactory.INVENTORY.loadItems(ret.id, !channelserver)) {
                ret.getInventory(item.getRight()).addFromDB(item.getLeft());
            }
            if (channelserver) {
                final MapleMapFactory mapFactory = client.getChannelServer().getMapFactory();
                ret.map = mapFactory.getMap(ret.mapid);
                if (ret.map == null) {
                    ret.map = mapFactory.getMap(100000000);
                }
                MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                if (portal == null) {
                    portal = ret.map.getPortal(0);
                    ret.initialSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());
                int partyid = rs.getInt("party");
                try {
                    MapleParty party = client.getChannelServer().getWorldInterface().getParty(partyid);
                    if (party != null) {
                        ret.party = party;
                    }
                } catch (RemoteException ex) {
                    client.getChannelServer().reconnectWorld();
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT `name`, `level` FROM `characters` WHERE `accountid` = ? AND `id` <> ? AND `world` = ? ORDER BY `level` DESC LIMIT 1");
            ps.setInt(1, ret.accountid);
            ps.setInt(2, ret.id);
            ps.setInt(3, ret.world);
            rs = ps.executeQuery();
            if (rs.next()) {
                ret.linkedName = rs.getString("name");
                ret.linkedLevel = rs.getInt("level");
            }
            rs.close();
            ps.close();
            if(ret.coupleId > 0) {
                ret.marriageRing = MapleRing.loadFromDB(ret);
            }
            ps = con.prepareStatement("SELECT `name`, `nx` FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            rs = ps.executeQuery();
            if (rs.next()) {
                ret.client.setAccountName(rs.getString("name"));
                ret.nx = rs.getInt("nx");
            }
            rs.close();
            ps.close();
            if (channelserver) {
                ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                PreparedStatement pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");
                while (rs.next()) {
                    final MapleQuest q = MapleQuest.getInstance(rs.getInt("quest"));
                    final MapleQuestStatus status = new MapleQuestStatus(q, rs.getInt("status"));
                    final long cTime = rs.getLong("time");
                    if (cTime > -1) {
                        status.setCompletionTime(cTime * 1000);
                    }
                    status.setForfeited(rs.getInt("forfeited"));
                    status.setQuestRecord(rs.getString("customData"));
                    ret.quests.put(q, status);
                    pse.setInt(1, rs.getInt("queststatusid"));
                    final ResultSet rsMobs = pse.executeQuery();
                    while (rsMobs.next()) {
                        status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
                    }
                    rsMobs.close();
                }
                rs.close();
                ps.close();
                pse.close();
                ps = con.prepareStatement("SELECT * FROM questinfo WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("questid");
                    String info = rs.getString("info");
                    ret.questinfo.add(new Pair<Integer, String>(id, info));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `skillid`, `skilllevel`, `masterlevel` FROM skills WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.skills.put(SkillFactory.getSkill(rs.getInt("skillid")), new SkillEntry(rs.getInt("skilllevel"), rs.getInt("masterlevel")));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int position = rs.getInt("position");
                    SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"), position);
                    ret.skillMacros[position] = macro;
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                Map<Integer, MapleKeyBinding> keys = ret.keymap;
                while (rs.next()) {
                    keys.put(Integer.valueOf(rs.getInt("key")), new MapleKeyBinding(rs.getInt("type"), rs.getInt("action")));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `locationtype`,`map`,`portal` FROM savedlocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.savedLocations[SavedLocationType.valueOf(rs.getString("locationtype")).ordinal()] = new SavedLocation(rs.getInt("map"), rs.getInt("portal"));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT mapId, type FROM telerockmaps WHERE characterId = ? ORDER BY type");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getInt("type") == 0) {
                        ret.rockMaps.add(Integer.valueOf(rs.getInt("mapid")));
                    } else {
                        ret.vipRockMaps.add(Integer.valueOf(rs.getInt("mapid")));
                    }
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                ret.lastFameTime = 0;
                ret.lastMonthFameIds = new ArrayList<Integer>(31);
                while (rs.next()) {
                    ret.lastFameTime = Math.max(ret.lastFameTime, rs.getTimestamp("when").getTime());
                    ret.lastMonthFameIds.add(Integer.valueOf(rs.getInt("characterid_to")));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `sn` FROM wishlist WHERE `characterid` = ?");
                ps.setInt(1, ret.id);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.wishList.add(rs.getInt("sn"));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `boss`, `count` FROM dojocounts WHERE `characterid` = ?");
                ps.setInt(1, ret.id);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.dojoBossCount[rs.getInt("boss")] = rs.getInt("count");
                }
                rs.close();
                ps.close();
                ret.buddylist.loadFromDb(charid);
                ret.storage = MapleStorage.loadOrCreateFromDB(ret.accountid, ret.world);
                ret.recalcLocalStats();
                ret.resetBattleshipHp();
                ret.silentEnforceMaxHpMp();
            }
            int mountid = ret.getJobType() * 10000000 + 1004;
            final IItem mount = ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
            if (mount != null) {
                ret.mount = new MapleMount(ret, mount.getId(), mountid);
            } else {
                ret.mount = new MapleMount(ret, 0, mountid);
            }
            ret.mount.setExp(mountexp);
            ret.mount.setLevel(mountlevel);
            ret.mount.setTiredness(mounttiredness);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public final static MapleCharacter loadCharFromTransfer(final CharacterTransfer ct, final MapleClient client, final boolean isChannel) {
        final MapleCharacter ret = new MapleCharacter();
        ret.client = client;
        if (!isChannel) {
            ret.client.setChannel(ct.channel);
        }
        ret.id = ct.characterid;
        ret.name = ct.name;
        ret.level = ct.level;
        ret.donorpts = ct.donorpts;
        ret.dojopts = ct.dojopts;
        ret.carnivalpts = ct.carnivalpts;
        ret.fame = ct.fame;
        ret.str = ct.str;
        ret.dex = ct.dex;
        ret.int_ = ct.int_;
        ret.luk = ct.luk;
        ret.exp.set(ct.exp);
        ret.hp = ct.hp;
        ret.maxhp = ct.maxhp;
        ret.mp = ct.mp;
        ret.maxmp = ct.maxmp;
        ret.hpApUsed = ct.hpApUsed;
        ret.mpApUsed = ct.mpApUsed;
        ret.remainingSp = ct.remainingSp;
        ret.remainingAp = ct.remainingAp;
        ret.meso.set(ct.meso);
        ret.gmLevel = ct.gmLevel;
        ret.skinColor = ct.skinColor;
        ret.gender = ct.gender;
        ret.job = ct.job;
        ret.hair = ct.hair;
        ret.face = ct.face;
        ret.accountid = ct.accountid;
        ret.mapid = ct.mapid;
        ret.initialSpawnPoint = ct.initialSpawnPoint;
        ret.world = ct.world;
        ret.coupleId = ct.coupleId;
        ret.reborns = ct.reborns;
        ret.rebornPts = ct.rebornPts;
        int mountexp = ct.mountexp;
        int mountlevel = ct.mountlevel;
        int mounttiredness = ct.mounttiredness;
        ret.omokWins = ct.omokwins;
        ret.omokLosses = ct.omoklosses;
        ret.omokTies = ct.omokties;
        ret.guildid = ct.guildid;
        ret.guildrank = ct.guildrank;
        ret.alliancerank = ct.alliancerank;
        ret.bookCover = ct.bookCover;
        ret.monsterBook = (MonsterBook) ct.monsterbook;
        ret.whitechat = ret.gmLevel > 0;
        ret.hasMerchant = ct.hasMerchant;
        ret.partyquestitems = ct.partyquestitems;
        ret.questinfo = (List<Pair<Integer, String>>) ct.questinfo;
        ret.dojoBossCount = (int[]) ct.dojoBossCount;
        if (ret.guildid > 0) {
            ret.mgc = new MapleGuildCharacter(ret);
        }
        ret.buddylist = new BuddyList(ct.buddyslots);
        ret.familyId = ct.familyid;
        ret.inventory = (MapleInventory[]) ct.inventories;
        if (isChannel) {
            final MapleMapFactory mapFactory = client.getChannelServer().getMapFactory();
            ret.map = mapFactory.getMap(ret.mapid);
            if (ret.map == null) {
                ret.map = mapFactory.getMap(100000000);
            }
            MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
            if (portal == null) {
                portal = ret.map.getPortal(0);
                ret.initialSpawnPoint = 0;
            }
            ret.setPosition(portal.getPosition());
        }
        ret.messenger = null;
        int partyid = ct.partyid;
        if (partyid >= 0) {
            MapleParty party = WorldRegistryImpl.getInstance().getParty(partyid);
            if (party != null && party.getMemberById(ret.id) != null) {
                ret.party = party;
            }
        }
        ret.linkedName = ct.linkedname;
        ret.linkedLevel = ct.linkedlevel;
        if(ret.coupleId > 0) {
            ret.marriageRing = (MapleRing) ct.marriageRing;
        }
        ret.client.setAccountName(ct.accountname);
        ret.nx = ct.nx;
        MapleQuestStatus queststatus;
        MapleQuestStatus queststatus_from;
        MapleQuest quest;
        for (final Map.Entry<Integer, Object> qs : ct.quests.entrySet()) {
            quest = MapleQuest.getInstance(qs.getKey());
            queststatus_from = (MapleQuestStatus) qs.getValue();

            queststatus = new MapleQuestStatus(quest, queststatus_from.getStatus());
            queststatus.setForfeited(queststatus_from.getForfeited());
            queststatus.setQuestRecord(queststatus_from.getQuestRecord());
            queststatus.setCompletionTime(queststatus_from.getCompletionTime());

            if (queststatus_from.getMobKills() != null) {
                for (final Map.Entry<Integer, Integer> mobkills : queststatus_from.getMobKills().entrySet()) {
                    queststatus.setMobKills(mobkills.getKey(), mobkills.getValue());
                }
            }
            ret.quests.put(quest, queststatus);
        }

        for (final Map.Entry<Integer, Object> qs : ct.skills.entrySet()) {
            ret.skills.put(SkillFactory.getSkill(qs.getKey()), (SkillEntry) qs.getValue());
        }
        ret.skillMacros = (SkillMacro[]) ct.skillmacro;
        ret.keymap = (Map<Integer, MapleKeyBinding>) ct.keymap;
        ret.savedLocations = (SavedLocation[]) ct.savedLocations;
        ret.rockMaps = (List<Integer>) ct.rockMaps;
        ret.vipRockMaps = (List<Integer>) ct.vipRockMaps;
        ret.lastFameTime = ct.lastFameTime;
        ret.lastMonthFameIds = (List<Integer>) ct.lastMonthFameIds;
        ret.wishList = (List<Integer>) ct.wishList;
        ret.buddylist.loadFromTransfer((Map<Integer, Pair<String, Boolean>>) ct.buddies);
        ret.storage = (MapleStorage) ct.storage;
        ret.recalcLocalStats();
        ret.resetBattleshipHp();
        ret.silentEnforceMaxHpMp();
        int mountid = ret.getJobType() * 10000000 + 1004;
        final IItem mount = ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
        if (mount != null) {
            ret.mount = new MapleMount(ret, mount.getId(), mountid);
        } else {
            ret.mount = new MapleMount(ret, 0, mountid);
        }
        ret.mount.setExp(mountexp);
        ret.mount.setLevel(mountlevel);
        ret.mount.setTiredness(mounttiredness);
        return ret;
    }

    public static String makeMapleReadable(String in) {
        String i = in.replace('I', 'i');
        i = i.replace('l', 'L');
        i = i.replace("rn", "Rn");
        i = i.replace("v", "V");
        return i;
    }

    public void message(String m) {
        dropMessage(5, m);
    }

    public void mobKilled(int id) {
        try {
            for (int i = 0; i < 5; i++) {
                if (id == MapleQuest.MISMATCHED_MOBS[i][1]) {
                    id = MapleQuest.MISMATCHED_MOBS[i][0];
                    break;
                }
            }
            for (MapleQuestStatus q : quests.values()) {
                if (q.getStatus() == 2 || !q.hasMobKills() || q.getQuest().canComplete(this, null)) {
                    continue;
                }
                if (q.mobKilled(id)) {
                    client.announce(QuestFactory.updateQuestMobKills(q));
                    saveQuests = true;
                    if (q.getQuest().canComplete(this, null)) {
                        client.announce(QuestFactory.getShowQuestCompletion(q.getQuest().getId()));
                    }
                }
            }
        } catch (NullPointerException e) {
        }
    }

    public void modifyCSPoints(int type, int dx) {
        this.nx += dx;
        saveAccount = true;
    }

    public void giveMount(int id, int skillid) {
        mount = new MapleMount(this, id, skillid);
    }

    public void offBeacon(boolean bf) {
        hasBeacon = false;
        beaconOid = -1;
        if (bf) {
            cancelEffectFromBuffStat(MapleBuffStat.HOMING_BEACON);
        }
    }

    public int getProjectile() {
        return projectile;
    }

    private String prepareKeymapQuery() {
        StringBuilder query = new StringBuilder("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES ");
        for (Iterator<Entry<Integer, MapleKeyBinding>> i = this.keymap.entrySet().iterator(); i.hasNext();) {
            String entry = "";
            Formatter itemEntry = new Formatter();
            Entry<Integer, MapleKeyBinding> e = i.next();
            itemEntry.format("(%d, %d, %d, %d)",
            id, e.getKey().intValue(), e.getValue().getType(), e.getValue().getAction());
            if (i.hasNext()) {
                entry = itemEntry.toString() + ", ";
            } else {
                entry = itemEntry.toString();
            }
            query.append(entry);
        }
        return query.toString();
    }
    
    private void playerDead() {
        cancelAllBuffs();
        dispelDebuffs();
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }
        final int[] charmID = {5130000, 4031283, 4140903};
        int possessed = 0;
        int i;
        for (i = 0; i < charmID.length; i++) {
            int quantity = getItemQuantity(charmID[i], false);
            if (possessed == 0 && quantity > 0) {
                possessed = quantity;
                break;
            }
        }
        if (possessed > 0) {
            message("You have used a safety charm, so your EXP points have not been decreased.");
            MapleInventoryManipulator.removeById(client, MapleItemInformationProvider.getInstance().getInventoryType(charmID[i]), charmID[i], 1, true, false);
        } else if (getJob() != 0) {
            int XPdummy = ExpTable.getExpNeededForLevel(getLevel());
            if (getMap().isTown()) {
                XPdummy /= 100;
            } else {
                if (getLuk() <= 100 && getLuk() > 8) {
                    XPdummy *= (5 + 20 / getLuk()) / 100;
                } else if (getLuk() <= 8) {
                    XPdummy /= 10;
                } else {
                    XPdummy /= 20;
                }
            }
            gainExp(getExp() > XPdummy ? -XPdummy : -getExp(), false, false);
        }
        if (getBuffedValue(MapleBuffStat.MORPH) != null) {
            cancelEffectFromBuffStat(MapleBuffStat.MORPH);
        }
        if (getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
            cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
        }
        if (getChair() == -1) {
            setChair(0);
            client.announce(InventoryFactory.cancelChair(-1));
            getMap().broadcastMessage(this, InventoryFactory.showChair(getId(), 0), false);
        }
        client.announce(IntraPersonalFactory.enableActions());
    }

    public int getMorphState() {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMorph()) {
                return mbsvh.effect.getSourceId();
            }
        }
        return -1;
    }

    private void prepareDragonBlood(final MapleStatEffect bloodEffect) {
        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(false);
        }
        dragonBloodSchedule = TimerManager.getInstance().register(new Runnable() {
            @Override
            public void run() {
                addHP(-bloodEffect.getX());
                client.announce(BuffFactory.showOwnBuffEffect(bloodEffect.getSourceId(), 5));
                getMap().broadcastMessage(MapleCharacter.this, BuffFactory.showBuffEffect(MapleCharacter.this, getId(), bloodEffect.getSourceId(), 5), false);
                checkBerserk();
            }
        }, 4000, 4000);
    }

    private static int rand(int l, int u) {
        return Randomizer.getInstance().nextInt(u - l + 1) + l;
    }

    private void recalcLocalStats() {
        int oldmaxhp = localmaxhp;
        localmaxhp = getMaxHp();
        localmaxmp = getMaxMp();
        localdex = getDex();
        localint_ = getInt();
        localstr = getStr();
        localluk = getLuk();
        localacc = 0;

        magic = localint_;
        watk = 0;
        for (IItem item : getInventory(MapleInventoryType.EQUIPPED)) {
            IEquip equip = (IEquip) item;
            localmaxhp += equip.getHp();
            localmaxmp += equip.getMp();
            localdex += equip.getDex();
            localint_ += equip.getInt();
            localstr += equip.getStr();
            localluk += equip.getLuk();
            localacc += equip.getAcc();
            magic += equip.getMatk() + equip.getInt();
            watk += equip.getWatk();
        }
        localacc = (int) ((localdex * 0.8) + (localluk * 0.5));

        magic = Math.min(magic, 2000);
        Integer hbhp = getBuffedValue(MapleBuffStat.HYPERBODYHP);
        if (hbhp != null) {
            localmaxhp += (hbhp.doubleValue() / 100) * localmaxhp;
        }
        Integer hbmp = getBuffedValue(MapleBuffStat.HYPERBODYMP);
        if (hbmp != null) {
            localmaxmp += (hbmp.doubleValue() / 100) * localmaxmp;
        }
        localmaxhp = Math.min(30000, localmaxhp);
        localmaxmp = Math.min(30000, localmaxmp);
        Integer watkbuff = getBuffedValue(MapleBuffStat.WATK);
        if (watkbuff != null) {
            watk += watkbuff.intValue();
        }
        Integer accbuff = getBuffedValue(MapleBuffStat.ACC);
        if (accbuff != null) {
            localacc += accbuff.intValue();
        }

        if (job == Marksman.ID || job == Bowmaster.ID) {
            ISkill expert = null;
            if (job == Marksman.ID) {
                expert = SkillFactory.getSkill(Marksman.MARKSMAN_BOOST);
            } else if (job == Bowmaster.ID) {
                expert = SkillFactory.getSkill(Bowmaster.BOW_EXPERT);
            }
            if (expert != null) {
                int boostLevel = getSkillLevel(expert);
                if (boostLevel > 0) {
                    watk += expert.getEffect(boostLevel).getX();
                }
            }
        }
        Integer matkbuff = getBuffedValue(MapleBuffStat.MATK);
        if (matkbuff != null) {
            magic += matkbuff.intValue();
        }
        if (oldmaxhp != 0 && oldmaxhp != localmaxhp) {
            updatePartyMemberHP();
        }
    }

    public void receivePartyMemberHP() {
        if (party != null) {
            int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    MapleCharacter other = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        client.announce(PartyFactory.updatePartyMemberHP(other.getId(), other.getHp(), other.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule) {
        if (effect.isHide() && gmLevel > 0) {
            this.hidden = true;
            getMap().broadcastNONGMMessage(this, InterPersonalFactory.removePlayerFromMap(id), false);
        } else if (effect.isDragonBlood()) {
            prepareDragonBlood(effect);
        } else if (effect.isBerserk()) {
            checkBerserk();
        } else if (effect.isBeholder()) {
            final int beholder = DarkKnight.BEHOLDER;
            if (beholderHealingSchedule != null) {
                beholderHealingSchedule.cancel(false);
            }
            if (beholderBuffSchedule != null) {
                beholderBuffSchedule.cancel(false);
            }
            final ISkill bHealing = SkillFactory.getSkill(DarkKnight.AURA_OF_BEHOLDER);
            int bHealingLvl = getSkillLevel(bHealing);
            if (bHealingLvl > 0) {
                final MapleStatEffect healEffect = bHealing.getEffect(bHealingLvl);
                int healInterval = healEffect.getX() * 1000;
                beholderHealingSchedule = TimerManager.getInstance().register(new Runnable() {

                    @Override
                    public void run() {
                        addHP(healEffect.getHp());
                        client.announce(BuffFactory.showOwnBuffEffect(beholder, 2));
                        getMap().broadcastMessage(MapleCharacter.this, SummonFactory.summonSkill(getId(), beholder, 5), true);
                        getMap().broadcastMessage(MapleCharacter.this, BuffFactory.showOwnBuffEffect(beholder, 2), false);
                    }
                }, healInterval, healInterval);
            }
            ISkill bBuff = SkillFactory.getSkill(DarkKnight.HEX_OF_BEHOLDER);
            if (getSkillLevel(bBuff) > 0) {
                final MapleStatEffect buffEffect = bBuff.getEffect(getSkillLevel(bBuff));
                int buffInterval = buffEffect.getX() * 1000;
                beholderBuffSchedule = TimerManager.getInstance().register(new Runnable() {

                    @Override
                    public void run() {
                        buffEffect.applyTo(MapleCharacter.this);
                        client.announce(BuffFactory.showOwnBuffEffect(beholder, 2));
                        getMap().broadcastMessage(MapleCharacter.this, SummonFactory.summonSkill(getId(), beholder, (int) (Math.random() * 3) + 6), true);
                        getMap().broadcastMessage(MapleCharacter.this, BuffFactory.showBuffEffect(getId(), beholder, 2), false);
                    }
                }, buffInterval, buffInterval);
            }
        }
        for (Pair<MapleBuffStat, Integer> statup : effect.getStatups()) {
            effects.put(statup.getLeft(), new MapleBuffStatValueHolder(effect, starttime, schedule, statup.getRight().intValue()));
        }
        recalcLocalStats();
    }

    public void removeAllCooldownsExcept(int id) {
        for (MapleCoolDownValueHolder mcvh : coolDowns.values()) {
            if (mcvh.skillId != id) {
                coolDowns.remove(mcvh.skillId);
                this.client.announce(BuffFactory.skillCooldown(mcvh.skillId, 0));
            }
        }
    }

    public void removeBuffStat(MapleBuffStat effect) {
        effects.remove(effect);
    }

    public void removeCooldown(int skillId) {
        if (this.coolDowns.containsKey(skillId)) {
            this.coolDowns.remove(skillId);
        }
    }

    public void removePartyQuestItem(String letter) {
        if(gotPartyQuestItem(letter))
        partyquestitems = partyquestitems.substring(0, partyquestitems.indexOf(letter)) + partyquestitems.substring(partyquestitems.indexOf(letter) + 1);
    }

    public void removeAllPartyQuestItems() {
        partyquestitems = "";
    }

    public String getPartyQuestItems() {
        return partyquestitems;
    }

    public void removePet(MaplePet pet, boolean shift_left) {
        int slot = -1;
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == pet.getUniqueId()) {
                    pets[i] = null;
                    slot = i;
                    break;
                }
            }
        }
        if (shift_left) {
            if (slot > -1) {
                for (int i = slot; i < 3; i++) {
                    if (i != 2) {
                        pets[i] = pets[i + 1];
                    } else {
                        pets[i] = null;
                    }
                }
            }
        }
    }

    public void removeVisibleMapObject(MapleMapObject mo) {
        visibleMapObjects.remove(mo);
    }

    public void resetBattleshipHp() {
        this.battleshipHp = 4000 * getSkillLevel(SkillFactory.getSkill(Corsair.BATTLE_SHIP)) + ((getLevel() - 120) * 2000);
    }

    public void resetEnteredScript() {
        if (entered.containsKey(map.getId())) {
            entered.remove(map.getId());
        }
    }

    public void resetEnteredScript(int mapId) {
        if (entered.containsKey(mapId)) {
            entered.remove(mapId);
        }
    }

    public void resetEnteredScript(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                entered.remove(mapId);
            }
        }
    }

    public void resetMGC() {
        this.mgc = null;
    }

    public void saveCooldowns() {
        if (getAllCooldowns().size() > 0) {
            try {
                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO cooldowns (characterid, skillid, starttime, length) VALUES (?, ?, ?, ?)");
                ps.setInt(1, getId());
                for (final PlayerCoolDownValueHolder cooling : getAllCooldowns()) {
                    ps.setInt(2, cooling.skillId);
                    ps.setLong(3, cooling.startTime);
                    ps.setLong(4, cooling.length);
                    ps.addBatch();
                }
                ps.executeBatch();
                ps.close();
            } catch (SQLException se) {
            }
        }
    }

    public void saveGuildStatus() {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET guildid = ?, guildrank = ?, alliancerank = ? WHERE id = ?");
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, alliancerank);
            ps.setInt(4, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
        }
    }

    public void saveLocation(String type) {
        MaplePortal closest = map.findClosestPortal(getPosition());
        savedLocations[SavedLocationType.fromString(type).ordinal()] = new SavedLocation(getMapId(), closest != null ? closest.getId() : 0);
        saveLocations = true;
    }

    public void saveToDB(boolean update) {
        if((update) && this.trade != null)
        return;
        Connection con = DatabaseConnection.getConnection();
        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);
            PreparedStatement ps;
            if (update) {
                ps = con.prepareStatement("UPDATE characters SET level = ?, donorpts = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, meso = ?, hpapused = ?, mpapused = ?, spawnpoint = ?, party = ?, reborns = ?, rebornpts = ?, family = ?, equipslots = ?, useslots = ?, setupslots = ?, etcslots = ?, buddyslots = ?, partyquestitems = ?, mountexp = ?, mountlevel = ?, mounttiredness = ?, fame = ?, monsterbookcover = ?, coupleid = ?, omokwins = ?, omoklosses = ?, omokties = ?, dojopts = ?, carnivalpts = ?, name = ? WHERE id = ?");
            } else {
                ps = con.prepareStatement("INSERT INTO characters (level, donorpts, str, dex, luk, `int`, exp, hp, mp, maxhp, maxmp, sp, ap, gm, skincolor, gender, job, hair, face, map, meso, hpapused, mpapused, spawnpoint, party, reborns, rebornpts, family, equipslots, useslots, setupslots, etcslots, buddyslots, partyquestitems, mountexp, mountlevel, mounttiredness, fame, monsterbookcover, coupleid, omokwins, omoklosses, omokties, dojopts, carnivalpts, accountid, name, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            }
            if (gmLevel < 1 && level > 199) {
                ps.setInt(1, 200);
            } else {
                ps.setInt(1, level);
            }
            ps.setInt(2, donorpts);
            ps.setInt(3, str);
            ps.setInt(4, dex);
            ps.setInt(5, luk);
            ps.setInt(6, int_);
            ps.setInt(7, exp.get());
            ps.setInt(8, hp);
            ps.setInt(9, mp);
            ps.setInt(10, maxhp);
            ps.setInt(11, maxmp);
            ps.setInt(12, remainingSp);
            ps.setInt(13, remainingAp);
            ps.setInt(14, gmLevel);
            ps.setInt(15, skinColor);
            ps.setInt(16, gender);
            ps.setInt(17, job);
            ps.setInt(18, hair);
            ps.setInt(19, face);
            if ((map == null) && (mapid == -1)) {
                ps.setInt(20, 0);
            } else if (map == null) {
                ps.setInt(20, mapid);
            } else if (hp == 0) {
                ps.setInt(20, map.getReturnMapId());
            } else if (map.getForcedReturnId() != 999999999) {
                ps.setInt(20, map.getForcedReturnId());
            } else {
                ps.setInt(20, map.getId());
            }
            ps.setInt(21, meso.get());
            ps.setInt(22, hpApUsed);
            ps.setInt(23, mpApUsed);
            if (map == null || map.getId() == 610020000 || map.getId() == 610020001) {
                ps.setInt(24, 0);
            } else {
                final MaplePortal closest = map.findClosestSpawnpoint(getPosition());
                if (closest != null) {
                    ps.setInt(24, closest.getId());
                } else {
                    ps.setInt(24, 0);
                }
            }
            ps.setInt(25, party != null ? party.getId() : -1);
            ps.setInt(26, reborns);
            ps.setInt(27, rebornPts);
            ps.setInt(28, familyId);
            for (int i = 29; i < 33; i++) {
                ps.setInt(i, getInventory(MapleInventoryType.getByType((byte) (i - 27))).getSlotLimit());
            }
            ps.setInt(33, buddylist.getCapacity());
            ps.setString(34, partyquestitems);
            if (mount != null) {
                ps.setInt(35, mount.getExp());
                ps.setInt(36, mount.getLevel());
                ps.setInt(37, mount.getTiredness());
            } else {
                ps.setInt(35, 0);
                ps.setInt(36, 1);
                ps.setInt(37, 0);
            }
            ps.setInt(38, fame);
            if (saveBook) {
                monsterBook.saveCards(getId());
            }
            ps.setInt(39, bookCover);
            ps.setInt(40, coupleId);
            ps.setInt(41, omokWins);
            ps.setInt(42, omokLosses);
            ps.setInt(43, omokTies);
            ps.setInt(44, dojopts);
            ps.setInt(45, carnivalpts);
            if (update) {
                ps.setString(46, name);
                ps.setInt(47, id);
            } else {
                ps.setInt(46, accountid);
                ps.setString(47, name);
                ps.setInt(48, world);
            }
            int updateRows = ps.executeUpdate();
            if (!update) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getInt(1);
                } else {
                    throw new RuntimeException("Inserting char failed.");
                }
                rs.close();
            } else if (updateRows < 1) {
                throw new RuntimeException("Character not in database (" + id + ")");
            }
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    pets[i].saveToDb();
                }
            }
            ps.close();
            if ((saveKeyMap && !keymap.isEmpty()) || !update) {
                deleteWhereCharacterId(con, "DELETE FROM keymap WHERE characterid = ?");
                ps = con.prepareStatement(prepareKeymapQuery());
                ps.executeUpdate();
                ps.close();
            }
            if (saveMacros) {
                deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, getId());
                for (int i = 0; i < 5; i++) {
                    final SkillMacro macro = skillMacros[i];
                    if (macro != null) {
                        ps.setInt(2, macro.getSkill1());
                        ps.setInt(3, macro.getSkill2());
                        ps.setInt(4, macro.getSkill3());
                        ps.setString(5, macro.getName());
                        ps.setInt(6, macro.getShout());
                        ps.setInt(7, i);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
                ps.close();
            }
            if (saveTeleportMaps) {
                deleteWhereCharacterId(con, "DELETE FROM telerockmaps WHERE characterId = ?");
                ps = con.prepareStatement("INSERT into telerockmaps (characterid, mapid, type) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (int mapId : rockMaps) {
                    ps.setInt(2, mapId);
                    ps.setInt(3, 0);
                    ps.addBatch();
                }
                for (int mapId : vipRockMaps) {
                    ps.setInt(2, mapId);
                    ps.setInt(3, 1);
                    ps.addBatch();
                }
                ps.executeBatch();
                ps.close();
            }
            //add save check? NAH
            List<Pair<IItem, MapleInventoryType>> itemsWithType = new ArrayList<Pair<IItem, MapleInventoryType>>();
            for (MapleInventory iv : inventory) {
                for (IItem item : iv.list())
                itemsWithType.add(new Pair<IItem, MapleInventoryType>(item, iv.getType()));
            }
            ItemFactory.INVENTORY.saveItems(itemsWithType, id);
            if (saveSkills) {
                deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel) VALUES (?, ?, ?, ?)");
                ps.setInt(1, id);
                for (Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
                    if (skill != null && skill.getKey() != null && skill.getValue() != null) {
                        if (skill.getKey().getId() % 10000000 == 1009) {
                            continue;
                        }
                        ps.setInt(2, skill.getKey().getId());
                        ps.setInt(3, skill.getValue().skillevel);
                        ps.setInt(4, skill.getValue().masterlevel);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
                ps.close();
            }
            if (saveLocations) {
                deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`, `portal`) VALUES (?, ?, ?, ?)");
                ps.setInt(1, id);
                for (final SavedLocationType savedLocationType : SavedLocationType.values()) {
                    if (savedLocations[savedLocationType.ordinal()] != null) {
                        ps.setString(2, savedLocationType.name());
                        ps.setInt(3, savedLocations[savedLocationType.ordinal()].getMapId());
                        ps.setInt(4, savedLocations[savedLocationType.ordinal()].getPortal());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
                ps.close();
            }
            if (saveBuddies) {
                deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ? AND pending = 0");
                ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`, `group`) VALUES (?, ?, 0, ?)");
                ps.setInt(1, id);
                for (BuddylistEntry entry : buddylist.getBuddies()) {
                    if (entry.isVisible()) {
                        ps.setInt(2, entry.getCharacterId());
                        ps.setString(3, entry.getGroup());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
                ps.close();
            }
            if (saveQuests) {
                deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)");
                PreparedStatement pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
                ps.setInt(1, id);
                for (final MapleQuestStatus q : quests.values()) {
                    ps.setInt(2, q.getQuest().getId());
                    ps.setInt(3, q.getStatus());
                    ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                    ps.setInt(5, q.getForfeited());
                    ps.setString(6, q.getQuestRecord() == null ? " " : q.getQuestRecord());
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    if (q.hasMobKills()) {
                        for (int mob : q.getMobKills().keySet()) {
                            pse.setInt(1, rs.getInt(1));
                            pse.setInt(2, mob);
                            pse.setInt(3, q.getMobKills(mob));
                            pse.executeUpdate();
                        }
                    }
                    rs.close();
                }
                pse.close();
                ps.close();
                pse = null;
            }
            if (saveQuestInfo) {
                deleteWhereCharacterId(con, "DELETE FROM questinfo WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO questinfo (`characterid`, `questid`, `info`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (Pair<Integer, String> info : questinfo) {
                    ps.setInt(2, info.getLeft());
                    ps.setString(3, info.getRight());
                    ps.addBatch();
                }
                ps.executeBatch();
                ps.close();
            }
            if (saveAccount) {
                ps = con.prepareStatement("UPDATE accounts SET `nx` = ?, `gm` = ? WHERE id = ?");
                ps.setInt(1, nx);
                ps.setInt(2, gmLevel);
                ps.setInt(3, client.getAccID());
                ps.executeUpdate();
                ps.close();
            }
            if (saveStorage && storage != null) {
                storage.saveToDB(world);
            }
            if (saveWishList) {
                ps = con.prepareStatement("DELETE FROM wishlist WHERE `characterid` = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("INSERT INTO wishlist(`sn`, `characterid`) VALUES(?, ?)");
                for (int sn : wishList) {
                    ps.setInt(1, sn);
                    ps.setInt(2, id);
                    ps.addBatch();
                }
                ps.executeBatch();
                ps.close();
            }
            if (saveDojoCount) {
                ps = con.prepareStatement("DELETE FROM dojocounts WHERE `characterid` = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("INSERT INTO dojocounts(`characterid`, `boss`, `count`) VALUES(?, ?, ?)");
                for (int i = 0; i < 32; i++) {
                    if (dojoBossCount[i] > 0) {
                        ps.setInt(1, id);
                        ps.setInt(2, i);
                        ps.setInt(3, dojoBossCount[i]);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
                ps.close();
            }
            if (gmLevel > 0) {
                ps = con.prepareStatement("INSERT INTO gmlog (`characterid`, `command`) VALUES (?, ?)");
                ps.setInt(1, id);
                for (String com : commands) {
                    ps.setString(2, com);
                    ps.addBatch();
                }
                ps.executeBatch();
                ps.close();
            }
            con.commit();
            ps = null;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                con.rollback();
            } catch (SQLException se) {
            }
        } finally {
            try {
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (Exception e) {
            }
        }
    }

    public void saveItems() {
        List<Pair<IItem, MapleInventoryType>> itemsWithType = new ArrayList<Pair<IItem, MapleInventoryType>>();
        for (final MapleInventory iv : inventory) {
            for (IItem item : iv.list())
            itemsWithType.add(new Pair<IItem, MapleInventoryType>(item, iv.getType()));
        }
        try {
            ItemFactory.INVENTORY.saveItems(itemsWithType, id);
        } catch (SQLException se) {
        }
    }

    public void removeItem(byte slot) throws SQLException {
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("DELETE from inventoryitems WHERE characterid = ? AND type = 1 AND position = ?");
        ps.setInt(1, this.getId());
        ps.setInt(2, slot);
        ps.executeUpdate();
        ps.close();
    }

    public void sendKeymap() {
        client.announce(IntraPersonalFactory.getKeymap(keymap));
    }

    public void sendMacros() {
        boolean macros = false;
        for (int i = 0; i < 5; i++) {
            if (skillMacros[i] != null) {
                macros = true;
            }
        }
        if (macros) {
            client.announce(BuffFactory.getMacros(skillMacros));
        }
    }

    public final SkillMacro[] getMacros() {
        return skillMacros;
    }

    public void sendNote(String to, String msg) throws SQLException {
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)");
        ps.setString(1, to);
        ps.setString(2, this.getName());
        ps.setString(3, msg);
        ps.setLong(4, System.currentTimeMillis());
        ps.executeUpdate();
        ps.close();
    }

    public int getAllianceRank() {
        return alliancerank;
    }

    public void setAllianceRank(int rank) {
        alliancerank = rank;
        if (mgc != null) {
            mgc.setAllianceRank(rank);
        }
    }

    public void setBattleshipHp(int battleshipHp) {
        this.battleshipHp = battleshipHp;
    }

    public void setBeacon(int oid) {
        beaconOid = oid;
    }

    public void setProjectile(int projectile) {
        this.projectile = projectile;
    }

    public void setBuddyCapacity(int capacity) {
        buddylist.setCapacity(capacity);
        client.announce(BuddyFactory.updateBuddyCapacity(capacity));
    }

    public void setBuffedValue(MapleBuffStat effect, int value) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.value = value;
    }

    public void setChair(int chair) {
        this.chair = chair;
    }

    public void setChalkboard(String text) {
        this.chalktext = text;
    }

    public void setDex(int dex) {
        this.dex = dex;
        recalcLocalStats();
    }

    public void setQuickSlot(int[] s) {
        this.quickslots = s;
    }

    public void setRates(boolean dispel) {
        if (this.expRate > 0) {
            this.expRate = ServerConstants.EXP_RATE;
            if (gmLevel == 1 || gmLevel == 2) {
                this.expRate = 0;
            } else if (hasDisease(MapleDisease.CURSE)) {
                if (dispel) {
                    this.expRate *= 2;
                } else {
                    this.expRate *= 0.5;
                }
            }
        }
    }

    public void setEnergyBar(int set) {
        energybar = set;
    }

    public void setDojoEnergy(int set) {
        dojoenergy = set;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void setExp(int amount) {
        this.exp.set(amount);
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setFallCounter(int fallcounter) {
        this.fallcounter = fallcounter;
    }

    public void setBanCount(int bancount) {
        this.banCount = bancount;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setFamilyId(int familyId) {
        this.familyId = familyId;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public void setDonorPts(int amt) {
        this.donorpts = amt;
    }

    public void setDojoPts(int amt) {
        this.dojopts = amt;
    }

    public void setCarnivalPts(int amt) {
        this.carnivalpts = amt;
    }

    public void setSaveBook() {
        this.saveBook = true;
    }

    public void setSaveKeyMap() {
        this.saveKeyMap = true;
    }

    public void setSaveMacros() {
        this.saveMacros = true;
    }

    public void setSaveTeleportMaps() {
        this.saveTeleportMaps = true;
    }

    public void setSaveSkills() {
        this.saveSkills = true;
    }

    public void setSaveLocations() {
        this.saveLocations = true;
    }

    public void setSaveBuddies() {
        this.saveBuddies = true;
    }

    public void setSaveQuests() {
        this.saveQuests = true;
    }

    public void setSaveAccount() {
        this.saveAccount = true;
    }

    public void setSaveStorage() {
        this.saveStorage = true;
    }

    public void setSaveWishList() {
        this.saveWishList = true;
    }

    public void setSaveDojoCount() {
        this.saveDojoCount = true;
    }

    public void setCoupleId(int coupleId) {
        this.coupleId = coupleId;
    }

    public void setGM(int level) {
        this.gmLevel = level;
        this.saveAccount = true;
    }

    public void setGuildId(int _id) {
        guildid = _id;
        if (guildid > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guildid);
            }
        } else {
            mgc = null;
        }
    }

    public void setGuildRank(int _rank) {
        guildrank = _rank;
        if (mgc != null) {
            mgc.setGuildRank(_rank);
        }
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public void setHasMerchant(boolean set) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET hasmerchant = ? WHERE id = ?");
            ps.setInt(1, set ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            return;
        }
        hasMerchant = set;
    }

    public void setHiredMerchant(HiredMerchant merchant) {
        this.hiredMerchant = merchant;
    }

    public void setHp(int newhp) {
        setHp(newhp, false);
    }

    public void setHp(int newhp, boolean silent) {
        int oldHp = hp;
        int thp = newhp;
        if (thp < 0) {
            thp = 0;
        }
        if (thp > localmaxhp) {
            thp = localmaxhp;
        }
        this.hp = thp;
        if (!silent) {
            updatePartyMemberHP();
        }
        if (oldHp > hp && !isAlive()) {
            playerDead();
        }
    }

    public void setHpApUsed(int hpApUsed) {
        this.hpApUsed = hpApUsed;
    }

    public void setMpApUsed(int mpApUsed) {
        this.mpApUsed = mpApUsed;
    }

    public void setHpMp(int x) {
        setHp(x);
        setMp(x);
        updateSingleStat(MapleStat.HP, hp);
        updateSingleStat(MapleStat.MP, mp);
    }

    public void setInt(int int_) {
        this.int_ = int_;
        recalcLocalStats();
    }

    public void setInventory(MapleInventoryType type, MapleInventory inv) {
        inventory[type.ordinal()] = inv;
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    public void setJob(int job) {
        this.job = job;
    }

    public void setLastHealed(long time) {
        this.lastHealed = time;
    }

    public void setLastAnnounced(long time) {
        this.lastAnnounced = time;
    }

    public void setMuted(long time) {
        this.muted = time;
    }

    public void setLastTalked(long time) {
        this.lastTalked = time;
    }

    public void setWordsTalked(String words) {
        this.wordsTalked = words;
    }

    public void setSpamCheck(int check) {
        this.spamCheck = check;
    }

    public void setLevel(final int level) {
        this.level = level;
    }

    public void setLuk(int luk) {
        this.luk = luk;
        recalcLocalStats();
    }

    public void setMap(int PmapId) {
        this.mapid = PmapId;
    }

    public void setMap(MapleMap newmap) {
        this.map = newmap;
    }

    public void setMaxHp(int hp) {
        this.maxhp = hp;
        recalcLocalStats();
    }

    public void setMaxMp(int mp) {
        this.maxmp = mp;
        recalcLocalStats();
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    public void setMiniGame(MapleMiniGame miniGame) {
        this.miniGame = miniGame;
    }

    public void setMiniGamePoints(MapleCharacter visitor, int winnerslot, boolean omok) {
        if (visitor != null) {
            if (hasPlayed(this, visitor)) {
                this.dropMessage("Both players have played each other before. Thus, the game score does not count.");
                visitor.dropMessage("Both players have played each other before. Thus, the game score does not count.");
                return;
            }
            if (omok) {
                if (winnerslot == 1) {
                    this.omokWins++;
                    visitor.omokLosses++;
                } else if (winnerslot == 2) {
                    visitor.omokWins++;
                    this.omokLosses++;
                } else {
                    this.omokTies++;
                    visitor.omokTies++;
                }
            }
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO omoklog (`id`, ownerid, visitorid) VALUES (DEFAULT, ?, ?)");
                ps.setInt(1, this.getId());
                ps.setInt(2, visitor.getId());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException ex) {
            }
        }
    }

    public boolean hasPlayed(MapleCharacter owner, MapleCharacter visitor) {
        if(visitor != null && owner != null) {
            try {
                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT visitorid FROM omoklog WHERE ownerid = ? AND visitorid = ?");
                ps.setInt(1, owner.getId());
                ps.setInt(2, visitor.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return true;
                }
                rs.close();
                ps.close();
                ps = DatabaseConnection.getConnection().prepareStatement("SELECT visitorid FROM omoklog WHERE ownerid = ? AND visitorid = ?");
                ps.setInt(1, visitor.getId());
                ps.setInt(2, owner.getId());
                rs = ps.executeQuery();
                if (rs.next()) {
                    return true;
                }
                rs.close();
                ps.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public void setMonsterBookCover(int bookCover) {
        this.bookCover = bookCover;
    }

    public void setMp(int newmp) {
        int tmp = newmp;
        if (tmp < 0) {
            tmp = 0;
        }
        if (tmp > localmaxmp) {
            tmp = localmaxmp;
        }
        this.mp = tmp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParty(MapleParty party) {
        this.party = party;
    }

    public void setPartyQuestItemObtained(String partyquestchar) {
        this.partyquestitems += partyquestchar;
    }

    public void setRemainingAp(int remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp = remainingSp;
    }

    public void setSearch(String find) {
        search = find;
    }

    public void setSkinColor(int skinColor) {
        this.skinColor = skinColor;
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public void setStr(int str) {
        this.str = str;
        recalcLocalStats();
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void shiftPetsRight() {
        if (pets[2] == null) {
            pets[2] = pets[1];
            pets[1] = pets[0];
            pets[0] = null;
        }
    }

    public void showNote() {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM notes WHERE `to` = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            rs.last();
            int count = rs.getRow();
            rs.first();
            client.announce(EffectFactory.showNotes(rs, count));
            rs.close();
            ps.close();
        } catch (SQLException e) {
        }
    }

    private void silentEnforceMaxHpMp() {
        setMp(getMp());
        setHp(getHp(), true);
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        if (buffs != null) {
            for (PlayerBuffValueHolder mbsvh : buffs) {
                mbsvh.effect.silentApplyBuff(this, mbsvh.startTime);
            }
        }
    }

    public void silentPartyUpdate() {
        if (party != null) {
            try {
                client.getChannelServer().getWorldInterface().updateParty(party.getId(), PartyOperation.SILENT_UPDATE, new MaplePartyCharacter(this));
            } catch (RemoteException e) {
                e.printStackTrace();
                client.getChannelServer().reconnectWorld();
            }
        }
    }

    public boolean skillisCooling(int skillId) {
        return coolDowns.containsKey(Integer.valueOf(skillId));
    }

    public void stopControllingMonster(MapleMonster monster) {
        controlled.remove(monster);
    }

    public void toggleGMChat() {
        whitechat = !whitechat;
    }

    public void unequipAllPets() {
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                unequipPet(pets[i], true);
            }
        }
    }

    public void unequipPet(MaplePet pet, boolean shift_left) {
        unequipPet(pet, shift_left, false);
    }

    public void unequipPet(MaplePet pet, boolean shift_left, boolean hunger) {
        if (this.getPet(this.getPetIndex(pet)) != null) {
            this.getPet(this.getPetIndex(pet)).saveToDb();
        }
        getMap().broadcastMessage(this, PetFactory.showPet(this, pet, true, hunger), true);
        client.announce(PetFactory.petStatUpdate(this));
        client.announce(IntraPersonalFactory.enableActions());
        removePet(pet, shift_left);
    }

    public void addFame(int famechange) {
        this.fame += famechange;
    }

    public final long getLastFameTime() {
        return lastFameTime;
    }

    public final List<Integer> getFamedCharacters() {
        return lastMonthFameIds;
    }

    public FameStatus canGiveFame(MapleCharacter from) {
        if (lastFameTime >= System.currentTimeMillis() - 60 * 60 * 24 * 1000) {
            return FameStatus.NOT_TODAY;
        } else if (lastMonthFameIds.contains(Integer.valueOf(from.getId()))) {
            return FameStatus.NOT_THIS_MONTH;
        }
        return FameStatus.OK;
    }

    public void hasGivenFame(MapleCharacter to) {
        lastFameTime = System.currentTimeMillis();
        lastMonthFameIds.add(Integer.valueOf(to.getId()));
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)");
            ps.setInt(1, getId());
            ps.setInt(2, to.getId());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("ERROR writing famelog for char " + getName() + " to " + to.getName() + e);
        }
    }

    public void updateMacros(int position, final SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
    }

    public void updatePartyMemberHP() {
        if (party != null) {
            final int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    final MapleCharacter other = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.client.announce(PartyFactory.updatePartyMemberHP(getId(), this.hp, localmaxhp));
                    }
                }
            }
        }
    }

    public void updateQuest(MapleQuestStatus quest, boolean updateQuestRecord) {
        saveQuests = true;
        if (quest.getQuest() != null) {
            quest.setQuestRecord(quest.getQuest().getRecord());
            quest.getQuest().setRecord(null);
        }
        quests.put(quest.getQuest(), quest);
        if (quest.getStatus() == 1) {
            client.announce(QuestFactory.startQuest((short) quest.getQuest().getId()));
            client.announce(QuestFactory.updateQuestInfo((short) quest.getQuest().getId(), false, quest.getNpc(), (byte) 8, (short) 0, true));
            if (updateQuestRecord) {
                client.announce(QuestFactory.updateQuestInfo((byte) 1, (short) quest.getQuest().getId(), quest.getQuestRecord()));
            }
        } else if (quest.getStatus() == 2) {
            client.announce(QuestFactory.completeQuest((short) quest.getQuest().getId()));
            client.announce(QuestFactory.updateQuestInfo((short) quest.getQuest().getId(), false, quest.getNpc(), (byte) 8, (short) 0, false));
            client.announce(EffectFactory.showSpecialEffect((byte) 9));
            getMap().broadcastMessage(this, EffectFactory.showForeignEffect(getId(), 9), false);
        } else if (quest.getStatus() == 0) {
            client.announce(QuestFactory.forfeitQuest((short) quest.getQuest().getId()));
        }
    }

    public void updateSingleStat(MapleStat stat, int newval) {
        updateSingleStat(stat, newval, false);
    }

    private void updateSingleStat(MapleStat stat, int newval, boolean itemReaction) {
        client.announce(IntraPersonalFactory.updatePlayerStats(Collections.singletonList(new Pair<MapleStat, Integer>(stat, Integer.valueOf(newval))), itemReaction));
    }

    @Override
    public int getObjectId() {
        return getId();
    }

    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    public void sendDestroyData(MapleClient client) {
        client.announce(InterPersonalFactory.removePlayerFromMap(this.getObjectId()));
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if ((this.isHidden() && client.getPlayer().isGM()) || !this.isHidden()) {
            client.announce(InterPersonalFactory.spawnPlayerMapobject(this));
            for (int i = 0; pets[i] != null; i++) {
                client.announce(PetFactory.showPet(this, pets[i], false, false));
            }
        }
    }

    @Override
    public void setObjectId(int id) {
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean allowedMapChange() {
        return this.allowMapChange;
    }

    public void setallowedMapChange(boolean allowed) {
        this.allowMapChange = allowed;
    }

    public final int getCombo() {
        return aranCombo;
    }
    public int setCombo(final int _new) {
        if (aranCombo % 10 == 0)
        client.announce(BuffFactory.addComboBuff(_new));
        return aranCombo = _new;
    }

    public void setStuck(boolean isStuck) {
        this.stuck = isStuck;
    }

    public boolean isStuck() {
        return stuck;
    }

    public void empty() {
        this.cancelMapTimeLimitTask();
        this.cancelAllBuffs();
        if(dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(false);
        }
        if(hpDecreaseTask != null) {
            hpDecreaseTask.cancel(false);
        }
        if(poisonTask != null) {
            poisonTask.cancel(false);
        }
        if(beholderHealingSchedule != null) {
            beholderHealingSchedule.cancel(false);
        }
        if(beholderBuffSchedule != null) {
            beholderBuffSchedule.cancel(false);
        }
        if(berserkSchedule != null) {
            berserkSchedule.cancel(false);
        }
        this.mount = null;
        if (this.mgc != null) {
            this.mgc.setOnline(false);
            this.mgc = null;
        }
        this.client = null;
    }

    public void setMegaLimit(long limit) {
        this.megaLimit = limit;
    }

    public long getMegaLimit() {
        return this.megaLimit;
    }

    public int updateMesosGetOverflow(int gain) {
        int origMesos = meso.get();
        int overflow = 0;
        if (((long) (origMesos) + gain) >= 2147483647L) {
            overflow = ((origMesos + gain) - 2147483647);
            updateSingleStat(MapleStat.MESO, meso.addAndGet(2147483647), true);
        } else {
            updateSingleStat(MapleStat.MESO, meso.addAndGet(gain), true);
        }
        client.announce(EffectFactory.getShowMesoGain(gain, false));
        return overflow;
    }

    public void startMapTimeLimitTask(final MapleMap from, final MapleMap to) {
        if (to.getTimeLimit() > 0 && from != null) {
            mapTimeLimitTask = TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    MaplePortal pfrom = null;
                    switch (from.getId()) {
                        case 100020000: // pig
                        case 105040304: // golem
                        case 105050100: // mushroom
                        case 221023400: // rabbit
                        case 240020500: // kentasaurus
                        case 240040511: // skelegons
                        case 240040520: // newties
                        case 260020600: // sand rats
                        case 261020300: // magatia
                            pfrom = from.getPortal("MD00");
                            break;
                        default:
                            pfrom = from.getPortal(0);
                    }
                    if (pfrom != null) {
                        MapleCharacter.this.changeMap(from, pfrom);
                    }
                }
            }, from.getTimeLimit() * 1000);
        }
    }

    public final int getFoo() {
        return foo;
    }

    public final void setFoo(final int foo) {
        this.foo = foo;
    }
}



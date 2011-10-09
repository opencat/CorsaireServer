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

package server;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import client.IItem;
import client.ISkill;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleDisease;
import client.MapleInventory;
import client.MapleInventoryType;
import client.MapleMount;
import client.MapleStat;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.skills.Assassin;
import constants.skills.Aran;
import constants.skills.Bandit;
import constants.skills.Beginner;
import constants.skills.Bishop;
import constants.skills.BlazeWizard;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.Buccaneer;
import constants.skills.ChiefBandit;
import constants.skills.Cleric;
import constants.skills.Corsair;
import constants.skills.Crossbowman;
import constants.skills.Crusader;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.DragonKnight;
import constants.skills.FPArchMage;
import constants.skills.FPMage;
import constants.skills.FPWizard;
import net.channel.ChannelServer;
import provider.MapleData;
import provider.MapleDataTool;
import server.life.MapleMonster;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMist;
import server.maps.MapleSummon;
import server.maps.SummonMovementType;
import net.world.PlayerCoolDownValueHolder;
import tools.ArrayMap;
import tools.Pair;
import constants.skills.Fighter;
import constants.skills.GM;
import constants.skills.Gunslinger;
import constants.skills.Hermit;
import constants.skills.Hero;
import constants.skills.Hunter;
import constants.skills.ILArchMage;
import constants.skills.ILMage;
import constants.skills.ILWizard;
import constants.skills.Magician;
import constants.skills.Marauder;
import constants.skills.Marksman;
import constants.skills.NightLord;
import constants.skills.NightWalker;
import constants.skills.Noblesse;
import constants.skills.Outlaw;
import constants.skills.Page;
import constants.skills.Paladin;
import constants.skills.Pirate;
import constants.skills.Priest;
import constants.skills.Ranger;
import constants.skills.Rogue;
import constants.skills.Shadower;
import constants.skills.Sniper;
import constants.skills.Spearman;
import constants.skills.SuperGM;
import constants.skills.ThunderBreaker;
import constants.skills.WhiteKnight;
import constants.skills.WindArcher;
import net.MaplePacket;
import server.maps.FieldLimit;
import tools.factory.BuffFactory;
import tools.factory.IntraPersonalFactory;

/**
 * @name        MapleStatEffect
 * @author      Matze
 *              Frz
 *              Modified by x711Li
 */
public class MapleStatEffect implements Serializable {
    private static final long serialVersionUID = 3692756402846632237L;
    private short watk, matk, wdef, mdef, acc, avoid, speed, jump;
    private short hp, mp;
    private double hpR, mpR;
    private short mpCon, hpCon;
    private int duration;
    private boolean overTime;
    private int sourceid;
    private int moveTo;
    private boolean skill;
    private List<Pair<MapleBuffStat, Integer>> statups;
    private Map<MonsterStatus, Integer> monsterStatus;
    private int x, y;
    private int weapon;
    private double prop;
    private int itemCon, itemConNo;
    private int damage, attackCount, bulletCount, bulletConsume;
    private Point lt, rb;
    private int mobCount;
    private int moneyCon;
    private int cooldown;
    private int morphId = 0;
    private boolean isGhost;
    private int fatigue;

    public final static MapleStatEffect loadSkillEffectFromData(final MapleData source, final int skillid, boolean overtime, final int weapon) {
        return loadFromData(source, skillid, true, overtime, weapon);
    }

    public final static MapleStatEffect loadItemEffectFromData(final MapleData source, final int itemid) {
        return loadFromData(source, itemid, false, true, -1);
    }

    private final static void addBuffStatPairToListIfNotZero(final List<Pair<MapleBuffStat, Integer>> list, final MapleBuffStat buffstat, final Integer val) {
        if (val.intValue() != 0) {
            list.add(new Pair<MapleBuffStat, Integer>(buffstat, val));
        }
    }

    private static MapleStatEffect loadFromData(final MapleData source, final int sourceid, final boolean skill, final boolean overTime, final int weapon) {
        MapleStatEffect ret = new MapleStatEffect();
        ret.cooldown = MapleDataTool.getInt("cooltime", source, 0);
        ret.sourceid = sourceid;
        ret.weapon = weapon;
        ret.duration = MapleDataTool.getIntConvert("time", source, -1);
        ret.hp = (short) MapleDataTool.getInt("hp", source, 0);
        ret.hpR = MapleDataTool.getInt("hpR", source, 0) / 100.0;
        ret.mp = (short) MapleDataTool.getInt("mp", source, 0);
        ret.mpR = MapleDataTool.getInt("mpR", source, 0) / 100.0;
        ret.mpCon = (short) MapleDataTool.getInt("mpCon", source, 0);
        ret.hpCon = (short) MapleDataTool.getInt("hpCon", source, 0);
        int iprop = MapleDataTool.getInt("prop", source, 100);
        ret.prop = iprop / 100.0;
        ret.mobCount = MapleDataTool.getInt("mobCount", source, 1);
        ret.morphId = MapleDataTool.getInt("morph", source, 0);
        ret.isGhost = MapleDataTool.getInt("ghost", source, 0) != 0;
        ret.fatigue = MapleDataTool.getInt("incFatigue", source, 0);
        ret.skill = skill;
        if (!ret.skill && ret.duration > -1) {
            ret.overTime = true;
        } else {
            ret.duration *= 1000; // items have their times stored in ms, of course
            ret.overTime = overTime;
        }
        final ArrayList<Pair<MapleBuffStat, Integer>> statups = new ArrayList<Pair<MapleBuffStat, Integer>>();
        ret.watk = (short) MapleDataTool.getInt("pad", source, 0);
        ret.wdef = (short) MapleDataTool.getInt("pdd", source, 0);
        ret.matk = (short) MapleDataTool.getInt("mad", source, 0);
        ret.mdef = (short) MapleDataTool.getInt("mdd", source, 0);
        ret.acc = (short) MapleDataTool.getIntConvert("acc", source, 0);
        ret.avoid = (short) MapleDataTool.getInt("eva", source, 0);
        ret.speed = (short) MapleDataTool.getInt("speed", source, 0);
        ret.jump = (short) MapleDataTool.getInt("jump", source, 0);
        if (ret.overTime && ret.getSummonMovementType() == null) {
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WATK, Integer.valueOf(ret.watk));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WDEF, Integer.valueOf(ret.wdef));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MATK, Integer.valueOf(ret.matk));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MDEF, Integer.valueOf(ret.mdef));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.ACC, Integer.valueOf(ret.acc));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.AVOID, Integer.valueOf(ret.avoid));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.SPEED, Integer.valueOf(ret.speed));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.JUMP, Integer.valueOf(ret.jump));
        }
        final MapleData ltd = source.getChildByPath("lt");
        if (ltd != null) {
            ret.lt = (Point) ltd.getData();
            ret.rb = (Point) source.getChildByPath("rb").getData();
        }
        int x = MapleDataTool.getInt("x", source, 0);
        ret.x = x;
        ret.y = MapleDataTool.getInt("y", source, 0);
        ret.damage = MapleDataTool.getIntConvert("damage", source, 100);
        ret.attackCount = MapleDataTool.getIntConvert("attackCount", source, 1);
        ret.bulletCount = MapleDataTool.getIntConvert("bulletCount", source, 1);
        ret.bulletConsume = MapleDataTool.getIntConvert("bulletConsume", source, 0);
        ret.moneyCon = MapleDataTool.getIntConvert("moneyCon", source, 0);
        ret.itemCon = MapleDataTool.getInt("itemCon", source, 0);
        ret.itemConNo = MapleDataTool.getInt("itemConNo", source, 0);
        ret.moveTo = MapleDataTool.getInt("moveTo", source, -1);
        Map<MonsterStatus, Integer> monsterStatus = new ArrayMap<MonsterStatus, Integer>();
        if (skill) {
            switch (sourceid) {
            case Beginner.RECOVERY:
            case Noblesse.RECOVERY:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.RECOVERY, Integer.valueOf(x)));
                break;
            case Beginner.ECHO_OF_HERO:
            case Noblesse.ECHO_OF_HERO:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.ECHO_OF_HERO, Integer.valueOf(ret.x)));
                break;
            case Beginner.MONSTER_RIDER:
            case Noblesse.MONSTER_RIDER:
            case Aran.MONSTER_RIDER:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MONSTER_RIDING, Integer.valueOf(1)));
                break;
            case Beginner.BERSERK_FURY:
            case Noblesse.BERSERK_FURY:
            case Aran.BERSERK_FURY:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.BERSERK_FURY, Integer.valueOf(ret.x)));
                break;
            case Beginner.INVINCIBLE_BARRIER:
            case Noblesse.INVINCIBLE_BARRIER:
            case Aran.INVINCIBLE_BARRIER:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.DIVINE_BODY, Integer.valueOf(1)));
                break;
            case Fighter.POWER_GUARD:
            case Page.POWER_GUARD:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.POWERGUARD, Integer.valueOf(x)));
                break;
            case Spearman.HYPER_BODY:
            case GM.HYPER_BODY:
            case SuperGM.HYPER_BODY:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HYPERBODYHP, Integer.valueOf(x)));
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HYPERBODYMP, Integer.valueOf(ret.y)));
                break;
            case Crusader.COMBO:
            case DawnWarrior.COMBO:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO, Integer.valueOf(1)));
                break;
            case WhiteKnight.BW_FIRE_CHARGE:
            case WhiteKnight.BW_ICE_CHARGE:
            case WhiteKnight.BW_LIT_CHARGE:
            case WhiteKnight.SWORD_FIRE_CHARGE:
            case WhiteKnight.SWORD_ICE_CHARGE:
            case WhiteKnight.SWORD_LIT_CHARGE:
            case Paladin.BW_HOLY_CHARGE:
            case Paladin.SWORD_HOLY_CHARGE:
            case DawnWarrior.SOUL_CHARGE:
            case ThunderBreaker.LIGHTNING_CHARGE:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.WK_CHARGE, Integer.valueOf(x)));
                break;
            case Aran.SNOW_CHARGE:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.WK_CHARGE, Integer.valueOf(ret.y)));
                monsterStatus.put(MonsterStatus.SPEED, x);
                break;
            case Aran.ROLLING_SPIN:
                monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                break;
            case DragonKnight.DRAGON_BLOOD:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.DRAGONBLOOD, Integer.valueOf(ret.x)));
                break;
            case DragonKnight.DRAGON_ROAR:
                ret.hpR = -x / 100.0;
                break;
            case Hero.STANCE:
            case Paladin.STANCE:
            case DarkKnight.STANCE:
            case Aran.FREEZE_STANDING:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.STANCE, Integer.valueOf(iprop)));
                break;
            case DawnWarrior.FINAL_ATTACK:
            case WindArcher.FINAL_ATTACK:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.FINALATTACK, Integer.valueOf(x)));
                break;
            case Magician.MAGIC_GUARD:
            case BlazeWizard.MAGIC_GUARD:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MAGIC_GUARD, Integer.valueOf(x)));
                break;
            case Cleric.INVINCIBLE:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.INVINCIBLE, Integer.valueOf(x)));
                break;
            case Priest.HOLY_SYMBOL:
            case SuperGM.HOLY_SYMBOL:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HOLY_SYMBOL, Integer.valueOf(x)));
                break;
            case FPArchMage.INFINITY:
            case ILArchMage.INFINITY:
            case Bishop.INFINITY:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.INFINITY, Integer.valueOf(x)));
                break;
            case FPArchMage.MANA_REFLECTION:
            case ILArchMage.MANA_REFLECTION:
            case Bishop.MANA_REFLECTION:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MANA_REFLECTION, Integer.valueOf(1)));
                break;
            case Bishop.HOLY_SHIELD:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HOLY_SHIELD, Integer.valueOf(x)));
                break;
            case Priest.MYSTIC_DOOR:
            case Hunter.SOUL_ARROW:
            case Crossbowman.SOUL_ARROW:
            case WindArcher.SOUL_ARROW:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SOULARROW, Integer.valueOf(x)));
                break;
            case Ranger.PUPPET:
            case Sniper.PUPPET:
            case WindArcher.PUPPET:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.PUPPET, Integer.valueOf(1)));
                break;
            case Bowmaster.CONCENTRATE:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.CONCENTRATE, x));
                break;
            case Bowmaster.HAMSTRING:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HAMSTRING, Integer.valueOf(x)));
                monsterStatus.put(MonsterStatus.SPEED, x);
                break;
            case Marksman.BLIND:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.BLIND, Integer.valueOf(x)));
                monsterStatus.put(MonsterStatus.ACC, x);
                break;
            case Bowmaster.SHARP_EYES:
            case Marksman.SHARP_EYES:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SHARP_EYES, Integer.valueOf(ret.x << 8 | ret.y)));
                break;
            case Rogue.DARK_SIGHT:
            case WindArcher.WIND_WALK:
            case NightWalker.DARK_SIGHT:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.DARKSIGHT, Integer.valueOf(x)));
                break;
            case Hermit.MESO_UP:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MESOUP, Integer.valueOf(x)));
                break;
            case Hermit.SHADOW_PARTNER:
            case NightWalker.SHADOW_PARTNER:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SHADOWPARTNER, Integer.valueOf(x)));
                break;
            case ChiefBandit.MESO_GUARD:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MESOGUARD, Integer.valueOf(x)));
                break;
            case ChiefBandit.PICKPOCKET:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.PICKPOCKET, Integer.valueOf(x)));
                break;
            case NightLord.SHADOW_STARS:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SHADOW_CLAW, Integer.valueOf(0)));
                break;
            case ThunderBreaker.SPARK:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SPARK, Integer.valueOf(x)));
                break;
            case BlazeWizard.ELEMENTAL_RESET:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.ELEMENTAL_RESET, Integer.valueOf(x)));
                break;
            case Pirate.DASH:
            case ThunderBreaker.DASH:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.DASH, Integer.valueOf(x)));
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.DASH2, Integer.valueOf(ret.y)));
                break;
            case Buccaneer.SPEED_INFUSION:
            case ThunderBreaker.SPEED_INFUSION:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SPEED_INFUSION, Integer.valueOf(x)));
                break;
            case Marauder.ENERGY_CHARGE:
            case ThunderBreaker.ENERGY_CHARGE:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.ENERGY_CHARGE, Integer.valueOf(x)));
                break;
            case Outlaw.HOMING_BEACON:
            case Corsair.BULLSEYE:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HOMING_BEACON, Integer.valueOf(x)));
                break;
            case Corsair.BATTLE_SHIP:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MONSTER_RIDING, Integer.valueOf(sourceid)));
                break;
            case GM.HIDE:
            case SuperGM.HIDE:
                ret.duration = 7200000;
                ret.overTime = true;
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.DARKSIGHT, Integer.valueOf(x)));
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.PYRAMID_PQ, Integer.valueOf(10000)));
                break;
            case Aran.COMBO_BARRIER:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO_BARRIER, Integer.valueOf(x)));
                break;
            case Aran.COMBO_DRAIN:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO_DRAIN, Integer.valueOf(x)));
                break;
            case Aran.SMART_KNOCKBACK:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SMART_KNOCKBACK, Integer.valueOf(x)));
                break;
            case Aran.BODY_PRESSURE:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.BODY_PRESSURE, Integer.valueOf(x)));
                break;
            case Fighter.AXE_BOOSTER:
            case Fighter.SWORD_BOOSTER:
            case Page.BW_BOOSTER:
            case Page.SWORD_BOOSTER:
            case Spearman.POLEARM_BOOSTER:
            case Spearman.SPEAR_BOOSTER:
            case Hunter.BOW_BOOSTER:
            case Crossbowman.CROSSBOW_BOOSTER:
            case Assassin.CLAW_BOOSTER:
            case Bandit.DAGGER_BOOSTER:
            case FPMage.SPELL_BOOSTER:
            case ILMage.SPELL_BOOSTER:
            case Brawler.KNUCKLER_BOOSTER:
            case Gunslinger.GUN_BOOSTER:
            case DawnWarrior.SWORD_BOOSTER:
            case BlazeWizard.SPELL_BOOSTER:
            case WindArcher.BOW_BOOSTER:
            case NightWalker.CLAW_BOOSTER:
            case ThunderBreaker.KNUCKLER_BOOSTER:
            case Aran.ARAN_POLEARM_BOOSTER:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.BOOSTER, Integer.valueOf(x)));
                break;
            case Hero.MAPLE_WARRIOR:
            case Paladin.MAPLE_WARRIOR:
            case DarkKnight.MAPLE_WARRIOR:
            case FPArchMage.MAPLE_WARRIOR:
            case ILArchMage.MAPLE_WARRIOR:
            case Bishop.MAPLE_WARRIOR:
            case Bowmaster.MAPLE_WARRIOR:
            case Marksman.MAPLE_WARRIOR:
            case NightLord.MAPLE_WARRIOR:
            case Shadower.MAPLE_WARRIOR:
            case Corsair.MAPLE_WARRIOR:
            case Buccaneer.MAPLE_WARRIOR:
            case Aran.ARAN_MAPLE_WARRIOR:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MAPLE_WARRIOR, Integer.valueOf(ret.x)));
                break;
            case Ranger.SILVER_HAWK:
            case Sniper.GOLDEN_EAGLE:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SUMMON, Integer.valueOf(1)));
                monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                break;
            case FPArchMage.ELQUINES:
            case Marksman.FROST_PREY:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SUMMON, Integer.valueOf(1)));
                monsterStatus.put(MonsterStatus.FREEZE, Integer.valueOf(1));
                break;
            case Priest.SUMMON_DRAGON:
            case Bowmaster.PHOENIX:
            case ILArchMage.IFRIT:
            case Bishop.BAHAMUT:
            case DarkKnight.BEHOLDER:
            case Outlaw.OCTOPUS:
            case Corsair.WRATH_OF_THE_OCTOPI:
            case Outlaw.GAVIOTA:
            case DawnWarrior.SOUL:
            case BlazeWizard.FLAME:
            case WindArcher.STORM:
            case NightWalker.DARKNESS:
            case ThunderBreaker.LIGHTNING:
            case BlazeWizard.IFRIT:
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SUMMON, Integer.valueOf(1)));
                break;
            case Rogue.DISORDER:
                monsterStatus.put(MonsterStatus.WATK, Integer.valueOf(ret.x));
                monsterStatus.put(MonsterStatus.WDEF, Integer.valueOf(ret.y));
                break;
            case Corsair.HYPNOTIZE:
                monsterStatus.put(MonsterStatus.INERTMOB, 1);
                break;
            case NightLord.NINJA_AMBUSH:
            case Shadower.NINJA_AMBUSH:
                monsterStatus.put(MonsterStatus.NINJA_AMBUSH, Integer.valueOf(1));
                break;
            case Page.THREATEN:
                monsterStatus.put(MonsterStatus.WATK, Integer.valueOf(ret.x));
                monsterStatus.put(MonsterStatus.WDEF, Integer.valueOf(ret.y));
                break;
            case Crusader.AXE_COMA:
            case Crusader.SWORD_COMA:
            case Crusader.SHOUT:
            case WhiteKnight.CHARGE_BLOW:
            case Hunter.ARROW_BOMB:
            case ChiefBandit.ASSAULTER:
            case Shadower.BOOMERANG_STEP:
            case Brawler.BACK_SPIN_BLOW:
            case Brawler.DOUBLE_UPPERCUT:
            case Buccaneer.DEMOLITION:
            case Buccaneer.SNATCH:
            case Buccaneer.BARRAGE:
            case Gunslinger.BLANK_SHOT:
            case DawnWarrior.COMA:
                monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                break;
            case NightLord.TAUNT:
            case Shadower.TAUNT:
                monsterStatus.put(MonsterStatus.SHOWDOWN, Integer.valueOf(1));
                break;
            case ILWizard.COLD_BEAM:
            case ILMage.ICE_STRIKE:
            case ILArchMage.BLIZZARD:
            case ILMage.ELEMENT_COMPOSITION:
            case Sniper.BLIZZARD:
            case Outlaw.ICE_SPLITTER:
            case FPArchMage.PARALYZE:
            case Aran.COMBO_TEMPEST:
                monsterStatus.put(MonsterStatus.FREEZE, Integer.valueOf(1));
                ret.duration *= 2;
                break;
            case FPWizard.SLOW:
            case ILWizard.SLOW:
            case BlazeWizard.SLOW:
                monsterStatus.put(MonsterStatus.SPEED, Integer.valueOf(ret.x));
                break;
            case FPWizard.POISON_BREATH:
            case FPMage.ELEMENT_COMPOSITION:
                monsterStatus.put(MonsterStatus.POISON, Integer.valueOf(1));
                break;
            case Priest.DOOM:
                monsterStatus.put(MonsterStatus.DOOM, Integer.valueOf(1));
                break;
            case ILMage.SEAL:
            case FPMage.SEAL:
            case BlazeWizard.SEAL:
                monsterStatus.put(MonsterStatus.SEAL, 1);
                break;
            case Hermit.SHADOW_WEB:
            case NightWalker.SHADOW_WEB:
                monsterStatus.put(MonsterStatus.SHADOW_WEB, 1);
                break;
            case FPArchMage.FIRE_DEMON:
            case ILArchMage.ICE_DEMON:
                monsterStatus.put(MonsterStatus.POISON, Integer.valueOf(1));
                monsterStatus.put(MonsterStatus.FREEZE, Integer.valueOf(1));
                break;
            default:
                break;
            }
        }
        if (ret.isMorph()) {
            statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MORPH, Integer.valueOf(ret.getMorph())));
        }
        if (ret.isGhost && !skill) {
            statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.GHOST_MORPH, Integer.valueOf(1)));
        }
        ret.monsterStatus = monsterStatus;
        statups.trimToSize();
        ret.statups = statups;
        return ret;
    }

    public final void applyPassive(final MapleCharacter applyto, final MapleMapObject obj, final int attack) {
        if (makeChanceResult()) {
            switch (sourceid) { // MP eater
            case FPWizard.MP_EATER:
            case ILWizard.MP_EATER:
            case Cleric.MP_EATER:
                if (obj == null || obj.getType() != MapleMapObjectType.MONSTER) {
                    return;
                }
                final MapleMonster mob = (MapleMonster) obj;
                if (!mob.isBoss()) {
                    final int absorbMp = Math.min((int) (mob.getMaxMp() * (getX() / 100.0)), mob.getMp());
                    if (absorbMp > 0) {
                        mob.setMp(mob.getMp() - absorbMp);
                        applyto.addMP(absorbMp);
                        applyto.getClient().announce(BuffFactory.showOwnBuffEffect(sourceid, 1));
                        applyto.getMap().broadcastMessage(applyto, BuffFactory.showBuffEffect(applyto, applyto.getId(), sourceid, 1), false);
                    }
                }
                break;
            }
        }
    }

    public final boolean applyTo(MapleCharacter chr) {
        return applyTo(chr, chr, true, null);
    }

    public final boolean applyTo(MapleCharacter chr, Point pos) {
        return applyTo(chr, chr, true, pos);
    }

    private final boolean applyTo(final MapleCharacter applyfrom, final MapleCharacter applyto, final boolean primary, final Point pos) {
        int hpchange = calcHPChange(applyfrom, primary);
        if(isHeal() && applyto != applyfrom && applyto.getHp() != applyto.getMaxHp()) {
            applyfrom.gainExp((int) (hpchange / 25.25), true, false);
        }
        int mpchange = calcMPChange(applyfrom, primary);
        if (primary) {
            if (itemConNo != 0) {
                MapleInventoryManipulator.removeById(applyto.getClient(), MapleItemInformationProvider.getInstance().getInventoryType(itemCon), itemCon, itemConNo, false, true);
            }
        }
        final List<Pair<MapleStat, Integer>> hpmpupdate = new ArrayList<Pair<MapleStat, Integer>>(2);
        if (!primary && isResurrection()) {
            hpchange = applyto.getMaxHp();
            applyto.setStance(0);
        }
        if(isDrain())
        applyto.setCombo(0);
        if (isDispel() && makeChanceResult()) {
            applyto.dispelDebuffs();
        } else if (isHeroWill()) {
            applyto.dispelDebuff(MapleDisease.SEDUCE);
        }
        if (hpchange != 0) {
            if (hpchange < 0 && -hpchange > applyto.getHp()) {
                return false;
            }
            int newHp = applyto.getHp() + hpchange;
            if (newHp < 1) {
                newHp = 1;
            }
            applyto.setHp(newHp);
            hpmpupdate.add(new Pair<MapleStat, Integer>(MapleStat.HP, Integer.valueOf(applyto.getHp())));
        }
        if (mpchange != 0) {
            if (mpchange < 0 && -mpchange > applyto.getMp()) {
                return false;
            }
            applyto.setMp(applyto.getMp() + mpchange);
            hpmpupdate.add(new Pair<MapleStat, Integer>(MapleStat.MP, Integer.valueOf(applyto.getMp())));
        }
        applyto.getClient().announce(IntraPersonalFactory.updatePlayerStats(hpmpupdate, true));
        if (moveTo != -1) {
            MapleMap target;
            if (moveTo == 999999999) {
                target = applyto.getMap().getReturnMap();
            } else {
                target = ChannelServer.getInstance(applyto.getClient().getChannel()).getMapFactory().getMap(moveTo);
                if (target.getId() / 10000000 != 21 && applyto.getMapId() / 10000000 != 20) {
                    if (target.getId() / 10000000 != applyto.getMapId() / 10000000) {
                        return false;
                    }
                }
            }
            applyto.changeMap(target, target.getPortal(0));
        }
        if (isShadowClaw()) {
            int projectile = 0;
            MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
            for (int i = 0; i < 97; i++) {
                IItem item = use.getItem((byte) i);
                if (item != null) {
                    if (item.getId() / 10000 == 207 && item.getQuantity() >= 200) {
                        projectile = item.getId();
                        break;
                    }
                }
            }
            if (projectile == 0) {
                return false;
            } else {
                MapleInventoryManipulator.removeById(applyto.getClient(), MapleInventoryType.USE, projectile, 200, false, true);
                applyto.setProjectile(projectile);
            }
        }
        if (overTime || isCygnusFA()) {
            applyBuffEffect(applyfrom, applyto, primary);
        }
        if (primary && (overTime || isHeal())) {
            applyBuff(applyfrom);
        }
        if (primary && isMonsterBuff()) {
            applyMonsterBuff(applyfrom);
        }
        if (this.getFatigue() != 0) {
            applyto.getMount().setTiredness(applyto.getMount().getTiredness() + this.getFatigue());
        }
        final SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null && pos != null) {
            final MapleSummon tosummon = new MapleSummon(applyfrom, sourceid, pos, summonMovementType);
            applyfrom.getMap().spawnSummon(tosummon);
            applyfrom.addSummon(sourceid, tosummon);
            tosummon.addHP(x);
            if (isBeholder()) {
                tosummon.addHP(1);
            }
        }
        if (isMagicDoor() && !FieldLimit.DOOR.check(applyto.getMap().getFieldLimit())) {
            Point doorPosition = new Point(applyto.getPosition());
            MapleDoor door = new MapleDoor(applyto, doorPosition);
            applyto.getMap().spawnDoor(door);
            applyto.addDoor(door);
            door = new MapleDoor(door);
            applyto.addDoor(door);
            door.getTown().spawnDoor(door);
            if (applyto.getParty() != null) {
                applyto.silentPartyUpdate();
            }
            applyto.disableDoor();
        } else if (isMist() && sourceid != NightWalker.POISON_BOMB) {
            final Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
            final MapleMist mist = new MapleMist(bounds, applyfrom, this);
            applyfrom.getMap().spawnMist(mist, getDuration(), sourceid != Shadower.SMOKE_SCREEN, false);
        } else if (isTimeLeap()) {
            for (PlayerCoolDownValueHolder i : applyto.getAllCooldowns()) {
                if (i.skillId != Buccaneer.TIME_LEAP) {
                    applyto.removeCooldown(i.skillId);
                    applyto.getClient().announce(BuffFactory.skillCooldown(i.skillId, 0));
                }
            }
        }
        return true;
    }

    private final void applyBuff(MapleCharacter applyfrom) {
        if (isPartyBuff() && (applyfrom.getParty() != null) && !(applyfrom.gmLevel() == 1 || applyfrom.gmLevel() == 2)) {
            final Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
            final List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInBox(bounds, Arrays.asList(MapleMapObjectType.PLAYER));
            List<MapleCharacter> affectedp = new ArrayList<MapleCharacter>(affecteds.size());
            for (final MapleMapObject affectedmo : affecteds) {
                final MapleCharacter affected = (MapleCharacter) affectedmo;
                if (affected != applyfrom && (applyfrom.getParty().equals(affected.getParty()))) {
                    if ((isResurrection() && !affected.isAlive()) || (!isResurrection() && affected.isAlive())) {
                        affectedp.add(affected);
                    }
                    if (isTimeLeap()) {
                        for (PlayerCoolDownValueHolder i : affected.getAllCooldowns()) {
                            affected.removeCooldown(i.skillId);
                            affected.getClient().announce(BuffFactory.skillCooldown(i.skillId, 0));
                        }
                    }
                }
            }
            for (MapleCharacter affected : affectedp) {
                applyTo(applyfrom, affected, false, null);
                affected.getClient().announce(BuffFactory.showOwnBuffEffect(sourceid, 2));
                affected.getMap().broadcastMessage(affected, BuffFactory.showBuffEffect(affected, affected.getId(), sourceid, 2), false);
            }
        }
    }

    private final void applyMonsterBuff(MapleCharacter applyfrom) {
        final Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
        final List<MapleMapObject> affected = applyfrom.getMap().getMapObjectsInBox(bounds, Arrays.asList(MapleMapObjectType.MONSTER));
        int i = 0;
        for (final MapleMapObject mo : affected) {
            MapleMonster monster = (MapleMonster) mo;
            if (makeChanceResult()) {
                monster.applyStatus(applyfrom, new MonsterStatusEffect(getMonsterStati(), SkillFactory.getSkill(sourceid), false), isPoison(), getDuration());
            }
            i++;
            if (i >= mobCount) {
                break;
            }
        }
    }

    private final Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft) {
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            mylt = new Point(-rb.x + posFrom.x, lt.y + posFrom.y);
            myrb = new Point(-lt.x + posFrom.x, rb.y + posFrom.y);
        }
        Rectangle bounds = new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
        return bounds;
    }

    public final void silentApplyBuff(final MapleCharacter chr, final long starttime) {
        int localDuration = duration;
        localDuration = alchemistModifyVal(chr, localDuration, false);
        CancelEffectAction cancelAction = new CancelEffectAction(chr, this, starttime);
        ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime + localDuration) - System.currentTimeMillis()));
        chr.registerEffect(this, starttime, schedule);
        final SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null) {
            final MapleSummon tosummon = new MapleSummon(chr, sourceid, chr.getPosition(), summonMovementType);
            if (!tosummon.isStationary()) {
                chr.addSummon(sourceid, tosummon);
                tosummon.addHP(x);
            }
        }
    }

    private final void applyBuffEffect(final MapleCharacter applyfrom, final MapleCharacter applyto, final boolean primary) {
        applyBuffEffect(applyfrom, applyto, primary, -1);
    }

    private final void applyBuffEffect(final MapleCharacter applyfrom, final MapleCharacter applyto, final boolean primary, final int oid) {
        if (sourceid != Corsair.BATTLE_SHIP) {
            if (!this.isMonsterRiding()) {
                if (isHomingBeacon()) {
                    applyto.offBeacon(true);
                } else {
                    applyto.cancelEffect(this, true, -1);
                }
            }
        } else {
            applyto.cancelEffect(this, true, -1);
        }
        List<Pair<MapleBuffStat, Integer>> localstatups = statups;
        int localDuration = duration;
        int localsourceid = sourceid;
        int seconds = localDuration / 1000;
        MapleMount givemount = null;
        if (isMonsterRiding()) {
            int ridingLevel = 0;
            IItem mount = applyfrom.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
            if (mount != null) {
                ridingLevel = mount.getId();
            }
            if (sourceid == Corsair.BATTLE_SHIP) {
                ridingLevel = 1932000;
            } else {
                if (applyto.getMount() == null) {
                    applyto.giveMount(ridingLevel, sourceid);
                }
                applyto.getMount().startSchedule();
            }
            if (sourceid == Corsair.BATTLE_SHIP) {
                givemount = new MapleMount(applyto, 1932000, Corsair.BATTLE_SHIP);
            } else {
                givemount = applyto.getMount();
            }
            localDuration = 300000;
            localsourceid = ridingLevel;
            localstatups = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MONSTER_RIDING, 0));
        } else if (isSkillMorph()) {
            localstatups = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MORPH, getMorph()));
        }
        if (primary) {
            localDuration = alchemistModifyVal(applyfrom, localDuration, false);
        }
        if (localstatups.size() > 0) {
            MaplePacket buff = BuffFactory.giveBuff((skill ? sourceid : -sourceid), localDuration, localstatups, false);
            if (isDash()) {
                if ((applyto.getJob() / 100) % 10 != 5) {
                    applyto.changeSkillLevel(SkillFactory.getSkill(sourceid), 0, 10);
                } else {
                    buff = BuffFactory.givePirateBuff(sourceid, localDuration / 1000, localstatups);
                }
            } else if (isEnergyCharge()) {
                localstatups = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.ENERGY_CHARGE, 10000));
                buff = BuffFactory.givePirateBuff(sourceid, localDuration / 1000, localstatups);
            } else if (isInfusion()) {
                buff = BuffFactory.giveSpeedInfusion(sourceid, localDuration / 1000, localstatups);
            } else if (isMonsterRiding()) {
                buff = BuffFactory.giveBuff(localsourceid, localDuration, localstatups, true);
            } else if (isCygnusFA()) {
                buff = BuffFactory.giveFinalAttack(sourceid, seconds);
            } else if (isHomingBeacon()) {
                applyto.setBeacon(oid);
                applyto.getClient().announce(BuffFactory.giveHomingBeacon(localsourceid, oid));
            }
            applyto.getClient().announce(buff);
        }
        if (isDash() || isEnergyCharge()) {
            applyto.getMap().broadcastMessage(applyto, BuffFactory.showPirateBuff(applyto.getId(), sourceid, localDuration, localstatups), false);
        } else if (isInfusion()) {
            applyto.getMap().broadcastMessage(applyto, BuffFactory.showSpeedInfusion(applyto.getId(), sourceid, localDuration, localstatups), false);
        } else if (isDs()) {
            final List<Pair<MapleBuffStat, Integer>> dsstat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.DARKSIGHT, 0));
            applyto.getMap().broadcastMessage(applyto, BuffFactory.giveForeignBuff(applyto.getId(), dsstat, false), false);
        } else if (isCombo()) {
            final List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO, 1));
            applyto.getMap().broadcastMessage(applyto, BuffFactory.giveForeignBuff(applyto.getId(), stat, false), false);
        } else if (isMonsterRiding()) {
            final List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MONSTER_RIDING, 1));
            applyto.getMap().broadcastMessage(applyto, BuffFactory.showMonsterRiding(applyto.getId(), stat, givemount), false);
        } else if (isShadowPartner()) {
            final List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SHADOWPARTNER, 0));
            applyto.getMap().broadcastMessage(applyto, BuffFactory.giveForeignBuff(applyto.getId(), stat, false), false);
        } else if (isSoulArrow()) {
            final List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SOULARROW, 0));
            applyto.getMap().broadcastMessage(applyto, BuffFactory.giveForeignBuff(applyto.getId(), stat, false), false);
        } else if (isEnrage()) {
            applyto.handleOrbConsume();
        } else if (isMorph()) {
            final List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MORPH, Integer.valueOf(getMorph(applyto))));
            applyto.getMap().broadcastMessage(applyto, BuffFactory.giveForeignBuff(applyto.getId(), stat, true), false);
        } else if (isTimeLeap()) {
            for (PlayerCoolDownValueHolder i : applyto.getAllCooldowns()) {
                if (i.skillId != Buccaneer.TIME_LEAP) {
                    applyto.removeCooldown(i.skillId);
                    applyto.getClient().announce(BuffFactory.skillCooldown(i.skillId, 0));
                }
            }
        }
        if (localstatups.size() > 0) {
            final long starttime = System.currentTimeMillis();
            final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
            final ScheduledFuture<?> schedule = isHomingBeacon() ? null : TimerManager.getInstance().schedule(cancelAction, localDuration);
            applyto.registerEffect(this, starttime, schedule);
        }
        if (primary && !isHomingBeacon()) {
            applyto.getMap().broadcastMessage(applyto, BuffFactory.showBuffEffect(applyto, applyto.getId(), sourceid, 1, (byte) 3), false);
        }
    }

    private final int calcHPChange(final MapleCharacter applyfrom, final boolean primary) {
        int hpchange = 0;
        if (hp != 0) {
            if (!skill) {
                if (primary) {
                    hpchange += alchemistModifyVal(applyfrom, hp, true);
                } else {
                    hpchange += hp;
                }
            } else {
                hpchange += makeHealHP(hp / 100.0, applyfrom.getTotalMagic(), 3, 5);
            }
        }
        if (hpR != 0) {
            hpchange += (int) (applyfrom.getCurrentMaxHp() * hpR);
            applyfrom.checkBerserk();
        }
        if (primary) {
            if (hpCon != 0) {
                hpchange -= hpCon;
            }
        }
        if (isChakra()) {
            hpchange += makeHealHP(getY() / 100.0, applyfrom.getTotalLuk(), 2.3, 3.5);
        }
        return hpchange;
    }

    private final int makeHealHP(double rate, double stat, double l, double u) {
        return (int) ((Math.random() * ((int) ((u - l) * stat * rate)) + 1) + (int) (stat * l * rate));
    }

    private final int calcMPChange(MapleCharacter applyfrom, boolean primary) {
        int mpchange = 0;
        if (mp != 0) {
            if (primary) {
                mpchange += alchemistModifyVal(applyfrom, mp, true);
            } else {
                mpchange += mp;
            }
        }
        if (mpR != 0) {
            mpchange += (int) (applyfrom.getCurrentMaxMp() * mpR);
        }
        if (primary) {
            if (mpCon != 0) {
                double mod = 1.0;
                boolean isAFpMage = applyfrom.isA(FPMage.ID);
                boolean isCygnus = applyfrom.isA(BlazeWizard.BLAZE_WIZARD2);
                if (isAFpMage || isCygnus || applyfrom.isA(ILMage.ID)) {
                    final ISkill amp = isAFpMage ? SkillFactory.getSkill(FPMage.ELEMENT_AMPLIFICATION) : (isCygnus ? SkillFactory.getSkill(BlazeWizard.ELEMENT_AMPLIFICATION) : SkillFactory.getSkill(ILMage.ELEMENT_AMPLIFICATION));
                    final int ampLevel = applyfrom.getSkillLevel(amp);
                    if (ampLevel > 0) {
                        mod = amp.getEffect(ampLevel).getX() / 100.0;
                    }
                }
                mpchange -= mpCon * mod;
                if (applyfrom.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                    mpchange = 0;
                } else if (applyfrom.getBuffedValue(MapleBuffStat.CONCENTRATE) != null) {
                    mpchange -= (int) (mpchange * (applyfrom.getBuffedValue(MapleBuffStat.CONCENTRATE).doubleValue() / 100));
                }
            }
        }
        return mpchange;
    }

    private final int alchemistModifyVal(final MapleCharacter chr, final int val, final boolean withX) {
        if (!skill && (chr.isA(Hermit.ID) || chr.isA(NightWalker.NIGHT_WALKER3))) {
            final MapleStatEffect alchemistEffect = getAlchemistEffect(chr);
            if (alchemistEffect != null) {
                return (int) (val * ((withX ? alchemistEffect.getX() : alchemistEffect.getY()) / 100.0));
            }
        }
        return val;
    }

    private final MapleStatEffect getAlchemistEffect(final MapleCharacter chr) {
        int id = Hermit.ALCHEMIST;
        if (chr.isCygnus()) {
            id = NightWalker.ALCHEMIST;
        }
        int alchemistLevel = chr.getSkillLevel(SkillFactory.getSkill(id));
        return alchemistLevel == 0 ? null : SkillFactory.getSkill(id).getEffect(alchemistLevel);
    }

    private final boolean isMonsterBuff() {
        if (!skill) {
            return false;
        }
        switch (sourceid) {
        case Page.THREATEN:
        case FPWizard.SLOW:
        case ILWizard.SLOW:
        case FPMage.SEAL:
        case ILMage.SEAL:
        case Priest.DOOM:
        case Hermit.SHADOW_WEB:
        case NightLord.NINJA_AMBUSH:
        case Shadower.NINJA_AMBUSH:
        case BlazeWizard.SLOW:
        case BlazeWizard.SEAL:
        case NightWalker.SHADOW_WEB:
            return true;
        }
        return false;
    }

    private final boolean isPartyBuff() {
        if (lt == null || rb == null) {
            return false;
        }
        if ((sourceid >= 1211003 && sourceid <= 1211008) || sourceid == Paladin.SWORD_HOLY_CHARGE || sourceid == Paladin.BW_HOLY_CHARGE || sourceid == DawnWarrior.SOUL_CHARGE || sourceid == Aran.SNOW_CHARGE || sourceid == ThunderBreaker.LIGHTNING_CHARGE) {
            return false;
        }
        return true;
    }

    private final boolean isHeal() {
        return sourceid == Cleric.HEAL || sourceid == SuperGM.HEAL_PLUS_DISPEL;
    }

    private final boolean isResurrection() {
        return sourceid == Bishop.RESURRECTION || sourceid == GM.RESURRECTION || sourceid == SuperGM.RESURRECTION;
    }

    private final boolean isTimeLeap() {
        return sourceid == Buccaneer.TIME_LEAP;
    }

    public final boolean isHide() {
        return skill && (sourceid == GM.HIDE || sourceid == SuperGM.HIDE);
    }

    public final boolean isDragonBlood() {
        return skill && sourceid == DragonKnight.DRAGON_BLOOD;
    }

    public final boolean isBerserk() {
        return skill && sourceid == DarkKnight.BERSERK;
    }

    private final boolean isDs() {
        return skill && (sourceid == Rogue.DARK_SIGHT || sourceid == WindArcher.WIND_WALK || sourceid == NightWalker.DARK_SIGHT);
    }

    private final boolean isCombo() {
        return skill && (sourceid == Crusader.COMBO || sourceid == DawnWarrior.COMBO);
    }

    private final boolean isEnrage() {
        return skill && sourceid == Hero.ENRAGE;
    }

    public final boolean isBeholder() {
        return skill && sourceid == DarkKnight.BEHOLDER;
    }

    private final boolean isShadowPartner() {
        return skill && (sourceid == Hermit.SHADOW_PARTNER || sourceid == NightWalker.SHADOW_PARTNER);
    }

    private final boolean isChakra() {
        return skill && sourceid == ChiefBandit.CHAKRA;
    }

    public final boolean isHomingBeacon() {
        return skill && (sourceid == Outlaw.HOMING_BEACON || sourceid == Corsair.BULLSEYE);
    }

    public final boolean isMonsterRiding() {
        return skill && (sourceid % 10000000 == 1004 || sourceid == Corsair.BATTLE_SHIP);
    }

    public final boolean isMagicDoor() {
        return skill && sourceid == Priest.MYSTIC_DOOR;
    }

    public final boolean isPoison() {
        return skill && (sourceid == FPMage.POISON_MIST || sourceid == FPWizard.POISON_BREATH || sourceid == FPMage.ELEMENT_COMPOSITION || sourceid == NightWalker.POISON_BOMB);
    }

    private final boolean isMist() {
        return skill && (sourceid == FPMage.POISON_MIST || sourceid == Shadower.SMOKE_SCREEN || sourceid == BlazeWizard.FLAME_GEAR || sourceid == NightWalker.POISON_BOMB);
    }

    private final boolean isSoulArrow() {
        return skill && (sourceid == Hunter.SOUL_ARROW || sourceid == Crossbowman.SOUL_ARROW || sourceid == WindArcher.SOUL_ARROW);
    }

    private final boolean isShadowClaw() {
        return skill && sourceid == NightLord.SHADOW_STARS;
    }

    private final boolean isDispel() {
        return skill && (sourceid == Priest.DISPEL || sourceid == SuperGM.HEAL_PLUS_DISPEL);
    }

    private final boolean isHeroWill() {
        if (skill) {
            switch (sourceid) {
            case Hero.HEROS_WILL:
            case Paladin.HEROS_WILL:
            case DarkKnight.HEROS_WILL:
            case FPArchMage.HEROS_WILL:
            case ILArchMage.HEROS_WILL:
            case Bishop.HEROS_WILL:
            case Bowmaster.HEROS_WILL:
            case Marksman.HEROS_WILL:
            case NightLord.HEROS_WILL:
            case Shadower.HEROS_WILL:
            case Buccaneer.PIRATES_RAGE:
            case Corsair.SPEED_INFUSION:
                return true;
            default:
                return false;
            }
        }
        return false;
    }

    private final boolean isDash() {
        return skill && (sourceid == Pirate.DASH || sourceid == ThunderBreaker.DASH);
    }

    private final boolean isSkillMorph() {
        return skill && (sourceid == Buccaneer.SUPER_TRANSFORMATION || sourceid == Marauder.TRANSFORMATION || sourceid == WindArcher.EAGLE_EYE || sourceid == ThunderBreaker.TRANSFORMATION);
    }

    public final boolean isEnergyCharge() {
        return skill && (sourceid == Marauder.ENERGY_CHARGE || sourceid == ThunderBreaker.ENERGY_CHARGE);
    }

    private final boolean isInfusion() {
        return skill && (sourceid == Buccaneer.SPEED_INFUSION || sourceid == ThunderBreaker.SPEED_INFUSION);
    }

    private final boolean isCygnusFA() {
        return skill && (sourceid == DawnWarrior.FINAL_ATTACK || sourceid == WindArcher.FINAL_ATTACK);
    }

    public final boolean isMorph() {
        return morphId > 0;
    }

    private final boolean isDrain() {
        return sourceid == Aran.COMBO_DRAIN;
    }

    private final int getFatigue() {
        return fatigue;
    }
    
    public final int getWeaponType() {
        return weapon;
    }

    private final int getMorph() {
        return morphId;
    }

    private final int getMorph(MapleCharacter chr) {
        if (morphId % 10 == 0) {
            return morphId + chr.getGender();
        }
        return morphId + 100 * chr.getGender();
    }

    private final SummonMovementType getSummonMovementType() {
        if (!skill) {
            return null;
        }
        switch (sourceid) {
        case Ranger.PUPPET:
        case Sniper.PUPPET:
        case WindArcher.PUPPET:
        case Outlaw.OCTOPUS:
        case Corsair.WRATH_OF_THE_OCTOPI:
            return SummonMovementType.STATIONARY;
        case Ranger.SILVER_HAWK:
        case Sniper.GOLDEN_EAGLE:
        case Priest.SUMMON_DRAGON:
        case Marksman.FROST_PREY:
        case Bowmaster.PHOENIX:
        case Outlaw.GAVIOTA:
            return SummonMovementType.CIRCLE_FOLLOW;
        case DarkKnight.BEHOLDER:
        case FPArchMage.ELQUINES:
        case ILArchMage.IFRIT:
        case Bishop.BAHAMUT:
        case DawnWarrior.SOUL:
        case BlazeWizard.FLAME:
        case BlazeWizard.IFRIT:
        case WindArcher.STORM:
        case NightWalker.DARKNESS:
        case ThunderBreaker.LIGHTNING:
            return SummonMovementType.FOLLOW;
        }
        return null;
    }

    public final boolean isSkill() {
        return skill;
    }

    public final int getSourceId() {
        return sourceid;
    }

    public final boolean makeChanceResult() {
        return prop == 1.0 || Math.random() < prop;
    }

    private static class CancelEffectAction implements Runnable {
        private final MapleStatEffect effect;
        private final WeakReference<MapleCharacter> target;
        private final long startTime;

        public CancelEffectAction(final MapleCharacter target, final MapleStatEffect effect, final long startTime) {
            this.effect = effect;
            this.target = new WeakReference<MapleCharacter>(target);
            this.startTime = startTime;
        }

        @Override
        public void run() {
            final MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.cancelEffect(effect, false, startTime);
            }
        }
    }

    public final short getHp() {
        return hp;
    }

    public final short getMp() {
        return mp;
    }

    public final short getMatk() {
        return matk;
    }

    public final int getDuration() {
        return duration;
    }

    public final List<Pair<MapleBuffStat, Integer>> getStatups() {
        return statups;
    }

    public final boolean sameSource(MapleStatEffect effect) {
        return this.sourceid == effect.sourceid && this.skill == effect.skill;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getDamage() {
        return damage;
    }

    public final int getAttackCount() {
        return attackCount;
    }

    public final int getBulletCount() {
        return bulletCount;
    }

    public final int getBulletConsume() {
        return bulletConsume;
    }

    public final int getMoneyCon() {
        return moneyCon;
    }

    public final int getCooldown() {
        return cooldown;
    }

    public final int getMobCount() {
        return mobCount;
    }

    public final Map<MonsterStatus, Integer> getMonsterStati() {
        return monsterStatus;
    }
}

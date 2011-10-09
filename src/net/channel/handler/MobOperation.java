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

package net.channel.handler;

import client.CancelCooldownAction;
import client.IItem;
import client.ISkill;
import client.Item;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MapleStat;
import client.MapleWeaponType;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.skills.Aran;
import constants.skills.Assassin;
import constants.skills.Bandit;
import constants.skills.Beginner;
import constants.skills.Bishop;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.Buccaneer;
import constants.skills.ChiefBandit;
import constants.skills.Cleric;
import constants.skills.Corsair;
import constants.skills.Crusader;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.DragonKnight;
import constants.skills.FPArchMage;
import constants.skills.Gunslinger;
import constants.skills.Hero;
import constants.skills.ILArchMage;
import constants.skills.Marauder;
import constants.skills.Marksman;
import constants.skills.NightLord;
import constants.skills.NightWalker;
import constants.skills.Outlaw;
import constants.skills.Paladin;
import constants.skills.Rogue;
import constants.skills.Shadower;
import constants.skills.ThunderBreaker;
import constants.skills.WindArcher;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.MaplePacket;
import net.SendPacketOpcode;
import net.channel.ChannelServer;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.TimerManager;
import server.life.AttackInfo;
import server.life.Element;
import server.life.ElementalEffectiveness;
import server.life.MapleMonster;
import server.life.MobAttackInfo;
import server.life.MobAttackInfoFactory;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.life.SummonAttackEntry;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMist;
import server.maps.MapleSummon;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.LittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;
import tools.factory.BuffFactory;
import tools.factory.EffectFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.MobFactory;
import tools.factory.SummonFactory;

/**
 * @name        MobOperation
 * @author      x711Li
 */
public class MobOperation {
    private static final int[] skills = {Buccaneer.BARRAGE, Buccaneer.DEMOLITION, ILArchMage.BLIZZARD, FPArchMage.METEO, Bishop.GENESIS, ChiefBandit.MESO_EXPLOSION, Aran.COMBO_PENRIL, Aran.COMBO_TEMPEST, Marksman.SNIPE, Shadower.ASSASSINATE, Corsair.BATTLE_SHIP_CANNON, Crusader.SWORD_PANIC, Crusader.AXE_PANIC, Marksman.PIERCING_ARROW};

    protected static void applyAttack(AttackInfo attack, final MapleCharacter player, int attackCount) {
        if(player.gmLevel() == 1 || player.gmLevel() == 2) {
            return;
        }
        ISkill theSkill = null;
        MapleStatEffect attackEffect = null;
        if (attack.skill != 0) {
            theSkill = SkillFactory.getSkill(attack.skill);
            if (player.getSkillLevel(theSkill) <= 0) {
                return;
            }
            attackEffect = attack.getAttackEffect(player, theSkill);
            if (attackEffect == null) {
                player.getClient().announce(IntraPersonalFactory.enableActions());
                return;
            }
            if (attack.numAttacked > attackEffect.getMobCount()) {
                return;
            }
            int weaponType = attackEffect.getWeaponType();
            if (weaponType != -1) {
                if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11).getId() / 10000 != weaponType + 100) {
                    return;
                }
            }
            if (attack.skill == Cleric.HEAL || attack.skill == Corsair.BULLSEYE) {
            } else if (attack.skill == NightWalker.POISON_BOMB) {// Poison Bomb
                attackEffect.applyTo(player, new Point(attack.xCoord, attack.yCoord));
            } else if (player.isAlive()) {
                attackEffect.applyTo(player);
            } else {
                player.getClient().announce(IntraPersonalFactory.enableActions());
            }
        }
        if (!player.isAlive()) {
            return;
        }
        int totDamage = 0;
        final MapleMap map = player.getMap();
        if (attack.skill == ChiefBandit.MESO_EXPLOSION) {
            int delay = 0;
            for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
                MapleMapObject mapobject = map.getMapObject(oned.getLeft().intValue());
                if (mapobject != null && mapobject.getType() == MapleMapObjectType.ITEM) {
                    final MapleMapItem mapitem = (MapleMapItem) mapobject;
                    if (mapitem.getMeso() > 9) {
                        mapitem.itemLock.lock();
                        try {
                            if (mapitem.isPickedUp()) {
                                return;
                            }
                            TimerManager.getInstance().schedule(new Runnable() {
                                public void run() {
                                    map.removeMapObject(mapitem);
                                    map.broadcastMessage(EffectFactory.removeItemFromMap(mapitem.getObjectId(), 4, 0), mapitem.getPosition());
                                    mapitem.setPickedUp(true);
                                }
                            }, delay);
                            delay += 100;
                        } finally {
                            mapitem.itemLock.unlock();
                        }
                    } else if (mapitem.getMeso() == 0) {
                        return;
                    }
                } else if (mapobject != null && mapobject.getType() != MapleMapObjectType.MONSTER) {
                    return;
                }
            }
        }
        for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
            final MapleMonster monster = map.getMonsterByOid(oned.getLeft().intValue());
            if (monster != null) {
                int totDamageToOneMonster = 0;
                for (int eachd : oned.getRight()) {
                    totDamageToOneMonster += eachd;
                }
                totDamage += totDamageToOneMonster;
                player.checkMonsterAggro(monster);
                if (player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null && (attack.skill == 0 || attack.skill == Rogue.DOUBLE_STAB || attack.skill == Bandit.SAVAGE_BLOW || attack.skill == ChiefBandit.ASSAULTER || attack.skill == ChiefBandit.BAND_OF_THIEVES || attack.skill == Shadower.ASSASSINATE || attack.skill == Shadower.TAUNT || attack.skill == Shadower.BOOMERANG_STEP)) {
                    ISkill pickpocket = SkillFactory.getSkill(ChiefBandit.PICKPOCKET);
                    int delay = 0;
                    int maxmeso = player.getBuffedValue(MapleBuffStat.PICKPOCKET).intValue();
                    final Point monsterPosition = monster.getPosition();
                    for (Integer eachd : oned.getRight()) {
                        if (pickpocket.getEffect(player.getSkillLevel(pickpocket)).makeChanceResult()) {
                            final int todrop = Math.min(Math.max(eachd * maxmeso / 200, 1), maxmeso);
                            TimerManager.getInstance().schedule(new Runnable() {
                                public void run() {
                                    player.getMap().spawnMesoDrop(todrop, todrop, new Point((int) (monsterPosition.getX() + Randomizer.getInstance().nextInt(101) - 50), (int) (monsterPosition.getY())), monster.getObjectId(), monster.getPosition(), player, false);
                                }
                            }, delay);
                            delay += 100;
                        }
                    }
                } else if (attack.skill == Marksman.SNIPE) {
                    totDamageToOneMonster = 195000 + Randomizer.getInstance().nextInt(5000);
                } else if (attack.skill == Marauder.ENERGY_DRAIN || attack.skill == ThunderBreaker.ENERGY_DRAIN || attack.skill == NightWalker.VAMPIRE || attack.skill == Assassin.DRAIN) {
                    player.addHP(Math.min(monster.getMaxHp(), Math.min((int) ((double) totDamage * (double) SkillFactory.getSkill(attack.skill).getEffect(player.getSkillLevel(SkillFactory.getSkill(attack.skill))).getX() / 100.0), player.getMaxHp() / 2)));
                } else if (attack.skill == Bandit.STEAL && !monster.isBoss()) {
                    ISkill steal = SkillFactory.getSkill(Bandit.STEAL);
                    if (!monster.getRaided() && steal.getEffect(player.getSkillLevel(steal)).makeChanceResult()) {
                        monster.getMap().dropFromMonster(player, monster);
                        monster.setRaided(true);
                    }
                } else if (attack.skill == Outlaw.ICE_SPLITTER) {
                    monster.setTempEffectiveness(Element.ICE, ElementalEffectiveness.WEAK, SkillFactory.getSkill(Outlaw.ICE_SPLITTER).getEffect(player.getSkillLevel(SkillFactory.getSkill(Outlaw.ICE_SPLITTER))).getDuration() * 1000);
                } else if (attack.skill == Outlaw.FLAME_THROWER) {
                    ISkill flamethrower = SkillFactory.getSkill(Outlaw.FLAME_THROWER);
                    monster.setTempEffectiveness(Element.FIRE, ElementalEffectiveness.WEAK, SkillFactory.getSkill(Outlaw.FLAME_THROWER).getEffect(player.getSkillLevel(SkillFactory.getSkill(Outlaw.FLAME_THROWER))).getDuration() * 1000);
                    monster.applyStatus(player, new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, false), false, flamethrower.getEffect(player.getSkillLevel(flamethrower)).getDuration() * 1000); //TODO
                } else if (attack.skill == Aran.COMBO_TEMPEST) {
                    monster.setTempested(true);
                } else if (!monster.isBoss() && (attack.skill == NightLord.TAUNT || attack.skill == Shadower.TAUNT)) {
                    monster.setTauntMultiplier(player.getSkillLevel(SkillFactory.getSkill(attack.skill)) + 110);
                    MobSkill skill = MobSkillFactory.getMobSkill(102, player.getSkillLevel(SkillFactory.getSkill(attack.skill)) / 10);
                    if (skill != null) {
                        skill.applyEffect(player, monster, false);
                    }
                } else if (attack.skill == Outlaw.HOMING_BEACON || attack.skill == Corsair.BULLSEYE) {
                    player.setBeacon(monster.getObjectId());
                }
                if (player.getBuffedValue(MapleBuffStat.HAMSTRING) != null) {
                    ISkill hamstring = SkillFactory.getSkill(Bowmaster.HAMSTRING);
                    if (hamstring.getEffect(player.getSkillLevel(hamstring)).makeChanceResult()) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, hamstring.getEffect(player.getSkillLevel(hamstring)).getX()), hamstring, false);
                        monster.applyStatus(player, monsterStatusEffect, false, hamstring.getEffect(player.getSkillLevel(hamstring)).getY() * 1000);
                    }
                } else if (player.getBuffedValue(MapleBuffStat.BLIND) != null) {
                    ISkill blind = SkillFactory.getSkill(Marksman.BLIND);
                    if (blind.getEffect(player.getSkillLevel(blind)).makeChanceResult()) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.ACC, blind.getEffect(player.getSkillLevel(blind)).getX()), blind, false);
                        monster.applyStatus(player, monsterStatusEffect, false, blind.getEffect(player.getSkillLevel(blind)).getY() * 1000);
                    }
                }

                if (player.getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
                    player.addHP(Math.min(monster.getMaxHp(), Math.min((int) ((double) totDamage * (double) SkillFactory.getSkill(Aran.COMBO_DRAIN).getEffect(player.getSkillLevel(SkillFactory.getSkill(Aran.COMBO_DRAIN))).getX() / 100.0), player.getMaxHp() / 2)));
                }
                final int id = player.getJob();
                if (id == 121 || id == 122) {
                    for (int charge = 1211005; charge < 1211007; charge++) {
                        ISkill chargeSkill = SkillFactory.getSkill(charge);
                        if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, chargeSkill)) {
                            final ElementalEffectiveness iceEffectiveness = monster.getEffectiveness(Element.ICE);
                            if (totDamageToOneMonster > 0 && iceEffectiveness == ElementalEffectiveness.NORMAL || iceEffectiveness == ElementalEffectiveness.WEAK) {
                                monster.applyStatus(player, new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.FREEZE, 1), chargeSkill, false), false, chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 2000);
                            }
                            break;
                        }
                    }
                } else if (attack.skill != ChiefBandit.MESO_EXPLOSION && (id == 412 || id == 422 || id == 1411 || id == 434)) {
                    ISkill type = SkillFactory.getSkill(player.getJob() == 412 ? NightLord.VENOMOUS_STAR : (player.getJob() == 1411 ? NightWalker.VENOM : Shadower.VENOMOUS_STAB));
                    if (player.getSkillLevel(type) > 0) {
                        MapleStatEffect venomEffect = type.getEffect(player.getSkillLevel(type));
                        for (int i = 0; i < attackCount; i++) {
                            if (venomEffect.makeChanceResult()) {
                                if (monster.getVenomMulti() < 3) {
                                    monster.setVenomMulti((monster.getVenomMulti() + 1));
                                    MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), type, false);
                                    monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
                                }
                            }
                        }
                    }
                }
                if (totDamageToOneMonster > 0 && attackEffect != null && attackEffect.getMonsterStati().size() > 0) {
                    if (attackEffect.makeChanceResult()) {
                        monster.applyStatus(player, new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, false), attackEffect.isPoison(), attackEffect.getDuration());
                    }
                }
                if (attack.isHH && !monster.isBoss() && !(player.getMap().getId() >= 925020000 && player.getMap().getId() < 925030000)) {
                    map.damageMonster(player, monster, monster.getHp() - 1);
                } else if (attack.isHH) {
                    int HHDmg = (player.calculateMaxBaseDamageForHH(player.getTotalWatk()) * (SkillFactory.getSkill(Paladin.HEAVENS_HAMMER).getEffect(player.getSkillLevel(SkillFactory.getSkill(Paladin.HEAVENS_HAMMER))).getDamage() / 100));
                    map.damageMonster(player, monster, (int) (Math.floor(Math.random() * (HHDmg / 5) + HHDmg * .8)));
                } else {
                    map.damageMonster(player, monster, totDamageToOneMonster, attack.skill);
                }
            }
        }
    }

    protected static AttackInfo parseDamage(MapleClient c, LittleEndianAccessor lea, boolean ranged, boolean magic) {
        AttackInfo ret = new AttackInfo();
        lea.readByte();
        ret.numAttackedAndDamage = lea.readByte();
        ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
        ret.numDamage = ret.numAttackedAndDamage & 0xF;
        ret.allDamage = new ArrayList<Pair<Integer, List<Integer>>>();
        ret.skill = substituteSIDForCheck(lea.readInt());
        if (ret.skill == FPArchMage.BIG_BANG || ret.skill == ILArchMage.BIG_BANG || ret.skill == Bishop.BIG_BANG || ret.skill == Gunslinger.GRENADE || ret.skill == Brawler.CORKSCREW_BLOW || ret.skill == ThunderBreaker.CORKSCREW_BLOW || ret.skill == NightWalker.POISON_BOMB) {
            ret.charge = lea.readInt();
        } else {
            ret.charge = 0;
        }
        lea.skip(8);
        ret.display = lea.readByte();
        ret.v80thing = lea.readByte();
        ret.stance = lea.readByte();
        if (ret.skill == ChiefBandit.MESO_EXPLOSION) {
            if (ret.numAttackedAndDamage == 0) {
                lea.skip(10);
                for (int j = 0; j < lea.readByte(); j++) {
                    ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(lea.readInt()), null));
                    lea.skip(1);
                }
                return ret;
            } else {
                lea.skip(6);
            }
            for (int i = 0; i < ret.numAttacked + 1; i++) {
                int oid = lea.readInt();
                if (i < ret.numAttacked) {
                    lea.skip(12);
                    List<Integer> allDamageNumbers = new ArrayList<Integer>();
                    for (int j = 0; j < lea.readByte(); j++) {
                        int damage = lea.readInt();
                        allDamageNumbers.add(Integer.valueOf(damage));
                    }
                    ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(oid), allDamageNumbers));
                    lea.skip(4);
                } else {
                    for (int j = 0; j < lea.readByte(); j++) {
                        ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(lea.readInt()), null));
                        lea.skip(1);
                    }
                }
            }
            return ret;
        } else if (ret.skill % 10000000 == Beginner.BAMBOO_RAIN) {
            c.getPlayer().setDojoEnergy(0);
            c.announce(EffectFactory.getEnergy(0));
        } else if (ret.skill == Paladin.HEAVENS_HAMMER) {
            ret.isHH = true;
        }
        lea.readByte();
        ret.speed = lea.readByte();
        if (ranged) {
            lea.readByte();
            ret.direction = lea.readByte();
            lea.skip(7);
            if (ret.skill == Bowmaster.HURRICANE || ret.skill == Marksman.PIERCING_ARROW || ret.skill == Corsair.RAPID_FIRE || ret.skill == WindArcher.HURRICANE) {
                lea.skip(4);
            }
        } else {
            lea.skip(4);//was 4 pre-v88
        }
        for (int i = 0; i < ret.numAttacked; i++) {
            int oid = lea.readInt();
            final MapleMonster mob = c.getPlayer().getMap().getMonsterByOid(oid);
            lea.skip(14);
            List<Integer> allDamageNumbers = new ArrayList<Integer>();
            int totalDamage = 0;
            for (int j = 0; j < ret.numDamage; j++) {
                int damage = lea.readInt();
                if (damage > 199999 && ret.skill % 10000000 != Beginner.BAMBOO_RAIN && ret.skill != Aran.COMBO_TEMPEST && ret.skill != Aran.COMBO_PENRIL && ret.skill != Shadower.ASSASSINATE && ret.skill != Marksman.PIERCING_ARROW && !mob.getTempested() && c.getPlayer().gmLevel() == 0 && c.getPlayer().getReborns() < 2) {
                    c.getPlayer().ban(MapleCharacter.makeMapleReadable(c.getPlayer().getName()) + " did insane single damage : " + damage + " at level " + c.getPlayer().getLevel() + " using skill " + ret.skill, true);
                    return null;
                }
                totalDamage += damage;
                if (ret.skill == Marksman.SNIPE) {
                    damage += 0x80000000;
                }
                allDamageNumbers.add(Integer.valueOf(damage));
            }
            if(totalDamage == c.getPlayer().getSavedDamage() && totalDamage > 10 && totalDamage < 99999) {
                c.getPlayer().setSameDamageCounter(c.getPlayer().getSameDamageCounter() + 1);
                if(c.getPlayer().getSameDamageCounter() >= ((ret.skill == 1000 || ret.skill == 10001000 || ret.skill == 20001000) ? 150 : 10) && !mob.getTempested()) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.broadcastGMPacket
                        (EffectFactory.serverNotice(6, MapleCharacter.makeMapleReadable
                        (c.getPlayer().getName()) + " did same single damage : " + totalDamage + " at level " + c.getPlayer().getLevel() + " using skill " + ret.skill));
                    }
                }
            } else {
                c.getPlayer().setSameDamageCounter(0);
            }
            c.getPlayer().setSavedDamage(totalDamage);

            if(mob.getAvoid() > 0) {
                int requiredAccuracy = (55 + 2 * (mob.getLevel() > c.getPlayer().getLevel() ? mob.getLevel() - c.getPlayer().getLevel() : 0)) * mob.getAvoid() / 15;
                if (totalDamage > 0 && ((c.getPlayer().getTotalAcc() < requiredAccuracy && mob.getLevel() - c.getPlayer().getLevel() > 25 && c.getPlayer().getStr() > c.getPlayer().getDex() && c.getPlayer().getStr() > c.getPlayer().getLuk()))) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.broadcastGMPacket
                        (EffectFactory.serverNotice(6, MapleCharacter.makeMapleReadable
                        (c.getPlayer().getName()) + " hit damage : " + totalDamage + " at level " + c.getPlayer().getLevel() + " using skill " + ret.skill + " to " + mob.getName() + " with " + c.getPlayer().getTotalAcc() + " accuracy, " + c.getPlayer().getDex() + " DEX stat. Required accuracy is " + requiredAccuracy));
                    }
                }
            }

            if (totalDamage > c.getPlayer().getLevel() * 1200 * (c.getPlayer().getReborns() < 4 ? 1 : 2) && c.getPlayer().gmLevel() == 0 && !mob.getTempested()) {
                List<MapleStatEffect> lmse = c.getPlayer().getBuffEffects();
                boolean isSuperPotted = false;
                for (MapleStatEffect mse : lmse) {
                    if (!mse.isSkill()) {
                        switch (mse.getSourceId()) {
                        case 2022245: //heartstopper
                        case 2012008: //unripe onyx apple
                        case 2022179: //onyx apple
                        case 2022282: //naricain's demon elixir
                        case 2022121: //gelt
                        case 2022123: //pie
                            isSuperPotted = true;
                            break;
                        }
                    }
                    if (isSuperPotted) {
                        break;
                    }
                }
                if (!isSuperPotted) {
                    boolean report = true;
                    for (int s : skills) {
                        if (ret.skill == s) {
                            report = false;
                            break;
                        }
                    }
                    if (report && ret.skill % 10000000 != Beginner.BAMBOO_RAIN && (c.getPlayer().getMapId() < 914000000 || c.getPlayer().getMapId() > 914000500) && c.getPlayer().getReborns() < 10) {
                        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                            cserv.broadcastGMPacket
                            (EffectFactory.serverNotice(6, MapleCharacter.makeMapleReadable
                            (c.getPlayer().getName()) + " (Level " + c.getPlayer().getLevel() + ") did " +
                            totalDamage + " to " + mob.getName() + " using skill " + ret.skill));
                        }
                    }
                }
            }
            if (ret.skill != Corsair.RAPID_FIRE) {
                lea.skip(4);
            }
            ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(oid), allDamageNumbers));
        }
        if (ret.skill == NightWalker.POISON_BOMB) { //TODO
            lea.skip(4);
            ret.xCoord = lea.readShort();
            ret.yCoord = lea.readShort();
        }
        return ret;
    }

    private static boolean ignoreDMGCheck(AttackInfo attack, MapleCharacter chr) {
        if(chr.getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
            return true;
        }
        if (chr.getJob() / 10 == 43 && attack.skill == 0) {
            return true;
        }

        switch(attack.skill) {
            case ChiefBandit.MESO_EXPLOSION:
            case NightWalker.VAMPIRE:
            case WindArcher.WIND_SHOT:
            case Aran.COMBO_PENRIL:
            case Aran.COMBO_TEMPEST:
            case 21110007:
            case 21110008:
            case 21120009:
            case 21120010:
            case 21110002:
            case 21120002:
                return true;
            default:
                return false;
        }
    }

    private static int substituteSIDForCheck(int skillID) {
        int newID = 0;
        switch(skillID) {
            case 21110007:
            case 21110008:
                newID = 21110002;
                break;
            case 21120009:
            case 21120010:
                newID = 21120002;
                break;
            default:
                newID = skillID;
        }
        return newID;
    }

    private static final boolean isFinisher(int skillId) {
        return skillId > 1111002 && skillId < 1111007 || skillId == 11111002 || skillId == 11111003;
    }

    public static final void CloseRangeDamageHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        AttackInfo attack = parseDamage(c, slea, false, false);
        if (attack.skill == 0) {
            if (c.getPlayer().getJob() > 2100) {
                int fswing = c.getPlayer().getSkillLevel(Aran.OVER_SWING) > 0 ? c.getPlayer().getSkillLevel(Aran.OVER_SWING) : c.getPlayer().getSkillLevel(Aran.FULL_SWING);
                if (c.getPlayer().getJob() % 10 == 1) {
                    c.announce(BuffFactory.updateSkill(Aran.DOUBLE_SWING2, fswing, 20));
                    c.announce(BuffFactory.updateSkill(Aran.TRIPLE_SWING2, fswing, 20));
                } else {
                    c.announce(BuffFactory.updateSkill(Aran.DOUBLE_SWING3, fswing, 20));
                    c.announce(BuffFactory.updateSkill(Aran.TRIPLE_SWING3, fswing, 20));
                }
            }
        } else if (attack.skill == Aran.TRIPLE_SWING2 || attack.skill == Aran.TRIPLE_SWING3) {
            c.announce(BuffFactory.updateSkill(Aran.DOUBLE_SWING2, 0, 0));
            c.announce(BuffFactory.updateSkill(Aran.TRIPLE_SWING2, 0, 0));
            c.announce(BuffFactory.updateSkill(Aran.DOUBLE_SWING3, 0, 0));
            c.announce(BuffFactory.updateSkill(Aran.TRIPLE_SWING3, 0, 0));
        } else if (attack.skill >= Aran.TUTORIAL1 && attack.skill <= Aran.TUTORIAL5) {
            if (c.getPlayer().getMapId() < 91400000 || c.getPlayer().getMapId() > 914000500) {
                for (int i = Aran.TUTORIAL1; i <= Aran.TUTORIAL5; i++) {
                    c.getPlayer().changeSkillLevel(SkillFactory.getSkill(i), 0, 0);
                }
                return;
            }
        }
        player.getMap().broadcastMessage(player, MobFactory.closeRangeAttack(attack, player.getId(), attack.allDamage), false, true);
        int numFinisherOrbs = 0;
        Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO);
        if (isFinisher(attack.skill)) {
            if (comboBuff != null) {
                numFinisherOrbs = comboBuff.intValue() - 1;
            }
            player.handleOrbConsume();
        } else if (attack.numAttacked > 0) {
            if (attack.skill != 1111008 && comboBuff != null) {
                int orbcount = player.getBuffedValue(MapleBuffStat.COMBO);
                int oid = player.isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
                int advcomboid = player.isCygnus() ? DawnWarrior.ADVANCED_COMBO : Hero.ADVANCED_COMBO;
                ISkill combo = SkillFactory.getSkill(oid);
                ISkill advcombo = SkillFactory.getSkill(advcomboid);
                MapleStatEffect ceffect = null;
                int advComboSkillLevel = player.getSkillLevel(advcombo);
                if (advComboSkillLevel > 0) {
                    ceffect = advcombo.getEffect(advComboSkillLevel);
                } else {
                    ceffect = combo.getEffect(player.getSkillLevel(combo));
                }
                if (orbcount < ceffect.getX() + 1) {
                    int neworbcount = orbcount + 1;
                    if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
                        if (neworbcount <= ceffect.getX()) {
                            neworbcount++;
                        }
                    }
                    List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO, neworbcount));
                    player.setBuffedValue(MapleBuffStat.COMBO, neworbcount);
                    int duration = ceffect.getDuration();
                    duration += (int) ((player.getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()));
                    c.announce(BuffFactory.giveBuff(oid, duration, stat, false));
                    player.getMap().broadcastMessage(player, BuffFactory.giveForeignBuff(player.getId(), stat, false), false);
                }
            } else if (player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(15100004) : SkillFactory.getSkill(5110001)) > 0 && (player.isA(Marauder.ID) || player.isA(ThunderBreaker.THUNDER_BREAKER2))) {
                for (int i = 0; i < attack.numAttacked; i++) {
                    player.handleEnergyChargeGain();
                }
            }
        }
        if (attack.numAttacked > 0 && attack.skill == DragonKnight.SACRIFICE) {
            int totDamageToOneMonster = attack.allDamage.get(0).getRight().get(0).intValue(); // sacrifice attacks only 1 mob with 1 attack
            int remainingHP = player.getHp() - totDamageToOneMonster * attack.getAttackEffect(player, null).getX() / 100;
            player.setHp(remainingHP > 1 ? remainingHP : 1);
            player.updateSingleStat(MapleStat.HP, player.getHp());
            player.checkBerserk();
        }
        if (attack.numAttacked > 0 && attack.skill == 1211002) {
            boolean advcharge_prob = false;
            int advcharge_level = player.getSkillLevel(SkillFactory.getSkill(1220010));
            if (advcharge_level > 0) {
                advcharge_prob = SkillFactory.getSkill(1220010).getEffect(advcharge_level).makeChanceResult();
            }
            if (!advcharge_prob) {
                player.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            }
        }
        int attackCount = 1;
        if (attack.skill != 0) {
            attackCount = attack.getAttackEffect(player, null).getAttackCount();
        }
        if (numFinisherOrbs == 0 && isFinisher(attack.skill)) {
            return;
        }
        if (attack.skill > 0) {
            ISkill skill = SkillFactory.getSkill(attack.skill);
            MapleStatEffect effect_ = skill.getEffect(player.getSkillLevel(skill));
            if (effect_.getCooldown() > 0) {
                if (player.skillisCooling(attack.skill)) {
                    return;
                } else {
                    c.announce(BuffFactory.skillCooldown(attack.skill, effect_.getCooldown()));
                    player.addCooldown(attack.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(player, attack.skill), effect_.getCooldown() * 1000));
                }
            }
        }
        if ((player.getSkillLevel(SkillFactory.getSkill(NightWalker.VANISH)) > 0 || player.getSkillLevel(SkillFactory.getSkill(WindArcher.WIND_WALK)) > 0) && player.getBuffedValue(MapleBuffStat.DARKSIGHT) != null && attack.numAttacked > 0 && player.getBuffSource(MapleBuffStat.DARKSIGHT) != 9101004) {
            player.cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
            player.cancelBuffStats(MapleBuffStat.DARKSIGHT);
        }
        applyAttack(attack, player, attackCount);
    }

    public static final void RangedAttackHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        AttackInfo attack = parseDamage(c, slea, true, false);
        MapleCharacter player = c.getPlayer();
        if ((attack.skill == Buccaneer.ENERGY_ORB) || (attack.skill == Aran.COMBO_SMASH) || (attack.skill == Aran.COMBO_PENRIL)) { //energy orb / aran skills
            player.getMap().broadcastMessage(player, MobFactory.rangedAttack(attack, player.getId(), 0, attack.allDamage, true), false);
            applyAttack(attack, player, 1);
        } else {
            IItem weapon = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            MapleWeaponType type = MapleItemInformationProvider.getInstance().getWeaponType(weapon.getId());
            if (type == MapleWeaponType.NOT_A_WEAPON) {
                return;
            }
            int projectile = 0;
            int bulletCount = 1;
            MapleStatEffect effect = null;
            if (attack.skill != 0) {
                effect = attack.getAttackEffect(player, null);
                bulletCount = effect.getBulletCount();
                if (effect.getCooldown() > 0) {
                    c.announce(BuffFactory.skillCooldown(attack.skill, effect.getCooldown()));
                }
            }
            boolean hasShadowPartner = player.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null;
            if (hasShadowPartner) {
                bulletCount *= 2;
            }
            for (int i = 0; i < 100; i++) {
                IItem item = player.getInventory(MapleInventoryType.USE).getItem((byte) i);
                if (item != null) {
                    boolean bow = item.getId() / 1000 == 2060;
                    boolean cbow = item.getId() / 1000 == 2061;
                    if (((type == MapleWeaponType.CLAW && item.getId() / 10000 == 207) || type == MapleWeaponType.BOW && bow || type == MapleWeaponType.CROSSBOW && cbow || (type == MapleWeaponType.GUN && item.getId() / 10000 == 233)) && item.getQuantity() >= bulletCount) {
                        projectile = item.getId();
                        break;
                    }
                }
            }
            boolean soulArrow = player.getBuffedValue(MapleBuffStat.SOULARROW) != null;
            boolean shadowClaw = player.getBuffedValue(MapleBuffStat.SHADOW_CLAW) != null;
            boolean special = attack.skill == Corsair.BULLSEYE || attack.skill == Outlaw.HOMING_BEACON || attack.skill == ThunderBreaker.SPARK || attack.skill == 11101004 || attack.skill == 15111007 || attack.skill == 14101006 || attack.skill == Aran.COMBO_PENRIL || attack.skill == Aran.COMBO_SMASH || attack.skill == Aran.COMBO_TEMPEST;
            if ((!soulArrow && !shadowClaw && !special) || (shadowClaw && projectile != player.getProjectile())) {
                int bulletConsume = bulletCount;
                if (effect != null && effect.getBulletConsume() != 0) {
                    bulletConsume = effect.getBulletConsume() * (hasShadowPartner ? 2 : 1);
                }
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile, bulletConsume, false, true);
            }
            if (projectile != 0 || soulArrow || special) {
                int visProjectile = projectile; //visible projectile sent to players
                if (soulArrow || attack.skill == 3111004 || attack.skill == 3211004 || special) {
                    visProjectile = 0;
                } else if (projectile / 10000 == 207) {
                    for (int i = 0; i < 100; i++) { // impose order...
                        IItem item = player.getInventory(MapleInventoryType.CASH).getItem((byte) i);
                        if (item != null) {
                            if (item.getId() / 1000 == 5021) {
                                visProjectile = item.getId();
                                break;
                            }
                        }
                    }
                }
                MaplePacket packet;
                switch (attack.skill) {
                    case 3121004: // Hurricane
                    case 3221001: // Pierce
                    case 5221004: // Rapid Fire
                    case 13111002: // KoC Hurricane
                        packet = MobFactory.rangedAttack(attack, player.getId(), visProjectile, attack.allDamage, false);
                        break;
                    default:
                        packet = MobFactory.rangedAttack(attack, player.getId(), visProjectile, attack.allDamage, true);
                        break;
                }
                player.getMap().broadcastMessage(player, packet, false, true);
                if (effect != null) {
                    int money = effect.getMoneyCon();
                    if (money != 0) {
                        int moneyMod = money / 2;
                        money += Randomizer.getInstance().nextInt(moneyMod);
                        if (money > player.getMeso()) {
                            money = player.getMeso();
                        }
                        player.gainMeso(-money, false);
                    }
                }
                if (attack.skill != 0) {
                    ISkill skill = SkillFactory.getSkill(attack.skill);
                    MapleStatEffect effect_ = skill.getEffect(player.getSkillLevel(skill));
                    if (effect_.getCooldown() > 0) {
                        if (player.skillisCooling(attack.skill)) {
                            return;
                        } else {
                            c.announce(BuffFactory.skillCooldown(attack.skill, effect_.getCooldown()));
                            player.addCooldown(attack.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(player, attack.skill), effect_.getCooldown() * 1000));
                        }
                    }
                }
                if ((player.getSkillLevel(SkillFactory.getSkill(NightWalker.VANISH)) > 0 || player.getSkillLevel(SkillFactory.getSkill(WindArcher.WIND_WALK)) > 0) && player.getBuffedValue(MapleBuffStat.DARKSIGHT) != null && attack.numAttacked > 0 && player.getBuffSource(MapleBuffStat.DARKSIGHT) != 9101004) {
                    player.cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                    player.cancelBuffStats(MapleBuffStat.DARKSIGHT);
                }
                applyAttack(attack, player, bulletCount);
                if (effect != null && attack != null && effect.isHomingBeacon() && attack.allDamage.size() > 0) {
                    effect.applyTo(player);
                }
            }
        }
    }

    public static final void MagicDamageHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        AttackInfo attack = parseDamage(c, slea, false, true);
        MapleCharacter player = c.getPlayer();
        MaplePacket packet = MobFactory.magicAttack(attack, player.getId(), attack.allDamage, -1);
        if (attack.skill == 2121001 || attack.skill == 2221001 || attack.skill == 2321001) {
            packet = MobFactory.magicAttack(attack, player.getId(), attack.allDamage, attack.charge);
        }
        player.getMap().broadcastMessage(player, packet, false, true);
        MapleStatEffect effect = attack.getAttackEffect(player, null);
        ISkill skill = SkillFactory.getSkill(attack.skill);
        MapleStatEffect effect_ = skill.getEffect(player.getSkillLevel(skill));
        if (effect_.getCooldown() > 0) {
            if (player.skillisCooling(attack.skill)) {
                return;
            } else {
                c.announce(BuffFactory.skillCooldown(attack.skill, effect_.getCooldown()));
                player.addCooldown(attack.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(player, attack.skill), effect_.getCooldown() * 1000));
            }
        }
        applyAttack(attack, player, effect.getAttackCount());
        ISkill eaterSkill = SkillFactory.getSkill((player.getJob() - (player.getJob() % 10)) * 10000);// MP Eater, works with right job
        int eaterLevel = player.getSkillLevel(eaterSkill);
        if (eaterLevel > 0) {
            for (Pair<Integer, List<Integer>> singleDamage : attack.allDamage) {
                eaterSkill.getEffect(eaterLevel).applyPassive(player, player.getMap().getMapObject(singleDamage.getLeft()), 0);
            }
        }
    }

    public static final void TakeDamageHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        slea.readInt(); //timestamp
        int damagefrom = slea.readByte();
        slea.readByte(); //element
        int damage = slea.readInt();
        int oid = 0;
        int monsteridfrom = 0;
        int direction = 0;
        int mpattack = 0;
        MapleMonster attacker = null;
        if (damagefrom != -3) {
            monsteridfrom = slea.readInt();
            oid = slea.readInt();
            attacker = (MapleMonster) player.getMap().getMapObject(oid);
            if ((player.getMap().getMonsterById(monsteridfrom) == null || attacker == null) && monsteridfrom != 9300166) {
                return;
            } else if (monsteridfrom == 9300166) {
                if (player.haveItem(4031868)) {
                    if (player.getItemQuantity(4031868, false) > 1) {
                        int amount = player.getItemQuantity(4031868, false) / 2;
                        Point position = new Point(c.getPlayer().getPosition().x, c.getPlayer().getPosition().y);
                        MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(4031868), 4031868, amount, false, false);
                        for (int i = 0; i < amount; i++) {
                            position.setLocation(c.getPlayer().getPosition().x + (i % 2 == 0 ? (i * 15) : (-i * 15)), c.getPlayer().getPosition().y);
                            c.getPlayer().getMap().spawnItemDrop(c.getPlayer().getObjectId(), c.getPlayer().getPosition(), c.getPlayer(), new Item(4031868, (byte) 0, (short) 1), position, true, true);
                        }
                    } else {
                        MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(4031868), 4031868, 1, false, false);
                        c.getPlayer().getMap().spawnItemDrop(c.getPlayer().getObjectId(), c.getPlayer().getPosition(), c.getPlayer(), new Item(4031868, (byte) 0, (short) 1), c.getPlayer().getPosition(), true, true);
                    }
                }
            }
            direction = slea.readByte();
        }
        if (damagefrom != -1 && damagefrom != -2 && attacker != null) {
            MobAttackInfo attackInfo = MobAttackInfoFactory.getInstance().getMobAttackInfo(attacker, damagefrom);
            if (attackInfo.isDeadlyAttack()) {
                mpattack = player.getMp() - 1;
            }
            mpattack += attackInfo.getMpBurn();
            MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
            if (skill != null && damage > 0) {
                skill.applyEffect(player, attacker, false);
            }
            if (attacker != null) {
                attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
                if (player.getBuffedValue(MapleBuffStat.MANA_REFLECTION) != null && damage > 0 && !attacker.isBoss()) {
                    int jobid = player.getJob();
                    if (jobid == 212 || jobid == 222 || jobid == 232) {
                        int id = jobid * 10000 + 2;
                        ISkill manaReflectSkill = SkillFactory.getSkill(id);
                        if (player.isBuffFrom(MapleBuffStat.MANA_REFLECTION, manaReflectSkill) && player.getSkillLevel(manaReflectSkill) > 0 && manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill)).makeChanceResult()) {
                            int bouncedamage = (damage * manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill)).getX() / 100);
                            if (bouncedamage > attacker.getMaxHp() / 5) {
                                bouncedamage = attacker.getMaxHp() / 5;
                            }
                            player.getMap().damageMonster(player, attacker, bouncedamage);
                            player.getMap().broadcastMessage(player, MobFactory.damageMonster(oid, bouncedamage), true);
                            player.getClient().announce(BuffFactory.showOwnBuffEffect(id, 5));
                            player.getMap().broadcastMessage(player, BuffFactory.showBuffEffect(player, player.getId(), id, 5), false);
                        }
                    }
                }
            }
        }
        if (!player.isHidden()) {
            if (attacker != null && player.getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null && damagefrom == -1) {
                int max = player.calculateMaxBaseDamageForHH(player.getTotalWatk());
                int damaged = Randomizer.getInstance().nextInt(max * 2 / 5) + max * 3 / 5;
                if (damage > 0) {
                    player.getMap().damageMonster(player, attacker, damaged);
                    player.getMap().broadcastMessage(player, MobFactory.damageMonster(oid, damaged), true, true);
                }
                player.checkMonsterAggro(attacker);
                player.addMPHP(-damage, -mpattack);
            } else if (damage > 0) {
                if (attacker != null) {
                    if (damagefrom == -1 && player.getBuffedValue(MapleBuffStat.POWERGUARD) != null) {
                        slea.readShort();
                        byte physical = slea.readByte();
                        int bouncedamage = (int) (damage * player.getBuffedValue(MapleBuffStat.POWERGUARD).doubleValue() / 100);
                        if (physical == 1) {
                            damage -= bouncedamage;
                        }
                        if (!player.isAran()) {
                            player.getMap().damageMonster(player, attacker, bouncedamage / 2); // weird
                            player.checkMonsterAggro(attacker);
                        }
                    }
                    int jobid = player.getJob();
                    if ((jobid < 200 && jobid % 10 == 2) || jobid == 2112) {
                        int achilles1 = Hero.ACHILLES;
                        if (jobid == 122) {
                            achilles1 = Paladin.ACHILLES;
                        } else if (jobid == 132) {
                            achilles1 = DarkKnight.ACHILLES;
                        } else {
                            achilles1 = Aran.HIGH_DEFENSE;
                        }
                        ISkill ach = SkillFactory.getSkill(achilles1);
                        int achilles = player.getSkillLevel(ach);
                        if (achilles != 0 && ach != null) {
                            damage = damage * ach.getEffect(achilles).getX() / 1000;
                        }
                    }
                }
                if (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null && mpattack == 0) {
                    int mploss = (int) (damage * (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0));
                    int hploss = damage - mploss;
                    if (mploss > player.getMp()) {
                        hploss += mploss - player.getMp();
                        mploss = player.getMp();
                    }
                    player.addMPHP(-hploss, -mploss);
                } else if (player.getBuffedValue(MapleBuffStat.MESOGUARD) != null) {
                    damage = Math.round(damage / 2);
                    int mesoloss = (int) (damage * (player.getBuffedValue(MapleBuffStat.MESOGUARD).doubleValue() / 100.0));
                    if (player.getMeso() < mesoloss) {
                        player.gainMeso(-player.getMeso(), false);
                        player.cancelBuffStats(MapleBuffStat.MESOGUARD);
                    } else {
                        player.gainMeso(-mesoloss, false);
                    }
                    player.addMPHP(-damage, -mpattack);
                } else if (player.getBuffedValue(MapleBuffStat.COMBO_BARRIER) != null) {
                    final ISkill skill = SkillFactory.getSkill(Aran.COMBO_BARRIER);
                    int barrier = player.getSkillLevel(skill);
                    if (barrier != 0) {
                        damage = damage * skill.getEffect(barrier).getX() / 1000;
                    }
                    player.addMPHP(-damage, -mpattack);
                } else if (player.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
                    if (player.getBuffedValue(MapleBuffStat.MONSTER_RIDING).intValue() == Corsair.BATTLE_SHIP) {
                        player.decreaseBattleshipHp(damage);
                    } else {
                        player.addMPHP(-damage, -mpattack);
                    }
                } else {
                    player.addMPHP(-damage, -mpattack);
                }
                if (player.getMap().getId() >= 925020000 && player.getMap().getId() < 925030000) { // in GMS it goes up by amount of damage done
                    player.setDojoEnergy(player.getDojoEnergy() < 10000 ? player.getDojoEnergy() + damage : 0); // something like this?
                    player.getClient().announce(EffectFactory.getEnergy(player.getDojoEnergy()));
                }
            }
            player.getMap().broadcastMessage(player, MobFactory.damagePlayer(damagefrom, monsteridfrom, player.getId(), damage, damage == -1 ? 4020002 + (player.getJob() / 10 - 40) * 100000 : 0, direction, false, 0, true, oid, 0, 0), false);
            player.updateSingleStat(MapleStat.HP, player.getHp());
            player.updateSingleStat(MapleStat.MP, player.getMp());
            player.checkBerserk();
        }
    }

    public static final void SummonDamageHandler(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        int oid = slea.readInt();
        final MapleCharacter player = c.getPlayer();
        if (!player.isAlive()) {
            return;
        }
        MapleSummon summon = null;

        for (MapleSummon sum : player.getSummons().values()) {
            if (sum.getObjectId() == oid) {
                summon = sum;
            }
        }
        if (summon == null || summon.getObjectId() != oid) {
            return;
        }

        ISkill summonSkill = SkillFactory.getSkill(summon.getSkill());
        MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
        slea.skip(5); // ticks, animation
        final List<SummonAttackEntry> allDamage = new ArrayList<SummonAttackEntry>();
        final int numAttacked = slea.readByte();
        slea.readLong();
        for (int x = 0; x < numAttacked; x++) {
            int monsterOid = slea.readInt(); // attacked oid
            slea.skip(18); // who knows
            final int damage = slea.readInt();
            if (damage >= 80000 && !c.getPlayer().isGM()) {
                if(c.getPlayer().getReborns() < 3 && c.getPlayer().gmLevel() == 0)
                    c.getPlayer().ban(c.getPlayer().getName() + " did insane summon damage : " + damage + " at level " + c.getPlayer().getLevel(), true);
                else
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.broadcastGMPacket(EffectFactory.serverNotice(6, MapleCharacter.makeMapleReadable(c.getPlayer().getName()) + " did insane summon damage : " + damage + " at level " + c.getPlayer().getLevel()));
                    }
                return;
            }
            allDamage.add(new SummonAttackEntry(monsterOid, damage));
        }
        player.getMap().broadcastMessage(player, SummonFactory.summonAttack(player.getId(), summon.getSkill(), 4, allDamage), summon.getPosition());
        for (SummonAttackEntry attackEntry : allDamage) {
            int damage = attackEntry.getDamage();
            MapleMonster target = player.getMap().getMonsterByOid(attackEntry.getMonsterOid());
            if (target != null) {
                if (damage > 0 && summonEffect.getMonsterStati().size() > 0) {
                    if (summonEffect.makeChanceResult()) {
                        target.applyStatus(player, new MonsterStatusEffect(summonEffect.getMonsterStati(), summonSkill, false), summonEffect.isPoison(), 4000);
                    }
                }
                player.getMap().damageMonster(player, target, damage);
            }
        }
        if(summon.getSkill() == Outlaw.GAVIOTA) {
            TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    player.cancelEffectFromBuffStat(MapleBuffStat.SUMMON);
                }
            }, 2000);
        }
    }

    public static final void AutoAggroHandler(int oid, MapleClient c) {
        MapleMonster monster = c.getPlayer().getMap().getMonsterByOid(oid);
        if (monster != null && monster.getController() != null) {
            if (!monster.isControllerHasAggro()) {
                if (c.getPlayer().getMap().getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(c.getPlayer(), true);
                } else {
                    monster.switchController(monster.getController(), true);
                }
            } else if (c.getPlayer().getMap().getCharacterById(monster.getController().getId()) == null) {
                monster.switchController(c.getPlayer(), true);
            }
        } else if (monster != null && monster.getController() == null) {
            monster.switchController(c.getPlayer(), true);
        }
    }

    public static final void EnergyOrbDamageHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getEnergyBar() == 15000 || c.getPlayer().getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null) {
            applyAttack(parseDamage(c, slea, false, false), c.getPlayer(), 1);
        }
    }

    public static final void MobDamageMobHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int from = slea.readInt();
        slea.readInt();
        int to = slea.readInt();
        slea.readByte();
        int dmg = slea.readInt();
        MapleMap map = c.getPlayer().getMap();
        if (map.getMonsterByOid(from) != null && map.getMonsterByOid(to) != null) {
            map.damageMonster(c.getPlayer(), map.getMonsterByOid(to), dmg);
        }
    }

    public static final void MobDamageMobFriendlyHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int attacker = slea.readInt();
        slea.readInt();
        int damaged = slea.readInt();
        MapleMap map = c.getPlayer().getMap();
        MapleMonster mob = map.getMonsterByOid(damaged);
        int damage = Randomizer.getInstance().nextInt(((c.getPlayer().getMap().getMonsterByOid(damaged).getMaxHp() / 13 + c.getPlayer().getMap().getMonsterByOid(attacker).getPADamage() * 10)) * 2 + 500);
        //Beng's formula.
        if (c.getPlayer().getMap().getMonsterByOid(damaged) == null || c.getPlayer().getMap().getMonsterByOid(attacker) == null) {
            return;
        }
        c.getPlayer().getMap().broadcastMessage(MobFactory.mobDamageMobFriendly(mob, damage), mob.getPosition());
        map.damageMonster(c.getPlayer(), mob, damage);
        c.announce(IntraPersonalFactory.enableActions());
    }

    public static final void UsePoisonBombHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int x = slea.readInt();
        int y = slea.readInt();
        int chargetime = slea.readShort(); // time in millis, you can actually read int
        slea.skip(2); // 0
        if (slea.readInt() != NightWalker.POISON_BOMB) {// this is also for pirate grenade or something, but that doesn't have mist?
            return;
        }
        int left = c.getPlayer().isFacingLeft() ? -1 : 1;
        int level = c.getPlayer().getSkillLevel(SkillFactory.getSkill(NightWalker.POISON_BOMB));
        Point newp = null;
        // equation is something like -x^2/360+x for fully charged projectile, but i'm not doing rectline thing again.
        try {
            newp = c.getPlayer().getMap().getGroundBelow(new Point(x + left * chargetime / 3, y - 30));
        } catch (NullPointerException e) {
            newp = c.getPlayer().getPosition(); // if it's a wall, too lazy to do the wall checks
        }
        c.getPlayer().getMap().spawnMist(new MapleMist(calculateBoundingBox(newp), c.getPlayer(), SkillFactory.getSkill(NightWalker.POISON_BOMB).getEffect(level)), (int) (4 * (Math.ceil(level / 3))) * 1000, true, false);
    }

    private static Rectangle calculateBoundingBox(Point posFrom) {
        Point mylt = new Point(-100 + posFrom.x, -82 + posFrom.y);
        Point myrb = new Point(100 + posFrom.x, 83 + posFrom.y);
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }

    public static final void ComboCounterHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getSkillLevel(SkillFactory.getSkill(Aran.COMBO_ABILITY)) > 0 || c.getPlayer().getSkillLevel(SkillFactory.getSkill(Aran.TUTORIAL4)) > 0) {
            if (c.getPlayer().getCombo() < 30001) {
                c.getPlayer().setCombo(c.getPlayer().getCombo() + 1); // change for easier testing
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
                mplew.writeShort(SendPacketOpcode.ARAN_COMBO_COUNTER);
                mplew.writeInt(c.getPlayer().getCombo());
                c.announce(mplew.getPacket());
            }
            if (c.getPlayer().getCombo() % 10 == 0 && c.getPlayer().getCombo() < 101) {
                c.announce(BuffFactory.addComboBuff(c.getPlayer().getCombo()));
            }
        }
    }
}
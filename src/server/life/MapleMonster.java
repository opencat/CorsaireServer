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

package server.life;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.skills.Bowmaster;
import constants.skills.Corsair;
import constants.skills.FPArchMage;
import constants.skills.FPMage;
import constants.skills.Hermit;
import constants.skills.ILArchMage;
import constants.skills.ILMage;
import constants.skills.NightLord;
import constants.skills.NightWalker;
import constants.skills.Shadower;
import constants.skills.WindArcher;
import net.MaplePacket;
import net.channel.ChannelServer;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import scripting.event.EventInstanceManager;
import server.TimerManager;
import server.life.MapleLifeFactory.BanishInfo;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.Pair;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantLock;
import tools.factory.MobFactory;

/**
 * @name        MapleMonster
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleMonster extends AbstractLoadedMapleLife {
    private MapleMonsterStats stats;
    private MapleMonsterStats overrideStats;
    private int hp;
    private int mp;
    private WeakReference<MapleCharacter> controller = new WeakReference<MapleCharacter>(null);
    private boolean controllerHasAggro, controllerKnowsAboutAggro;
    private Collection<AttackerEntry> attackers = new LinkedList<AttackerEntry>();
    private EventInstanceManager eventInstance = null;
    private Collection<MonsterListener> listeners = new LinkedList<MonsterListener>();
    private MapleCharacter highestDamageChar;
    private Map<MonsterStatus, MonsterStatusEffect> stati = new LinkedHashMap<MonsterStatus, MonsterStatusEffect>();
    private List<MonsterStatusEffect> activeEffects = new ArrayList<MonsterStatusEffect>();
    private MapleMap map;
    private int VenomMultiplier = 0;
    private int TauntMultiplier = 0;
    private boolean fake = false;
    private boolean tempested = false;
    private boolean dropsDisabled = false;
    private boolean raided = false;
    private List<Pair<Integer, Integer>> usedSkills = new ArrayList<Pair<Integer, Integer>>();
    private Map<Pair<Integer, Integer>, Integer> skillsUsed = new HashMap<Pair<Integer, Integer>, Integer>();
    private List<MonsterStatus> monsterBuffs = new ArrayList<MonsterStatus>();
    public ReentrantLock monsterLock = new ReentrantLock();

    public MapleMonster(final int id, final MapleMonsterStats stats) {
        super(id);
        initWithStats(stats);
    }

    public MapleMonster(final MapleMonster monster) {
        super(monster);
        initWithStats(monster.stats);
    }

    private final void initWithStats(final MapleMonsterStats stats) {
        setStance(5);
        this.stats = stats;
        hp = stats.getHp();
        mp = stats.getMp();
    }

    public final void disableDrops() {
        this.dropsDisabled = true;
    }

    public final boolean dropsDisabled() {
        return dropsDisabled;
    }

    public final void setMap(final MapleMap map) {
        this.map = map;
    }

    public final boolean getTempested() {
        return tempested;
    }

    public final void setTempested(final boolean tempested) {
        this.tempested = tempested;
    }

    public final boolean getRaided() {
        return raided;
    }

    public final void setRaided(final boolean raided) {
        this.raided = raided;
    }

    public final int getHp() {
        return hp;
    }

    public final void setHp(final int hp) {
        this.hp = hp;
    }

    public final int getMaxHp() {
        if (overrideStats != null) {
            return overrideStats.getHp();
        }
        return stats.getHp();
    }

    public final int getMp() {
        return mp;
    }

    public final void setMp(final int mp) {
        this.mp = mp < 0 ? 0 : mp;
    }

    public final int getMaxMp() {
        if (overrideStats != null) {
            return overrideStats.getMp();
        }
        return stats.getMp();
    }

    public final int getExp() {
        if (overrideStats != null) {
            return overrideStats.getExp();
        }
        return stats.getExp();
    }

    public final void setTauntMultiplier(final int multi) {
        this.TauntMultiplier = multi;
    }

    public final int getLevel() {
        return stats.getLevel();
    }

    public final int getAvoid() {
        return stats.getAvoid();
    }

    public final int getVenomMulti() {
        return this.VenomMultiplier;
    }

    public final void setVenomMulti(final int multiplier) {
        this.VenomMultiplier = multiplier;
    }

    public final boolean isBoss() {
        return stats.isBoss() || isHT();
    }

    public final int getAnimationTime(final String name) {
        return stats.getAnimationTime(name);
    }

    private final List<Integer> getRevives() {
        return stats.getRevives();
    }

    private final byte getTagColor() {
        return stats.getTagColor();
    }

    private final byte getTagBgColor() {
        return stats.getTagBgColor();
    }

    /**
    *
    * @param from the player that dealt the damage
    * @param damage
    */
    public final void damage(final MapleCharacter from, final int damage, final boolean updateAttackTime) {
        damage(from, damage, updateAttackTime, -1);
    }

    public final void damage(final MapleCharacter from, final int damage, final boolean updateAttackTime, final int skill) { // TODO: get overridestats working here
        if (damage <= 0 || !isAlive()) {
            return;
        }
        AttackerEntry attacker = null;
        if (from.getParty() != null) {
            attacker = new PartyAttackerEntry(from.getParty().getId(), from.getClient().getChannelServer());
        } else {
            attacker = new SingleAttackerEntry(from, from.getClient().getChannelServer());
        }
        boolean replaced = false;
        for (final AttackerEntry aentry : attackers) {
            if (aentry.equals(attacker)) {
                attacker = aentry;
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            attackers.add(attacker);
        }
        final int rDamage = Math.max(0, Math.min(damage, this.hp));
        attacker.addDamage(from, rDamage);
        this.hp -= rDamage;
        int remhppercentage = (int) Math.ceil((this.hp * 100.0) / getMaxHp());
        if (remhppercentage < 1) {
            remhppercentage = 1;
        }
        if (hasBossHPBar()) {
            from.getMap().broadcastMessage(makeBossHPBarPacket(), getPosition());
        } else if (!isBoss()) {
            if(from.getMap().getId() >= 925020000 && from.getMap().getId() <= 925023814) {
                boolean fastattack = (skill == Bowmaster.HURRICANE || skill == Corsair.RAPID_FIRE || skill == WindArcher.HURRICANE);
                from.handleDojoEnergyGain(fastattack);
            }
            for (final AttackerEntry mattacker : attackers) {
                for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                    if (cattacker.getAttacker().getMap() == from.getMap()) {
                        cattacker.getAttacker().getClient().announce(MobFactory.showMonsterHP(getObjectId(), remhppercentage));
                    }
                }
            }
        }
    }

    public void taskDamage(int damage) {
        int rDamage = Math.max(0, Math.min(damage, this.hp));
        this.hp -= rDamage;
    }

    public final void heal(final int hp, final int mp) {
        int hp2Heal = getHp() + hp;
        int mp2Heal = getMp() + mp;
        if (hp2Heal >= getMaxHp()) {
            hp2Heal = getMaxHp();
        }
        if (mp2Heal >= getMaxMp()) {
            mp2Heal = getMaxMp();
        }
        setHp(hp2Heal);
        setMp(mp2Heal);
        getMap().broadcastMessage(MobFactory.healMonster(getObjectId(), hp));
    }

    public final boolean isAttackedBy(final MapleCharacter chr) {
        for (final AttackerEntry aentry : attackers) {
            if (aentry.contains(chr)) {
                return true;
            }
        }
        return false;
    }

    public final void giveExpToCharacter(final MapleCharacter attacker, int exp, final boolean highestDamage, final int numExpSharers, final int party) {
        if (highestDamage) {
            if (eventInstance != null) {
                eventInstance.monsterKilled(attacker, this);
            }
            highestDamageChar = attacker;
        }
        if (attacker.getHp() > 0) {
            if (exp > 0) {
                final Integer holySymbol = attacker.getBuffedValue(MapleBuffStat.HOLY_SYMBOL);
                if (holySymbol != null) {
                    exp *= 1.0 + (holySymbol.doubleValue() / (numExpSharers == 1 ? 500.0 : 100.0));
                }
                if(TauntMultiplier > 0) {
                    exp *= (double) TauntMultiplier / 100;
                }
            }
            attacker.gainExp(exp, true, false, highestDamage, party);
            attacker.mobKilled(this.getId());
        }
    }

    public final MapleCharacter killBy(final MapleCharacter killer) {
        long totalBaseExpL = (long) (this.getExp() * killer.getExpRate());
        int totalBaseExp = (int) (Math.min(Integer.MAX_VALUE, totalBaseExpL));
        AttackerEntry highest = null;
        int highdamage = 0;
        for (final AttackerEntry attackEntry : attackers) {
            if (attackEntry.getDamage() > highdamage) {
                highest = attackEntry;
                highdamage = attackEntry.getDamage();
            }
        }
        for (final AttackerEntry attackEntry : attackers) {
            attackEntry.killedMob(killer.getMap(), (int) Math.ceil(totalBaseExp * ((double) attackEntry.getDamage() / getMaxHp())), attackEntry == highest);
        }
        final MapleCharacter controller = this.getController();
        if (controller != null) { // this can/should only happen when a hidden gm attacks the monster
            controller.getClient().announce(MobFactory.stopControllingMonster(this.getObjectId()));
            controller.stopControllingMonster(this);
        }
        final List<Integer> toSpawn = this.getRevives();
        if (toSpawn != null) {
            final MapleMap reviveMap = killer.getMap();
            for (Integer mid : toSpawn) {
                final MapleMonster mob = MapleLifeFactory.getInstance().getMonster(mid);
                if (eventInstance != null) {
                    eventInstance.registerMonster(mob);
                }
                mob.setPosition(getPosition());
                if (dropsDisabled()) {
                    mob.disableDrops();
                }
                TimerManager.getInstance().schedule(new Runnable() {
                    public void run() {
                        reviveMap.spawnMonster(mob);
                    }
                }, this.getAnimationTime("die1"));
            }
        }
        if (eventInstance != null) {
            eventInstance.unregisterMonster(this);
        }
        for (MonsterListener listener : listeners.toArray(new MonsterListener[listeners.size()])) {
            listener.monsterKilled(this, highestDamageChar);
        }
        final MapleCharacter ret = highestDamageChar;
        highestDamageChar = null; // may not keep hard references to chars outside of PlayerStorage or MapleMap
        return ret;
    }

    public final boolean isAlive() {
        return this.hp > 0;
    }

    public final MapleCharacter getController() {
        return controller.get();
    }

    public final void setController(final MapleCharacter controller) {
        this.controller = new WeakReference<MapleCharacter>(controller);
    }

    public final void switchController(final MapleCharacter newController, final boolean immediateAggro) {
        final MapleCharacter controllers = getController();
        if (controllers == newController) {
            return;
        }
        if (controllers != null) {
            controllers.stopControllingMonster(this);
            controllers.getClient().announce(MobFactory.stopControllingMonster(getObjectId()));
        }
        newController.controlMonster(this, immediateAggro);
        setController(newController);
        if (immediateAggro) {
            setControllerHasAggro(true);
        }
        setControllerKnowsAboutAggro(false);
    }

    public final void addListener(final MonsterListener listener) {
        listeners.add(listener);
    }

    public final boolean isControllerHasAggro() {
        return fake ? false : controllerHasAggro;
    }

    public final void setControllerHasAggro(final boolean controllerHasAggro) {
        if (fake) {
            return;
        }
        this.controllerHasAggro = controllerHasAggro;
    }

    public final boolean isControllerKnowsAboutAggro() {
        return fake ? false : controllerKnowsAboutAggro;
    }

    public final void setControllerKnowsAboutAggro(final boolean controllerKnowsAboutAggro) {
        if (fake) {
            return;
        }
        this.controllerKnowsAboutAggro = controllerKnowsAboutAggro;
    }

    public final MaplePacket makeBossHPBarPacket() {
        return MobFactory.showBossHP(getId(), getHp(), getMaxHp(), getTagColor(), getTagBgColor());
    }

    public final boolean hasBossHPBar() {
        return (isBoss() && getTagColor() > 0) || isHT();
    }

    private final boolean isHT() {
        return getId() == 8810018 || getId() == 8810026;
    }

    @Override
    public final void sendSpawnData(final MapleClient c) {
        if (!isAlive()) {
            return;
        }
        if (isFake()) {
            c.announce(MobFactory.spawnFakeMonster(this, 0));
        } else {
            c.announce(MobFactory.spawnMonster(this, false));
        }
        if (stati.size() > 0) {
            for (final MonsterStatusEffect mse : activeEffects) {
                MaplePacket packet = MobFactory.applyMonsterStatus(getObjectId(), mse.getStati(), mse.getSkill().getId(), false, 0);
                c.announce(packet);
            }
        }
        if (hasBossHPBar()) {
            if (this.getMap().countMonster(8810026) > 2 && this.getMap().getId() == 240060200) {
                this.getMap().killAllMonsters();
                return;
            }
            c.announce(makeBossHPBarPacket());
        }
    }

    @Override
    public final void sendDestroyData(final MapleClient client) {
        client.announce(MobFactory.killMonster(getObjectId(), false));
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.MONSTER;
    }

    public final void setEventInstance(final EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public final boolean isMobile() {
        return stats.isMobile();
    }

    public final ElementalEffectiveness getEffectiveness(final Element e) {
        if (activeEffects.size() > 0 && stati.get(MonsterStatus.DOOM) != null) {
            return ElementalEffectiveness.NORMAL;
        }
        return stats.getEffectiveness(e);
    }

    public final boolean applyStatus(final MapleCharacter from, final MonsterStatusEffect status, final boolean poison, final long duration) {
        return applyStatus(from, status, poison, duration, false);
    }

    public final boolean applyStatus(final MapleCharacter from, final MonsterStatusEffect status, final boolean poison, final long duration, final boolean venom) {
        switch (stats.getEffectiveness(status.getSkill().getElement())) {
        case IMMUNE:
        case STRONG:
        case NEUTRAL:
            return false;
        case NORMAL:
        case WEAK:
            break;
        default: {
                System.out.println("Unknown elemental effectiveness: " + stats.getEffectiveness(status.getSkill().getElement()));
                return false;
            }
        }
        final int skillid = status.getSkill().getId();
        if (skillid == FPMage.ELEMENT_COMPOSITION) {
            ElementalEffectiveness effectiveness = stats.getEffectiveness(Element.POISON);
            if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG) {
                return false;
            }
        } else if (skillid == ILMage.ELEMENT_COMPOSITION) {
            ElementalEffectiveness effectiveness = stats.getEffectiveness(Element.ICE);
            if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG) {
                return false;
            }
        } else if (skillid == NightLord.VENOMOUS_STAR || skillid == Shadower.VENOMOUS_STAB || skillid == NightWalker.VENOM) {
            ElementalEffectiveness effectiveness = stats.getEffectiveness(Element.POISON);
            if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG) {
                return false;
            }
        }
        if (poison && getHp() <= 1) {
            return false;
        }
        if (isBoss() && !status.getStati().containsKey(MonsterStatus.SPEED)) {
            return false;
        }
        for (MonsterStatus stat : status.getStati().keySet()) {
            MonsterStatusEffect oldEffect = stati.get(stat);
            if (oldEffect != null) {
                oldEffect.removeActiveStatus(stat);
                if (oldEffect.getStati().size() == 0) {
                    oldEffect.getCancelTask().cancel(false);
                    oldEffect.cancelDamageSchedule();
                    activeEffects.remove(oldEffect);
                    oldEffect = null;
                }
            }
        }
        final TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = new Runnable() {

            @Override
            public final void run() {
                if (isAlive()) {
                    MaplePacket packet = MobFactory.cancelMonsterStatus(getObjectId(), status.getStati());
                    map.broadcastMessage(packet, getPosition());
                    if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                        getController().getClient().announce(packet);
                    }
                }
                activeEffects.remove(status);
                for (final MonsterStatus stat : status.getStati().keySet()) {
                    stati.remove(stat);
                }
                setVenomMulti(0);
                status.cancelDamageSchedule();
            }
        };
        if (poison && getHp() > 1) {
            final int poisonDamage = Math.min(Short.MAX_VALUE, (int) (getMaxHp() / (70.0 - from.getSkillLevel(status.getSkill())) + 0.999));
            status.setValue(MonsterStatus.POISON, Integer.valueOf(poisonDamage));
            status.setDamageSchedule(timerManager.register(new DamageTask(poisonDamage, from, status, cancelTask, 0), 1000, 1000));
        } else if (venom) {
            if (from.getJob() == NightLord.ID || from.getJob() == Shadower.ID || from.isA(NightWalker.NIGHT_WALKER3)) {
                int poisonLevel = 0;
                int matk = 0;
                int id = from.getJob();
                int skill = (id == 412 ? 4120005 : (id == 422 ? 4220005 : 14110004));
                poisonLevel = from.getSkillLevel(SkillFactory.getSkill(skill));
                if (poisonLevel <= 0) {
                    return false;
                }
                matk = SkillFactory.getSkill(skill).getEffect(poisonLevel).getMatk();
                final int luk = from.getLuk();
                final int maxDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.2 * luk * matk));
                final int minDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.1 * luk * matk));
                int gap = maxDmg - minDmg;
                if (gap == 0) {
                    gap = 1;
                }
                int poisonDamage = 0;
                for (int i = 0; i < getVenomMulti(); i++) {
                    poisonDamage = poisonDamage + ((int) (gap * Math.random()) + minDmg);
                }
                poisonDamage = Math.min(Short.MAX_VALUE, poisonDamage);
                status.setValue(MonsterStatus.POISON, Integer.valueOf(poisonDamage));
                status.setDamageSchedule(timerManager.register(new DamageTask(poisonDamage, from, status, cancelTask, 0), 1000, 1000));
            } else {
                return false;
            }
        } else if (status.getSkill().getId() == Hermit.SHADOW_WEB || status.getSkill().getId() == NightWalker.SHADOW_WEB) {
            status.setDamageSchedule(timerManager.schedule(new DamageTask((int) (getMaxHp() / 50.0 + 0.999), from, status, cancelTask, 1), 3500));
        } else if (status.getSkill().getId() == NightLord.NINJA_AMBUSH || status.getSkill().getId() == Shadower.NINJA_AMBUSH) { // Ninja Ambush
            final int ambushDamage = (int) ((from.getStr() + from.getLuk()) * (1.5 + (from.getSkillLevel(status.getSkill().getId()) * 0.05)));
            status.setValue(MonsterStatus.NINJA_AMBUSH, Integer.valueOf(ambushDamage < 1 ? 1 : ambushDamage));
            status.setDamageSchedule(timerManager.register(new DamageTask(ambushDamage, from, status, cancelTask, 2), 1000, 1000));
        /*} else if (status.getSkill().getId() == Outlaw.FLAME_THROWER) { // DoT
            final int flameDamage = (int) InitialHitDamage * (5% + AmpBulletDamage%);
            status.setDamageSchedule(timerManager.register(new DamageTask(flameDamage, from, status, cancelTask, 2), 1000, 1000));
        */} else if (status.getSkill().getId() == FPArchMage.FIRE_DEMON || status.getSkill().getId() == ILArchMage.ICE_DEMON) { // DoT
            final int demonDamage = Math.min(Short.MAX_VALUE, (int) (getMaxHp() / (70.0 - from.getSkillLevel(status.getSkill())) + 0.999));
            status.setValue(MonsterStatus.NINJA_AMBUSH, Integer.valueOf(demonDamage < 1 ? 1 : demonDamage));
            status.setDamageSchedule(timerManager.register(new DamageTask(demonDamage, from, status, cancelTask, 0), 1000, 1000));
        }

        for (final MonsterStatus stat : status.getStati().keySet()) {
            stati.put(stat, status);
        }
        activeEffects.add(status);
        int animationTime = status.getSkill().getAnimationTime();
        MaplePacket packet = MobFactory.applyMonsterStatus(getObjectId(), status.getStati(), status.getSkill().getId(), false, 0);
        map.broadcastMessage(packet, getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().announce(packet);
        }
        status.setCancelTask(timerManager.schedule(cancelTask, duration + animationTime));
        return true;
    }

    public final void applyMonsterBuff(final MonsterStatus status, final int x, final int skillId, final long duration, final MobSkill skill) {
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = new Runnable() {

            @Override
            public final void run() {
                if (isAlive()) {
                    MaplePacket packet = MobFactory.cancelMonsterStatus(getObjectId(), Collections.singletonMap(status, Integer.valueOf(x)));
                    map.broadcastMessage(packet, getPosition());
                    if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                        getController().getClient().announce(packet);
                    }
                    removeMonsterBuff(status);
                }
            }
        };
        MaplePacket packet = MobFactory.applyMonsterStatus(getObjectId(), Collections.singletonMap(status, x), skillId, true, 0, skill);
        map.broadcastMessage(packet, getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().announce(packet);
        }
        timerManager.schedule(cancelTask, duration);
        addMonsterBuff(status);
    }

    public final void addMonsterBuff(final MonsterStatus status) {
        this.monsterBuffs.add(status);
    }

    public final void removeMonsterBuff(final MonsterStatus status) {
        this.monsterBuffs.remove(status);
    }

    public final boolean isBuffed(final MonsterStatus status) {
        return this.monsterBuffs.contains(status);
    }

    public final void setFake(final boolean fake) {
        this.fake = fake;
    }

    public final boolean isFake() {
        return fake;
    }

    public final MapleMap getMap() {
        return map;
    }

    public final List<Pair<Integer, Integer>> getSkills() {
        return stats.getSkills();
    }

    public final boolean hasSkill(final int skillId, final int level) {
        return stats.hasSkill(skillId, level);
    }

    public final boolean canUseSkill(final MobSkill toUse) {
        if (toUse == null) {
            return false;
        }
        for (Pair<Integer, Integer> skill : usedSkills) {
            if (skill.getLeft() == toUse.getSkillId() && skill.getRight() == toUse.getSkillLevel()) {
                return false;
            }
        }
        if (toUse.getLimit() > 0) {
            if (this.skillsUsed.containsKey(new Pair<Integer, Integer>(toUse.getSkillId(), toUse.getSkillLevel()))) {
                int times = this.skillsUsed.get(new Pair<Integer, Integer>(toUse.getSkillId(), toUse.getSkillLevel()));
                if (times >= toUse.getLimit()) {
                    return false;
                }
            }
        }
        if (toUse.getSkillId() == 200) {
            Collection<MapleMapObject> mmo = getMap().getMapObjects();
            int i = 0;
            for (MapleMapObject mo : mmo) {
                if (mo.getType() == MapleMapObjectType.MONSTER) {
                    i++;
                }
            }
            if (i > 100) {
                return false;
            }
        }
        return true;
    }

    public final void usedSkill(final int skillId, final int level, long cooltime) {
        this.usedSkills.add(new Pair<Integer, Integer>(skillId, level));
        if (this.skillsUsed.containsKey(new Pair<Integer, Integer>(skillId, level))) {
            int times = this.skillsUsed.get(new Pair<Integer, Integer>(skillId, level)) + 1;
            this.skillsUsed.remove(new Pair<Integer, Integer>(skillId, level));
            this.skillsUsed.put(new Pair<Integer, Integer>(skillId, level), times);
        } else {
            this.skillsUsed.put(new Pair<Integer, Integer>(skillId, level), 1);
        }
        final MapleMonster mons = this;
        TimerManager tMan = TimerManager.getInstance();
        tMan.schedule(
        new Runnable() {

            @Override
            public void run() {
                mons.clearSkill(skillId, level);
            }
        }, cooltime);
    }

    public final void clearSkill(final int skillId, final int level) {
        int index = -1;
        for (Pair<Integer, Integer> skill : usedSkills) {
            if (skill.getLeft() == skillId && skill.getRight() == level) {
                index = usedSkills.indexOf(skill);
                break;
            }
        }
        if (index != -1) {
            usedSkills.remove(index);
        }
    }

    public final int getNoSkills() {
        return this.stats.getNoSkills();
    }

    public final boolean isFirstAttack() {
        return this.stats.isFirstAttack();
    }

    public final int getBuffToGive() {
        return this.stats.getBuffToGive();
    }

    private final class DamageTask implements Runnable {

        private final int dealDamage;
        private final MapleCharacter chr;
        private final MonsterStatusEffect status;
        private final Runnable cancelTask;
        private final int type;
        private final MapleMap map;

        private DamageTask(final int dealDamage, final MapleCharacter chr, final MonsterStatusEffect status, final Runnable cancelTask, final int type) {
            this.dealDamage = dealDamage;
            this.chr = chr;
            this.status = status;
            this.cancelTask = cancelTask;
            this.type = type;
            this.map = chr.getMap();
        }

        @Override
        public void run() {
            int damage = dealDamage;
            if (damage >= hp) {
                damage = hp - 1;
                if (type == 1 || type == 2) {
                    map.broadcastMessage(MobFactory.damageMonster(getObjectId(), damage), getPosition());
                    cancelTask.run();
                    status.getCancelTask().cancel(false);
                }
            }
            if (hp > 1 && damage > 0) {
                AttackerEntry attacker = new SingleAttackerEntry(chr, chr.getClient().getChannelServer());
                boolean replaced = false;
                for (AttackerEntry aentry : attackers) {
                    if (aentry.equals(attacker)) {
                        attacker = aentry;
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) {
                    attackers.add(attacker);
                }
                attacker.addDamage(chr, damage);
                taskDamage(damage);
                if (type == 1) {
                    map.broadcastMessage(MobFactory.damageMonster(getObjectId(), damage), getPosition());
                }
            }
        }
    }


    public String getName() {
        return stats.getName();
    }

    private class AttackingMapleCharacter {

        private MapleCharacter attacker;

        public AttackingMapleCharacter(final MapleCharacter attacker) {
            super();
            this.attacker = attacker;
        }

        public final MapleCharacter getAttacker() {
            return attacker;
        }
    }

    private interface AttackerEntry {

        List<AttackingMapleCharacter> getAttackers();

        public void addDamage(MapleCharacter from, int damage);

        public int getDamage();

        public boolean contains(MapleCharacter chr);

        public void killedMob(MapleMap map, int baseExp, boolean mostDamage);
    }

    private final class SingleAttackerEntry implements AttackerEntry {

        private int damage;
        private int chrid;
        private ChannelServer cserv;

        public SingleAttackerEntry(final MapleCharacter from, final ChannelServer cserv) {
            this.chrid = from.getId();
            this.cserv = cserv;
        }

        @Override
        public void addDamage(final MapleCharacter from, final int damage) {
            if (chrid == from.getId()) {
                this.damage += damage;
            } else {
                throw new IllegalArgumentException("Not the attacker of this entry");
            }
        }

        @Override
        public final List<AttackingMapleCharacter> getAttackers() {
            final MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(chrid);
            if (chr != null) {
                return Collections.singletonList(new AttackingMapleCharacter(chr));
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean contains(final MapleCharacter chr) {
            return chrid == chr.getId();
        }

        @Override
        public int getDamage() {
            return damage;
        }

        @Override
        public void killedMob(final MapleMap map, final int baseExp, final boolean mostDamage) {
            final MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(chrid);
            if (chr != null && chr.getMap() == map) {
                giveExpToCharacter(chr, baseExp, mostDamage, 1, 0);
            }
        }

        @Override
        public int hashCode() {
            return chrid;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SingleAttackerEntry other = (SingleAttackerEntry) obj;
            return chrid == other.chrid;
        }
    }

    private final static class OnePartyAttacker {

        public MapleParty lastKnownParty;
        public int damage;

        public OnePartyAttacker(final MapleParty lastKnownParty, final int damage) {
            this.lastKnownParty = lastKnownParty;
            this.damage = damage;
        }
    }
    
    private class PartyAttackerEntry implements AttackerEntry {

        private int totDamage;
        private final Map<Integer, OnePartyAttacker> attackers;
        private ChannelServer cserv;
        private int partyid;

        public PartyAttackerEntry(final int partyid, final ChannelServer cserv) {
            this.partyid = partyid;
            this.cserv = cserv;
            attackers = new HashMap<Integer, OnePartyAttacker>(6);
        }

        public List<AttackingMapleCharacter> getAttackers() {
            final List<AttackingMapleCharacter> ret = new ArrayList<AttackingMapleCharacter>(attackers.size());
            for (final Entry<Integer, OnePartyAttacker> entry : attackers.entrySet()) {
                final MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(entry.getKey());
                if (chr != null) {
                    ret.add(new AttackingMapleCharacter(chr));
                }
            }
            return ret;
        }

        private final Map<MapleCharacter, OnePartyAttacker> resolveAttackers() {
            final Map<MapleCharacter, OnePartyAttacker> ret = new HashMap<MapleCharacter, OnePartyAttacker>(attackers.size());
            for (final Entry<Integer, OnePartyAttacker> aentry : attackers.entrySet()) {
                final MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(aentry.getKey());
                if (chr != null) {
                    ret.put(chr, aentry.getValue());
                }
            }
            return ret;
        }

        @Override
        public final boolean contains(final MapleCharacter chr) {
            return attackers.containsKey(chr.getId());
        }

        @Override
        public final int getDamage() {
            return totDamage;
        }

        public void addDamage(final MapleCharacter from, final int damage) {
            final OnePartyAttacker oldPartyAttacker = attackers.get(from.getId());
            if (oldPartyAttacker != null) {
                oldPartyAttacker.damage += damage;
                oldPartyAttacker.lastKnownParty = from.getParty();
            } else {
                OnePartyAttacker onePartyAttacker = new OnePartyAttacker(from.getParty(), damage);
                attackers.put(from.getId(), onePartyAttacker);
            }
            totDamage += damage;
        }

    @Override
        public final void killedMob(final MapleMap map, final int baseExp, final boolean mostDamage) {
            Map<MapleCharacter, OnePartyAttacker> attackers_ = resolveAttackers();
            MapleCharacter pchr, highest = null;
            int iDamage, iexp, ptysize = 0, highestDamage = 0;
            MapleParty party;
            double averagePartyLevel, expBonus, expWeight, levelMod, innerBaseExp, expFraction;
            List<MapleCharacter> expApplicable;
            final Map<MapleCharacter, ExpMap> expMap = new HashMap<MapleCharacter, ExpMap>(6);
            for (final Entry<MapleCharacter, OnePartyAttacker> attacker : attackers_.entrySet()) {
                party = attacker.getValue().lastKnownParty;
                averagePartyLevel = 0;
                ptysize = 0;
                expApplicable = new ArrayList<MapleCharacter>();
                for (final MaplePartyCharacter partychar : party.getMembers()) {
                    if (attacker.getKey().getLevel() - partychar.getLevel() <= 5 || getLevel() - partychar.getLevel() <= 5) {
                        pchr = cserv.getPlayerStorage().getCharacterByName(partychar.getName());
                        if (pchr != null) {
                            if (pchr.isAlive() && pchr.getMap() == map) {
                                expApplicable.add(pchr);
                                averagePartyLevel += pchr.getLevel();
                                ptysize++;
                            }
                        }
                    }
                }
                expBonus = 1.0;
                if (expApplicable.size() > 1) {
                    expBonus = 1.10 + 0.05 * expApplicable.size();
                    averagePartyLevel /= expApplicable.size();
                }
                iDamage = attacker.getValue().damage;
                if (iDamage > highestDamage) {
                    highest = attacker.getKey();
                    highestDamage = iDamage;
                }
                innerBaseExp = baseExp * ((double) iDamage / totDamage);
                expFraction = (innerBaseExp * expBonus) / (expApplicable.size() + 1);
                for (final MapleCharacter expReceiver : expApplicable) {
                    iexp = expMap.get(expReceiver) == null ? 0 : expMap.get(expReceiver).exp;
                    expWeight = (expReceiver == attacker.getKey() ? 2.0 : 1.0);
                    levelMod = expReceiver.getLevel() / averagePartyLevel;
                    if (levelMod > 1.0 || this.attackers.containsKey(expReceiver.getId())) {
                        levelMod = 1.0;
                    }
                    iexp += (int) Math.round(expFraction * expWeight * levelMod);
                    expMap.put(expReceiver, new ExpMap(iexp, (short) ptysize));
                }
            }
            for (final Entry<MapleCharacter, ExpMap> expReceiver : expMap.entrySet()) {
                giveExpToCharacter(expReceiver.getKey(), expReceiver.getValue().exp, mostDamage ? expReceiver.getKey() == highest : false, expMap.size(), expReceiver.getValue().ptysize);
            }
        }

        @Override
        public final int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + partyid;
            return result;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PartyAttackerEntry other = (PartyAttackerEntry) obj;
            if (partyid != other.partyid) {
                return false;
            }
            return true;
        }
    }

    public final void setTempEffectiveness(final Element e, final ElementalEffectiveness ee, final long milli) {
        final Element fE = e;
        final ElementalEffectiveness fEE = stats.getEffectiveness(e);
        if (!stats.getEffectiveness(e).equals(ElementalEffectiveness.WEAK)) {
            stats.setEffectiveness(e, ee);
            TimerManager.getInstance().schedule(new Runnable() {

                public void run() {
                    stats.removeEffectiveness(fE);
                    stats.setEffectiveness(fE, fEE);
                }
            }, milli);
        }
    }

    public final BanishInfo getBanish() {
        return stats.getBanishInfo();
    }

    public final void setBoss(final boolean boss) {
        this.stats.setBoss(boss);
    }

    public final int getDropPeriodTime() {
        return stats.getDropPeriod();
    }

    public final int getPADamage() {
        return stats.getPADamage();
    }


    public final void setOverrideStats(final MapleMonsterStats override) {
        this.overrideStats = override;
        this.hp = override.getHp();
        this.mp = override.getMp();
    }
    
    public final void empty() {
        try {
            this.monsterLock.unlock();
        } catch (Exception e) {
        }
        this.monsterLock = null;
        for (MonsterStatus ms : stati.keySet()) {
            ms = null;
        }
        this.stati = null;
        this.usedSkills = null;
        this.listeners = null;
        this.skillsUsed = null;
        for (MonsterStatusEffect mse : activeEffects) {
            mse.getCancelTask().cancel(false);
            mse.cancelDamageSchedule();
            mse = null;
        }
        this.activeEffects = null;
        this.monsterBuffs = null;
        this.stats = null;
        this.highestDamageChar = null;
        this.map = null;
        this.eventInstance = null;
        this.attackers = null;
        this.controller = null;
    }

    private final static class ExpMap {
        public int exp;
        public final short ptysize;

        public ExpMap(final int exp, final short ptysize) {
            this.exp = exp;
            this.ptysize = ptysize;
        }
    }

    public final int getRemoveAfter() {
        return stats.getRemoveAfter();
    }
}

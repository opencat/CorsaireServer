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
import client.ISkill;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleInventoryType;
import client.MapleKeyBinding;
import client.MapleStat;
import client.SkillFactory;
import client.SkillMacro;
import constants.skills.Aran;
import constants.skills.Archer;
import constants.skills.Beginner;
import constants.skills.Bishop;
import constants.skills.BlazeWizard;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.Buccaneer;
import constants.skills.ChiefBandit;
import constants.skills.Corsair;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.FPArchMage;
import constants.skills.FPMage;
import constants.skills.Gunslinger;
import constants.skills.Hero;
import constants.skills.ILArchMage;
import constants.skills.Magician;
import constants.skills.Marksman;
import constants.skills.NightWalker;
import constants.skills.Noblesse;
import constants.skills.Paladin;
import constants.skills.Pirate;
import constants.skills.Priest;
import constants.skills.Swordsman;
import constants.skills.Thief;
import constants.skills.ThunderBreaker;
import constants.skills.WindArcher;
import java.awt.Point;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.channel.ChannelServer;
import server.MapleInventoryManipulator;
import server.MaplePortal;
import server.MapleStatEffect;
import server.TimerManager;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleSummon;
import tools.DataTool;
import tools.DatabaseConnection;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.factory.BuffFactory;
import tools.factory.EffectFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.SummonFactory;

/**
 * @name        IntraPersonalOperation
 * @author      x711Li
 */
public class IntraPersonalOperation {
    public static final void ChangeMapHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        slea.readByte();
        int targetid = slea.readInt();
        String startwp = slea.readMapleAsciiString();
        MaplePortal portal = player.getMap().getPortal(startwp);
        slea.readByte();
        boolean wheel = slea.readShort() > 0;
        if (targetid != -1 && !player.isAlive()) {
            boolean executeStandardPath = true;
            if (executeStandardPath) {
                MapleMap to = player.getMap();
                if (wheel && player.getItemQuantity(5510000, false) > 0) {
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5510000, 1, true, false);
                } else {
                    player.cancelAllBuffs();
                    to = player.getMap().getReturnMap();
                    player.setStance(0);
                }
                player.setHp(50);
                player.changeMap(to, to.getPortal(0));
            }
        } else if (targetid != -1 && player.isGM()) {
            MapleMap to = c.getChannelServer().getMapFactory().getMap(targetid);
            player.changeMap(to, to.getPortal(0));
            player.setBanCount(0);
        } else if (portal != null) {
            portal.enterPortal(c);
        }
        c.announce(IntraPersonalFactory.enableActions());
        player.setRates(false);
    }

    public static final void HealOverTimeHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(8);
        short healHP = slea.readShort();
        if (healHP != 0) {
            if (healHP > 140) {
                return;
            }
            c.getPlayer().addHP(healHP);
            c.getPlayer().checkBerserk();
        }
        short healMP = slea.readShort();
        int skill = c.getPlayer().getSkillLevel(SkillFactory.getSkill(Magician.IMPROVED_MP_RECOVERY));
        if (healMP != 0 && 2 * healMP <= ((skill != 0 ? skill : 16) * c.getPlayer().getLevel() / 10 + 3) * 3 + 50) {
            c.getPlayer().addMP(healMP);
        }
    }

    public static final void SpecialMoveHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().hasDisease(MapleDisease.SEAL) || !c.getPlayer().isAlive()) {
            return;
        }
        slea.readInt();
        int skillid = slea.readInt();
        ISkill skill = SkillFactory.getSkill(skillid);
        if (skill == null) {
            return;
        }
        int skillLevel_ = slea.readByte();
        int skillLevel = c.getPlayer().getSkillLevel(skill);
        if (skillid % 10000000 == 1010 || skillid % 10000000 == 1011) {
            skillLevel = 1;
            c.getPlayer().setDojoEnergy(0);
            c.announce(EffectFactory.getEnergy(0));
        }
        if ((skillLevel != skillLevel_ && !c.getPlayer().isAran()) || skillLevel == 0 || skillLevel_ == 0) {
            return;
        }
        MapleStatEffect effect = skill.getEffect(skillLevel);
        if (effect.getCooldown() > 0) {
            if (c.getPlayer().skillisCooling(skillid)) {
                return;
            } else if (skillid != Corsair.BATTLE_SHIP) {
                c.announce(BuffFactory.skillCooldown(skillid, effect.getCooldown()));
                c.getPlayer().addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown() * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(c.getPlayer(), skillid), effect.getCooldown() * 1000));
            }
        }
        if (skillid == Hero.MONSTER_MAGNET || skillid == Paladin.MONSTER_MAGNET || skillid == DarkKnight.MONSTER_MAGNET) { // Monster Magnet
            int num = slea.readInt();
            int mobId;
            for (int i = 0; i < num; i++) {
                mobId = slea.readInt();
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), BuffFactory.showMagnet(mobId, slea.readByte()), false);
                MapleMonster monster = c.getPlayer().getMap().getMonsterByOid(mobId);
                if (monster != null) {
                    monster.switchController(c.getPlayer(), monster.isControllerHasAggro());
                }
            }
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), BuffFactory.showBuffEffect(c.getPlayer(), c.getPlayer().getId(), skillid, 1, slea.readByte()), false);
            c.announce(IntraPersonalFactory.enableActions());
            return;
        } else if (skillid == Buccaneer.TIME_LEAP) {
            if (c.getPlayer().getParty() != null) {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    for (MapleCharacter mc : cserv.getPartyMembers(c.getPlayer().getParty(), c.getPlayer().getMapId())) {
                        mc.removeAllCooldownsExcept(Buccaneer.TIME_LEAP);
                    }
                }
            } else {
                c.getPlayer().removeAllCooldownsExcept(Buccaneer.TIME_LEAP);
            }
        } else if (skillid == Brawler.MP_RECOVERY) {
            ISkill s = SkillFactory.getSkill(skillid);
            MapleStatEffect ef = s.getEffect(c.getPlayer().getSkillLevel(s));
            int lose = c.getPlayer().getMaxHp() / ef.getX();
            c.getPlayer().setHp(c.getPlayer().getHp() - lose);
            c.getPlayer().updateSingleStat(MapleStat.HP, c.getPlayer().getHp());
            double perc = ef.getY() / 100.0;
            int gain = (int) (lose * perc);
            c.getPlayer().setMp(c.getPlayer().getMp() + gain);
            c.getPlayer().updateSingleStat(MapleStat.MP, c.getPlayer().getMp());
        } else if (skillid % 10000000 == 1004) {
            slea.readShort();
        }
        Point pos = null;
        if (slea.available() >= 4) {
            pos = new Point(slea.readShort(), slea.readShort());
        }
        if (c.getPlayer().isAlive() && skill.getId() != Aran.COMBAT_STEP) {
            if (skill.getId() != Priest.MYSTIC_DOOR || c.getPlayer().canDoor()) {
                skill.getEffect(skillLevel).applyTo(c.getPlayer(), pos);
            } else {
                c.getPlayer().message("Please wait 5 seconds before casting Mystic Door again");
                c.announce(IntraPersonalFactory.enableActions());
            }
        } else {
            c.announce(IntraPersonalFactory.enableActions());
        }
    }

    public static final void CancelBuffHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int sourceid = slea.readInt();
        switch (sourceid) {
            case FPArchMage.BIG_BANG:
            case ILArchMage.BIG_BANG:
            case Bishop.BIG_BANG:
            case Bowmaster.HURRICANE:
            case Marksman.PIERCING_ARROW:
            case Corsair.RAPID_FIRE:
            case WindArcher.HURRICANE:
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), BuffFactory.skillCancel(c.getPlayer(), sourceid), false);
                break;
            default:
                c.getPlayer().cancelEffect(SkillFactory.getSkill(sourceid).getEffect(1), false, -1);
                break;
            case 21000000:
                c.getPlayer().setCombo(0);
        }
    }

    public static final void DistributeAPHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        List<Pair<MapleStat, Integer>> statupdate = new ArrayList<Pair<MapleStat, Integer>>(2);
        c.announce(IntraPersonalFactory.updatePlayerStats(statupdate, true));
        slea.readInt();
        int update = slea.readInt();
        if (c.getPlayer().getRemainingAp() > 0) {
            int job = c.getPlayer().getJob();
            switch (update) {
            case 64: // Str
                if (c.getPlayer().getStr() >= 30000) {
                    return;
                }
                c.getPlayer().addStat(1, 1);
                break;
            case 128: // Dex
                if (c.getPlayer().getDex() >= 30000) {
                    return;
                }
                c.getPlayer().addStat(2, 1);
                break;
            case 256: // Int
                if (c.getPlayer().getInt() >= 30000) {
                    return;
                }
                c.getPlayer().addStat(3, 1);
                break;
            case 512: // Luk
                if (c.getPlayer().getLuk() >= 30000) {
                    return;
                }
                c.getPlayer().addStat(4, 1);
                break;
            case 2048: // HP
                int MaxHP = c.getPlayer().getMaxHp();
                if (c.getPlayer().getHpApUsed() == 10000 || MaxHP == 30000) {
                    return;
                }
                ISkill improvingMaxHP = null;
                int improvingMaxHPLevel = 0;
                if (job == Beginner.ID || job == Noblesse.ID || job == Aran.ARAN1) {
                    MaxHP += rand(8, 12);
                } else if (c.getPlayer().isA(Swordsman.ID) || c.getPlayer().isA(DawnWarrior.DAWN_WARRIOR1) || c.getPlayer().isA(Aran.ARAN2)) {
                    MaxHP += rand(20, 24);
                    if (c.getPlayer().isA(Swordsman.ID)) {
                        improvingMaxHP = SkillFactory.getSkill(1000001);
                    } else {
                        improvingMaxHP = SkillFactory.getSkill(11000000);
                    }
                    improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                    if (improvingMaxHPLevel >= 1) {
                        MaxHP += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                    }
                } else if (c.getPlayer().isA(Magician.ID) || c.getPlayer().isA(BlazeWizard.BLAZE_WIZARD1)) {
                    MaxHP += rand(6, 10);
                } else if (c.getPlayer().isA(Archer.ID) || c.getPlayer().isA(WindArcher.WIND_ARCHER1) || c.getPlayer().isA(Thief.ID) || c.getPlayer().isA(NightWalker.NIGHT_WALKER1)) {
                    MaxHP += rand(16, 20);
                } else if (c.getPlayer().isA(Pirate.ID) || c.getPlayer().isA(ThunderBreaker.THUNDER_BREAKER1)) {
                    MaxHP += rand(18, 22);
                    if (c.getPlayer().isA(Pirate.ID)) {
                        improvingMaxHP = SkillFactory.getSkill(5100000);
                    } else {
                        improvingMaxHP = SkillFactory.getSkill(15100000);
                    }
                    improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                    if (improvingMaxHPLevel >= 1) {
                        MaxHP += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                    }
                }
                MaxHP = Math.min(30000, MaxHP);
                c.getPlayer().setHpApUsed(c.getPlayer().getHpApUsed() + 1);
                c.getPlayer().setMaxHp(MaxHP);
                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, MaxHP));
                break;
            case 8192: // MP
                int MaxMP = c.getPlayer().getMaxMp();
                ISkill improvingMaxMP = null;
                if (c.getPlayer().getMpApUsed() == 10000 || c.getPlayer().getMaxMp() == 30000) {
                    return;
                }
                if (job == Beginner.ID || job == Noblesse.ID || job == Aran.ARAN1) {
                    MaxMP += rand(6, 8);
                } else if (c.getPlayer().isA(Swordsman.ID) || c.getPlayer().isA(DawnWarrior.DAWN_WARRIOR1) || c.getPlayer().isA(Aran.ARAN2)) {
                    MaxMP += rand(2, 4);
                } else if (c.getPlayer().isA(Magician.ID) || c.getPlayer().isA(BlazeWizard.BLAZE_WIZARD1)) {
                    MaxMP += rand(18, 20);
                    if (c.getPlayer().isA(Magician.ID)) {
                        improvingMaxMP = SkillFactory.getSkill(2000001);
                    } else {
                        improvingMaxMP = SkillFactory.getSkill(12000000);
                    }
                    int improvingMaxMPLevel = c.getPlayer().getSkillLevel(improvingMaxMP);
                    if (improvingMaxMPLevel >= 1) {
                        MaxMP += improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
                    }
                } else if (c.getPlayer().isA(Archer.ID) || c.getPlayer().isA(WindArcher.WIND_ARCHER1) || c.getPlayer().isA(Thief.ID) || c.getPlayer().isA(NightWalker.NIGHT_WALKER1)) {
                    MaxMP += rand(10, 12);
                } else if (c.getPlayer().isA(Pirate.ID) || c.getPlayer().isA(ThunderBreaker.THUNDER_BREAKER1)) {
                    MaxMP += rand(14, 16);
                }
                MaxMP += c.getPlayer().getInt() * 0.075;
                MaxMP = Math.min(30000, MaxMP);
                c.getPlayer().setMpApUsed(c.getPlayer().getMpApUsed() + 1);
                c.getPlayer().setMaxMp(MaxMP);
                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, MaxMP));
                break; //TODO CHECK AP DIS FOR NEW CLASSES
            default:
                c.announce(IntraPersonalFactory.updatePlayerStats(DataTool.EMPTY_STATUPDATE, true));
                return;
            }
            c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - 1);
            statupdate.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp()));
            c.announce(IntraPersonalFactory.updatePlayerStats(statupdate, true));
        }
    }

    private static int rand(int lbound, int ubound) {
        return (int) (Math.random() * (ubound - lbound + 1) + lbound);
    }

    public static final void DistributeSPHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        int skillid = slea.readInt();
        if (!c.getPlayer().isA(skillid / 10000) || skillid == Aran.TRIPLE_SWING2 || skillid == Aran.TRIPLE_SWING3 || skillid == Aran.DOUBLE_SWING2 || skillid == Aran.DOUBLE_SWING3) {
            return;
        }
        MapleCharacter player = c.getPlayer();
        boolean isBeginnerSkill = false;
        if (skillid % 10000000 < 2000) {
            isBeginnerSkill = true;
        }
        ISkill skill = SkillFactory.getSkill(skillid);
        int curLevel = player.getSkillLevel(skill);
        if (((player.getRemainingSp() > 0 || isBeginnerSkill) && curLevel + 1 <= (skill.isFourthJob() ? player.getMasterLevel(skill) : skill.getMaxLevel()))) {
            if (!isBeginnerSkill) {
                player.setRemainingSp(player.getRemainingSp() - 1);
            }
            if (skill.getId() == Aran.OVER_SWING) {
                player.changeSkillLevel(SkillFactory.getSkill(21120009), curLevel + 1, 0);
                player.changeSkillLevel(SkillFactory.getSkill(21120010), curLevel + 1, 0);
            } else if (skill.getId() == Aran.FULL_SWING) {
                player.changeSkillLevel(SkillFactory.getSkill(21110007), curLevel + 1, 0);
                player.changeSkillLevel(SkillFactory.getSkill(21110008), curLevel + 1, 0);
            }
            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
            player.changeSkillLevel(skill, curLevel + 1, player.getMasterLevel(skill));
        }
    }

    public static final void ChangeKeyMapHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (slea.available() != 8) {
            slea.readInt();
            int numChanges = slea.readInt();
            for (int i = 0; i < numChanges; i++) {
                int key = slea.readInt();
                int type = slea.readByte();
                int action = slea.readInt();
                c.getPlayer().changeKeybinding(key, new MapleKeyBinding(type, action));
            }
            c.getPlayer().setSaveKeyMap();
        }
    }

    public static final void DamageSummonHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int objectid = slea.readInt();
        int unkByte = slea.readByte();
        int damage = slea.readInt();
        int monsterIdFrom = slea.readInt();
        if (c.getPlayer().getMap().getMapObject(objectid) != null) {
            MapleCharacter player = c.getPlayer();
            MapleSummon summon = player.getSummons().get(objectid);
            if (summon != null) {
                summon.addHP(-damage);
                if (summon.getHP() <= 0) {
                    player.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                } else {
                    player.getMap().broadcastMessage(player, SummonFactory.damageSummon(player.getId(), objectid, damage, unkByte, monsterIdFrom), summon.getPosition());
                }
            }
        }
    }

    public static final void SkillEffectHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int skillId = slea.readInt();
        int level = slea.readByte();
        if (SkillFactory.getSkill(skillId) == null || c.getPlayer().getSkillLevel(skillId) != level) {
            return;
        }
        byte flags = slea.readByte();
        int speed = slea.readByte();
        switch (skillId) {
            case Hero.MONSTER_MAGNET:
            case Paladin.MONSTER_MAGNET:
            case DarkKnight.MONSTER_MAGNET:
            case FPMage.EXPLOSION:
            case FPArchMage.BIG_BANG:
            case ILArchMage.BIG_BANG:
            case Bishop.BIG_BANG:
            case Bowmaster.HURRICANE:
            case Marksman.PIERCING_ARROW:
            case ChiefBandit.CHAKRA:
            case Brawler.CORKSCREW_BLOW:
            case Gunslinger.GRENADE:
            case Corsair.RAPID_FIRE:
            case WindArcher.HURRICANE:
            case NightWalker.POISON_BOMB:
            case ThunderBreaker.CORKSCREW_BLOW:
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), BuffFactory.skillEffect(c.getPlayer(), skillId, level, flags, speed), false);
                return;
            default:
                System.out.println(c.getPlayer() + " entered SkillEffectHandler without being handled using " + skillId + ". " + slea);
                return;
        }
    }

    public static final void SkillMacroHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int num = slea.readByte();
        for (int i = 0; i < num; i++) {
            String name = slea.readMapleAsciiString();
            int shout = slea.readByte();
            int skill1 = slea.readInt();
            int skill2 = slea.readInt();
            int skill3 = slea.readInt();
            SkillMacro macro = new SkillMacro(skill1, skill2, skill3, name, shout, i);
            c.getPlayer().updateMacros(i, macro);
            c.getPlayer().setSaveMacros();
        }
    }

    public static final void NoteActionHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int action = slea.readByte();
        if (action == 1) {
            int num = slea.readByte();
            slea.readByte();
            slea.readByte();
            for (int i = 0; i < num; i++) {
                int id = slea.readInt();
                slea.readByte();
                try {
                    PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM notes WHERE `id`=?");
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public static final void TeleportRockRecordHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte action = slea.readByte();
        byte rocktype = slea.readByte();
        if (rocktype != 0 && rocktype != 1) {
            return;
        }
        if (c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5040000) == null && c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5041000) == null) {
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        int mapId;
        if (action == 0) {
            mapId = slea.readInt();
            c.getPlayer().deleteTeleportRockMap(Integer.valueOf(mapId), rocktype);
        } else if (action == 1) {
            mapId = c.getPlayer().getMapId();
            c.getPlayer().addTeleportRockMap(Integer.valueOf(mapId), rocktype);
        }
        c.announce(IntraPersonalFactory.refreshTeleportRockMapList(c.getPlayer(), rocktype));
        c.getPlayer().setSaveTeleportMaps();
    }

    public static final void MonsterBookCoverHandler(int id, MapleClient c) {
        if (id == 0 || id / 10000 == 238) {
            c.getPlayer().setMonsterBookCover(id);
            c.announce(EffectFactory.changeCover(id));
        }
    }

    public static final void AutoDistributeAPHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        slea.skip(8);
        if (chr.getRemainingAp() < 1) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            int type = slea.readInt();
            int tempVal = slea.readInt();
            if (tempVal < 0 || tempVal > c.getPlayer().getRemainingAp()) {
                return;
            }
            int newVal = 0;
            if (type == 64) {
                newVal = chr.getStr() + tempVal;
                chr.setStr(newVal);
            } else if (type == 128) {
                newVal = chr.getDex() + tempVal;
                chr.setDex(newVal);
            } else if (type == 256) {
                newVal = chr.getInt() + tempVal;
                chr.setInt(newVal);
            } else if (type == 512) {
                newVal = chr.getLuk() + tempVal;
                chr.setLuk(newVal);
            }
            chr.updateSingleStat(type == 64 ? MapleStat.STR : (type == 128 ? MapleStat.DEX : (type == 256 ? MapleStat.INT : (type == 512 ? MapleStat.LUK : null))), newVal);
        }
        chr.setRemainingAp(0);
        chr.updateSingleStat(MapleStat.AVAILABLEAP, 0);
        c.announce(IntraPersonalFactory.enableActions());
    }

    public static final void BeholderHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        Collection<MapleSummon> summons = c.getPlayer().getSummons().values();
        int oid = slea.readInt();
        MapleSummon summon = null;
        for (MapleSummon sum : summons) {
            if (sum.getObjectId() == oid) {
                summon = sum;
            }
        }
        if (summon != null) {
            int skillId = slea.readInt();
            if (skillId == DarkKnight.AURA_OF_BEHOLDER) {
                slea.readShort();
            } else if (skillId == DarkKnight.HEX_OF_BEHOLDER) {
                slea.readByte();
            }
        } else {
            c.getPlayer().getSummons().clear();
        }
    }

    public static final void AdminCommandHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().gmLevel() != 3) {
            return;
        }
    }

    public static final void QuickSlotChange(SeekableLittleEndianAccessor slea, MapleClient c) {
        int[] slots = new int[8];
        for (int i = 0; i < 8; i++) {
            slots[i] = slea.readInt();
        }
        c.getPlayer().setQuickSlot(slots);
    }

    public static final void UseInnerPortalHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        MaplePortal portal = c.getPlayer().getMap().getPortal(slea.readMapleAsciiString());
        if (portal == null || portal.getPosition().distanceSq(c.getPlayer().getPosition()) > 22500) {
            return;
        }
        c.getPlayer().getMap().movePlayer(c.getPlayer(), new Point(slea.readShort(), slea.readShort()));
    }

    public static final void ChangeMapSpecialHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        String startwp = slea.readMapleAsciiString();
        MaplePortal portal = c.getPlayer().getMap().getPortal(startwp);
        if (portal != null) {
            portal.enterPortal(c);
        }
        c.announce(IntraPersonalFactory.enableActions());
    }
}
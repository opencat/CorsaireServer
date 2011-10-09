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

package tools.factory;

import client.ISkill;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleDisease;
import client.MapleMount;
import client.SkillEntry;
import client.SkillMacro;
import constants.skills.Buccaneer;
import constants.skills.Marauder;
import constants.skills.ThunderBreaker;
import constants.skills.WindArcher;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.MaplePacket;
import net.SendPacketOpcode;
import net.world.PlayerCoolDownValueHolder;
import server.life.MobSkill;
import server.maps.MapleMist;
import tools.DataTool;
import tools.HexTool;
import tools.Pair;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        BuffFactory
 * @author      x711Li
 */
public class BuffFactory {
    public static void addSkillInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(0);
        Map<ISkill, SkillEntry> skills = chr.getSkills();
        mplew.writeShort(skills.size());
        for (Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
            if(skill.getKey().getId() % 10000000 == 1009) {
                mplew.writeInt(skill.getKey().getId());
                mplew.writeInt(0);
            } else {
                mplew.writeInt(skill.getKey().getId());
                mplew.writeInt(skill.getValue().skillevel);
            }
            mplew.write(0);
            mplew.write(DataTool.ITEM_MAGIC);
            mplew.writeInt(400967355);
            mplew.write(2);
            if (skill.getKey().isFourthJob()) {
                mplew.writeInt(skill.getValue().masterlevel);
            }
        }
        mplew.writeShort(chr.getAllCooldowns().size());
        for (PlayerCoolDownValueHolder cooling : chr.getAllCooldowns()) {
            mplew.writeInt(cooling.skillId);
            int timeLeft = (int) (cooling.length + cooling.startTime - System.currentTimeMillis());
            mplew.writeShort(timeLeft / 1000);
        }
    }

    public static MaplePacket giveBuff(int buffid, int bufflength, List<Pair<MapleBuffStat, Integer>> statups, boolean mount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF);
        long mask = DataTool.getLongMask(statups);
        boolean isFirst = false;
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            if (statup.getLeft().isFirst()) {
                isFirst = true;
                break;
            }
        }
        if (isFirst) {
            mplew.writeLong(mask);
            mplew.writeLong(0);
        } else if (!mount) {
            mplew.writeLong(0);
            mplew.writeLong(mask);
        } else {
            mplew.writeInt(0);
            mplew.writeLong(mask);
            mplew.writeInt(0);
        }
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().shortValue());
            mplew.writeInt(buffid);
            mplew.writeInt(bufflength);
        }
        if (mount) {
            mplew.writeInt(0);
        } else {
            mplew.writeShort(0);
        }
        mplew.write(0); // combo 600, too
        mplew.write(0); // new in v0.56
        mplew.write(0);
        if (mount) {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket giveMount(int buffid, int skillid, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        DataTool.writeLongMask(mplew, statups);
        mplew.writeShort(0);
        mplew.writeInt(buffid);
        mplew.writeInt(skillid);
        mplew.write(HexTool.getByteArrayFromHexString("01 01 00 00 00 00 00 02"));
        return mplew.getPacket();
    }

    public static MaplePacket showMonsterRiding(int cid, List<Pair<MapleBuffStat, Integer>> statups, MapleMount mount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(MapleBuffStat.MONSTER_RIDING.getValue());
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.writeInt(mount.getItemId());
        mplew.writeInt(mount.getSkillId());
        mplew.write0(7);
        return mplew.getPacket();
    }
    
    public static MaplePacket giveDebuff(List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        long mask = DataTool.getLongMaskD(statups);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        for (Pair<MapleDisease, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().shortValue());
            mplew.writeShort(skill.getSkillId());
            mplew.writeShort(skill.getSkillLevel());
            mplew.writeInt((int) skill.getDuration());
        }
        mplew.writeShort(0);
        mplew.writeShort(900);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket giveForeignDebuff(int cid, List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        long mask = DataTool.getLongMaskD(statups);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        for (int i = 0; i < statups.size(); i++) {
            if (statups.get(i).getLeft().equals(MapleDisease.POISON)) {
                mplew.writeShort(statups.get(i).getRight());
            }
            mplew.writeShort(skill.getSkillId());
            mplew.writeShort(skill.getSkillLevel());
        }
        mplew.writeShort(0);
        mplew.writeShort(900);//Delay
        mplew.write0(4);
        return mplew.getPacket();
    }

    public static MaplePacket cancelForeignDebuff(int cid, long mask) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        return mplew.getPacket();
    }

    public static MaplePacket cancelDebuff(long mask) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
        mplew.writeLong(0);
        mplew.writeLong(mask);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket giveForeignBuff(int cid, List<Pair<MapleBuffStat, Integer>> statups, boolean morph) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        long mask = DataTool.getLongMask(statups);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            if (morph) {
                mplew.writeInt(statup.getRight().intValue());
            } else {
                mplew.writeShort(statup.getRight().shortValue());
            }
        }
        mplew.write(0);
        mplew.writeShort(0); //delay?
        return mplew.getPacket();
    }

    public static MaplePacket cancelForeignBuff(int cid, List<MapleBuffStat> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        long mask = DataTool.getLongMaskFromList(statups);
        if (!DataTool.isFirstLong(statups)) {
            if (mask == MapleBuffStat.MONSTER_RIDING.getValue() || mask == MapleBuffStat.BATTLESHIP.getValue()) {
                mplew.writeInt(0);
                mplew.writeLong(mask);
                mplew.writeInt(0);
            } else {
                mplew.writeLong(0);
                mplew.writeLong(mask);
            }
        } else {
            mplew.writeLong(mask);
            mplew.writeLong(0);
        }
        return mplew.getPacket();
    }
    
    public static MaplePacket cancelBuff(List<MapleBuffStat> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
        long mask = DataTool.getLongMaskFromList(statups);
        if (!DataTool.isFirstLong(statups)) {
            if (mask == MapleBuffStat.MONSTER_RIDING.getValue() || mask == MapleBuffStat.BATTLESHIP.getValue()) {
                mplew.writeInt(0);
                mplew.writeLong(mask);
                mplew.writeInt(0);
            } else {
                mplew.writeLong(0);
                mplew.writeLong(mask);
            }
        } else {
            mplew.writeLong(mask);
            mplew.writeLong(0);
        }
        mplew.write(mask == MapleBuffStat.DASH.getValue() ? 4 : 3);
        return mplew.getPacket();
    }
    
    public static MaplePacket giveHomingBeacon(int buffid, int moid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        mplew.writeLong(MapleBuffStat.HOMING_BEACON.getValue());
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.writeInt(1);
        mplew.writeInt(buffid);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(moid);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket addComboBuff(int combo) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue()); // actually it's the same as giveBuff
        mplew.writeLong(MapleBuffStat.ARAN_COMBO.getValue());
        mplew.writeLong(0);
        mplew.writeShort(combo);
        mplew.writeInt(21000000);
        mplew.writeInt(0x100);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket showBuffEffect(MapleCharacter player, int cid, int skillid, int effectid) {
        return showBuffEffect(player, cid, skillid, effectid, (byte) 3);
    }

    public static MaplePacket showBuffEffect(MapleCharacter player, int cid, int skillid, int effectid, byte direction) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(13);
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid); // ?
        mplew.write(effectid); //buff level
        mplew.writeInt(skillid);
        mplew.write(2); // don't know
        mplew.write(player.getSkillLevel(skillid));
        mplew.write0(4);
        return mplew.getPacket();
    }

    public static MaplePacket showBuffEffect(int cid, int skillid, int effectid) {
        return showBuffEffect(cid, skillid, effectid, (byte) 3);
    }

    public static MaplePacket showBuffEffect(int cid, int skillid, int effectid, byte direction) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(12);
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        if (skillid == Buccaneer.SUPER_TRANSFORMATION || skillid == Marauder.TRANSFORMATION || skillid == WindArcher.EAGLE_EYE || skillid == ThunderBreaker.TRANSFORMATION) {
            mplew.write(1);
            mplew.writeInt(skillid);
            mplew.write(direction);
            mplew.write(1);
        } else {
            mplew.write(effectid);
            mplew.writeInt(skillid);
            mplew.write(direction);
            mplew.write(1);
        }
        return mplew.getPacket();
    }

    public static MaplePacket showOwnBuffEffect(int skillid, int effectid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(8);
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(0xA9);
        mplew.write(1); // probably buff level but we don't know it and it doesn't really matter
        return mplew.getPacket();
    }

    public static MaplePacket showOwnBerserk(int skilllevel, boolean berserk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(9);
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(1);
        mplew.writeInt(1320006);
        mplew.write(0xA9);
        mplew.write(skilllevel);
        mplew.write(berserk ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket showBerserk(int cid, int skilllevel, boolean berserk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(13);
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(1);
        mplew.writeInt(1320006);
        mplew.write(0xA9);
        mplew.write(skilllevel);
        mplew.write(berserk ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket updateSkill(int skillid, int level, int masterlevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_SKILLS.getValue());
        mplew.write(1);
        mplew.writeShort(1);
        mplew.writeInt(skillid);
        mplew.writeInt(level);
        mplew.writeInt(masterlevel);
        mplew.write(0);//*
        mplew.write(DataTool.ITEM_MAGIC);//*
        DataTool.addExpirationTime(mplew, 0, false);//*
        mplew.write(1);
        return mplew.getPacket();
    }
    
    public static MaplePacket giveSpeedInfusion(int buffid, int bufflength, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        DataTool.writeLongMask(mplew, statups);
        mplew.writeShort(0);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().shortValue());
            mplew.writeShort(-1);
            mplew.writeInt(buffid);
            mplew.write0(10);
            mplew.writeShort(bufflength);
        }
        mplew.write(0x58);
        mplew.write(0x02);
        return mplew.getPacket();
    }

    public static MaplePacket givePirateBuff(int buffid, int bufflength, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        DataTool.writeLongMask(mplew, statups);
        mplew.writeShort(0);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().shortValue());
            mplew.writeShort(0);
            mplew.writeInt(buffid);
            mplew.write0(5);
            mplew.writeShort(bufflength);
        }
        mplew.write0(3);
        return mplew.getPacket();
    }

    public static MaplePacket showPirateBuff(int cid, int skillid, int time, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        long mask = DataTool.getLongMask(statups);
        mplew.writeLong(mask);
        mplew.writeLong(0);
        mplew.writeShort(0);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeShort(statup.getRight());
            mplew.writeShort(0);
            mplew.writeInt(skillid);
            mplew.writeInt(0);
            mplew.write(0);
            mplew.writeShort(time);
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket showSpeedInfusion(int cid, int skillid, int time, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        long mask = DataTool.getLongMask(statups);
        mplew.writeLong(mask);
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.writeInt(statups.get(0).getRight());
        mplew.writeInt(skillid);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.writeShort(time);
        mplew.writeShort(0);
        return mplew.getPacket();
    }
    
    public static MaplePacket spawnMist(int oid, int ownerCid, int skill, int level, MapleMist mist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_MIST.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(mist.getMistType().ordinal());
        mplew.writeInt(ownerCid);
        mplew.writeInt(skill);
        mplew.write(level);
        mplew.writeShort(mist.getSkillDelay()); // Skill delay
        mplew.writeInt(mist.getBox().x);
        mplew.writeInt(mist.getBox().y);
        mplew.writeInt(mist.getBox().x + mist.getBox().width);
        mplew.writeInt(mist.getBox().y + mist.getBox().height);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket spawnMist(final MapleMist mist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_MIST.getValue());
        mplew.writeInt(mist.getObjectId());
        mplew.writeInt(mist.getMistType().ordinal());

        if (mist.getOwner() != null) {
            mplew.writeInt(mist.getOwner().getId());
            mplew.writeInt(mist.getSourceSkill().getId());
            mplew.write(mist.getSkillLevel());
        } else {
            mplew.writeInt(mist.getMobOwner().getId());
            mplew.writeInt(mist.getMobSkill().getSkillId());
            mplew.write(mist.getMobSkill().getSkillLevel());
        }
        mplew.writeShort(mist.getSkillDelay());
        mplew.writeInt(mist.getBox().x);
        mplew.writeInt(mist.getBox().y);
        mplew.writeInt(mist.getBox().x + mist.getBox().width);
        mplew.writeInt(mist.getBox().y + mist.getBox().height);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static MaplePacket removeMist(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_MIST.getValue());
        mplew.writeInt(oid);
        return mplew.getPacket();
    }
    
    public static MaplePacket skillEffect(MapleCharacter from, int skillId, int level, byte flags, int speed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);
        mplew.write(level);
        mplew.write(flags);
        //mplew.write(0);
        mplew.write(speed);
        mplew.write0(8);
        return mplew.getPacket();
    }

    public static MaplePacket skillCancel(MapleCharacter from, int skillId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.write(1);
        mplew.writeInt(skillId);
        mplew.write0(8);
        return mplew.getPacket();
    }
    
    public static MaplePacket showMagnet(int mobid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(17);
        mplew.writeShort(SendPacketOpcode.SHOW_MAGNET.getValue());
        mplew.writeInt(mobid);
        mplew.write(success);
        mplew.write0(10);
        return mplew.getPacket();
    }
    
    public static MaplePacket skillCooldown(int sid, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.COOLDOWN.getValue());
        mplew.writeInt(sid);
        mplew.writeShort(time);
        return mplew.getPacket();
    }

    public static MaplePacket getMacros(SkillMacro[] macros) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SKILL_MACRO.getValue());
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (macros[i] != null) {
                count++;
            }
        }
        mplew.write(count);
        for (int i = 0; i < 5; i++) {
            SkillMacro macro = macros[i];
            if (macro != null) {
                mplew.writeMapleAsciiString(macro.getName());
                mplew.write(macro.getShout());
                mplew.writeInt(macro.getSkill1());
                mplew.writeInt(macro.getSkill2());
                mplew.writeInt(macro.getSkill3());
            }
        }
        return mplew.getPacket();
    }
    
    public static MaplePacket giveFinalAttack(int skillid, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.write(0);//some 80 and 0 bs
        mplew.write(0x80);//let's just do 80, then 0
        mplew.writeInt(0);
        mplew.writeShort(1);
        mplew.writeInt(skillid);
        mplew.writeInt(time);
        mplew.writeInt(0);
        return mplew.getPacket();
    }
    
    public static MaplePacket giveEnergyCharge(int barammount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        long mask = 0;
        mask |= MapleBuffStat.ENERGY_CHARGE.getValue();
        mplew.writeLong(mask);
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.writeShort(barammount); // 0 = no bar, 10000 = full bar
        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.write(0);
        mplew.writeInt(50);
        return mplew.getPacket();
    }

    public static MaplePacket giveForeignEnergyCharge(int cid, int barammount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(MapleBuffStat.ENERGY_CHARGE.getValue());
        mplew.write0(10);
        mplew.writeShort(barammount); // 0 = no bar, 15000 = full bar
        mplew.write0(11);
        mplew.writeInt(50);
        return mplew.getPacket();
    }
}
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

import client.status.MonsterStatus;
import constants.skills.ChiefBandit;
import java.util.List;
import java.util.Map;
import net.MaplePacket;
import net.SendPacketOpcode;
import server.life.AttackInfo;
import server.life.MapleMonster;
import server.life.MobSkill;
import tools.Pair;
import tools.data.output.LittleEndianWriter;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        MobFactory
 * @author      x711Li
 */
public class MobFactory {
    private static void addSpawnInfo(LittleEndianWriter lew, MapleMonster life, int effect, boolean spawn, boolean newSpawn, boolean fake, boolean fr) {
        lew.writeInt(life.getObjectId());
        lew.write(life.getController() == null ? 5 : 1);
        lew.writeInt(life.getId());
        lew.write(new byte[15]);
        lew.write(0x88);
        lew.write(new byte[6]);
        lew.writeShort(life.getPosition().x);
        lew.writeShort(life.getPosition().y);
        lew.write(life.getStance());
        lew.writeShort(life.getStartFh());
        lew.writeShort(life.getFh());
        if (effect > 0) {
            lew.write(effect);
            lew.write(0);
            lew.writeShort(0);
            if (effect == 15) {
                lew.write(0);
            }
        }
        if (spawn) {
            if (newSpawn) {
                lew.writeShort(-2);
            } else {
                lew.writeShort(-1);
            }
            lew.writeInt(0);
            return;
        }
        if (fr) {
            lew.writeShort(fake ? -2 : -1);
        } else {
            lew.write(-1);
        }
        lew.writeInt(0);
    }

    private static void addAttackBody(LittleEndianWriter lew, AttackInfo ai, int cid, int projectile, List<Pair<Integer, List<Integer>>> damage, boolean stance) {
        lew.writeInt(cid);
        lew.write(ai.numAttackedAndDamage);
        lew.write(0x5B);
        if (ai.skill > 0) {
            lew.write(0xFF);
            lew.writeInt(ai.skill);
        } else {
            lew.write(0);
        }
        lew.write(stance ? ai.display : ai.direction);
        lew.write(ai.v80thing);
        lew.write(stance ? ai.stance : ai.direction);
        lew.write(ai.speed);
        lew.write(0x0A);
        lew.writeInt(projectile);
        for (Pair<Integer, List<Integer>> oned : damage) {
            if (oned.getRight() != null) {
                lew.writeInt(oned.getLeft().intValue());
                lew.write(0x06);
                if (ai.skill == ChiefBandit.MESO_EXPLOSION) {
                    lew.write(oned.getRight().size());
                }
                for (int eachd : oned.getRight()) {
                    lew.writeInt(eachd);
                }
            }
        }
    }

    public static MaplePacket closeRangeAttack(AttackInfo ai, int cid, List<Pair<Integer, List<Integer>>> damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CLOSE_RANGE_ATTACK.getValue());
        addAttackBody(mplew, ai, cid, 0, damage, true);
        return mplew.getPacket();
    }

    public static MaplePacket rangedAttack(AttackInfo ai, int cid, int projectile, List<Pair<Integer, List<Integer>>> damage, boolean hurricane) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.RANGED_ATTACK.getValue());
        addAttackBody(mplew, ai, cid, projectile, damage, hurricane);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket magicAttack(AttackInfo ai, int cid, List<Pair<Integer, List<Integer>>> damage, int charge) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAGIC_ATTACK.getValue());
        addAttackBody(mplew, ai, cid, 0, damage, true);
        if (charge != -1) {
            mplew.writeInt(charge);
        }
        return mplew.getPacket();
    }

    public static MaplePacket damagePlayer(int skill, int monsteridfrom, int cid, int damage, int fake, int direction, boolean pgmr, int pgmr_1, boolean is_pg, int oid, int pos_x, int pos_y) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DAMAGE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.write(skill);
        mplew.writeInt(damage);
        mplew.writeInt(monsteridfrom);
        mplew.write(direction);
        if (pgmr) {
            mplew.write(pgmr_1);
            mplew.write(is_pg ? 1 : 0);
            mplew.writeInt(oid);
            mplew.write(6);
            mplew.writeShort(pos_x);
            mplew.writeShort(pos_y);
            mplew.write(0);
        } else {
            mplew.writeShort(0);
        }
        mplew.writeInt(damage);
        if (fake > 0) {
            mplew.writeInt(fake);
        }
        return mplew.getPacket();
    }

    public static MaplePacket mobDamageMobFriendly(MapleMonster mob, int damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.write(1); // direction ?
        mplew.writeInt(damage);
        mplew.writeInt(mob.getHp() - damage);
        mplew.writeInt(mob.getMaxHp());
        return mplew.getPacket();
    }

    public static MaplePacket showMonsterHP(int oid, int remhppercentage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_MONSTER_HP.getValue());
        mplew.writeInt(oid);
        mplew.write(remhppercentage);
        return mplew.getPacket();
    }

    public static MaplePacket showBossHP(int oid, int currHP, int maxHP, byte tagColor, byte tagBgColor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(5);
        mplew.writeInt(oid);
        mplew.writeInt(currHP);
        mplew.writeInt(maxHP);
        mplew.write(tagColor);
        mplew.write(tagBgColor);
        return mplew.getPacket();
    }

    public static MaplePacket applyMonsterStatus(int oid, Map<MonsterStatus, Integer> stats, int skill, boolean monsterSkill, int delay) {
        return applyMonsterStatus(oid, stats, skill, monsterSkill, delay, null);
    }

    public static MaplePacket applyMonsterStatus(int oid, Map<MonsterStatus, Integer> stats, int skill, boolean monsterSkill, int delay, MobSkill mobskill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        mplew.writeLong(0);
        mplew.writeInt(0);
        int mask = 0;
        for (MonsterStatus stat : stats.keySet()) {
            mask |= stat.getValue();
        }
        mplew.writeInt(mask);
        for (Integer val : stats.values()) {
            mplew.writeShort(val);
            if (monsterSkill) {
                mplew.writeShort(mobskill.getSkillId());
                mplew.writeShort(mobskill.getSkillLevel());
            } else {
                mplew.writeInt(skill);
            }
            mplew.writeShort(-1); // as this looks similar to giveBuff this
        }
        mplew.writeShort(delay); // delay in ms
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket cancelMonsterStatus(int oid, Map<MonsterStatus, Integer> stats) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        mplew.writeLong(0);//?
        mplew.writeInt(0);//?
        int mask = 0;
        for (MonsterStatus stat : stats.keySet()) {
            mask |= stat.getValue();
        }
        mplew.writeInt(mask);
        mplew.writeInt(0); //old:write 1
        return mplew.getPacket();
    }

    public static MaplePacket damageMonster(int oid, int damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeInt(damage);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket healMonster(int oid, int heal) {
        return damageMonster(oid, -heal);
    }

    public static MaplePacket spawnMonster(MapleMonster life, boolean newSpawn) {
        return spawnMonsterInternal(life, false, newSpawn, false, 0, false);
    }

    public static MaplePacket spawnMonster(MapleMonster life, boolean newSpawn, int effect) {
        return spawnMonsterInternal(life, false, newSpawn, false, effect, false);
    }

    public static MaplePacket controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
        return spawnMonsterInternal(life, true, newSpawn, aggro, 0, false);
    }

    public static MaplePacket makeMonsterInvisible(MapleMonster life) {
        return spawnMonsterInternal(life, true, false, false, 0, true);
    }

    private static MaplePacket spawnMonsterInternal(MapleMonster life, boolean requestController, boolean newSpawn, boolean aggro, int effect, boolean makeInvis) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (makeInvis) {
            mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
            mplew.write(0);
            mplew.writeInt(life.getObjectId());
            return mplew.getPacket();
        }
        if (requestController) {
            mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
            if (aggro) {
                mplew.write(2);
            } else {
                mplew.write(1);
            }
        } else {
            mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
        }
        addSpawnInfo(mplew, life, effect, true, newSpawn, false, false);
        return mplew.getPacket();
    }

    public static MaplePacket spawnFakeMonster(MapleMonster life, int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(1);
        addSpawnInfo(mplew, life, effect, false, false, true, true);
        return mplew.getPacket();
    }

    public static MaplePacket makeMonsterReal(MapleMonster life) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(30);
        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
        addSpawnInfo(mplew, life, 0, false, false, false, true);
        return mplew.getPacket();
    }

    public static MaplePacket stopControllingMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(0);
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    public static MaplePacket killMonster(int oid, boolean animation) {
        return killMonster(oid, animation ? 1 : 0);
    }

    public static MaplePacket killMonster(int oid, int animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(8);
        mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(animation);
        mplew.write(animation); // Not a boolean, really an int type
        return mplew.getPacket();
    }
}
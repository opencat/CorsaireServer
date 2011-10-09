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

import client.MapleCharacter;
import client.MapleClient;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.AnimatedMapleMapObject;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleSummon;
import server.movement.AbsoluteLifeMovement;
import server.movement.AranComboMovement;
import server.movement.ChairMovement;
import server.movement.ChangeEquip;
import server.movement.JumpDownMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import server.movement.RelativeLifeMovement;
import server.movement.TeleportMovement;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.LittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.factory.MovementFactory;

/**
 * @name        MovementOperation
 * @author      x711Li
 */
public class MovementOperation {
    protected static List<LifeMovementFragment> parseMovement(LittleEndianAccessor lea) {
        List<LifeMovementFragment> res = new ArrayList<LifeMovementFragment>();
        int numCommands = lea.readByte();
        for (int i = 0; i < numCommands; i++) {
            int command = lea.readByte();
            switch (command) {
                case 0: // normal move
                case 5:
                case 17: { // Float
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short xwobble = lea.readShort();
                    short ywobble = lea.readShort();
                    short unk = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    alm.setUnk(unk);
                    alm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(alm);
                    break;
                }
                case 1:
                case 2:
                case 6: // fj
                case 12:
                case 13: // Shot-jump-back thing
                case 16:
                case 20:
                { // Float
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    res.add(rlm);
                    break;
                }
                case 3:
                case 4: // tele... -.-
                case 7: // assaulter
                case 8: // assassinate
                case 9: // rush
                case 14: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short xwobble = lea.readShort();
                    short ywobble = lea.readShort();
                    byte newstate = lea.readByte();
                    TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), newstate);
                    tm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(tm);
                    break;
                }
                case 10: // Change Equip
                    byte equip = lea.readByte();
                    res.add(new ChangeEquip(equip));
                    break;
                case 11: { // Chair
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short unk = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate);
                    cm.setUnk(unk);
                    res.add(cm);
                    break;
                }
                case 15: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short xwobble = lea.readShort();
                    short ywobble = lea.readShort();
                    short unk = lea.readShort();
                    short fh = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    JumpDownMovement jdm = new JumpDownMovement(command, new Point(xpos, ypos), duration, newstate);
                    jdm.setUnk(unk);
                    jdm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    jdm.setFH(fh);
                    res.add(jdm);
                    break;
                }
                case 21: { //combo
                    byte newstate = lea.readByte();
                    short unk = lea.readShort();
                    AranComboMovement acm = new AranComboMovement(command, new Point(0, 0), unk, newstate);
                    res.add(acm);
                    break;
                }
                default:
                    return null;
            }
        }
        if (numCommands != res.size()) {
            return null;
        }
        return res;
    }

    protected static void updatePosition(List<LifeMovementFragment> movement, AnimatedMapleMapObject target, int yoffset) {
        for (LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    Point position = ((LifeMovement) move).getPosition();
                    position.y += yoffset;
                    target.setPosition(position);
                }
                target.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }

    public static final void MoveLifeHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int objectid = slea.readInt();
        short moveid = slea.readShort();
        MapleMapObject mmo = c.getPlayer().getMap().getMapObject(objectid);
        if (mmo == null || mmo.getType() != MapleMapObjectType.MONSTER) {
            return;
        }
        MapleMonster monster = (MapleMonster) mmo;
        List<LifeMovementFragment> res = null;
        byte skillByte = slea.readByte();
        byte skill = slea.readByte();
        int skill_1 = slea.readByte() & 0xFF;
        byte skill_2 = slea.readByte();
        byte skill_3 = slea.readByte();
        slea.readByte();
        MobSkill toUse = null;
        if (skillByte == 1 && monster.getNoSkills() > 0) {
            Pair<Integer, Integer> skillToUse = monster.getSkills().get(Randomizer.getInstance().nextInt(monster.getNoSkills()));
            toUse = MobSkillFactory.getMobSkill(skillToUse.getLeft(), skillToUse.getRight());
            if (toUse.getHP() < (monster.getHp() / monster.getMaxHp()) * 100 || !monster.canUseSkill(toUse)) {
                toUse = null;
            }
        }
        if ((skill_1 >= 100 && skill_1 <= 200) && monster.hasSkill(skill_1, skill_2)) {
            MobSkill skillData = MobSkillFactory.getMobSkill(skill_1, skill_2);
            if (skillData != null && monster.canUseSkill(skillData)) {
                skillData.applyEffect(c.getPlayer(), monster, true);
            }
        }
        slea.skip(13);
        short start_x = slea.readShort(); // hmm.. startpos?
        short start_y = slea.readShort(); // hmm...
        Point startPos = new Point(start_x, start_y);
        res = parseMovement(slea);
        if (monster.getController() != c.getPlayer()) {
            if (monster.isAttackedBy(c.getPlayer())) {// aggro and controller change
                monster.switchController(c.getPlayer(), true);
            } else {
                return;
            }
        } else if (skill == -1 && monster.isControllerKnowsAboutAggro() && !monster.isMobile() && !monster.isFirstAttack()) {
            monster.setControllerHasAggro(false);
            monster.setControllerKnowsAboutAggro(false);
        }
        boolean aggro = monster.isControllerHasAggro();
        if (toUse != null) {
            c.announce(MovementFactory.moveMonsterResponse(objectid, moveid, monster.getMp(), aggro, toUse.getSkillId(), toUse.getSkillLevel()));
        } else {
            c.announce(MovementFactory.moveMonsterResponse(objectid, moveid, monster.getMp(), aggro));
        }
        if (aggro) {
            monster.setControllerKnowsAboutAggro(true);
        }
        if (res != null && slea.available() == 9) {
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MovementFactory.moveMonster(skillByte, skill, skill_1, skill_2, skill_3, objectid, startPos, res), monster.getPosition());
            updatePosition(res, monster, -1);
            c.getPlayer().getMap().moveMonster(monster, monster.getPosition());
        }
    }

    public static final void MovePlayerHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(9);
        final List<LifeMovementFragment> res = parseMovement(slea);
        if (res != null && res.size() > 0) {
            if (c.getPlayer().isHidden()) {
                c.getPlayer().getMap().broadcastGMMessage(c.getPlayer(), MovementFactory.movePlayer(c.getPlayer().getId(), res), false);
            } else {
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MovementFactory.movePlayer(c.getPlayer().getId(), res), false);
            }
            final MapleMap map = c.getPlayer().getMap();
            updatePosition(res, c.getPlayer(), 0);
            map.movePlayer(c.getPlayer(), c.getPlayer().getPosition());
            int count = c.getPlayer().getFallCounter(); // from vana
            if (map.getFootholds().findBelow(c.getPlayer().getPosition()) == null) {
                if (count > 3) {
                    c.getPlayer().changeMap(map, map.getPortal(0));
                } else {
                    c.getPlayer().setFallCounter(++count);
                }
            } else if (count > 0) {
                c.getPlayer().setFallCounter(0);
            }
        }
    }

    public static final void MoveSummonHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        Point startPos = new Point(slea.readShort(), slea.readShort());
        List<LifeMovementFragment> res = parseMovement(slea);
        MapleCharacter player = c.getPlayer();
        Collection<MapleSummon> summons = player.getSummons().values();
        MapleSummon summon = null;
        for (MapleSummon sum : summons) {
            if (sum.getObjectId() == oid) {
                summon = sum;
                break;
            }
        }
        if (summon != null) {
            updatePosition(res, summon, 0);
            player.getMap().broadcastMessage(player, MovementFactory.moveSummon(player.getId(), oid, startPos, res), summon.getPosition());
        }
    }

    public static final void MovePetHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int petId = slea.readInt();
        slea.readLong();
        List<LifeMovementFragment> res = parseMovement(slea);
        if (res.size() == 0) {
            return;
        }
        MapleCharacter player = c.getPlayer();
        int slot = player.getPetIndex(petId);
        if (slot == -1) {
            return;
        }
        player.getPet(slot).updatePosition(res);
        player.getMap().broadcastMessage(player, MovementFactory.movePet(player.getId(), petId, slot, res), false);
    }
}
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

import client.ISkill;
import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;
import constants.skills.BlazeWizard;
import constants.skills.FPMage;
import constants.skills.NightWalker;
import constants.skills.Shadower;
import java.awt.Point;
import java.awt.Rectangle;
import net.MaplePacket;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.life.MobSkill;
import tools.factory.BuffFactory;

/**
 * @name        MapleMist
 * @author      LaiLaiNoob
 *              Modified by x711Li
 */
public class MapleMist extends AbstractMapleMapObject {
    private Rectangle mistPosition;
    private MapleCharacter owner = null;
    private MapleMonster mob = null;
    private MapleStatEffect source;
    private MobSkill skill;
    private MapleMistType type;
    private int skillDelay, skillLevel;

    public MapleMist(Rectangle mistPosition, MapleMonster mob, MobSkill skill) {
        this.mistPosition = mistPosition;
        this.mob = mob;
        this.skill = skill;
        skillLevel = owner.getSkillLevel(SkillFactory.getSkill(source.getSourceId()));
        type = MapleMistType.MONSTER;
        skillDelay = 0;
    }

    public MapleMist(Rectangle mistPosition, MapleCharacter owner, MapleStatEffect source) {
        this.mistPosition = mistPosition;
        this.owner = owner;
        this.source = source;
        this.skillDelay = 8;
        switch (source.getSourceId()) {
            case Shadower.SMOKE_SCREEN: // Smoke Screen
                type = MapleMistType.NORMAL;
                break;
            case FPMage.POISON_MIST: // FP mist
            case BlazeWizard.FLAME_GEAR: // Flame Gear
            case NightWalker.POISON_BOMB: // Poison Bomb
                type = MapleMistType.POISON;
                break;
        }
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.MIST;
    }

    @Override
    public Point getPosition() {
        return mistPosition.getLocation();
    }

    public ISkill getSourceSkill() {
        return SkillFactory.getSkill(source.getSourceId());
    }

    public MobSkill getMobSkill() {
        return this.skill;
    }

    public MapleMistType getMistType() {
        return type;
    }
    
    public int getSkillDelay() {
        return skillDelay;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public MapleMonster getMobOwner() {
        return mob;
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public Rectangle getBox() {
        return mistPosition;
    }

    @Override
    public void setPosition(Point position) {
        throw new UnsupportedOperationException();
    }

    public MaplePacket makeDestroyData() {
        return BuffFactory.removeMist(getObjectId());
    }

    public MaplePacket makeSpawnData() {
        if (owner != null) {
            return BuffFactory.spawnMist(getObjectId(), owner.getId(), getSourceSkill().getId(), owner.getSkillLevel(SkillFactory.getSkill(source.getSourceId())), this);
        }
        return BuffFactory.spawnMist(getObjectId(), mob.getId(), skill.getSkillId(), skill.getSkillLevel(), this);
    }

    public MaplePacket makeFakeSpawnData(int level) {
        if (owner != null) {
            return BuffFactory.spawnMist(getObjectId(), owner.getId(), getSourceSkill().getId(), level, this);
        }
        return BuffFactory.spawnMist(getObjectId(), mob.getId(), skill.getSkillId(), skill.getSkillLevel(), this);
    }

    @Override
    public void sendSpawnData(final MapleClient client) {
        client.announce(makeSpawnData());
    }

    @Override
    public void sendDestroyData(final MapleClient client) {
        client.announce(makeDestroyData());
    }

    public boolean makeChanceResult() {
        return source.makeChanceResult();
    }
}

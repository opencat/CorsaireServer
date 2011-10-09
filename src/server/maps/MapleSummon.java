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

import java.awt.Point;
import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;
import tools.factory.SummonFactory;

/**
 * @name        MapleSummon
 * @author      Jan
 *              Modified by x711Li
 */
public class MapleSummon extends AbstractAnimatedMapleMapObject {
    private final int ownerid;
    private final int skillLevel;
    private final int skill;
    private int hp;
    private SummonMovementType movementType;

    public MapleSummon(final MapleCharacter owner, final int skill, final Point pos, final SummonMovementType movementType) {
        this.ownerid = owner.getId();
        this.skill = skill;
        this.skillLevel = owner.getSkillLevel(SkillFactory.getSkill(skill));
        if (skillLevel == 0) {
            return;
        }
        this.movementType = movementType;
        setPosition(pos);
    }

    public final void sendSpawnData(final MapleClient client) {
        if (this != null) {
            client.announce(SummonFactory.spawnSpecialMapObject(this, skillLevel, false));
        }
    }

    public final void sendDestroyData(final MapleClient client) {
        client.announce(SummonFactory.removeSpecialMapObject(this, true));
    }

    public final int getOwnerId() {
        return ownerid;
    }

    public final int getSkill() {
        return skill;
    }

    public final int getHP() {
        return hp;
    }

    public final void addHP(final int delta) {
        this.hp += delta;
    }

    public final SummonMovementType getMovementType() {
        return movementType;
    }

    public final boolean isStationary() {
        return (skill == 3111002 || skill == 3211002 || skill == 5211001 || skill == 13111004);
    }

    public final int getSkillLevel() {
        return skillLevel;
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.SUMMON;
    }
}

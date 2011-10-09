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

import client.ISkill;
import client.MapleCharacter;
import client.SkillFactory;
import java.util.List;
import server.MapleStatEffect;
import tools.Pair;

/**
 * @name        AttackInfo
 * @author      x711Li
 */
public class AttackInfo {
    public int numAttacked, numDamage, numAttackedAndDamage, skill, stance, direction, charge, display;
    public int v80thing;
    public int xCoord, yCoord;
    public List<Pair<Integer, List<Integer>>> allDamage;
    public boolean isHH = false;
    public int speed = 4;

    public MapleStatEffect getAttackEffect(MapleCharacter chr, ISkill theSkill) {
        ISkill mySkill = theSkill;
        if (mySkill == null) {
            mySkill = SkillFactory.getSkill(skill);
        }
        int skillLevel = chr.getSkillLevel(mySkill);
        if (skillLevel == 0) {
            return null;
        }
        return mySkill.getEffect(skillLevel);
    }
}
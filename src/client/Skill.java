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

package client;

import java.util.ArrayList;
import java.util.List;

import server.MapleStatEffect;
import server.life.Element;

/**
 * @name        Skill
 * @author      Matze
 *              Modified by x711Li
 */
public class Skill implements ISkill {
    public int id;
    public final List<MapleStatEffect> effects = new ArrayList<MapleStatEffect>();
    public Element element;
    public int animationTime;
    public int masterLevel;
    public boolean isBuff;
    public boolean isInvisible;

    public Skill(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public MapleStatEffect getEffect(final int level) {
        return effects.get(level - 1);
    }

    @Override
    public int getMaxLevel() {
        return effects.size();
    }

    @Override
    public boolean isFourthJob() {
        return (id / 10000) % 10 == 2;
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public int getAnimationTime() {
        return animationTime;
    }

    @Override
    public boolean isBeginnerSkill() {
        return id % 10000000 < 10000;
    }

    //v88

    public int getMasterLevel() {
        return masterLevel;
    }

    public void setIsBuff(boolean isBuff) {
        this.isBuff = isBuff;
    }

    public boolean isBuff() {
        return isBuff;
    }
    public void setIsInvisible(boolean isInvisible) {
        this.isInvisible = isInvisible;
    }

    public boolean isInvisible() {
        return isInvisible;
    }
}
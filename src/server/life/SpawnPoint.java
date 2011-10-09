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

import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;
import client.MapleCharacter;
import server.maps.MapleMap;

/**
 * @name        SpawnPoint
 * @author      Matze
 */
public class SpawnPoint {
    private int monsterId; // yay for not having a bunch of bloated objects in memory
    private Point pos;
    private long nextPossibleSpawn;
    private int mobTime;
    private AtomicInteger spawnedMonsters = new AtomicInteger(0);
    private boolean immobile;

    public SpawnPoint(final int monsterId, final Point pos, final int mobTime, final boolean mobile) {
        this.monsterId = monsterId;
        this.pos = new Point(pos);
        this.mobTime = mobTime;
        this.immobile = !mobile;
        this.nextPossibleSpawn = System.currentTimeMillis();
    }

    public final boolean shouldSpawn() {
        return shouldSpawn(System.currentTimeMillis());
    }

    private final boolean shouldSpawn(long now) {
        if (mobTime < 0 || ((mobTime != 0 || immobile) && spawnedMonsters.get() > 0) || spawnedMonsters.get() > 2) {
            return false;
        }
        return nextPossibleSpawn <= now;
    }

    private final boolean spawned() {
        return spawnedMonsters.get() > 0;
    }

    public final MapleMonster spawnMonster(final MapleMap mapleMap) {
        final MapleMonster mob = MapleLifeFactory.getInstance().getMonster(monsterId);
        mob.setPosition(new Point(pos));
        spawnedMonsters.incrementAndGet();
        mob.addListener(new MonsterListener() {
            public void monsterKilled(MapleMonster monster, MapleCharacter highestDamageChar) {
                nextPossibleSpawn = System.currentTimeMillis();
                if (mobTime > 0) {
                    nextPossibleSpawn += mobTime * 1000;
                } else {
                    nextPossibleSpawn += monster.getAnimationTime("die1");
                }
                spawnedMonsters.decrementAndGet();
            }
        });
        mapleMap.spawnMonster(mob);
        if (mobTime == 0) {
            nextPossibleSpawn = System.currentTimeMillis() + 5000;
        }
        return mob;
    }

    public Point getPosition() {
        return pos;
    }
}

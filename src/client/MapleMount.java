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

import java.util.concurrent.ScheduledFuture;
import java.lang.ref.WeakReference;
import java.io.Serializable;
import server.TimerManager;
import tools.factory.IntraPersonalFactory;

/**
 * @name        MapleMount
 * @author      PurpleMadness
 *              Modified by x711Li
 */
public class MapleMount implements Serializable {
    private static final long serialVersionUID = 9179541993413738569L;
    private int id;
    private int skillid;
    private int tiredness;
    private int exp;
    private int level;
    private transient ScheduledFuture<?> tirednessSchedule;
    private transient WeakReference<MapleCharacter> owner;

    public MapleMount(MapleCharacter owner, int id, int skillid) {
        this.id = id;
        this.skillid = skillid;
        this.tiredness = 0;
        this.level = 1;
        this.exp = 0;
        this.owner = new WeakReference<MapleCharacter>(owner);
    }

    public int getItemId() {
        return id;
    }

    public int getSkillId() {
        return skillid;
    }

    public int getId() {
        if (this.id < 1903000) {
            return id - 1901999;
        }
        return 5;
    }

    public int getTiredness() {
        return tiredness;
    }

    public int getExp() {
        return exp;
    }

    public int getLevel() {
        return level;
    }

    public void setTiredness(int newtiredness) {
        this.tiredness = newtiredness;
        if (tiredness < 0) {
            tiredness = 0;
        }
    }

    public void increaseTiredness() {
        this.tiredness++;
        owner.get().getMap().broadcastMessage(IntraPersonalFactory.updateMount(owner.get().getId(), this, false));
        if (tiredness > 49 + this.level) {
            owner.get().cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            owner.get().cancelBuffStats(MapleBuffStat.MONSTER_RIDING);
            cancelSchedule();
        }
    }

    public void setExp(int newexp) {
        this.exp = newexp;
    }

    public void setLevel(int newlevel) {
        this.level = newlevel;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void startSchedule() {
        int interval = 500 * this.level;
        this.tirednessSchedule = TimerManager.getInstance().register(new Runnable() {
            @Override
            public void run() {
                increaseTiredness();
            }
        }, interval, interval);
    }

    public void cancelSchedule() {
        if (this.tirednessSchedule != null) {
            this.tirednessSchedule.cancel(false);
        }
    }

    public void empty() {
        cancelSchedule();
    }
}

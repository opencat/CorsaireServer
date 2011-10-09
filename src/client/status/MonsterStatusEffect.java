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

package client.status;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import client.ISkill;
import tools.ArrayMap;

/**
 * @name        MonsterStatusEffect
 * @author      Matze
 *              Modified by x711Li
 */
public class MonsterStatusEffect {
    private final Map<MonsterStatus, Integer> stati;
    private final ISkill skill;
    private final boolean monsterSkill;
    private ScheduledFuture<?> cancelTask;
    private ScheduledFuture<?> damageSchedule;

    public MonsterStatusEffect(final Map<MonsterStatus, Integer> stati, final ISkill skillId, final boolean monsterSkill) {
        this.stati = new ArrayMap<MonsterStatus, Integer>(stati);
        this.skill = skillId;
        this.monsterSkill = monsterSkill;
    }

    public final Map<MonsterStatus, Integer> getStati() {
        return stati;
    }

    public final Integer setValue(final MonsterStatus status, final Integer newVal) {
        return stati.put(status, newVal);
    }

    public final ISkill getSkill() {
        return skill;
    }

    public final boolean isMonsterSkill() {
        return monsterSkill;
    }

    public final ScheduledFuture<?> getCancelTask() {
        return cancelTask;
    }

    public final void setCancelTask(final ScheduledFuture<?> cancelTask) {
        this.cancelTask = cancelTask;
    }

    public final void removeActiveStatus(final MonsterStatus stat) {
        stati.remove(stat);
    }

    public final void setDamageSchedule(final ScheduledFuture<?> damageSchedule) {
        this.damageSchedule = damageSchedule;
    }

    public final void cancelDamageSchedule() {
        if (damageSchedule != null) {
            damageSchedule.cancel(false);
        }
    }
}

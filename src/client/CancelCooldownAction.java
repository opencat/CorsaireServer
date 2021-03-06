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

import java.lang.ref.WeakReference;
import tools.factory.BuffFactory;

/**
 * @name        CancelCooldownAction
 * @author      x711Li
 */
public class CancelCooldownAction implements Runnable {
    private int skillId;
    private WeakReference<MapleCharacter> target;

    public CancelCooldownAction(MapleCharacter target, int skillId) {
        this.target = new WeakReference<MapleCharacter>(target);
        this.skillId = skillId;
    }

    @Override
    public void run() {
        final MapleCharacter realTarget = target.get();
        if (realTarget != null) {
            realTarget.removeCooldown(skillId);
            realTarget.getClient().announce(BuffFactory.skillCooldown(skillId, 0));
        }
    }
}

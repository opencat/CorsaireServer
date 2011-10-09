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

package server.movement;

import java.awt.Point;
import tools.data.output.LittleEndianWriter;

/**
 * @name        ChangeEquip
 * @author      Matze
 */
public class ChangeEquip implements LifeMovementFragment {
    private int wui;

    public ChangeEquip(int wui) {
        this.wui = wui;
    }

    @Override
    public void serialize(LittleEndianWriter lew) {
        lew.write(10);
        lew.write(wui);
    }

    @Override
    public Point getPosition() {
        return new Point(0, 0);
    }
}

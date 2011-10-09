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

package constants;

/**
 * @name        InventoryConstants
 * @author      HalfDemi
 *              Modified by x711Li
 */
public final class InventoryConstants {
    public static final int LOCK = 0x01;
    public static final int SPIKES = 0x02;
    public static final int COLD = 0x04;
    public static final int UNTRADEABLE = 0x08;
    public static final int KARMA = 0x10;
    public static final int PET_COME = 0x80;
    public static final int UNKNOWN_SKILL = 0x100;
    public static final float ITEM_ARMOR_EXP = 1 / 350000;
    public static final float ITEM_WEAPON_EXP = 1 / 700000;

    public static final boolean isRechargable(final int itemId) {
        return itemId / 10000 == 233 || itemId / 10000 == 207;
    }
}

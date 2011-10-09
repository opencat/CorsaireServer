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

/**
 * @name        MapleLegion
 * @author      x711Li
 */
public enum MapleLegion {
    LIGHT(1),
    FIRE(2),
    WATER(3),
    THUNDER(4),
    DARKNESS(5);
    final int legionid;

    private MapleLegion(int id) {
        legionid = id;
    }

    public int getId() {
        return legionid;
    }

    public static String legionAlert(int level, int world) {
        switch(level) {
        case 15:
            return "You are now eligible to begin the Test of Nobility <Easy Mode>. Speak with Agent Kitty in Nautilus Port for more details.";
        case 20:
            return "You are now eligible to begin the Kerning PQ. Speak with Lakelis in Kerning City for more details.";
        case 25:
            return "You are now eligible to begin the Allegiance PQ. Speak with Mia in Ellinia for more details.";
        case 45:
            return "You can begin Pirate PQ. Speak with Guon at Over the Pirate Ship.";
        case 50:
        case 100:
        case 150:
            return "The Wolf Spirit, Ryko, howls your name. Meet him in Henesys to " + (level > 50 ? "upgrade your mount." : "get your Level 50 mount.");
        case 51:
            return "You are now eligible to begin the Ludibrium Maze PQ. Speak with Rolly in Ludibrium for more details.";
        case 52:
            return "You are now eligible to fish. Speak with Puro in the Free Market for more details.";
        case 71:
            return "You are now eligible to begin the Test of Nobility <Normal Mode>. Speak with Agent Kitty in Nautilus Port for more details.";
        case 90:
            return "You are now eligible to begin the Crimsonwood Keep PQ. Speak with Jack in Crimsonwood Keep for more details.";
        case 101:
            return "You are now eligible to begin the Test of Nobility <Hard Mode>. Speak with Agent Kitty in Nautilus Port for more details.";
        case 125:
            return "You are now eligible to begin the Horntail PQ.";
        case 130:
            return "You are now eligible to farm. Speak with Ms. Jo in the Free Market for more details.";
        case 131:
            return "You are now eligible to begin the Test of Nobility <Hell Mode>. Speak with Agent Kitty in Nautilus Port for more details.";
        case 140:
            if (world == 0) {
                return "You are now eligible to hire merchants.";
            } else {
                return "";
            }
        }
        return "";
    }
}

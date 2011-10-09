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
 * @name        ServerConstants
 * @author      x711Li
 */
public class ServerConstants {
    public static final int EXP_RATE = 300;
    public static final int MESO_RATE = 10;
    public static final int DROP_RATE = 2;
    public static final int NUM_WORLDS = 1;
    public static final int NUM_CHANNELS = 2;
    public static final int CHANNEL_LOAD = 150;
    public static final String SERVER_NAME = "CorsaireMS";
    public static final String EVENT_MESSAGE = "[#bWelcome to CorsaireMS!#k]\r\n#bScania#k : 300x EXP\r\n#bBera#k : 18x EXP\r\n[#bhttp://www.corsairems.com/#k]";
    public static final String RECOMMENDED_MESSAGE = "Play now!";
    public static String SERVER_MESSAGE = "";
    public static final String EVENTS = "";
    public static final String LIMITED_NAMES = "maple7market q225290 q225291 q225292 lie_3 -1 stalker00 stalker01 royalFace";
    public static final String HOST = "127.0.0.1";
    public static final boolean DEBUG = false;
    public static final String DATABASE_URL = "jdbc:mysql://localhost:3306/corsairems";
    public static final String DATABASE_USER = "root";
    public static final String DATABASE_PASS = "";
    public static final boolean ALL_IN_ONE = true;
    public static final boolean WARPER = true;
    public static final boolean REBIRTH = true;
    public static final boolean AUTO_JOB = true;
    public static final boolean AUTO_SKILL = true;
    public static final String MAPLE_TIPS[] = {
        "Please refrain from using foul language in this game.",
        "Need a list of commands? Type @commands for a list.",
        "Verbal and other forms of abuse will NOT be tolerated. Abusers will be blocked from the game.",
        "If you have any questions, feel free to check our forums for support.",
        "Gamemasters are here to help you! Type @ask <question> to get your questions answered by an online GM.",
        "Don't forget to check out our forum for on-going news and update information!",
        "Be sure to type @faq for help and tips!",
        "Type @notify to ask for help from GMs." };
}

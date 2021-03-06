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

import java.util.Map;
import java.util.HashMap;

/**
 * @name        MapleNPCStats
 * @author      Matze
 */
public class MapleNPCStats {
    private String name;
    private Map<Byte, Integer> equips;
    private int face, hair;
    private byte skin;
    private int FH, RX0, RX1, CY;
    private boolean custom;

    public MapleNPCStats(String name, boolean playerNpc) {
        this.name = name;

        if (playerNpc) {
            equips = new HashMap<Byte, Integer>();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Byte, Integer> getEquips() {
        return equips;
    }

    public int getFH() {
        return FH;
    }

    public int getRX0() {
        return RX0;
    }

    public int getRX1() {
        return RX1;
    }

    public int getCY() {
        return CY;
    }

    public byte getSkin() {
        return skin;
    }

    public int getFace() {
        return face;
    }

    public int getHair() {
        return hair;
    }

    public void setEquips(Map<Byte, Integer> equips) {
        this.equips = equips;
    }

    public void setFH(int FH) {
        this.FH = FH;
    }

    public void setRX0(int RX0) {
        this.RX0 = RX0;
    }

    public void setRX1(int RX1) {
        this.RX1 = RX1;
    }

    public void setCY(int CY) {
        this.CY = CY;
    }

    public void setSkin(byte skin) {
        this.skin = skin;
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }
}

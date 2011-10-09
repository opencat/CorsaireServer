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

package server;

/**
 * @name        DropEntry
 * @author      x711Li
 */
public class DropEntry {
    public DropEntry(int id, int chance, int quest) {
        this.id = id;
        this.chance = chance;
        this.quest = quest;
    }
    private int id;
    private int chance;
    private int quest;
    private int assignedRangeStart;
    private int assignedRangeLength;

    public int getId() {
        return id;
    }

    public int getChance() {
        return chance;
    }

    public int getQuest() {
        return quest;
    }

    public int getAssignedRangeStart() {
        return assignedRangeStart;
    }

    public void setAssignedRangeStart(int assignedRangeStart) {
        this.assignedRangeStart = assignedRangeStart;
    }

    public int getAssignedRangeLength() {
        return assignedRangeLength;
    }

    public void setAssignedRangeLength(int assignedRangeLength) {
        this.assignedRangeLength = assignedRangeLength;
    }
}

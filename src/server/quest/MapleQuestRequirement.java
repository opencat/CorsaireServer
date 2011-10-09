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

package server.quest;

import java.util.Calendar;

import client.IItem;
import client.MapleCharacter;
import client.MapleInventoryType;
import client.MapleQuestStatus;
import client.SkillFactory;
import provider.MapleData;
import provider.MapleDataTool;
import server.MapleItemInformationProvider;

/**
 * @name        MapleQuestRequirement
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleQuestRequirement {
    private MapleQuestRequirementType type;
    private MapleData data;
    private MapleQuest quest;
    
    // Creates a new instance of MapleQuestRequirement
    public MapleQuestRequirement(MapleQuest quest, MapleQuestRequirementType type, MapleData data) {
        this.type = type;
        this.data = data;
        this.quest = quest;
    }
    
    boolean check(MapleCharacter c, Integer npcid) {
        switch (getType()) {
        case job:
            for (MapleData jobEntry : getData().getChildren())
            if (c.getJob() == MapleDataTool.getInt(jobEntry)) return true;
            return false;
        case skill:
            for (final MapleData questEntry : getData().getChildren()) {
                final boolean acquire = MapleDataTool.getInt(questEntry.getChildByPath("acquire"), 0) > 0;
                final int skill = MapleDataTool.getInt(questEntry.getChildByPath("id"));
                if (acquire) {
                    if (c.getMasterLevel(SkillFactory.getSkill(skill)) == 0) {
                        return false;
                    }
                } else {
                    if (c.getMasterLevel(SkillFactory.getSkill(skill)) > 0) {
                        return false;
                    }
                }
            }
            break;
        case quest:
            for (final MapleData questEntry : getData().getChildren()) {
                final MapleQuestStatus q = c.getQuest(MapleQuest.getInstance(
                MapleDataTool.getInt(questEntry.getChildByPath("id"))));
                if (q == null && MapleDataTool.getInt(questEntry.getChildByPath("state")) == 0) {
                    continue;
                } else if (q == null || q.getStatus() != MapleDataTool.getInt(questEntry.getChildByPath("state"), 0)) {
                    return false;
                }
            }
            return true;
        case item:
            for (final MapleData itemEntry : getData().getChildren()) {
                int itemId = MapleDataTool.getInt(itemEntry.getChildByPath("id"));
                int stored = MapleDataTool.getInt(itemEntry.getChildByPath("count"), 0);
                int quantity = c.getItemQuantity(itemId, true);
                if (quantity < stored || stored <= 0 && quantity > 0) {
                    return false;
                }
            }
            return true;
        case lvmin:
            return c.getLevel() >= MapleDataTool.getInt(getData());
        case lvmax:
            return c.getLevel() <= MapleDataTool.getInt(getData());
        case end:
            String timeStr = MapleDataTool.getString(getData());
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(timeStr.substring(0, 4)), Integer.parseInt(timeStr.substring(4, 6)), 
            Integer.parseInt(timeStr.substring(6, 8)), Integer.parseInt(timeStr.substring(8, 10)),
            0);
            return cal.getTimeInMillis() >= System.currentTimeMillis();
        case mob:
            for (final MapleData mobEntry : getData().getChildren()) {
                if (c.getQuest(quest).getMobKills(MapleDataTool.getInt(mobEntry.getChildByPath("id"))) < MapleDataTool.getInt(mobEntry.getChildByPath("count")))
                return false;
            }
            return true;
        case npc:
            return npcid == null || npcid == MapleDataTool.getInt(getData());
        case fieldEnter:
            MapleData zeroField = getData().getChildByPath("0");
            if (zeroField != null) {
                return MapleDataTool.getInt(zeroField) == c.getMapId();
            }
            return false;
        case interval:
            return c.getQuest(quest).getStatus() != 2 ||
            c.getQuest(quest).getCompletionTime() <= System.currentTimeMillis() - MapleDataTool.getInt(getData()) * 60 * 1000;
        case pettamenessmin:
            return c.getPet(0).getCloseness() >= MapleDataTool.getInt(getData());
        case tamingmoblevelmin:
            return c.getMount().getLevel() >= MapleDataTool.getInt(getData());
        case mbmin:
            return c.getMonsterBook().getTotalCards() >= MapleDataTool.getInt(getData());
        case pop:
            return c.getFame() <= MapleDataTool.getInt(getData());
        case questComplete:
            int completedQuests = 0;
            for (final MapleQuestStatus q : c.getQuests().values()) {
                if (q.getStatus() == 2) {
                    completedQuests++;
                }
            }
            if (completedQuests >= MapleDataTool.getInt(getData())) {
                return true;
            }
        default:
            return true;
        }
        return true;
    }

    public MapleQuestRequirementType getType() {
        return type;
    }

    public MapleData getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return type.toString() + " " + data.toString() + " " + quest.toString();
    }
}

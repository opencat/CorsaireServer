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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import client.MapleCharacter;
import client.MapleQuestStatus;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;

/**
 * @name        MapleQuest
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleQuest {
    private static Map<Integer, MapleQuest> quests = new LinkedHashMap<Integer, MapleQuest>();
    protected int id;
    protected int npcId;
    protected int startNpcId;
    protected final List<MapleQuestRequirement> startReqs = new LinkedList<MapleQuestRequirement>();
    protected final List<MapleQuestRequirement> completeReqs = new LinkedList<MapleQuestRequirement>();
    protected final List<MapleQuestAction> startActs = new LinkedList<MapleQuestAction>();
    protected final List<MapleQuestAction> completeActs = new LinkedList<MapleQuestAction>();
    protected final Map<Integer, Integer> relevantMobs = new LinkedHashMap<Integer, Integer>();
    private boolean autoStart;
    private boolean autoComplete;
    private boolean autoPreComplete;
    private boolean repeatable = false, customend = false;
    private String record = null;
    private static final MapleDataProvider questData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Quest.wz"));
    private static final MapleData actions = questData.getData("Act.img");
    private static final MapleData requirements = questData.getData("Check.img");
    private static final MapleData info = questData.getData("QuestInfo.img");
    public static final int MISMATCHED_MOBS[][] = { {9101000, 1110100}, {9101001, 2230101}, {9101002, 1140100}, {9101003, 8130100}, {9101004, 9409018} };

    private static boolean loadQuest(MapleQuest ret, int id) {
        ret.id = id;
        final MapleData basedata1 = requirements.getChildByPath(String.valueOf(id));
        final MapleData basedata2 = actions.getChildByPath(String.valueOf(id));
        if (basedata1 == null || basedata2 == null) {
            return false;
        }
        final MapleData startReqData = basedata1.getChildByPath("0");
        if (startReqData != null) {
            for (MapleData startReq : startReqData.getChildren()) {
                final MapleQuestRequirementType type = MapleQuestRequirementType.getByWZName(startReq.getName());
                if (type.equals(MapleQuestRequirementType.interval)) {
                    ret.repeatable = true;
                }
                final MapleQuestRequirement req = new MapleQuestRequirement(ret, type, startReq);
                if (req.getType().equals(MapleQuestRequirementType.mob)) {
                    for (MapleData mob : startReq.getChildren()) {
                        ret.relevantMobs.put(
                        MapleDataTool.getInt(mob.getChildByPath("id")),
                        MapleDataTool.getInt(mob.getChildByPath("count"), 0));
                    }
                } else if (req.getType().equals(MapleQuestRequirementType.npc)) {
                    ret.startNpcId = MapleDataTool.getInt(req.getData());
                }
                ret.startReqs.add(req);
            }
        }
        final MapleData completeReqData = basedata1.getChildByPath("1");
        if (completeReqData.getChildByPath("endscript") != null) {
            ret.customend = true;
        }
        if (completeReqData != null) {
            for (MapleData completeReq : completeReqData.getChildren()) {
                MapleQuestRequirement req = new MapleQuestRequirement(ret, MapleQuestRequirementType.getByWZName(completeReq.getName()), completeReq);
                if (req.getType().equals(MapleQuestRequirementType.mob)) {
                    for (MapleData mob : completeReq.getChildren()) {
                        int mobid = MapleDataTool.getInt(mob.getChildByPath("id"));
                        ret.relevantMobs.put(mobid, MapleDataTool.getInt(mob.getChildByPath("count"), 0));
                    }
                }
                ret.completeReqs.add(req);
            }
        }
        final MapleData startActData = basedata2.getChildByPath("0");
        if (startActData != null) {
            for (MapleData startAct : startActData.getChildren()) {
                ret.startActs.add(new MapleQuestAction(MapleQuestActionType.getByWZName(startAct.getName()), startAct, ret));
            }
        }
        final MapleData completeActData = basedata2.getChildByPath("1");
        if (completeActData != null) {
            for (MapleData completeAct : completeActData.getChildren()) {
                ret.completeActs.add(new MapleQuestAction(MapleQuestActionType.getByWZName(completeAct.getName()), completeAct, ret));
            }
        }
        final MapleData questInfo = info.getChildByPath(String.valueOf(id));
        ret.autoStart = MapleDataTool.getInt("autoStart", questInfo, 0) == 1;
        ret.autoComplete = MapleDataTool.getInt("autoComplete", questInfo, 0) == 1;
        ret.autoPreComplete = MapleDataTool.getInt("autoPreComplete", questInfo, 0) == 1;
        return true;
    }

    public int getStartNpcId() {
        return startNpcId;
    }

    public static MapleQuest getInstance(int id) {
        MapleQuest ret = quests.get(id);
        if (ret == null) {
            ret = new MapleQuest();
            if (!loadQuest(ret, id)) {
                ret = null;
            }
            quests.put(id, ret);
        }
        return ret;
    }

    private boolean canStart(MapleCharacter c, Integer npcid) {
        this.npcId = npcid;
        if (c.getQuest(this).getStatus() != 0 && !(c.getQuest(this).getStatus() == 2 && repeatable)) {
            return false;
        }
        for (MapleQuestRequirement r : startReqs) {
            if (!r.check(c, npcid)) {
                return false;
            }
        }
        return true;
    }

    public boolean canComplete(MapleCharacter c, Integer npcid) {
        if (c.getQuest(this).getStatus() == 0 && c.getQuest(this).getForfeited() > 0) {
            return false;
        }
        for (MapleQuestRequirement r : completeReqs) {
            if (!r.check(c, npcid)) {
                return false;
            }
        }
        return true;
    }

    public void start(MapleCharacter c, int npc) {
        if ((autoStart || checkNPCOnMap(c, npc)) && canStart(c, npc)) {
            for (MapleQuestAction a : startActs) {
                a.run(c, null);
            }
            MapleQuestStatus newStatus = new MapleQuestStatus(this, 1, npc);
            newStatus.setCompletionTime(c.getQuest(this).getCompletionTime());
            newStatus.setForfeited(c.getQuest(this).getForfeited());
            c.updateQuest(newStatus, false);
        }
    }

    public void complete(MapleCharacter c, int npc) {
        complete(c, npc, null);
    }

    public void complete(MapleCharacter c, int npc, Integer selection) {
        if ((autoComplete || autoPreComplete || checkNPCOnMap(c, npc)) && canComplete(c, npc)) {
            for (MapleQuestAction a : completeActs) {
            if (!a.check(c, selection)) {
                    return;
                }
            }
            for (MapleQuestAction a : completeActs) {
                a.run(c, selection);
            }
            forceComplete(c, npc);
        }
    }

    public void reset(MapleCharacter c) {
        c.updateQuest(new MapleQuestStatus(this, 0), false);
    }

    public void forfeit(MapleCharacter c) {
        if (c.getQuest(this).getStatus() != 1) {
            return;
        }
        MapleQuestStatus newStatus = new MapleQuestStatus(this, 0);
        newStatus.setForfeited(c.getQuest(this).getForfeited() + 1);
        newStatus.setCompletionTime(c.getQuest(this).getCompletionTime());
        c.updateQuest(newStatus, false);
    }

    public void forceStart(MapleCharacter c, int npc) {
        forceStart(c, npc, null);
    }

    public void forceStart(MapleCharacter c, int npc, String customData) {
        final MapleQuestStatus newStatus = new MapleQuestStatus(this, 1, npc);
        newStatus.setForfeited(c.getQuest(this).getForfeited());
        newStatus.setQuestRecord(customData);
        c.updateQuest(newStatus, customData != null);
    }

    public void forceComplete(MapleCharacter c, int npc) {
        MapleQuestStatus newStatus = new MapleQuestStatus(this, 2, npc);
        newStatus.setForfeited(c.getQuest(this).getForfeited());
        c.updateQuest(newStatus, false);
    }

    public int getId() {
        return id;
    }

    public int getNpcId() {
        return npcId;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }

    public Map<Integer, Integer> getRelevantMobs() {
        return relevantMobs;
    }

    private boolean checkNPCOnMap(MapleCharacter player, int npcid) {
        return player.getMap().containsNPC(npcid) || npcid == 1013000;
    }

    public static void clear() {
        quests.clear();
    }
}

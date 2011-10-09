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

import java.util.Map;
import client.ISkill;
import client.MapleCharacter;
import client.MapleInventoryType;
import client.MapleQuestStatus;
import client.MapleStat;
import client.SkillFactory;
import constants.InventoryConstants;
import provider.MapleData;
import provider.MapleDataTool;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.Randomizer;
import java.util.HashMap;
import tools.factory.EffectFactory;

/**
 * @name        MapleQuestAction
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleQuestAction {
    private MapleQuestActionType type;
    private MapleData data;
    private MapleQuest quest;

    public MapleQuestAction(MapleQuestActionType type, MapleData data, MapleQuest quest) {
        this.type = type;
        this.data = data;
        this.quest = quest;
    }

    public boolean check(MapleCharacter c, Integer extSelection) {
        switch (type) {
        case item: {
                final Map<Integer, Integer> props = new HashMap<Integer, Integer>();
                for (MapleData iEntry : data.getChildren()) {
                    final MapleData prop = iEntry.getChildByPath("prop");
                    if (prop != null && MapleDataTool.getInt(prop) != -1 && canGetItem(iEntry, c)) {
                        for (int i = 0; i < MapleDataTool.getInt(iEntry.getChildByPath("prop")); i++) {
                            props.put(props.size(), MapleDataTool.getInt(iEntry.getChildByPath("id")));
                        }
                    }
                }
                int selection = 0;
                int extNum = 0;
                if (props.size() > 0) {
                    selection = props.get(Randomizer.nextInt(props.size()));
                }
                byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
                for (MapleData iEntry : data.getChildren()) {
                    if (!canGetItem(iEntry, c)) {
                        continue;
                    }
                    final int id = MapleDataTool.getInt(iEntry.getChildByPath("id"), -1);
                    if (iEntry.getChildByPath("prop") != null) {
                        if (MapleDataTool.getInt(iEntry.getChildByPath("prop")) == -1) {
                            if (extSelection != extNum++) {
                                continue;
                            }
                        } else if (id != selection) {
                            continue;
                        }
                    }
                    final short count = (short) MapleDataTool.getInt(iEntry.getChildByPath("count"), 1);
                    if (count < 0) {
                        if (!c.haveItem(id, -count)) {
                            c.dropMessage(1, "You do not have the item required for the quest!");
                            return false;
                        }
                    } else {
                        if (id >= 5000000) {
                            cash++;
                        } else if (id >= 4000000) {
                            etc++;
                        } else if (id >= 3000000) {
                            setup++;
                        } else if (id >= 2000000) {
                            use++;
                        } else {
                            eq++;
                        }
                    }
                }
                if (c.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() <= eq) {
                    c.dropMessage(1, "Please make space for your EQUIP inventory.");
                    return false;
                } else if (c.getInventory(MapleInventoryType.USE).getNumFreeSlot() <= use) {
                    c.dropMessage(1, "Please make space for your USE inventory.");
                    return false;
                } else if (c.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() <= setup) {
                    c.dropMessage(1, "Please make space for your SETUP inventory.");
                    return false;
                } else if (c.getInventory(MapleInventoryType.ETC).getNumFreeSlot() <= etc) {
                    c.dropMessage(1, "Please make space for your ETC inventory.");
                    return false;
                } else if (c.getInventory(MapleInventoryType.CASH).getNumFreeSlot() <= cash) {
                    c.dropMessage(1, "Please make space for your CASH inventory.");
                    return false;
                }
                return true;
            }
            case money:
                final int mesos = MapleDataTool.getInt(data, 0);
                if (c.getMeso() < mesos) {
                    c.dropMessage(1, "You don't have enough mesos to complete this quest.");
                    return false;
                }
                return true;
            default:
                break;
        }
        return true;
    }

    private boolean canGetItem(MapleData item, MapleCharacter c) {
        if (item.getChildByPath("gender") != null) {
            int gender = MapleDataTool.getInt(item.getChildByPath("gender"));
            if (gender != 2 && gender != c.getGender())
            return false;
        }
        if (item.getChildByPath("job") != null) {
            final int job = MapleDataTool.getInt(item.getChildByPath("job"));
            if (job < 100) {
                final int codec = getJobBy5ByteEncoding(job);
                if (codec != -1 && c.getJob() % 1000 / 100 != codec) {
                    return false;
                }
            } else if (job > 3000) {
                final int playerjob = c.getJob();
                final int codec = getJobByEncoding(job);
                if (codec != -1 && playerjob % 1000 / 100 != codec) {
                    return false;
                }
            } else {
                if (job != c.getJob()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void run(MapleCharacter c, Integer extSelection) {
        switch (type) {
            case exp: {
                c.gainExp(MapleDataTool.getInt(data, 0) * c.getExpRate(), true, true);
                break;
            }
            case item: {
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                Map<Integer, Integer> props = new HashMap<Integer, Integer>();
                for (MapleData iEntry : data.getChildren()) {
                    if (iEntry.getChildByPath("count") != null && MapleDataTool.getInt(iEntry.getChildByPath("count"), 0) > 0 && iEntry.getChildByPath("prop") != null && MapleDataTool.getInt(iEntry.getChildByPath("prop"), -1) != -1 && canGetItem(iEntry, c)) {
                        for (int i = 0; i < MapleDataTool.getInt(iEntry.getChildByPath("prop")); i++) {
                            props.put(props.size(), MapleDataTool.getInt(iEntry.getChildByPath("id")));
                        }
                    }
                }
                int selection = 0;
                int extNum = 0;
                if (props.size() > 0) {
                    selection = props.get(Randomizer.getInstance().nextInt(props.size()));
                }
                for (MapleData iEntry : data.getChildren()) {
                    if (!canGetItem(iEntry, c)) {
                        continue;
                    }
                    if (iEntry.getChildByPath("count") != null && MapleDataTool.getInt(iEntry.getChildByPath("count"), 0) > 0 && iEntry.getChildByPath("prop") != null) {
                        if (MapleDataTool.getInt(iEntry.getChildByPath("prop")) == -1) {
                            if (extSelection != null && extSelection != extNum++) {
                                continue;
                            }
                        } else if (iEntry.getChildByPath("id") != null && MapleDataTool.getInt(iEntry.getChildByPath("id")) != selection) {
                            continue;
                        }
                    }
                    if (MapleDataTool.getInt(iEntry.getChildByPath("count"), 0) < 0) {
                        int itemId = MapleDataTool.getInt(iEntry.getChildByPath("id"));
                        MapleInventoryType iType = ii.getInventoryType(itemId);
                        short quantity = (short) (-MapleDataTool.getInt(iEntry.getChildByPath("count"), 0));
                        if(c.haveItem(itemId, quantity)) {
                            MapleInventoryManipulator.removeById(c.getClient(), iType, itemId, quantity, true, false);
                            c.getClient().announce(EffectFactory.getShowItemGain(itemId, (short) MapleDataTool.getInt(iEntry.getChildByPath("count"), 0), true));
                        } else {
                            c.dropMessage(1, "You do not have the item required for the quest!");
                            break;
                        }
                    } else {
                        int itemId = MapleDataTool.getInt(iEntry.getChildByPath("id"));
                        short quantity = (short) MapleDataTool.getInt(iEntry.getChildByPath("count"), 0);
                        if (c.getInventory(MapleItemInformationProvider.getInstance().getInventoryType(itemId)).getNextFreeSlot() > -1) {
                            MapleInventoryManipulator.addById(c.getClient(), itemId, quantity);
                            c.getClient().announce(EffectFactory.getShowItemGain(itemId, quantity, true));
                        } else {
                            c.dropMessage(1, "Your Inventory was too full!");
                        }
                    }
                }
                break;
            }
            case nextQuest: {
                quest.forceComplete(c, c.getQuest(quest).getNpc());
                System.out.println(c.getQuest(quest).getNpc());
                System.out.println(MapleDataTool.getInt(data));
                MapleQuest nextQuest = MapleQuest.getInstance(MapleDataTool.getInt(data));
                System.out.println(nextQuest.getStartNpcId());
                nextQuest.start(c, nextQuest.getStartNpcId());
                //nextQuest.forceStart(c, nextQuest.getStartNpcId());
                break;
            }
            case money: {
                c.gainMeso(MapleDataTool.getInt(data, 0), true, false, true);
                break;
            }
            case quest: {
                for (MapleData qEntry : data) {
                    int quest = MapleDataTool.getInt(qEntry.getChildByPath("id"));
                    int state = MapleDataTool.getInt(qEntry.getChildByPath("state"), 0);
                    c.updateQuest(new MapleQuestStatus(MapleQuest.getInstance(quest), state), false);
                }
                break;
            }
            case skill: {
                for (MapleData sEntry : data) {
                    final int skillid = MapleDataTool.getInt(sEntry.getChildByPath("id"));
                    int skillLevel = MapleDataTool.getInt(sEntry.getChildByPath("skillLevel"), 0);
                    int masterLevel = MapleDataTool.getInt(sEntry.getChildByPath("masterLevel"), 0);
                    final ISkill skillObject = SkillFactory.getSkill(skillid);
                    for (MapleData applicableJob : sEntry.getChildByPath("job")) {
                        if (skillObject.isBeginnerSkill() || c.getJob() == MapleDataTool.getInt(applicableJob)) {
                            c.changeSkillLevel(skillObject,
                            (byte) Math.max(skillLevel, c.getSkillLevel(skillObject)),
                            (byte) Math.max(masterLevel, c.getMasterLevel(skillObject)));
                            break;
                        }
                    }
                }
                break;
            }
            case pop: {
                int fameGain = MapleDataTool.getInt(data, 0);
                c.addFame(fameGain);
                c.updateSingleStat(MapleStat.FAME, c.getFame());
                c.getClient().announce(EffectFactory.getShowFameGain(fameGain));
                break;
            }
            case buffItemID: {
                MapleItemInformationProvider.getInstance().getItemEffect(MapleDataTool.getInt(data, -1)).applyTo(c);
                break;
            }
            case petskill: {
                int flag = MapleDataTool.getInt("petskill", data);
                c.getPet(0).setFlag((byte) (c.getPet(0).getFlag() | getFlagByInt(flag)));
                break;
            }
            case info: {
                quest.setRecord(MapleDataTool.getString(data, "0"));
                break;
            }
            case UNDEFINED: {
                break;
            }
            default:
                break;
        }
    }

    private static int getJobBy5ByteEncoding(int encoded) {
        switch (encoded) {
            case 2:
            case 3:
                return 1;
            case 4:
            case 5:
                return 2;
            case 8:
            case 9:
                return 3;
            case 16:
            case 17:
                return 4;
            case 32:
            case 33:
                return 5;
            case 63:
                return -1;
        }
        return 0;
    }

    private static int getJobByEncoding(int encoded) {
        switch (encoded) {
            case 64512: //NOBLESSE
            case 1049601:
            case 1180673: //WORK GLOVES? BEGINNER CLASSES
                return -1;
            case 2050: //? 2H Axe
            case 3075: //? 1H Sword
            case 2044802: //1H MELEE
            case 2099202: //FORGING MANUALS
            case 3279875: //WARRIORS
                return 1;
            case 4100:
            case 4198404: //? Mage Boots
            case 5379077: //WAND/STAFF SCROLLS
                return 2;
            case 8200:
            case 1188873: //BOW/XBOW SCROLLS
            case 49200: //DEXTERITY POTION
                return 3;
            case 16400:
            case 18450: //DAGGER SCROLL?
            case 1197073: //CLAW/DAGGER SCROLLS?
                return 4;
            case 32800:
            case 1213473: //KNUCKLE/GUN
                return 5;
        }
        return 0;
    }
    
    private static final int getFlagByInt(final int type) {
        if (type == 128) {
            return InventoryConstants.PET_COME;
        } else if (type == 256) {
            return InventoryConstants.UNKNOWN_SKILL;
        }
        return 0;
    }

    public MapleQuestActionType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + ": " + data;
    }
}

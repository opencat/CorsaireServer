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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import client.Equip;
import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import client.MapleWeaponType;
import client.SkillFactory;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import constants.InventoryConstants;
import server.MapleTreasure;
import tools.Randomizer;
import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.DatabaseConnection;
import tools.Pair;

/**
 * @name        MapleItemInformationProvider
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleItemInformationProvider {
    private final static MapleItemInformationProvider instance = new MapleItemInformationProvider();
    protected final MapleDataProvider itemData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Item.wz"));
    protected final MapleDataProvider equipData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Character.wz"));
    protected final MapleDataProvider stringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz"));
    protected final MapleData cashStringData = stringData.getData("Cash.img");
    protected final MapleData consumeStringData = stringData.getData("Consume.img");
    protected final MapleData eqpStringData = stringData.getData("Eqp.img");
    protected final MapleData etcStringData = stringData.getData("Etc.img");
    protected final MapleData insStringData = stringData.getData("Ins.img");
    protected final MapleData petStringData = stringData.getData("Pet.img");
    protected final Map<Integer, Short> slotMaxCache = new HashMap<Integer, Short>();
    protected final Map<Integer, MapleStatEffect> itemEffects = new HashMap<Integer, MapleStatEffect>();
    protected final Map<Integer, Map<String, Integer>> equipStatsCache = new HashMap<Integer, Map<String, Integer>>();
    protected final Map<Integer, Map<String, Byte>> itemMakeStatsCache = new HashMap<Integer, Map<String, Byte>>();
    protected final Map<Integer, Short> itemMakeLevel = new HashMap<Integer, Short>();
    protected final Map<Integer, Equip> equipCache = new HashMap<Integer, Equip>();
    protected final Map<Integer, Double> priceCache = new HashMap<Integer, Double>();
    protected final Map<Integer, Integer> wholePriceCache = new HashMap<Integer, Integer>();
    protected final Map<Integer, Integer> projectileWatkCache = new HashMap<Integer, Integer>();
    protected final Map<Integer, Integer> monsterBookID = new HashMap<Integer, Integer>();
    protected final Map<Integer, String> nameCache = new HashMap<Integer, String>();
    protected final Map<Integer, String> msgCache = new HashMap<Integer, String>();
    protected final Map<Integer, Map<String, Integer>> skillStatsCache = new HashMap<Integer, Map<String, Integer>>();
    protected final Map<Integer, Byte> consumeOnPickupCache = new HashMap<Integer, Byte>();
    protected final Map<Integer, Boolean> dropRestrictionCache = new HashMap<Integer, Boolean>();
    protected final Map<Integer, Boolean> pickupRestrictionCache = new HashMap<Integer, Boolean>();
    protected final Map<Integer, Integer> stateChangeCache = new HashMap<Integer, Integer>(40);
    protected final Map<Integer, Boolean> karmaCache = new HashMap<Integer, Boolean>();
    protected final Map<Integer, Boolean> isQuestItemCache = new HashMap<Integer, Boolean>();
    protected final Map<Integer, List<Pair<Integer, Integer>>> summonMobCache = new HashMap<Integer, List<Pair<Integer, Integer>>>();
    protected final Map<Integer, Pair<Integer, Integer>> catchMobCache = new HashMap<Integer, Pair<Integer, Integer>>();
    protected final Map<Integer, Integer> getMesoCache = new HashMap<Integer, Integer>();
    protected final Map<Integer, Boolean> onEquipUntradableCache = new HashMap<Integer, Boolean>();
    protected final Map<Integer, Integer> scriptedItemNPCCache = new HashMap<Integer, Integer>();
    protected final Map<Integer, Integer> expCache = new HashMap<Integer, Integer>();
    protected final Map<Integer, List<MapleTreasure>> treasureCache = new HashMap<Integer, List<MapleTreasure>>();

    private MapleItemInformationProvider() {
        loadCardIdData();
    }

    public final static MapleItemInformationProvider getInstance() {
        return instance;
    }

    public final MapleInventoryType getInventoryType(final int id) {
        return MapleInventoryType.getByType((byte) (id / 1000000));
    }

    private final MapleData getStringData(final int Id) {
        String cat = "null";
        MapleData theData;
        if (Id >= 5010000) {
            theData = cashStringData;
        } else if (Id >= 2000000 && Id < 3000000) {
            theData = consumeStringData;
        } else if ((Id >= 1010000 && Id < 1040000) || (Id >= 1122000 && Id < 1123000) || (Id >= 1142000 && Id < 1143000)) {
            theData = eqpStringData;
            cat = "Eqp/Accessory";
        } else if (Id >= 1000000 && Id < 1010000) {
            theData = eqpStringData;
            cat = "Eqp/Cap";
        } else if (Id >= 1102000 && Id < 1103000) {
            theData = eqpStringData;
            cat = "Eqp/Cape";
        } else if (Id >= 1040000 && Id < 1050000) {
            theData = eqpStringData;
            cat = "Eqp/Coat";
        } else if (Id >= 20000 && Id < 22000) {
            theData = eqpStringData;
            cat = "Eqp/Face";
        } else if (Id >= 1080000 && Id < 1090000) {
            theData = eqpStringData;
            cat = "Eqp/Glove";
        } else if (Id >= 30000 && Id < 32000) {
            theData = eqpStringData;
            cat = "Eqp/Hair";
        } else if (Id >= 1050000 && Id < 1060000) {
            theData = eqpStringData;
            cat = "Eqp/Longcoat";
        } else if (Id >= 1060000 && Id < 1070000) {
            theData = eqpStringData;
            cat = "Eqp/Pants";
        } else if (Id >= 1802000 && Id < 1810000) {
            theData = eqpStringData;
            cat = "Eqp/PetEquip";
        } else if (Id >= 1112000 && Id < 1120000) {
            theData = eqpStringData;
            cat = "Eqp/Ring";
        } else if (Id >= 1092000 && Id < 1100000) {
            theData = eqpStringData;
            cat = "Eqp/Shield";
        } else if (Id >= 1070000 && Id < 1080000) {
            theData = eqpStringData;
            cat = "Eqp/Shoes";
        } else if (Id >= 1900000 && Id < 2000000) {
            theData = eqpStringData;
            cat = "Eqp/Taming";
        } else if (Id >= 1300000 && Id < 1800000) {
            theData = eqpStringData;
            cat = "Eqp/Weapon";
        } else if (Id >= 4000000 && Id < 5000000) {
            theData = etcStringData;
        } else if (Id >= 3000000 && Id < 4000000) {
            theData = insStringData;
        } else if (Id >= 5000000 && Id < 5010000) {
            theData = petStringData;
        } else {
            return null;
        }
        if (cat.equalsIgnoreCase("null")) {
            return theData.getChildByPath(String.valueOf(Id));
        } else {
            return theData.getChildByPath(cat + "/" + Id);
        }
    }

    private final MapleData getItemData(final int Id) {
        MapleData ret = null;
        final String idStr = "0" + String.valueOf(Id);
        MapleDataDirectoryEntry root = itemData.getRoot();
        for (final MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            for (final MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
                    ret = itemData.getData(topDir.getName() + "/" + iFile.getName());
                    if (ret == null) {
                        return null;
                    }
                    ret = ret.getChildByPath(idStr);
                    return ret;
                } else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
                    return itemData.getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        root = equipData.getRoot();
        for (final MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            for (final MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr + ".img")) {
                    return equipData.getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        return ret;
    }

    public final short getSlotMax(final MapleClient c, final int Id) {
        if (slotMaxCache.containsKey(Id)) {
            return slotMaxCache.get(Id);
        }
        short ret = 0;
        final MapleData item = getItemData(Id);
        if (item != null) {
            final MapleData smEntry = item.getChildByPath("info/slotMax");
            if (smEntry == null) {
                if (Id / 1000000 == 1) {
                    ret = 1;
                } else {
                    ret = 100;
                }
            } else {
                if (InventoryConstants.isRechargable(Id) || (MapleDataTool.getInt(smEntry) == 0)) {
                    ret = 1;
                }
                ret = (short) MapleDataTool.getInt(smEntry);
                if (Id / 10000 == 207) {
                    ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(4100000)) * 10;
                } else {
                    ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(5200000)) * 10;
                }
            }
        }
        if (!InventoryConstants.isRechargable(Id)) {
            slotMaxCache.put(Id, ret);
        }
        return ret;
    }

    public int getMeso(int Id) {
        if (getMesoCache.containsKey(Id)) {
            return getMesoCache.get(Id);
        }
        MapleData item = getItemData(Id);
        if (item == null) {
            return -1;
        }
        int pEntry = 0;
        MapleData pData = item.getChildByPath("info/meso");
        if (pData == null) {
            return -1;
        }
        pEntry = MapleDataTool.getInt(pData);
        getMesoCache.put(Id, pEntry);
        return pEntry;
    }

    public final int getWholePrice(final int Id) {
        if (wholePriceCache.containsKey(Id)) {
            return wholePriceCache.get(Id);
        }
        final MapleData item = getItemData(Id);
        if (item == null) {
            return -1;
        }
        int pEntry = 0;
        final MapleData pData = item.getChildByPath("info/price");
        if (pData == null) {
            return -1;
        }
        pEntry = MapleDataTool.getInt(pData);
        wholePriceCache.put(Id, pEntry);
        return pEntry;
    }

    public final double getPrice(final int Id) {
        if (priceCache.containsKey(Id)) {
            return priceCache.get(Id);
        }
        final MapleData item = getItemData(Id);
        if (item == null) {
            return -1;
        }
        double pEntry = 0.0;
        MapleData pData = item.getChildByPath("info/unitPrice");
        if (pData != null) {
            try {
                pEntry = MapleDataTool.getDouble(pData);
            } catch (Exception e) {
                pEntry = (double) MapleDataTool.getInt(pData);
            }
        } else {
            pData = item.getChildByPath("info/price");
            if (pData == null) {
                return -1;
            }
            pEntry = (double) MapleDataTool.getInt(pData);
        }
        priceCache.put(Id, pEntry);
        return pEntry;
    }

    public final Map<String, Byte> getItemMakeStats(final int Id) {
        if (itemMakeStatsCache.containsKey(Id)) {
            return itemMakeStatsCache.get(Id);
        }
        if (Id / 10000 != 425) {
            return null;
        }
        final Map<String, Byte> ret = new LinkedHashMap<String, Byte>();
        final MapleData item = getItemData(Id);
        if (item == null) {
            return null;
        }
        final MapleData info = item.getChildByPath("info");
        if (info == null) {
            return null;
        }
        ret.put("incPAD", (byte) MapleDataTool.getInt("incPAD", info, 0)); // WATK
        ret.put("incMAD", (byte) MapleDataTool.getInt("incMAD", info, 0)); // MATK
        ret.put("incACC", (byte) MapleDataTool.getInt("incACC", info, 0)); // ACC
        ret.put("incEVA", (byte) MapleDataTool.getInt("incEVA", info, 0)); // AVOID
        ret.put("incSpeed", (byte) MapleDataTool.getInt("incSpeed", info, 0)); // SPEED
        ret.put("incJump", (byte) MapleDataTool.getInt("incJump", info, 0)); // JUMP
        ret.put("incMaxHP", (byte) MapleDataTool.getInt("incMaxHP", info, 0)); // HP
        ret.put("incMaxMP", (byte) MapleDataTool.getInt("incMaxMP", info, 0)); // MP
        ret.put("incSTR", (byte) MapleDataTool.getInt("incSTR", info, 0)); // STR
        ret.put("incINT", (byte) MapleDataTool.getInt("incINT", info, 0)); // INT
        ret.put("incLUK", (byte) MapleDataTool.getInt("incLUK", info, 0)); // LUK
        ret.put("incDEX", (byte) MapleDataTool.getInt("incDEX", info, 0)); // DEX
        ret.put("randOption", (byte) MapleDataTool.getInt("randOption", info, 0)); // Black Crystal Wa/MA
        ret.put("randStat", (byte) MapleDataTool.getInt("randStat", info, 0)); // Dark Crystal - Str/Dex/int/Luk

        itemMakeStatsCache.put(Id, ret);
        return ret;
    }

    protected final Map<String, Integer> getEquipStats(final int Id) {
        if (equipStatsCache.containsKey(Id)) {
            return equipStatsCache.get(Id);
        }
        final Map<String, Integer> ret = new LinkedHashMap<String, Integer>();
        final MapleData item = getItemData(Id);
        if (item == null) {
            return null;
        }
        final MapleData info = item.getChildByPath("info");
        if (info == null) {
            return null;
        }
        for (final MapleData data : info.getChildren()) {
            if (data.getName().startsWith("inc")) {
                ret.put(data.getName().substring(3), MapleDataTool.getIntConvert(data));
            }
        }
        ret.put("tuc", MapleDataTool.getInt("tuc", info, 0));
        ret.put("reqLevel", MapleDataTool.getInt("reqLevel", info, 0));
        ret.put("cursed", MapleDataTool.getInt("cursed", info, 0));
        ret.put("success", MapleDataTool.getInt("success", info, 0));
        ret.put("fs", MapleDataTool.getInt("fs", info, 0));
        ret.put("only", MapleDataTool.getInt("only", info, 0));
        ret.put("cash", MapleDataTool.getInt("cash", info, 0));
        equipStatsCache.put(Id, ret);
        return ret;
    }

    public final int getReqLevel(final int Id) {
        return getEquipStats(Id).get("reqLevel");
    }

    public final int getCash(final int Id) {
        return getEquipStats(Id).get("cash");
    }

    public final int getOnly(final int Id) {
        return getEquipStats(Id).get("only");
    }

    public List<Integer> getScrollReqs(int Id) {
        List<Integer> ret = new ArrayList<Integer>();
        MapleData data = getItemData(Id);
        data = data.getChildByPath("req");
        if (data == null) {
            return ret;
        }
        for (MapleData req : data.getChildren()) {
            ret.add(MapleDataTool.getInt(req));
        }
        return ret;
    }

    public MapleWeaponType getWeaponType(int Id) {
        int cat = (Id / 10000) % 100;
        MapleWeaponType[] type = {MapleWeaponType.SWORD1H, MapleWeaponType.AXE1H, MapleWeaponType.BLUNT1H, MapleWeaponType.DAGGER, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.WAND, MapleWeaponType.STAFF, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.SWORD2H, MapleWeaponType.AXE2H, MapleWeaponType.BLUNT2H, MapleWeaponType.SPEAR, MapleWeaponType.POLE_ARM, MapleWeaponType.BOW, MapleWeaponType.CROSSBOW, MapleWeaponType.CLAW, MapleWeaponType.KNUCKLE, MapleWeaponType.GUN};
        if (cat < 30 || cat > 49) {
            return MapleWeaponType.NOT_A_WEAPON;
        }
        return type[cat - 30];
    }

    public boolean isCleanSlate(int scrollId) {
        return scrollId > 2048999 && scrollId < 2049009;
    }

    public boolean isException(int scrollId) {
        return (scrollId != 2040727 && scrollId != 2041058 && !isCleanSlate(scrollId));
    }

    private final void enhanceEquip(Equip nEquip, int offset, boolean chaos) {
        int inc = 1;
        if (Randomizer.getInstance().nextInt(2) == 0 && chaos) {
            inc = -1;
        }
        if (nEquip.getStr() > 0) {
            nEquip.setStr((short) Math.max(0, (nEquip.getStr() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getDex() > 0) {
            nEquip.setDex((short) Math.max(0, (nEquip.getDex() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getInt() > 0) {
            nEquip.setInt((short) Math.max(0, (nEquip.getInt() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getLuk() > 0) {
            nEquip.setLuk((short) Math.max(0, (nEquip.getLuk() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getWatk() > 0) {
            nEquip.setWatk((short) Math.max(0, (nEquip.getWatk() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getWdef() > 0) {
            nEquip.setWdef((short) Math.max(0, (nEquip.getWdef() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getMatk() > 0) {
            nEquip.setMatk((short) Math.max(0, (nEquip.getMatk() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getMdef() > 0) {
            nEquip.setMdef((short) Math.max(0, (nEquip.getMdef() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getAcc() > 0) {
            nEquip.setAcc((short) Math.max(0, (nEquip.getAcc() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getAvoid() > 0) {
            nEquip.setAvoid((short) Math.max(0, (nEquip.getAvoid() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getSpeed() > 0) {
            nEquip.setSpeed((short) Math.max(0, (nEquip.getSpeed() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getJump() > 0) {
            nEquip.setJump((short) Math.max(0, (nEquip.getJump() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getHp() > 0) {
            nEquip.setHp((short) Math.max(0, (nEquip.getHp() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
        if (nEquip.getMp() > 0) {
            nEquip.setMp((short) Math.max(0, (nEquip.getMp() + Randomizer.getInstance().nextInt(offset) * inc)));
        }
    }

    private final void cleanSlateEquip(Equip nEquip, int tuc) {
        if (nEquip.getLevel() + nEquip.getUpgradeSlots() < this.getEquipStats(nEquip.getId()).get("tuc")) {
            nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + tuc));
        }
    }

    public final IItem scrollEquipWithId(final IItem equip, final int scrollId, final boolean usingWhiteScroll) {
        if (equip instanceof Equip) {
            final Equip nEquip = (Equip) equip;
            final Map<String, Integer> stats = this.getEquipStats(scrollId);
            if (((nEquip.getUpgradeSlots() > 0 || isCleanSlate(scrollId)) && Math.ceil(Math.random() * 100) <= stats.get("success"))) {
                short flag = nEquip.getFlag();
                switch (scrollId) {
                case 2040727:
                    flag |= InventoryConstants.SPIKES;
                    nEquip.setFlag((byte) flag);
                    return equip;
                case 2041058:
                    flag |= InventoryConstants.COLD;
                    nEquip.setFlag((byte) flag);
                    return equip;
                case 2049000:
                case 2049001:
                case 2049002:
                case 2049003:
                    cleanSlateEquip(nEquip, 1);
                    break;
                case 2049100:
                case 2049101:
                case 2049102:
                case 2049103:
                case 2049104:
                case 2049112:
                case 2049113:
                case 2049114:
                    enhanceEquip(nEquip, 6, true);
                    break;
                default:
                    for (Entry<String, Integer> stat : stats.entrySet()) {
                        final String key = stat.getKey();
                        if (key.equals("STR")) {
                            nEquip.setStr((short) (nEquip.getStr() + stat.getValue().intValue()));
                        } else if (key.equals("DEX")) {
                            nEquip.setDex((short) (nEquip.getDex() + stat.getValue().intValue()));
                        } else if (key.equals("INT")) {
                            nEquip.setInt((short) (nEquip.getInt() + stat.getValue().intValue()));
                        } else if (key.equals("LUK")) {
                            nEquip.setLuk((short) (nEquip.getLuk() + stat.getValue().intValue()));
                        } else if (key.equals("PAD")) {
                            nEquip.setWatk((short) (nEquip.getWatk() + stat.getValue().intValue()));
                        } else if (key.equals("PDD")) {
                            nEquip.setWdef((short) (nEquip.getWdef() + stat.getValue().intValue()));
                        } else if (key.equals("MAD")) {
                            nEquip.setMatk((short) (nEquip.getMatk() + stat.getValue().intValue()));
                        } else if (key.equals("MDD")) {
                            nEquip.setMdef((short) (nEquip.getMdef() + stat.getValue().intValue()));
                        } else if (key.equals("ACC")) {
                            nEquip.setAcc((short) (nEquip.getAcc() + stat.getValue().intValue()));
                        } else if (key.equals("EVA")) {
                            nEquip.setAvoid((short) (nEquip.getAvoid() + stat.getValue().intValue()));
                        } else if (key.equals("Speed")) {
                            nEquip.setSpeed((short) (nEquip.getSpeed() + stat.getValue().intValue()));
                        } else if (key.equals("Jump")) {
                            nEquip.setJump((short) (nEquip.getJump() + stat.getValue().intValue()));
                        } else if (key.equals("MHP")) {
                            nEquip.setHp((short) (nEquip.getHp() + stat.getValue().intValue()));
                        } else if (key.equals("MMP")) {
                            nEquip.setMp((short) (nEquip.getMp() + stat.getValue().intValue()));
                        } else if (key.equals("afterImage")) {
                        }
                    }
                    break;
                }
                if (isException(scrollId)) {
                    nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                    nEquip.setLevel((byte) (nEquip.getLevel() + 1));
                }
            } else {
                if (!usingWhiteScroll && isException(scrollId)) {
                    nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                }
                if (Math.ceil(Math.random() * 100) <= stats.get("cursed")) {
                    return null;
                }
            }
        }
        return equip;
    }

    public final IItem getEquipById(final int equipId) {
        return getEquipById(equipId, -1);
    }

    public final IItem getEquipById(final int equipId, final int ringId) {
        final Equip nEquip = new Equip(equipId, (byte) 0, ringId);
        nEquip.setQuantity((short) 1);
        final Map<String, Integer> stats = this.getEquipStats(equipId);
        if (stats != null) {
            for (Entry<String, Integer> stat : stats.entrySet()) {
                final String key = stat.getKey();
                if (key.equals("STR")) {
                    nEquip.setStr((short) stat.getValue().intValue());
                } else if (key.equals("DEX")) {
                    nEquip.setDex((short) stat.getValue().intValue());
                } else if (key.equals("INT")) {
                    nEquip.setInt((short) stat.getValue().intValue());
                } else if (key.equals("LUK")) {
                    nEquip.setLuk((short) stat.getValue().intValue());
                } else if (key.equals("PAD")) {
                    nEquip.setWatk((short) stat.getValue().intValue());
                } else if (key.equals("PDD")) {
                    nEquip.setWdef((short) stat.getValue().intValue());
                } else if (key.equals("MAD")) {
                    nEquip.setMatk((short) stat.getValue().intValue());
                } else if (key.equals("MDD")) {
                    nEquip.setMdef((short) stat.getValue().intValue());
                } else if (key.equals("ACC")) {
                    nEquip.setAcc((short) stat.getValue().intValue());
                } else if (key.equals("EVA")) {
                    nEquip.setAvoid((short) stat.getValue().intValue());
                } else if (key.equals("Speed")) {
                    nEquip.setSpeed((short) stat.getValue().intValue());
                } else if (key.equals("Jump")) {
                    nEquip.setJump((short) stat.getValue().intValue());
                } else if (key.equals("MHP")) {
                    nEquip.setHp((short) stat.getValue().intValue());
                } else if (key.equals("MMP")) {
                    nEquip.setMp((short) stat.getValue().intValue());
                } else if (key.equals("tuc")) {
                    nEquip.setUpgradeSlots((byte) stat.getValue().intValue());
                } else if (isDropRestricted(equipId)) {
                    byte flag = nEquip.getFlag();
                    flag |= InventoryConstants.UNTRADEABLE;
                    nEquip.setFlag(flag);
                } else if (stats.get("fs") > 0) {
                    byte flag = nEquip.getFlag();
                    flag |= InventoryConstants.SPIKES;
                    nEquip.setFlag(flag);
                }
            }
        } else {
            return null;
        }
        return nEquip.copy();
    }

    private final short getRandStat(short defaultValue, int maxRange) {
        if (defaultValue == 0) {
            return 0;
        }
        final int lMaxRange = (int) Math.min(Math.ceil(defaultValue * 0.1), maxRange);
        return (short) ((defaultValue - lMaxRange) + Math.floor(Randomizer.getInstance().nextDouble() * (lMaxRange * 2 + 1)));
    }

    public final Equip randomizeStats(Equip equip) {
        equip.setStr(getRandStat(equip.getStr(), 5));
        equip.setDex(getRandStat(equip.getDex(), 5));
        equip.setInt(getRandStat(equip.getInt(), 5));
        equip.setLuk(getRandStat(equip.getLuk(), 5));
        equip.setMatk(getRandStat(equip.getMatk(), 5));
        equip.setWatk(getRandStat(equip.getWatk(), 5));
        equip.setAcc(getRandStat(equip.getAcc(), 5));
        equip.setAvoid(getRandStat(equip.getAvoid(), 5));
        equip.setJump(getRandStat(equip.getJump(), 5));
        equip.setSpeed(getRandStat(equip.getSpeed(), 5));
        equip.setWdef(getRandStat(equip.getWdef(), 10));
        equip.setMdef(getRandStat(equip.getMdef(), 10));
        equip.setHp(getRandStat(equip.getHp(), 10));
        equip.setMp(getRandStat(equip.getMp(), 10));
        return equip;
    }

    public final MapleStatEffect getItemEffect(final int Id) {
        MapleStatEffect ret = itemEffects.get(Integer.valueOf(Id));
        if (ret == null) {
            final MapleData item = getItemData(Id);
            if (item == null) {
                return null;
            }
            MapleData spec = item.getChildByPath("spec");
            ret = MapleStatEffect.loadItemEffectFromData(spec, Id);
            itemEffects.put(Integer.valueOf(Id), ret);
        }
        return ret;
    }

    public final List<Pair<Integer, Integer>> getSummonMobs(final int itemId) {
        if (summonMobCache.containsKey(Integer.valueOf(itemId))) {
            return summonMobCache.get(itemId);
        }
        if (itemId / 10000 != 210) {
            return null;
        }
        final MapleData data = getItemData(itemId).getChildByPath("mob");
        if (data == null) {
            return null;
        }
        final List<Pair<Integer, Integer>> mobPairs = new ArrayList<Pair<Integer, Integer>>();
        for (final MapleData child : data.getChildren()) {
            mobPairs.add(new Pair(
            MapleDataTool.getIntConvert("id", child),
            MapleDataTool.getIntConvert("prob", child)));
        }
        summonMobCache.put(itemId, mobPairs);
        return mobPairs;
    }

    public final Pair<Integer, Integer> getCatchMob(final int itemId) {
        if (catchMobCache.containsKey(Integer.valueOf(itemId))) {
            return catchMobCache.get(itemId);
        }
        final MapleData data = getItemData(itemId);
        if (data == null) {
            return null;
        }
        Pair<Integer, Integer> ret = new Pair(MapleDataTool.getIntConvert("info/mob", data, 0), MapleDataTool.getIntConvert("info/create", data, 0));
        catchMobCache.put(itemId, ret);
        return ret;
    }

    public final int getWatkForProjectile(final int Id) {
        if (projectileWatkCache.containsKey(Id)) {
            return projectileWatkCache.get(Id);
        }
        final MapleData data = getItemData(Id);
        int ret = Integer.valueOf(MapleDataTool.getInt("info/incPAD", data, 0));
        projectileWatkCache.put(Id, ret);
        return ret;
    }

    public final String getName(int Id) {
        if (nameCache.containsKey(Id)) {
            return nameCache.get(Id);
        }
        final MapleData strings = getStringData(Id);
        if (strings == null) {
            return null;
        }
        final String ret = MapleDataTool.getString("name", strings, null);
        nameCache.put(Id, ret);
        return ret;
    }

    public final String getMsg(int Id) {
        if (msgCache.containsKey(Id)) {
            return msgCache.get(Id);
        }
        final MapleData strings = getStringData(Id);
        if (strings == null) {
            return null;
        }
        final String ret = MapleDataTool.getString("msg", strings, null);
        msgCache.put(Id, ret);
        return ret;
    }

    public final boolean isDropRestricted(int Id) {
        if (dropRestrictionCache.containsKey(Id)) {
            return dropRestrictionCache.get(Id);
        }
        final MapleData data = getItemData(Id);
        boolean bRestricted = MapleDataTool.getIntConvert("info/tradeBlock", data, 0) == 1;
        if (!bRestricted) {
            bRestricted = MapleDataTool.getIntConvert("info/quest", data, 0) == 1;
        }
        dropRestrictionCache.put(Id, bRestricted);
        return bRestricted;
    }

    public boolean isPickupRestricted(int Id) {
        if (pickupRestrictionCache.containsKey(Id)) {
            return pickupRestrictionCache.get(Id);
        }
        MapleData data = getItemData(Id);
        boolean bRestricted = MapleDataTool.getIntConvert("info/only", data, 0) == 1;
        pickupRestrictionCache.put(Id, bRestricted);
        return bRestricted;
    }

    public final Map<String, Integer> getSkillStats(final int itemId, final int playerJob) {
        if (skillStatsCache.containsKey(itemId)) {
            return skillStatsCache.get(itemId);
        }
        final Map<String, Integer> ret = new LinkedHashMap<String, Integer>();
        final MapleData item = getItemData(itemId);
        if (item == null) {
            return null;
        }
        final MapleData info = item.getChildByPath("info");
        if (info == null) {
            return null;
        }
        for (final MapleData data : info.getChildren()) {
            if (data.getName().startsWith("inc")) {
                ret.put(data.getName().substring(3), MapleDataTool.getIntConvert(data));
            }
        }
        ret.put("masterLevel", MapleDataTool.getInt("masterLevel", info, 0));
        ret.put("reqSkillLevel", MapleDataTool.getInt("reqSkillLevel", info, 0));
        ret.put("success", MapleDataTool.getInt("success", info, 0));
        final MapleData skill = info.getChildByPath("skill");
        int curskill = 1;
        for (int i = 0; i < skill.getChildren().size(); i++) {
            curskill = MapleDataTool.getInt(Integer.toString(i), skill, 0);
            if (curskill == 0) {
                break;
            }
            if (skill.getChildren().size() == 1 || curskill / 10000 == playerJob) {
                ret.put("skillid", curskill);
                break;
            }
        }
        if (ret.get("skillid") == null) {
            ret.put("skillid", 0);
        }
        skillStatsCache.put(itemId, ret);
        return ret;
    }

    public final List<Integer> petsCanConsume(final int Id) {
        final List<Integer> ret = new ArrayList<Integer>();
        final MapleData data = getItemData(Id);
        int curPetId = 0;
        for (int i = 0; i < data.getChildren().size(); i++) {
            curPetId = MapleDataTool.getInt("spec/" + Integer.toString(i), data, 0);
            if (curPetId == 0) {
                break;
            }
            ret.add(Integer.valueOf(curPetId));
        }
        return ret;
    }

    public final boolean isQuestItem(int Id) {
        if (isQuestItemCache.containsKey(Id)) {
            return isQuestItemCache.get(Id);
        }
        final boolean questItem = MapleDataTool.getIntConvert("info/quest", getItemData(Id), 0) == 1;
        isQuestItemCache.put(Id, questItem);
        return questItem;
    }

    private final void loadCardIdData() {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("SELECT cardid, mobid FROM monstercarddata");
            rs = ps.executeQuery();
            while (rs.next()) {
                monsterBookID.put(rs.getInt(1), rs.getInt(2));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    public final int getCardMobId(int id) {
        return monsterBookID.get(id);
    }

    public final boolean isUntradeableOnEquip(int Id) {
        if (onEquipUntradableCache.containsKey(Id)) {
            return onEquipUntradableCache.get(Id);
        }
        boolean untradableOnEquip = MapleDataTool.getIntConvert("info/equipTradeBlock", getItemData(Id), 0) > 0;
        onEquipUntradableCache.put(Id, untradableOnEquip);
        return untradableOnEquip;
    }

    public final int getScriptedItemNpc(final int Id) {
        if (scriptedItemNPCCache.containsKey(Id)) {
            return scriptedItemNPCCache.get(Id);
        }
        int npcId = MapleDataTool.getInt("spec/npc", getItemData(Id), 0);
        scriptedItemNPCCache.put(Id, npcId);
        return scriptedItemNPCCache.get(Id);
    }

    public final boolean isKarmaAble(final int Id) {
        if (karmaCache.containsKey(Id)) {
            return karmaCache.get(Id);
        }
        final boolean bRestricted = MapleDataTool.getIntConvert("info/tradeAvailable", getItemData(Id), 0) > 0;
        karmaCache.put(Id, bRestricted);
        return bRestricted;
    }

    public final int getStateChangeItem(final int Id) {
        if (stateChangeCache.containsKey(Id)) {
            return stateChangeCache.get(Id);
        } else {
            final int triggerItem = MapleDataTool.getIntConvert("info/stateChangeItem", getItemData(Id), 0);
            stateChangeCache.put(Id, triggerItem);
            return triggerItem;
        }
    }

    public final int getExpById(final int Id) {
        if (expCache.containsKey(Id)) {
            return expCache.get(Id);
        } else {
            final int exp = MapleDataTool.getIntConvert("spec/exp", getItemData(Id), 0);
            expCache.put(Id, exp);
            return exp;
        }
    }

    public final List<MapleTreasure> getTreasureReward(final int Id) {
        if (treasureCache.containsKey(Id)) {
            return treasureCache.get(Id);
        } else {
            List<MapleTreasure> rewards = new ArrayList<MapleTreasure>();
            for (final MapleData child : getItemData(Id).getChildByPath("reward").getChildren()) {
                rewards.add(new MapleTreasure(MapleDataTool.getInt("item", child, 0), MapleDataTool.getInt("prob", child, 0), MapleDataTool.getInt("count", child, 0), MapleDataTool.getString("Effect", child, "")));
            }
            treasureCache.put(Id, rewards);
            return rewards;
        }
    }

    public final short getItemMakeLevel(final int Id) {
        if (itemMakeLevel.containsKey(Id)) {
            return itemMakeLevel.get(Id);
        }
        if (Id / 10000 != 400) {
            return 0;
        }
        final MapleData item = getItemData(Id);
        if (item == null) {
            return -1;
        }
        short lvlEntry = 0;
        final MapleData lvlData = item.getChildByPath("info/lv");
        if (lvlData == null) {
            return -1;
        }
        lvlEntry = (short) MapleDataTool.getInt(lvlData, 0);
        itemMakeLevel.put(Id, lvlEntry);
        return lvlEntry;
    }

    public final byte isConsumeOnPickup(final int itemId) {
        if (consumeOnPickupCache.containsKey(itemId)) {
            return consumeOnPickupCache.get(itemId);
        }
        final MapleData data = getItemData(itemId);
        byte consume = (byte) MapleDataTool.getIntConvert("spec/consumeOnPickup", data, 0);
        if (consume == 0) {
            consume = (byte) MapleDataTool.getIntConvert("specEx/consumeOnPickup", data, 0);
        }
        if (consume == 1) {
            if (MapleDataTool.getIntConvert("spec/party", getItemData(itemId), 0) > 0) {
                consume = 2;
            }
        }
        consumeOnPickupCache.put(itemId, consume);
        return consume;
    }

    public final boolean isTwoHanded(int Id) {
        switch (getWeaponType(Id)) {
        case AXE2H:
        case BLUNT2H:
        case BOW:
        case CLAW:
        case CROSSBOW:
        case POLE_ARM:
        case SPEAR:
        case SWORD2H:
        case GUN:
        case KNUCKLE:
            return true;
        default:
            return false;
        }
    }

    public final void clear() {
        equipStatsCache.clear();
    }
}
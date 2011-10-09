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

import java.awt.Point;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import provider.wz.MapleDataType;
import tools.Pair;
import tools.StringUtil;
import java.util.HashMap;
import tools.DatabaseConnection;

/**
 * @name        MapleLifeFactory
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleLifeFactory {
    private final static MapleLifeFactory instance = new MapleLifeFactory();
    protected final MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Mob.wz"));
    protected final MapleDataProvider stringDataWZ = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz"));
    protected final MapleData mobStringData = stringDataWZ.getData("Mob.img");
    protected final MapleData npcStringData = stringDataWZ.getData("Npc.img");
    protected final Map<Integer, MapleMonsterStats> monsterStats = new HashMap<Integer, MapleMonsterStats>();

    public static MapleLifeFactory getInstance() {
        return instance;
    }

    public final AbstractLoadedMapleLife getLife(int id, String type) {
        if (type.equalsIgnoreCase("n")) {
            return getNPC(id);
        } else if (type.equalsIgnoreCase("m")) {
            return getMonster(id);
        } else {
            System.out.println("Unknown Life type: " + type);
            return null;
        }
    }

    public final void clearMonsterStats() {
        monsterStats.clear();
    }

    public final MapleMonster getMonster(int mid) {
        MapleMonsterStats stats = monsterStats.get(Integer.valueOf(mid));
        if (stats == null) {
            MapleData monsterData = data.getData(StringUtil.getLeftPaddedStr(Integer.toString(mid) + ".img", '0', 11));
            if (monsterData == null) {
                return null;
            }
            MapleData monsterInfoData = monsterData.getChildByPath("info");
            stats = new MapleMonsterStats();
            stats.setHp(MapleDataTool.getIntConvert("maxHP", monsterInfoData));
            stats.setMp(MapleDataTool.getIntConvert("maxMP", monsterInfoData, 0));
            stats.setExp(MapleDataTool.getIntConvert("exp", monsterInfoData, 0));
            stats.setLevel(MapleDataTool.getIntConvert("level", monsterInfoData));
            stats.setAvoid(MapleDataTool.getIntConvert("eva", monsterInfoData, -1));
            stats.setRemoveAfter(MapleDataTool.getIntConvert("removeAfter", monsterInfoData, 0));
            stats.setBoss(MapleDataTool.getIntConvert("boss", monsterInfoData, 0) > 0);
            stats.setFfaLoot(MapleDataTool.getIntConvert("publicReward", monsterInfoData, 0) > 0);
            stats.setUndead(MapleDataTool.getIntConvert("undead", monsterInfoData, 0) > 0);
            stats.setName(MapleDataTool.getString(mid + "/name", mobStringData, "MISSINGNO"));
            stats.setBuffToGive(MapleDataTool.getIntConvert("buff", monsterInfoData, -1));
            final MapleData firstAttackData = monsterInfoData.getChildByPath("firstAttack");
            int firstAttack = 0;
            if (firstAttackData != null) {
                if (firstAttackData.getType() == MapleDataType.FLOAT) {
                    firstAttack = Math.round(MapleDataTool.getFloat(firstAttackData));
                } else {
                    firstAttack = MapleDataTool.getInt(firstAttackData);
                }
            }
            stats.setFirstAttack(firstAttack > 0);
            stats.setDropPeriod(MapleDataTool.getIntConvert("dropItemPeriod", monsterInfoData, 0) * 10000);
            if (stats.isBoss() || mid == 8810018 || mid == 8810026) {
                MapleData hpTagColor = monsterInfoData.getChildByPath("hpTagColor");
                MapleData hpTagBgColor = monsterInfoData.getChildByPath("hpTagBgcolor");
                if (hpTagBgColor == null || hpTagColor == null) {
                    stats.setTagColor(0);
                    stats.setTagBgColor(0);
                } else {
                    stats.setTagColor(MapleDataTool.getIntConvert("hpTagColor", monsterInfoData));
                    stats.setTagBgColor(MapleDataTool.getIntConvert("hpTagBgcolor", monsterInfoData));
                }
            }
            for (MapleData idata : monsterData) {
                if (!idata.getName().equals("info")) {
                    int delay = 0;
                    for (MapleData pic : idata.getChildren()) {
                        delay += MapleDataTool.getIntConvert("delay", pic, 0);
                    }
                    stats.setAnimationTime(idata.getName(), delay);
                }
            }
            final MapleData reviveInfo = monsterInfoData.getChildByPath("revive");
            if (reviveInfo != null) {
                List<Integer> revives = new LinkedList<Integer>();
                for (MapleData data_ : reviveInfo) {
                    revives.add(MapleDataTool.getInt(data_));
                }
                stats.setRevives(revives);
            }
            decodeElementalString(stats, MapleDataTool.getString("elemAttr", monsterInfoData, ""));
            final MapleData monsterSkillData = monsterInfoData.getChildByPath("skill");
            if (monsterSkillData != null) {
                int i = 0;
                List<Pair<Integer, Integer>> skills = new ArrayList<Pair<Integer, Integer>>();
                while (monsterSkillData.getChildByPath(Integer.toString(i)) != null) {
                    skills.add(new Pair<Integer, Integer>(Integer.valueOf(MapleDataTool.getInt(i + "/skill", monsterSkillData, 0)), Integer.valueOf(MapleDataTool.getInt(i + "/level", monsterSkillData, 0))));
                    i++;
                }
                stats.setSkills(skills);
            }
            final MapleData banishData = monsterInfoData.getChildByPath("ban");
            if (banishData != null) {
                stats.setBanishInfo(new BanishInfo(MapleDataTool.getString("banMsg", banishData), MapleDataTool.getInt("banMap/0/field", banishData, -1), MapleDataTool.getString("banMap/0/portal", banishData, "sp")));
            }
            monsterStats.put(Integer.valueOf(mid), stats);
        }
        MapleMonster ret = new MapleMonster(mid, stats);
        return ret;
    }

    private final void decodeElementalString(MapleMonsterStats stats, String elemAttr) {
        for (int i = 0; i < elemAttr.length(); i += 2) {
            stats.setEffectiveness(Element.getFromChar(elemAttr.charAt(i)), ElementalEffectiveness.getByNumber(Integer.valueOf(String.valueOf(elemAttr.charAt(i + 1)))));
        }
    }

    public final MapleNPC getNPC(final int nid) {
        if (nid >= 9901000 && nid <= 9901919) {
            final MapleNPCStats stats = new MapleNPCStats("", true);
            final MapleNPC npc = new MapleNPC(nid, stats);

            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                Connection con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM playernpcs WHERE npcid = ?");
                ps.setInt(1, nid);

                rs = ps.executeQuery();
                if (rs.next()) {
                    stats.setCY(rs.getInt("cy"));
                    stats.setName(rs.getString("name"));
                    stats.setHair(rs.getInt("hair"));
                    stats.setFace(rs.getInt("face"));
                    stats.setSkin(rs.getByte("skin"));
                    stats.setFH(rs.getInt("Foothold"));
                    stats.setRX0(rs.getInt("rx0"));
                    stats.setRX1(rs.getInt("rx1"));
                    npc.setPosition(new Point(rs.getInt("x"), stats.getCY()));
                    ps.close();
                    rs.close();

                    ps = con.prepareStatement("SELECT * FROM playernpcs_equip WHERE npcid = ?");
                    ps.setInt(1, rs.getInt("id"));
                    rs = ps.executeQuery();

                    Map<Byte, Integer> equips = new HashMap<Byte, Integer>();

                    while (rs.next()) {
                        equips.put(rs.getByte("equippos"), rs.getInt("equipid"));
                    }
                    stats.setEquips(equips);
                    rs.close();
                    ps.close();
                }
            } catch (SQLException ex) {
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                    if (rs != null) {
                        rs.close();
                    }
                } catch (SQLException ignore) {
                }
            }
            return npc;
        } else {
            final String name = MapleDataTool.getString(nid + "/name", npcStringData, "MISSINGNO");
            return new MapleNPC(nid, new MapleNPCStats(name, false));
        }
    }

    public final void clear() {
        monsterStats.clear();
    }

    public final class BanishInfo {
        private int map;
        private String portal, msg;

        public BanishInfo(String msg, int map, String portal) {
            this.msg = msg;
            this.map = map;
            this.portal = portal;
        }

        public int getMap() {
            return map;
        }

        public String getPortal() {
            return portal;
        }

        public String getMsg() {
            return msg;
        }
    }
}

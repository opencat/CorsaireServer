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

package tools.factory;

import client.IItem;
import client.MapleCharacter;
import client.MapleInventoryType;
import client.MapleKeyBinding;
import client.MapleMount;
import client.MaplePet;
import client.MapleStat;
import constants.ServerConstants;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.MaplePacket;
import net.SendPacketOpcode;
import net.world.guild.MapleAlliance;
import net.world.guild.MapleGuildSummary;
import server.MapleItemInformationProvider;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import tools.DataTool;
import tools.Pair;
import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        IntraPersonalFactory
 * @author      x711Li
 */
public class IntraPersonalFactory {
    public static void addCharLook(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean mega) {
        mplew.write(chr.getGender());
        mplew.write(chr.getSkinColor());
        mplew.writeInt(chr.getFace());
        mplew.write(mega ? 0 : 1);
        mplew.writeInt(chr.getHair());
        InventoryFactory.addCharEquips(mplew, chr.getInventory(MapleInventoryType.EQUIPPED));
        for (int i = 0; i < 3; i++) {
            if (chr.getPet(i) != null) {
                mplew.writeInt(chr.getPet(i).getId());
            } else {
                mplew.write0(4);
            }
        }
    }

    public static void addCharStats(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getId());
        mplew.writeAsciiString(chr.getName());
        mplew.write0(13 - chr.getName().length());
        mplew.write(chr.getGender());
        mplew.write(chr.getSkinColor());
        mplew.writeInt(chr.getFace());
        mplew.writeInt(chr.getHair());
        for (int i = 0; i < chr.getPets().length; i++) {
            if (chr.getPet(i) != null) {
                mplew.writeInt(chr.getPetIndex(i));
            } else {
                mplew.writeInt(0);
            }
            mplew.writeInt(0);
        }
        mplew.write(chr.getLevel());
        mplew.writeShort(chr.getJob());
        mplew.writeShort(chr.getStr());
        mplew.writeShort(chr.getDex());
        mplew.writeShort(chr.getInt());
        mplew.writeShort(chr.getLuk());
        mplew.writeShort(chr.getHp());
        mplew.writeShort(chr.getMaxHp());
        mplew.writeShort(chr.getMp());
        mplew.writeShort(chr.getMaxMp());
        mplew.writeShort(chr.getRemainingAp());
        mplew.writeShort(chr.getRemainingSp());
        mplew.writeInt(chr.getExp());
        mplew.writeShort(chr.getFame());
        mplew.writeInt(0);
        mplew.writeInt(chr.getMapId());
        mplew.write(chr.getInitialSpawnpoint());
        mplew.writeInt(0);
    }

    public static MaplePacket updatePlayerStats(List<Pair<MapleStat, Integer>> stats) {
        return updatePlayerStats(stats, false);
    }

    public static MaplePacket updatePlayerStats(List<Pair<MapleStat, Integer>> stats, boolean itemReaction) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_STATS);
        mplew.write(itemReaction ? 1 : 0);
        int updateMask = 0;
        for (Pair<MapleStat, Integer> statupdate : stats) {
            updateMask |= statupdate.getLeft().getValue();
        }
        List<Pair<MapleStat, Integer>> mystats = stats;
        if (mystats.size() > 1) {
            Collections.sort(mystats, new Comparator<Pair<MapleStat, Integer>>() {
                @Override
                public int compare(Pair<MapleStat, Integer> o1, Pair<MapleStat, Integer> o2) {
                    int val1 = o1.getLeft().getValue();
                    int val2 = o2.getLeft().getValue();
                    return (val1 < val2 ? -1 : (val1 == val2 ? 0 : 1));
                }
            });
        }
        mplew.writeInt(updateMask);
        for (Pair<MapleStat, Integer> statupdate : mystats) {
            if (statupdate.getLeft().getValue() >= 1) {
                if (statupdate.getLeft().getValue() == 0x1) {
                    mplew.writeShort(statupdate.getRight().shortValue());
                } else if (statupdate.getLeft().getValue() <= 0x4) {
                    mplew.writeInt(statupdate.getRight());
                } else if (statupdate.getLeft().getValue() < 0x20) {
                    mplew.write(statupdate.getRight().shortValue());
                } else if (statupdate.getLeft().getValue() < 0xFFFF) {
                    mplew.writeShort(statupdate.getRight().shortValue());
                } else {
                    mplew.writeInt(statupdate.getRight().intValue());
                }
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket getKeymap(Map<Integer, MapleKeyBinding> keybindings) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.KEYMAP.getValue());
        mplew.write(0);
        for (int x = 0; x < 90; x++) {
            MapleKeyBinding binding = keybindings.get(Integer.valueOf(x));
            if (binding != null) {
                mplew.write(binding.getType());
                mplew.writeInt(binding.getAction());
            } else {
                mplew.write(0);
                mplew.writeInt(0);
            }
        }
        return mplew.getPacket();
    }

    private static void addMonsterBookInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getMonsterBookCover());
        mplew.write(0);
        Map<Integer, Integer> cards = chr.getMonsterBook().getCards();
        mplew.writeShort(cards.size());
        for (Entry<Integer, Integer> all : cards.entrySet()) {
            mplew.writeShort(all.getKey() % 10000);
            mplew.write(all.getValue());
        }
    }

    private static void addRingInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeShort(chr.getMarriageRing() != null ? 1 : 0);
        if (chr.getMarriageRing() != null) {
            mplew.writeInt(chr.getMarriageRing().getPartnerChrId());
            mplew.writeAsciiString(DataTool.getRightPaddedStr(chr.getMarriageRing().getCoupleName(), '\0', 13));
            mplew.writeInt(chr.getMarriageRing().getRingId());
            mplew.writeInt(0);
            mplew.writeInt(chr.getMarriageRing().getCoupleRingId());
            mplew.writeInt(0);
        }
        mplew.writeShort(0);
        mplew.writeShort(0);
    }

    private static void addTeleportRockRecord(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        List<Integer> maps = chr.getTeleportRockMaps(0);
        for (int map : maps) {
            mplew.writeInt(map);
        }
        for (int i = maps.size(); i < 5; i++) {
            mplew.writeInt(999999999);
        }
        maps = chr.getTeleportRockMaps(1);
        for (int map : maps) {
            mplew.writeInt(map);
        }
        for (int i = maps.size(); i < 10; i++) {
            mplew.writeInt(999999999);
        }
    }

    public static MaplePacket getCharInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WARP_TO_MAP.getValue());
        mplew.writeInt(chr.getClient().getChannel() - (chr.getWorld() * ServerConstants.NUM_CHANNELS) - 1);
        mplew.write(1);
        mplew.write(1);
        mplew.writeShort(0);
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(Randomizer.getInstance().nextInt());
        }
        mplew.writeLong(-1);
        mplew.write(0);
        addCharStats(mplew, chr);
        mplew.write(chr.getBuddylist().getCapacity());
        mplew.write(0);
        InventoryFactory.addInventoryInfo(mplew, chr); // <<< EL PROBLEM
        BuffFactory.addSkillInfo(mplew, chr);
        QuestFactory.addQuestInfo(mplew, chr);
        mplew.writeShort(0);
        addRingInfo(mplew, chr);
        addTeleportRockRecord(mplew, chr);
        addMonsterBookInfo(mplew, chr);
        mplew.writeShort(0);
        mplew.writeShort(0); //todo: area keys and w/e
        mplew.writeShort(0);
        mplew.writeLong(DataTool.getKoreanTimestamp(System.currentTimeMillis()));
        return mplew.getPacket();
    }

    public static MaplePacket enableActions() {
        return updatePlayerStats(DataTool.EMPTY_STATUPDATE, true);
    }

    public static MaplePacket updateMount(int charid, MapleMount mount, boolean levelup) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_MOUNT.getValue());
        mplew.writeInt(charid);
        mplew.writeInt(mount.getLevel());
        mplew.writeInt(mount.getExp());
        mplew.writeInt(mount.getTiredness());
        mplew.write(levelup ? (byte) 1 : (byte) 0);
        return mplew.getPacket();
    }

    public static void addRingLook(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(chr.getMarriageRing() != null ? 1 : 0);
        if (chr.getMarriageRing() != null) {
            mplew.writeInt(chr.getMarriageRing().getRingId());
            mplew.writeInt(0);
            mplew.writeInt(chr.getMarriageRing().getCoupleRingId());
            mplew.writeInt(0);
            mplew.writeInt(chr.getMarriageRing().getId());
        }
    }

    public static MaplePacket updateCharLook(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_LOOK);
        mplew.writeInt(chr.getId());
        mplew.write(1);
        addCharLook(mplew, chr, false);
        addRingLook(mplew, chr);
        mplew.write0(5);
        return mplew.getPacket();
    }

    public static MaplePacket charInfo(MapleCharacter chr, boolean isSelf) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHAR_INFO);
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel());
        mplew.writeShort(chr.getJob());
        mplew.writeShort(chr.getFame());
        mplew.write(chr.getCoupleId() > 0 ? 1 : 0);
        String guildName = "";
        String allianceName = "";
        MapleGuildSummary gs = chr.getClient().getChannelServer().getGuildSummary(chr.getGuildId());
        if (chr.getGuildId() > 0 && gs != null) {
            guildName = gs.getName();
            try {
                MapleAlliance alliance = chr.getClient().getChannelServer().getWorldInterface().getAlliance(gs.getAllianceId());
                if (alliance != null) {
                    allianceName = alliance.getName();
                }
            } catch (RemoteException re) {
                re.printStackTrace();
                chr.getClient().getChannelServer().reconnectWorld();
            }
        }
        mplew.writeMapleAsciiString(guildName);
        mplew.writeMapleAsciiString(allianceName);
        mplew.write(isSelf ? 1 : 0);
        MaplePet[] pets = chr.getPets();
        IItem inv = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -114);
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                mplew.write(pets[i].getPosition());
                mplew.writeInt(pets[i].getId());
                mplew.writeMapleAsciiString(pets[i].getName());
                mplew.write(pets[i].getLevel());
                mplew.writeShort(pets[i].getCloseness());
                mplew.write(pets[i].getFullness());
                mplew.writeShort(0);
                mplew.writeInt(inv != null ? inv.getId() : 0);
            }
        }
        mplew.write(0);
        if ((chr.getMount() != null) && (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -18) != null)) {
            mplew.write(chr.getMount().getId());
            mplew.writeInt(chr.getMount().getLevel());
            mplew.writeInt(chr.getMount().getExp());
            mplew.writeInt(chr.getMount().getTiredness());
        } else {
            mplew.write(0);
        }
        List wishList = chr.getWishList();
        mplew.write(wishList.size());
        for (Iterator i$ = wishList.iterator(); i$.hasNext(); ) { int sn = ((Integer)i$.next()).intValue();
            mplew.writeInt(sn);
        }
        mplew.writeInt(chr.getMonsterBook().getBookLevel());
        mplew.writeInt(chr.getMonsterBook().getNormalCard());
        mplew.writeInt(chr.getMonsterBook().getSpecialCard());
        mplew.writeInt(chr.getMonsterBook().getTotalCards());
        mplew.writeInt(chr.getMonsterBookCover() > 0 ? MapleItemInformationProvider.getInstance().getCardMobId(chr.getMonsterBookCover()) : 0);
        IItem medal = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -49);
        if (medal != null) {
            mplew.writeInt(medal.getId());
        } else {
            mplew.writeInt(0);
        }
        List<Integer> quests = new ArrayList<Integer>();
        for (int i = 0; i < 24; i++) {
            if (chr.getQuests().containsKey(MapleQuest.getInstance(29900 + i)) && chr.getQuests().get(MapleQuest.getInstance(29900 + i)).getStatus() == 2) {
                quests.add(29900 + i);
            }
        }
        mplew.writeShort(quests.size());
        for (int i : quests) {
            mplew.writeShort(i);
        }
        return mplew.getPacket();
    }

    public static MaplePacket refreshTeleportRockMapList(MapleCharacter chr, byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(44);
        mplew.writeShort(SendPacketOpcode.TROCK_LOCATIONS.getValue());
        mplew.write(3);
        mplew.write(type);
        List<Integer> maps = chr.getTeleportRockMaps(type);
        int limit = 5;
        if (type == 1) {
            limit = 10;
        }
        for (int map : maps) {
            mplew.writeInt(map);
        }
        for (int i = maps.size(); i < limit; i++) {
            mplew.writeInt(999999999);
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateGender(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.GENDER.getValue());
        mplew.write(chr.getGender());
        return mplew.getPacket();
    }

    public static MaplePacket enableReport() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.ENABLE_REPORT.getValue());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket getWarpToMap(MapleMap to, int spawnPoint, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WARP_TO_MAP);
        mplew.writeInt(chr.getClient().getChannel() - (chr.getWorld() * ServerConstants.NUM_CHANNELS) - 1);
        mplew.writeInt(0); // Count
        mplew.write(0);
        mplew.writeInt(to.getId());
        mplew.write(spawnPoint);
        mplew.writeShort(chr.getHp());
        mplew.write(0);
        mplew.writeLong(DataTool.getTime(System.currentTimeMillis()));
        return mplew.getPacket();
    }
}
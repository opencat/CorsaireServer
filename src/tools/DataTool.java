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

package tools;

import client.MapleBuffStat;
import client.MapleDisease;
import client.MapleStat;
import java.util.Collections;
import java.util.List;
import net.LongValueHolder;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        DataTool
 * @author      Matze
 *              Modified by x711Li
 */
public class DataTool {
    public static final byte[] CHAR_INFO_MAGIC = new byte[]{(byte) 0xff, (byte) 0xc9, (byte) 0x9a, 0x3b};
    public static final byte[] NON_EXPIRE = {0x00, (byte) 0x80, 0x05, (byte) 0xBB, 0x46, (byte) 0xE6, 0x17, 0x02};
    public static final byte[] ITEM_MAGIC = new byte[]{(byte) 0x80, 5};
    public static final byte[] WARP_PACKET_MAGIC = HexTool.getByteArrayFromHexString("02 00 01 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00");
    public static final List<Pair<MapleStat, Integer>> EMPTY_STATUPDATE = Collections.emptyList();

    public static void addExpirationTime(MaplePacketLittleEndianWriter mplew, long time, boolean showexpirationtime) {
        mplew.writeInt(getItemTimestamp(time));
        mplew.write(showexpirationtime ? 1 : 2);
    }

    public static int getItemTimestamp(long realTimestamp) {
        return (int) (((int) ((realTimestamp - 946681229830l)) / 60000) * 35.762787) + -1085019342;
    }

    public static int getQuestTimestamp(long realTimestamp) {
        return (int) (((int) (realTimestamp / 1000 / 60)) * 0.1396987) + 27111908;
    }

    public static long getKoreanTimestamp(long realTimestamp) {
        return realTimestamp * 10000 + 116444592000000000L;
    }

    public static long getTempBanTimestamp(long realTimestamp) {
        return ((realTimestamp * 10000) + 116444736000000000L);
    }
    
    public static long getTime(long realTimestamp) {
        return realTimestamp * 10000 + 116444592000000000L;
    }

    public static String getRightPaddedStr(String in, char padchar, int length) {
        StringBuilder builder = new StringBuilder(in);
        for (int x = in.length(); x < length; x++) {
            builder.append(padchar);
        }
        return builder.toString();
    }

    public static void writeLongMask(MaplePacketLittleEndianWriter mplew, List<Pair<MapleBuffStat, Integer>> statups) {
        long firstmask = 0;
        long secondmask = 0;
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            if (statup.getLeft().isFirst()) {
                firstmask |= statup.getLeft().getValue();
            } else {
                secondmask |= statup.getLeft().getValue();
            }
        }
        mplew.writeLong(firstmask);
        mplew.writeLong(secondmask);
    }

    public static <E extends LongValueHolder> long getLongMask(List<Pair<E, Integer>> statups) {
        long mask = 0;
        for (Pair<E, Integer> statup : statups) {
            mask |= statup.getLeft().getValue();
        }
        return mask;
    }

    public static <E extends LongValueHolder> long getLongMaskFromList(List<E> statups) {
        long mask = 0;
        for (E statup : statups) {
            mask |= statup.getValue();
        }
        return mask;
    }

    public static <E extends LongValueHolder> long getLongMaskD(List<Pair<MapleDisease, Integer>> statups) {
        long mask = 0;
        for (Pair<MapleDisease, Integer> statup : statups) {
            mask |= statup.getLeft().getValue();
        }
        return mask;
    }

    public static boolean isFirstLong(List<MapleBuffStat> statups) {
        for (MapleBuffStat stat : statups) {
            if (stat.isFirst()) {
                return true;
            }
        }
        return false;
    }
}
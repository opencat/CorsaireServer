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

import client.MapleCharacter;
import client.MapleQuestStatus;
import java.util.List;
import net.MaplePacket;
import net.SendPacketOpcode;
import tools.StringUtil;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        QuestFactory
 * @author      x711Li
 */
public class QuestFactory {
    public static void addQuestInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        List<MapleQuestStatus> started = chr.getStartedQuests();
        mplew.writeShort(started.size());
        for (MapleQuestStatus q : started) {
            mplew.writeShort(q.getQuest() == null ? 0 : q.getQuest().getId());
            mplew.writeMapleAsciiString(q.getQuestRecord().length() > 0 ? q.getQuestRecord() : "10000000000000000000000000");
        }
        List<MapleQuestStatus> completed = chr.getCompletedQuests();
        mplew.writeShort(completed.size());
        for (MapleQuestStatus q : completed) {
            mplew.writeShort(q.getQuest().getId());
            mplew.writeLong(q.getCompletionTime());
        }
    }

    public static MaplePacket updateQuestInfo(short quest, boolean message, byte progress) {
        return updateQuestInfo(quest, message, 0, progress, 0, false);
    }

    public static MaplePacket updateQuestInfo(short quest, boolean message, int npc, byte progress, int nextquest, boolean start) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(progress);
        mplew.writeShort(quest);
        if (!message) {
            mplew.writeInt(npc);
            mplew.writeShort(nextquest);
            if (start) {
                mplew.writeShort(0);
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket forfeitQuest(short quest) {
        return updateQuestInfo((byte) 0, quest, "");
    }

    public static MaplePacket completeQuest(short quest) {
        return updateQuestInfo((byte) 2, quest, "");
    }

    public static MaplePacket updateQuestInfo(byte mode, short quest, String info) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.write(mode);
        if (mode == 0) {
            mplew.writeMapleAsciiString(info);
            mplew.writeLong(0L);
        } else if (mode == 1) {
            mplew.writeMapleAsciiString(info);
        } else if (mode == 2) {
            mplew.writeLong(System.currentTimeMillis());
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateQuestRecordExInfo(short quest, String info) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(11);
        mplew.writeShort(quest);
        mplew.writeMapleAsciiString(info);
        return mplew.getPacket();
    }

    public static MaplePacket updateQuestMobKills(MapleQuestStatus status) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(status.getQuest().getId());
        mplew.write(1);
        String killStr = "";
        for (int kills : status.getMobKills().values()) {
            killStr += StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3); // possibly wrong
        }
        mplew.writeMapleAsciiString(killStr);
        return mplew.getPacket();
    }

    public static MaplePacket getShowQuestCompletion(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_QUEST_COMPLETION.getValue());
        mplew.writeShort(id);
        return mplew.getPacket();
    }

    public static MaplePacket startQuest(short quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.write(1);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket updateQuestFinish(short quest, int npc, short nextquest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(8);
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeShort(nextquest);
        return mplew.getPacket();
    }

    public static MaplePacket showQuestExpired(String expire) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(9);
        mplew.writeMapleAsciiString(expire);
        return mplew.getPacket();
    }

    public static MaplePacket questError(short quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(10);
        mplew.writeShort(quest);
        return mplew.getPacket();
    }
}
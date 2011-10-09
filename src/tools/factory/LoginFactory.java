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
import client.MapleClient;
import constants.ServerConstants;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.MaplePacket;
import net.SendPacketOpcode;
import tools.DataTool;
import tools.HexTool;
import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        LoginFactory
 * @author      x711Li
 */
public class LoginFactory {
    public static MaplePacket getRelogResponse() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.RELOG_RESPONSE.getValue());
        mplew.write(1);
        return mplew.getPacket();
    }

    private static void addCharEntry(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean viewAll) {
        IntraPersonalFactory.addCharStats(mplew, chr);
        IntraPersonalFactory.addCharLook(mplew, chr, false);
        if (!viewAll) {
            mplew.write(0);
        }
        if (chr.getJob() % 1000 >= 800) {
            mplew.write(0);
            return;
        }
        mplew.write(1);
        mplew.writeInt(chr.getRank());
        mplew.writeInt(chr.getRankMove());
        mplew.writeInt(chr.getJobRank());
        mplew.writeInt(chr.getJobRankMove());
    }

    public static MaplePacket deleteCharResponse(int cid, byte state) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.DELETE_CHAR_RESPONSE.getValue());
        mplew.writeInt(cid);
        mplew.write(state);
        return mplew.getPacket();
    }

    public static MaplePacket sendGuestTOS() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SEND_LINK.getValue());
        mplew.writeShort(0x100);
        mplew.writeInt(Randomizer.nextInt(999999));
        mplew.writeLong(0);
        mplew.write(new byte[]{(byte) 0x40, (byte) 0xE0, (byte) 0xFD, (byte) 0x3B, (byte) 0x37, (byte) 0x4F, 1});
        mplew.writeLong(DataTool.getKoreanTimestamp(System.currentTimeMillis()));
        mplew.writeInt(0);
        mplew.writeMapleAsciiString("http://corsairems.com");
        return mplew.getPacket();
    }


    public static MaplePacket getHello(short mapleVersion, byte[] sendIv, byte[] recvIv) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(0x0E);
        mplew.writeShort(mapleVersion);
        mplew.writeShort(1);
        mplew.write(49);
        mplew.write(recvIv);
        mplew.write(sendIv);
        mplew.write(8);
        return mplew.getPacket();
    }

    public static MaplePacket getPing() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
        mplew.writeShort(SendPacketOpcode.PING.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket getLoginFailed(int reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(8);
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeInt(reason);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket getPermBan(byte reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS);
        mplew.writeShort(0x02);
        mplew.write(0x0);
        mplew.write(reason);
        mplew.write(HexTool.getByteArrayFromHexString("01 01 01 01 00"));
        return mplew.getPacket();
    }

    public static MaplePacket getTempBan(long timestampTill, byte reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(17);
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write(0x02);
        mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00")); // Account is banned
        mplew.write(reason);
        mplew.writeLong(timestampTill);
        return mplew.getPacket();
    }

    public static MaplePacket getAuthSuccessRequestPin(MapleClient c, String account) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS);
        mplew.write0(6);
        mplew.writeInt(c.getAccID());
        mplew.write(0);
        mplew.write((c.gmLevel() > 0 ? 1 : 0));
        mplew.write(1);
        mplew.write(1);
        mplew.writeMapleAsciiString(account);
        mplew.write(1);
        mplew.write(0);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.write(1);
        mplew.write(2);
        return mplew.getPacket();
    }

    public static MaplePacket wrongPIC() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.WRONG_PIC.getValue());
        mplew.write(20);
        return mplew.getPacket();
    }

    public static MaplePacket pinOperation(int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PIN_OPERATION.getValue());
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static MaplePacket pinRegistered() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PIN_ASSIGNED.getValue());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getServerList(int serverId, String serverName, Map<Integer, Integer> channelLoad) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.write(serverId);
        mplew.writeMapleAsciiString(serverName);
        mplew.write(0);
        mplew.writeMapleAsciiString(ServerConstants.EVENT_MESSAGE); //EVENT_MESSAGE
        mplew.write(0x64); // rate modifier, don't ask O.O!
        mplew.write(0x0); // event xp * 2.6 O.O!
        mplew.write(0x64); // rate modifier, don't ask O.O!
        mplew.write(0x0); // drop rate * 2.6
        mplew.write(0x0);
        int lastChannel = 1;
        Set<Integer> channels = channelLoad.keySet();
        for (int i = 30; i > 0; i--) {
            if (channels.contains(i)) {
                lastChannel = i;
                break;
            }
        }
        mplew.write(lastChannel);
        int load;
        for (int i = 1; i <= lastChannel; i++) {
            if (channels.contains(i)) {
                load = channelLoad.get(i) * 1200 / ServerConstants.CHANNEL_LOAD; // try this
            } else {
                load = ServerConstants.CHANNEL_LOAD; //CHANNEL_LOAD
            }
            mplew.writeMapleAsciiString(serverName + "-" + i);
            mplew.writeInt(load);
            mplew.write(serverId);
            mplew.writeShort(i - 1);
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket enableRecommendedServers() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ENABLE_RECOMMENDED.getValue());
        mplew.writeInt(4);
        return mplew.getPacket();
    }

    public static MaplePacket sendRecommendedServers() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SEND_RECOMMENDED.getValue());
        mplew.write(1);
        mplew.writeInt(0);
        mplew.writeMapleAsciiString(ServerConstants.RECOMMENDED_MESSAGE);
        return mplew.getPacket();
    }

    public static MaplePacket getEndOfServerList() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.write(0xFF);
        return mplew.getPacket();
    }

    public static MaplePacket getServerStatus(int status) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendPacketOpcode.SERVERSTATUS.getValue());
        mplew.writeShort(status);
        return mplew.getPacket();
    }

    public static MaplePacket getServerIP(InetAddress inetAddr, int port, int clientId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVER_IP.getValue());
        mplew.writeShort(0);
        mplew.write(inetAddr.getAddress());
        mplew.writeShort(port);
        mplew.writeInt(clientId);
        mplew.write(new byte[] { 0, 0, 0, 0, 0 } );
        return mplew.getPacket();
    }

    public static MaplePacket authAccountName(String c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0x02);
        mplew.write(HexTool.getByteArrayFromHexString("00 5A 30 5E 00 00 00 DC"));
        mplew.writeMapleAsciiString(c);
        mplew.write(HexTool.getByteArrayFromHexString("01 00 00 00 00 00 00 00 00 00 00 AA B0 9B 96 B8 C7 01 18 00 00 00")); // More Unknown Bytes.
        return mplew.getPacket();
    }

    public static MaplePacket getCharList(MapleClient c, int serverId) {
        List<MapleCharacter> chars = c.loadCharacters(serverId);
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHARLIST.getValue());
        mplew.write(0);
        mplew.write(chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr, false);
        }
        mplew.write(1); //used to be 2
        mplew.writeInt(7); //V88 (in v83 this used to be 6)
        mplew.writeInt(0); //V88 new
        return mplew.getPacket();
    }

    public static MaplePacket showAllCharacter(int chars, int unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALL_CHARLIST.getValue());
        mplew.write(1);
        mplew.writeInt(chars);
        mplew.writeInt(unk);
        return mplew.getPacket();
    }

    public static MaplePacket showAllCharacterInfo(int worldid, List<MapleCharacter> chars) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALL_CHARLIST.getValue());
        mplew.write(0);
        mplew.write(worldid);
        mplew.write(chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr, true);
        }
        return mplew.getPacket();
    }

    public static MaplePacket charNameResponse(String charname, boolean nameUsed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHAR_NAME_RESPONSE.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket addNewCharEntry(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        mplew.write(0);
        addCharEntry(mplew, chr, false);
        return mplew.getPacket();
    }
}
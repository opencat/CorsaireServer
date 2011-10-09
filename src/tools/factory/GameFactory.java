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
import net.MaplePacket;
import net.PlayerInteraction;
import net.SendPacketOpcode;
import server.MapleMiniGame;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        GameFactory
 * @author      x711Li
 */
public class GameFactory {
    public static MaplePacket getMiniGame(MapleClient c, MapleMiniGame minigame, boolean owner, int piece) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(5);
        mplew.write(1);
        mplew.write(2);
        mplew.write(owner ? 0 : 1);
        mplew.write(0);
        IntraPersonalFactory.addCharLook(mplew, minigame.getOwner(), false);
        mplew.writeMapleAsciiString(minigame.getOwner().getName());
        if (minigame.getVisitor() != null) {
            MapleCharacter visitor = minigame.getVisitor();
            mplew.write(1);
            IntraPersonalFactory.addCharLook(mplew, visitor, false);
            mplew.writeMapleAsciiString(visitor.getName());
        }
        mplew.write(0xFF);
        mplew.write(0);
        mplew.writeInt(1);
        mplew.writeInt(minigame.getOwner().getMiniGamePoints("wins", true));
        mplew.writeInt(minigame.getOwner().getMiniGamePoints("ties", true));
        mplew.writeInt(minigame.getOwner().getMiniGamePoints("losses", true));
        mplew.writeInt(minigame.getOwner().getMiniGamePoints("wins", true) - minigame.getOwner().getMiniGamePoints("losses", true) * 2 + 2000);
        if (minigame.getVisitor() != null) {
            MapleCharacter visitor = minigame.getVisitor();
            mplew.write(1);
            mplew.writeInt(1);
            mplew.writeInt(visitor.getMiniGamePoints("wins", true));
            mplew.writeInt(visitor.getMiniGamePoints("ties", true));
            mplew.writeInt(visitor.getMiniGamePoints("losses", true));
            mplew.writeInt(visitor.getMiniGamePoints("wins", true) - visitor.getMiniGamePoints("losses", true) * 2 + 2000);
        }
        mplew.write(0xFF);
        mplew.writeMapleAsciiString(minigame.getDescription());
        mplew.write(piece);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameReady(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.READY.getCode());
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameUnReady(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.UN_READY.getCode());
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameStart(MapleMiniGame game, int loser) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.START.getCode());
        mplew.write(loser);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameSkipOwner(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.SKIP.getCode());
        mplew.write(0x01);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameRequestTie(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.REQUEST_TIE.getCode());
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameDenyTie(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.ANSWER_TIE.getCode());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.writeShort(PlayerInteraction.ROOM.getCode());
        mplew.write(2);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameSkipVisitor(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.SKIP.getCode());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameMoveOmok(MapleMiniGame game, int move1, int move2, int move3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(12);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.MOVE_OMOK.getCode());
        mplew.writeInt(move1);
        mplew.writeInt(move2);
        mplew.write(move3);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameNewVisitor(MapleCharacter c, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("04 0" + slot));
        IntraPersonalFactory.addCharLook(mplew, c, false);
        mplew.writeMapleAsciiString(c.getName());
        mplew.writeInt(1);
        mplew.writeInt(c.getMiniGamePoints("wins", true));
        mplew.writeInt(c.getMiniGamePoints("ties", true));
        mplew.writeInt(c.getMiniGamePoints("losses", true));
        mplew.writeInt(c.getMiniGamePoints("wins", true) - c.getMiniGamePoints("losses", true) * 2 + 2000);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameRemoveVisitor() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("0A 01"));
        return mplew.getPacket();
    }

    private static MaplePacket getMiniGameResult(MapleMiniGame game, int win, int lose, int tie, int result, int forfeit, boolean omok) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.GET_RESULT.getCode());
        if (tie == 0 && forfeit != 1) {
            mplew.write(0);
        } else if (tie == 1) {
            mplew.write(1);
        } else if (forfeit == 1) {
            mplew.write(2);
        }
        mplew.write(0); // owner
        mplew.writeInt(1); // unknown
        mplew.writeInt(game.getOwner().getMiniGamePoints("wins", true) + win); // wins
        mplew.writeInt(game.getOwner().getMiniGamePoints("ties", true) + tie); // ties
        mplew.writeInt(game.getOwner().getMiniGamePoints("losses", true) + lose); // losses
        int points = game.getOwner().getMiniGamePoints("wins", true) + game.getOwner().getMiniGamePoints("losses", true) * 2 + 2000;
        mplew.writeInt(points < 0 ? 0 : points); // points
        mplew.writeInt(1); // start of visitor; unknown
        mplew.writeInt(game.getVisitor().getMiniGamePoints("wins", true) + lose); // wins
        mplew.writeInt(game.getVisitor().getMiniGamePoints("ties", true) + tie); // ties
        mplew.writeInt(game.getVisitor().getMiniGamePoints("losses", true) + win); // losses
        points = game.getVisitor().getMiniGamePoints("wins", true) + game.getVisitor().getMiniGamePoints("losses", true) * 2 + 2000;
        mplew.writeInt(points < 0 ? 0 : points); // points
        game.getOwner().setMiniGamePoints(game.getVisitor(), result, true);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameOwnerWin(MapleMiniGame game) {
        return getMiniGameResult(game, 0, 1, 0, 1, 0, true);
    }

    public static MaplePacket getMiniGameVisitorWin(MapleMiniGame game) {
        return getMiniGameResult(game, 1, 0, 0, 2, 0, true);
    }

    public static MaplePacket getMiniGameTie(MapleMiniGame game) {
        return getMiniGameResult(game, 0, 0, 1, 3, 0, true);
    }

    public static MaplePacket getMiniGameOwnerForfeit(MapleMiniGame game) {
        return getMiniGameResult(game, 1, 0, 0, 2, 1, true);
    }

    public static MaplePacket getMiniGameVisitorForfeit(MapleMiniGame game) {
        return getMiniGameResult(game, 0, 1, 0, 1, 1, true);
    }

    public static MaplePacket getMiniGameClose(byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.EXIT.getCode());
        mplew.write(1);
        mplew.write(3);
        return mplew.getPacket();
    }

    public static MaplePacket getMatchCard(MapleClient c, MapleMiniGame minigame, boolean owner, int piece) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("05 02 02"));
        mplew.write(owner ? 0 : 1);
        mplew.write(0);
        IntraPersonalFactory.addCharLook(mplew, minigame.getOwner(), false);
        mplew.writeMapleAsciiString(minigame.getOwner().getName());
        if (minigame.getVisitor() != null) {
            MapleCharacter visitor = minigame.getVisitor();
            mplew.write(1);
            IntraPersonalFactory.addCharLook(mplew, visitor, false);
            mplew.writeMapleAsciiString(visitor.getName());
        }
        mplew.write(0xFF);
        mplew.write(0);
        mplew.writeInt(2);
        mplew.writeInt(minigame.getOwner().getMiniGamePoints("wins", false));
        mplew.writeInt(minigame.getOwner().getMiniGamePoints("ties", false));
        mplew.writeInt(minigame.getOwner().getMiniGamePoints("losses", false));
        mplew.writeInt(2000);
        if (minigame.getVisitor() != null) {
            MapleCharacter visitor = minigame.getVisitor();
            mplew.write(1);
            mplew.writeInt(2);
            mplew.writeInt(visitor.getMiniGamePoints("wins", false));
            mplew.writeInt(visitor.getMiniGamePoints("ties", false));
            mplew.writeInt(visitor.getMiniGamePoints("losses", false));
            mplew.writeInt(2000);
        }
        mplew.write(0xFF);
        mplew.writeMapleAsciiString(minigame.getDescription());
        mplew.write(piece);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getMatchCardStart(MapleMiniGame game, int loser) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteraction.START.getCode());
        mplew.write(loser);
        mplew.write(0x0C);
        int last = 13;
        if (game.getMatchesToWin() > 10) {
            last = 31;
        } else if (game.getMatchesToWin() > 6) {
            last = 21;
        }
        for (int i = 1; i < last; i++) {
            mplew.writeInt(game.getCardId(i));
        }
        return mplew.getPacket();
    }

    public static MaplePacket getMatchCardNewVisitor(MapleCharacter c, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("04 0" + slot));
        IntraPersonalFactory.addCharLook(mplew, c, false);
        mplew.writeMapleAsciiString(c.getName());
        mplew.writeInt(1);
        mplew.writeInt(c.getMiniGamePoints("wins", false));
        mplew.writeInt(c.getMiniGamePoints("ties", false));
        mplew.writeInt(c.getMiniGamePoints("losses", false));
        mplew.writeInt(2000);
        return mplew.getPacket();
    }

    public static MaplePacket getMatchCardSelect(MapleMiniGame game, int turn, int slot, int firstslot, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("3E 0" + turn));
        if (turn == 1) {
            mplew.write(slot);
        } else if (turn == 0) {
            mplew.write(slot);
            mplew.write(firstslot);
            mplew.write(type);
        }
        return mplew.getPacket();
    }

    public static MaplePacket getMatchCardOwnerWin(MapleMiniGame game) {
        return getMiniGameResult(game, 1, 0, 0, 1, 0, false);
    }

    public static MaplePacket getMatchCardVisitorWin(MapleMiniGame game) {
        return getMiniGameResult(game, 0, 1, 0, 2, 0, false);
    }

    public static MaplePacket getMatchCardTie(MapleMiniGame game) {
        return getMiniGameResult(game, 0, 0, 1, 3, 0, false);
    }

    public static MaplePacket addOmokBox(MapleCharacter c, int ammount, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        addAnnounceBox(mplew, c.getMiniGame(), 1, 0, ammount, type);
        return mplew.getPacket();
    }

    public static MaplePacket removeOmokBox(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        mplew.write(0);
        return mplew.getPacket();
    }

    private static void addAnnounceBox(MaplePacketLittleEndianWriter mplew, MapleMiniGame game, int gametype, int type, int ammount, int joinable) {
        mplew.write(gametype);
        mplew.writeInt(game.getObjectId());
        mplew.writeMapleAsciiString(game.getDescription());
        mplew.write(0);
        mplew.write(type);
        mplew.write(ammount);
        mplew.write(2);
        mplew.write(joinable);
    }

    public static MaplePacket addMatchCardBox(MapleCharacter c, int ammount, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        addAnnounceBox(mplew, c.getMiniGame(), 2, 0, ammount, type);
        return mplew.getPacket();
    }

    public static MaplePacket removeMatchcardBox(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameChat(MapleCharacter c, String chat, boolean owner) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("06 08"));
        mplew.write(owner ? 0 : 1);
        mplew.writeMapleAsciiString(c.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static MaplePacket removeCharBox(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        mplew.write(0);
        return mplew.getPacket();
    }
}
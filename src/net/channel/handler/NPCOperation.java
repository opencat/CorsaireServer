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

package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import net.SendPacketOpcode;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;
import server.DatabaseInformationProvider;
import server.MapleItemInformationProvider;
import server.life.MapleNPC;
import server.maps.MapleMapObject;
import server.quest.MapleQuest;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;
import tools.factory.IntraPersonalFactory;

/**
 * @name        NPCOperation
 * @author      x711Li
 */
public class NPCOperation {
    public static final void NPCTalkHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        int oid = slea.readInt();
        MapleMapObject obj = c.getPlayer().getMap().getMapObject(oid);
        if (obj instanceof MapleNPC) {
            MapleNPC npc = (MapleNPC) obj;
            if (npc.hasShop() && npc.getShopId() < 9999999) {
                if (c.getPlayer().getShop() != null) {
                    return;
                }
                npc.sendShop(c);
            } else {
                if (c.getCM() != null || c.getQM() != null) {
                    c.announce(IntraPersonalFactory.enableActions());
                    return;
                }
                NPCScriptManager.getInstance().start(c, npc.getId(), null, null, -1);
            }
        }
    }

    public static final void NPCTalkMoreHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte lastMsg = slea.readByte(); // 00 (last msg type I think)
        byte action = slea.readByte(); // 00 = end chat, 01 == follow
        if (lastMsg == 2) {
            if (action != 0) {
                String returnText = slea.readMapleAsciiString();
                if (c.getQM() != null) {
                    c.getQM().setGetText(returnText);
                    if (c.getQM().isStart()) {
                        QuestScriptManager.getInstance().start(c, action, lastMsg, -1);
                    } else {
                        QuestScriptManager.getInstance().end(c, action, lastMsg, -1);
                    }
                } else {
                    c.getCM().setGetText(returnText);
                    NPCScriptManager.getInstance().action(c, action, lastMsg, -1);
                }
            } else if (c.getQM() != null) {
                c.getQM().dispose();
            } else {
                c.getCM().dispose();
            }
        } else {
            int selection = -1;
            if (slea.available() >= 4) {
                selection = slea.readInt();
            } else if (slea.available() > 0) {
                selection = slea.readByte();
            }
            if (c.getQM() != null) {
                if (c.getQM().isStart()) {
                    QuestScriptManager.getInstance().start(c, action, lastMsg, selection);
                } else {
                    QuestScriptManager.getInstance().end(c, action, lastMsg, selection);
                }
            } else if (c.getCM() != null) {
                NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
            }
        }
    }

    public static final void QuestActionHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte action = slea.readByte();
        short quest = slea.readShort();
        MapleCharacter player = c.getPlayer();
        if (action == 1) { // start quest
            int npc = slea.readInt();
            if (DatabaseInformationProvider.getInstance().isScripted(quest)) {
                QuestScriptManager.getInstance().start(c, npc, quest);
                return;
            }
            MapleQuest.getInstance(quest).start(player, npc);
        } else if (action == 2) { // complete quest
            if (c.getPlayer().getQuest(MapleQuest.getInstance(quest)).getStatus() == 1) {
                int npc = slea.readInt();
                if (DatabaseInformationProvider.getInstance().isScripted(quest)) {
                    QuestScriptManager.getInstance().end(c, npc, quest);
                    return;
                }
                slea.readInt();
                if (slea.available() >= 4) {
                    int selection = slea.readInt();
                    MapleQuest.getInstance(quest).complete(player, npc, selection);
                } else {
                    MapleQuest.getInstance(quest).complete(player, npc);
                }
            }
        } else if (action == 3) { // forfeit quest
            MapleQuest.getInstance(quest).forfeit(player);
        } else if (action == 4) { // scripted start quest
            int npc = slea.readInt();
            QuestScriptManager.getInstance().start(c, npc, quest);
        } else if (action == 5) { // scripted end quests
            int npc = slea.readInt();
            QuestScriptManager.getInstance().end(c, npc, quest);
        }
    }

    public static final void NPCShopHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getShop() == null) {
            c.disconnect(true, false);
            return;
        }
        if (c.getPlayer().gmLevel() == 1 || c.getPlayer().gmLevel() == 2) {
            return;
        }
        byte bmode = slea.readByte();
        if (bmode == 0) { //BUY
            slea.readShort();//?
            int itemId = slea.readInt();
            short quantity = slea.readShort();
            if (c.getPlayer().getShop() != null && quantity > 0) {
                c.getPlayer().getShop().buy(c, itemId, quantity);
            }
        } else if (bmode == 1) { //SELL
            byte slot = (byte) slea.readShort();
            int itemId = slea.readInt();
            short quantity = slea.readShort();
            c.getPlayer().getShop().sell(c, MapleItemInformationProvider.getInstance().getInventoryType(itemId), slot, quantity);
        } else if (bmode == 2) { //RECHARGE
            byte slot = (byte) slea.readShort();
            c.getPlayer().getShop().recharge(c, slot);
        } else if (bmode == 3) { //CLOSE
            c.getPlayer().setNpcId(-1);
            c.getPlayer().setShop(null);
        }
    }

    public static final void NPCActionHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        int length = (int) slea.available();
        if (length == 6) { // NPC Talk
            mplew.writeShort(SendPacketOpcode.NPC_ACTION);
            mplew.writeInt(slea.readInt());
            mplew.writeShort(slea.readShort());
            c.announce(mplew.getPacket());
        } else if (length > 6) { // NPC Move
            final byte[] bytes = slea.read(length - 9);
            mplew.writeShort(SendPacketOpcode.NPC_ACTION);
            mplew.write(bytes);
            c.announce(mplew.getPacket());
        }
    }
}
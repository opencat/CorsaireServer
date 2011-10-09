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

import java.util.List;
import net.MaplePacket;
import net.SendPacketOpcode;
import server.DueyPackages;
import tools.DataTool;
import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @name        DueyFactory
 * @author      x711Li
 */
public class DueyFactory {
    public static MaplePacket removeItemFromDuey(boolean remove, int Package) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(0x17);
        mplew.writeInt(Package);
        mplew.write(remove ? 3 : 4);
        return mplew.getPacket();
    }

    public static MaplePacket sendDueyMessage(byte operation) {
        return sendDuey(operation, null);
    }

    public static MaplePacket sendDuey(byte operation, List<DueyPackages> packages) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(operation);
        if (operation == 8) {
            mplew.write(0);
            mplew.write(packages.size());
            for (DueyPackages dp : packages) {
                mplew.writeInt(dp.getPackageId());
                mplew.writeAsciiString(dp.getSender());
                for (int i = dp.getSender().length(); i < 13; i++) {
                    mplew.write(0);
                }
                mplew.writeInt(dp.getMesos());
                mplew.writeLong(DataTool.getQuestTimestamp(dp.sentTimeInMilliseconds()));
                mplew.writeLong(0); // Contains message o____o.
                for (int i = 0; i < 48; i++) {
                    mplew.writeInt(Randomizer.getInstance().nextInt(Integer.MAX_VALUE));
                }
                mplew.writeInt(0);
                mplew.write(0);
                if (dp.getItem() != null) {
                    mplew.write(1);
                    InventoryFactory.addItemInfo(mplew, dp.getItem(), true, true);
                } else {
                    mplew.write(0);
                }
            }
            mplew.write(0);
        }
        return mplew.getPacket();
    }
}
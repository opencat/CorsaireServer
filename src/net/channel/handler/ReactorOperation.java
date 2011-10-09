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

import client.MapleClient;
import scripting.reactor.ReactorScriptManager;
import server.maps.MapleReactor;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @name        ReactorOperation
 * @author      x711Li
 */
public class ReactorOperation {
    public static final void ReactorDamageHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        int charPos = slea.readInt();
        short stance = slea.readShort();
        MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (reactor != null && reactor.isAlive()) {
            reactor.hitReactor(charPos, stance, c);
        }
    }

    public static final void ReactorTouchHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        /*int oid = slea.readInt();
        MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (reactor != null) {
            if (slea.readByte() != 0) {
                ReactorScriptManager.getInstance().touch(c, reactor);
            } else {
                ReactorScriptManager.getInstance().untouch(c, reactor);
            }
        }*/
    }
}
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
import java.util.Arrays;
import java.util.List;
import net.channel.ChannelServer;
import server.shops.HiredMerchant;
import server.maps.MapleMap;
import server.maps.MapleMapObjectType;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.factory.IntraPersonalFactory;
import tools.factory.MarketFactory;

/**
 * @name        MarketOperation
 * @author      x711Li
 */
public class MarketOperation {
    public static final void MerchantRequestHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if(c.getChannelServer().isShuttingDown()) {
            c.getPlayer().dropMessage(1, "The server is currently restarting. You may set up a Hired Merchant once the server is completely reset.");
        } else if (c.getPlayer().getHiredMerchantItems().size() > 0) {
            c.getPlayer().dropMessage(1, "Please withdraw all items from Fredrick before setting up shop.");
            return;
        } else if (c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 23000, Arrays.asList(MapleMapObjectType.HIRED_MERCHANT)).size() == 0 && c.getPlayer().getMapId() > 910000000 && c.getPlayer().getMapId() < 910000023) {
            if (!c.getPlayer().hasMerchant()) {
                c.announce(MarketFactory.hiredMerchantBox());
            } else {
                c.getPlayer().dropMessage(1, "You already have a store open.");
            }
        } else {
            c.getPlayer().dropMessage(1, "You cannot open your hired merchant here.");
        }
    }

    public static final void MerchantRemoteAccessHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().hasMerchant()) {
            c.getPlayer().dropMessage(1, "You do not have a shop open!");
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        for (int i = 910000001; i < 910000023; i++) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                List<HiredMerchant> merc = cserv.getMapFactory().getMap(i).getHiredMerchants();
                for (HiredMerchant hm : merc) {
                    if (hm.getOwnerId() == c.getPlayer().getId()) {
                        c.getPlayer().setHiredMerchant(hm);
                        hm.setOpen(false);
                        hm.removeAllVisitors();
                        c.announce(MarketFactory.getHiredMerchant(c.getPlayer(), hm, false));
                        break;
                    }
                }
            }
        }
    }

    public static final void MerchantGoHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int id = slea.readInt();
        for (int i = 910000001; i < 910000023; i++) {
            MapleMap map = c.getChannelServer().getMapFactory().getMap(i);
            for (HiredMerchant hm : map.getHiredMerchants()) {
                if (hm.getOwnerId() == id) {
                    c.getPlayer().changeMap(map);
                    c.getPlayer().setHiredMerchant(hm);
                    hm.addVisitor(c.getPlayer());
                    c.announce(MarketFactory.getHiredMerchant(c.getPlayer(), hm, false));
                    return;
                }
            }
        }
    }
}
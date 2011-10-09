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

import client.MapleClient;
import server.shops.MapleShopFactory;
import server.maps.MapleMapObjectType;
import tools.factory.NPCFactory;

/**
 * @name        MapleNPC
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleNPC extends AbstractLoadedMapleLife {
    private final MapleNPCStats stats;

    public MapleNPC(final int id, final MapleNPCStats stats) {
        super(id);
        this.stats = stats;
    }

    public final boolean hasShop() {
        return MapleShopFactory.getInstance().getShopForNPC(getId()) != null;
    }

    public final int getShopId() {
        return MapleShopFactory.getInstance().getShopForNPC(getId()).getId();
    }

    public final void sendShop(final MapleClient c) {
        MapleShopFactory.getInstance().getShopForNPC(getId()).sendShop(c);
    }

    @Override
    public final void sendSpawnData(final MapleClient client) {
        client.announce(NPCFactory.spawnNPC(this));
    }

    @Override
    public final void sendDestroyData(final MapleClient client) {
        client.announce(NPCFactory.removeNPC(getObjectId()));
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.NPC;
    }

    public final String getName() {
        return stats.getName();
    }

    public final void setCustom(boolean custom) {
        stats.setCustom(custom);
    }

    public final Boolean isCustom() {
        return stats.isCustom();
    }
}

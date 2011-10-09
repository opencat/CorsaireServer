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

package server;

import java.io.Serializable;

/**
 * @name        MapleEvent
 * @author      x711Li
 */
public final class MapleEvent implements Serializable {
    private static final long serialVersionUID = 5621488276389817456L;
    private int map, channel;
    private String host, description;

    public MapleEvent() {
    }

    public MapleEvent(int map, int channel, String host, String description) {
        this.map = map;
        this.channel = channel;
        this.host = host;
        this.description = description;
    }

    public int getMap() {
        return map;
    }

    public int getChannel() {
        return channel;
    }

    public String getHost() {
        return host;
    }

    public String getDescription() {
        return description;
    }
}

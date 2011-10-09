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

package server.maps;

import java.awt.Point;
import client.MapleClient;
import scripting.portal.PortalScriptManager;
import server.MaplePortal;

/**
 * @name        MapleGenericPortal
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleGenericPortal implements MaplePortal {
    private String name;
    private String target;
    private Point position;
    private int targetmap;
    private int type;
    private boolean status = true;
    private int id;
    private String scriptName;
    private boolean portalState;

    public MapleGenericPortal(final int type) {
        this.type = type;
    }

    @Override
    public final int getId() {
        return id;
    }

    public final void setId(final int id) {
        this.id = id;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final Point getPosition() {
        return position;
    }

    @Override
    public final String getTarget() {
        return target;
    }

    @Override
    public final void setPortalStatus(final boolean newStatus) {
        this.status = newStatus;
    }

    @Override
    public final boolean getPortalStatus() {
        return status;
    }

    @Override
    public final int getTargetMapId() {
        return targetmap;
    }

    @Override
    public final int getType() {
        return type;
    }

    @Override
    public final String getScriptName() {
        return scriptName;
    }

    public final void setName(final String name) {
        this.name = name;
    }

    public final void setPosition(final Point position) {
        this.position = position;
    }

    public final void setTarget(final String target) {
        this.target = target;
    }

    public final void setTargetMapId(final int targetmapid) {
        this.targetmap = targetmapid;
    }

    @Override
    public final void setScriptName(final String scriptName) {
        this.scriptName = scriptName;
    }

    @Override
    public final void enterPortal(final MapleClient c) {
        if (getScriptName() != null) {
            PortalScriptManager.getInstance().start(c, null, "portal/" + scriptName);
        } else if (getTargetMapId() != 999999999) {
        final MapleMap to = c.getChannelServer().getMapFactory().getMap(getTargetMapId());
        c.getPlayer().changeMap(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()));
    }
    }

    public void setPortalState(boolean state) {
        this.portalState = state;
    }

    public boolean getPortalState() {
        return portalState;
    }
}
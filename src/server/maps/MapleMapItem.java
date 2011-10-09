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
import client.IItem;
import client.MapleCharacter;
import client.MapleClient;
import java.util.concurrent.locks.ReentrantLock;
import tools.factory.EffectFactory;

/**
 * @name        MapleMapItem
 * @author      Matze
 *              Modified by x711Li
 */
public class MapleMapItem extends AbstractMapleMapObject {
    protected IItem item;
    protected MapleMapObject dropper;
    protected int ownerid;
    protected int meso;
    protected int displayMeso;
    protected int dropperId;
    protected Point dropperPos;
    protected boolean pickedUp = false;
    public ReentrantLock itemLock = new ReentrantLock();
    
    public MapleMapItem(IItem item, Point position, int _dropperId, Point _dropperPos, MapleCharacter owner) {
        setPosition(position);
        this.item = item;
        this.dropperId = _dropperId;
        this.dropperPos = _dropperPos;
        this.ownerid = owner.getId();
        this.meso = 0;
    }

    public MapleMapItem(int meso, int displayMeso, Point position, int _dropperId, Point _dropperPos, MapleCharacter owner) {
        setPosition(position);
        this.item = null;
        this.meso = meso;
        this.displayMeso = displayMeso;
        this.dropperId = _dropperId;
        this.dropperPos = _dropperPos;
        this.ownerid = owner.getId();
    }

    public final IItem getItem() {
        return item;
    }

    public final int getDropperId() {
        return this.dropperId;
    }

    public final Point getDropperPos() {
        return this.dropperPos;
    }

    public final int getOwnerId() {
        return ownerid;
    }

    public final int getMeso() {
        return meso;
    }

    public final boolean isPickedUp() {
        return pickedUp;
    }

    public void setPickedUp(final boolean pickedUp) {
        this.pickedUp = pickedUp;
        if (pickedUp)
        try {
            super.finalize();
        } catch (Throwable t){}
    }

    @Override
    public void sendDestroyData(final MapleClient client) {
        client.announce(EffectFactory.removeItemFromMap(getObjectId(), 1, 0));
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.ITEM;
    }

    @Override
    public void sendSpawnData(final MapleClient client) {
        if (getMeso() > 0) {
            client.announce(EffectFactory.dropMesoFromMapObject(displayMeso, getObjectId(), this.dropperId, ownerid, null, getPosition(), (byte) 2, (byte) 2));
        } else {
            client.announce(EffectFactory.dropItemFromMapObject(getItem().getId(), getObjectId(), 0, ownerid, null, getPosition(), (byte) 2, (byte) 2));
        }
    }
}

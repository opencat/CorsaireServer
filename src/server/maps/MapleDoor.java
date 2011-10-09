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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import client.MapleCharacter;
import client.MapleClient;
import net.world.MaplePartyCharacter;
import server.MaplePortal;
import tools.factory.EffectFactory;
import tools.factory.IntraPersonalFactory;

/**
 * @name        MapleDoor
 * @author      Matze
 */
public class MapleDoor extends AbstractMapleMapObject {
    private MapleCharacter owner;
    private MapleMap town;
    private MaplePortal townPortal;
    private MapleMap target;
    private Point targetPosition;

    public MapleDoor(final MapleCharacter owner, final Point targetPosition) {
        super();
        this.owner = owner;
        this.target = owner.getMap();
        this.targetPosition = targetPosition;
        setPosition(this.targetPosition);
        this.town = this.target.getReturnMap();
        this.townPortal = getFreePortal();
    }

    public MapleDoor(final MapleDoor origDoor) {
        super();
        this.owner = origDoor.owner;
        this.town = origDoor.town;
        this.townPortal = origDoor.townPortal;
        this.target = origDoor.target;
        this.targetPosition = origDoor.targetPosition;
        this.townPortal = origDoor.townPortal;
        setPosition(this.townPortal.getPosition());
    }

    private final MaplePortal getFreePortal() {
        List<MaplePortal> freePortals = new ArrayList<MaplePortal>();
        for (final MaplePortal port : town.getPortals()) {
            if (port.getType() == 6) {
                freePortals.add(port);
            }
        }
        Collections.sort(freePortals, new Comparator<MaplePortal>() {
            public final int compare(final MaplePortal o1, final MaplePortal o2) {
                if (o1.getId() < o2.getId()) {
                    return -1;
                } else if (o1.getId() == o2.getId()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
        for (final MapleMapObject obj : town.getMapObjects()) {
            if (obj instanceof MapleDoor) {
                final MapleDoor door = (MapleDoor) obj;
                if (door.getOwner().getParty() != null &&
                        owner.getParty().containsMembers(new MaplePartyCharacter(door.getOwner()))) {
                    freePortals.remove(door.getTownPortal());
                }
            }
        }
        return freePortals.iterator().next();
    }

    public final void sendSpawnData(final MapleClient client) {
        if (target.getId() == client.getPlayer().getMapId() || owner == client.getPlayer() && owner.getParty() == null) {
            client.announce(EffectFactory.spawnDoor(owner.getId(), town.getId() == client.getPlayer().getMapId() ? townPortal.getPosition() : targetPosition, true));
            if (owner.getParty() != null && (owner == client.getPlayer() || owner.getParty().containsMembers(new MaplePartyCharacter(client.getPlayer())))) {
                client.announce(EffectFactory.partyPortal(town.getId(), target.getId(), targetPosition));
            }
            client.announce(EffectFactory.spawnPortal(town.getId(), target.getId(), targetPosition));
        }
    }

    public final void sendDestroyData(final MapleClient client) {
        if (target.getId() == client.getPlayer().getMapId() || owner == client.getPlayer() || owner.getParty() != null && owner.getParty().containsMembers(new MaplePartyCharacter(client.getPlayer()))) {
            if (owner.getParty() != null && (owner == client.getPlayer() || owner.getParty().containsMembers(new MaplePartyCharacter(client.getPlayer())))) {
                client.announce(EffectFactory.partyPortal(999999999, 999999999, new Point(-1, -1)));
            }
            client.announce(EffectFactory.removeDoor(owner.getId(), false));
            client.announce(EffectFactory.removeDoor(owner.getId(), true));
        }
    }

    public final void warp(final MapleCharacter chr, final boolean toTown) {
        int mapid = chr.getMap().getId();

        if(mapid == 101000100 || mapid == 100040004 || mapid == 109030001 || mapid == 108000200 || mapid == 108000201 || mapid == 108000202
                || mapid == 677000010 || mapid == 682010200 || mapid == 682010201 || mapid == 682010202 || mapid == 682010203 || mapid == 682010000) {
            chr.dropMessage("You cannot warp out of a Party Quest map.");
            return;
        }

        if (chr == owner || owner.getParty() != null && owner.getParty().containsMembers(new MaplePartyCharacter(chr))) {
            if (!toTown) {
                chr.changeMap(target, targetPosition);
            } else {
                chr.changeMap(town, townPortal);
            }
        } else {
            chr.getClient().announce(IntraPersonalFactory.enableActions());
        }
    }

    public final MapleCharacter getOwner() {
        return owner;
    }

    public final MapleMap getTown() {
        return town;
    }

    public final MaplePortal getTownPortal() {
        return townPortal;
    }

    public final MapleMap getTarget() {
        return target;
    }

    public final Point getTargetPosition() {
        return targetPosition;
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.DOOR;
    }
}

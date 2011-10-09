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

package net.channel;

import client.MapleCharacter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.MaplePacket;
import net.world.CharacterTransfer;
import server.TimerManager;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @name        PlayerStorage
 * @author      Matze
 *              Modified by x711Li
 */
public class PlayerStorage implements IPlayerStorage {
    private final Map<String, MapleCharacter> nameToChar = new LinkedHashMap<String, MapleCharacter>();
    private final Map<Integer, MapleCharacter> idToChar = new LinkedHashMap<Integer, MapleCharacter>();
    private final Map<Integer, CharacterTransfer> pendingCharacter = new HashMap<Integer, CharacterTransfer>();
    private final Lock cservLock = new ReentrantLock();
    private final Lock transferLock = new ReentrantLock();

    public PlayerStorage() {
        TimerManager.getInstance().schedule(new PersistingTask(), 900000);
    }

    public final void registerPlayer(final MapleCharacter chr) {
        cservLock.lock();
        try {
            nameToChar.put(chr.getName().toLowerCase(), chr);
            idToChar.put(chr.getId(), chr);
        } finally {
            cservLock.unlock();
        }
    }
    
    public final void registerPendingPlayer(final CharacterTransfer chr, final int playerid) {
        transferLock.lock();
        try {
            pendingCharacter.put(playerid, chr);
        } finally {
            transferLock.unlock();
        }
    }

    public final void deregisterPlayer(final MapleCharacter chr) {
        cservLock.lock();
        try {
            nameToChar.remove(chr.getName().toLowerCase());
            idToChar.remove(chr.getId());
        } finally {
            cservLock.unlock();
        }
    }
    
    public final void deregisterPendingPlayer(final int charid) {
        transferLock.lock();
        try {
            pendingCharacter.remove(charid);
        } finally {
            transferLock.unlock();
        }
    }

    public final MapleCharacter getCharacterByName(final String name) {
        return nameToChar.get(name.toLowerCase());
    }

    public final MapleCharacter getCharacterById(final int id) {
        return idToChar.get(Integer.valueOf(id));
    }
    
    public final CharacterTransfer getPendingCharacter(final int charid) {
        final CharacterTransfer toreturn = pendingCharacter.get(charid);
        if (toreturn != null) {
            deregisterPendingPlayer(charid);
        }
        return toreturn;
    }

    public Collection<MapleCharacter> getAllCharacters() {
        return Collections.unmodifiableCollection(nameToChar.values());
    }

    public final void broadcastPacket(final MaplePacket data) {
        cservLock.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            while (itr.hasNext()) {
                itr.next().getClient().announce(data);
            }
        } finally {
            cservLock.unlock();
        }
    }

    public final void broadcastAnnouncementPacket(final MaplePacket data) {
        cservLock.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr != null && chr.getClient() != null && chr.getClient().isLoggedIn() && !chr.isDeaf()) {
                    chr.getClient().announce(data);
                }
            }
        } finally {
            cservLock.unlock();
        }
    }

    public final void broadcastGMPacket(final MaplePacket data) {
        cservLock.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getClient().isLoggedIn() && chr.isGM()) {
                    chr.getClient().announce(data);
                }
            }
        } finally {
            cservLock.unlock();
        }
    }

    public final void broadcastNONGMPacket(final MaplePacket data) {
        cservLock.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getClient().isLoggedIn() && !chr.isGM()) {
                    chr.getClient().announce(data);
                }
            }
        } finally {
            cservLock.unlock();
        }
    }

    public final boolean isCharacterConnected(final String name) {
        boolean connected = false;
        cservLock.lock();
        try {
            final Iterator<MapleCharacter> itr = idToChar.values().iterator();
            while (itr.hasNext()) {
                if (itr.next().getName().equals(name)) {
                    connected = true;
                    break;
                }
            }
        } finally {
            cservLock.unlock();
        }
        return connected;
    }

    public final void disconnectAll() {
        cservLock.lock();
        try {
            final List<MapleCharacter> dcList = new ArrayList();
            final Iterator<MapleCharacter> itr = idToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();
                chr.getClient().disconnect(false, true);
                dcList.add(chr);
                chr.getClient().getSession().close();
            }
            for (final MapleCharacter character : dcList) {
                this.deregisterPlayer(character);
            }
        } finally {
            cservLock.unlock();
        }
    }

    public class PersistingTask implements Runnable {
        @Override
        public void run() {
            transferLock.lock();
            try {
                final long currenttime = System.currentTimeMillis();
                final Iterator<Map.Entry<Integer, CharacterTransfer>> itr = pendingCharacter.entrySet().iterator();
                while (itr.hasNext()) {
                    if (currenttime - itr.next().getValue().transfertime > 40000) {
                        itr.remove();
                    }
                }
                TimerManager.getInstance().schedule(new PersistingTask(), 900000);
            } finally {
                transferLock.unlock();
            }
        }
    }
}

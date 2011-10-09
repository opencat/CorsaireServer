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

import server.shops.HiredMerchant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import client.MapleCharacter;
import java.util.LinkedHashMap;

/**
 * @name        HiredMerchantRegistry
 * @author      Simon
 *              Modified by x711Li
 */
public class HiredMerchantRegistry {
    private Map<Integer, HiredMerchant> registry;
    private Map<String, Integer> idLookup;
    private ReentrantReadWriteLock merchantLock = new ReentrantReadWriteLock();

    public HiredMerchantRegistry(int channel) {
        registry = new LinkedHashMap<Integer, HiredMerchant>();
        idLookup = new LinkedHashMap<String, Integer>();
    }

    public void registerMerchant(HiredMerchant h, MapleCharacter c) {
        merchantLock.writeLock().lock();
        try {
            idLookup.put(c.getName(), c.getId());
            registry.put(c.getId(), h);
        } finally {
            merchantLock.writeLock().unlock();
        }
    }

    public void deregisterMerchant(HiredMerchant h) {
        merchantLock.writeLock().lock();
        try {
            if (registry.containsValue(h)) {
                idLookup.remove(h.getOwner());
                registry.remove(h.getOwnerId());
            }
        } finally {
            merchantLock.writeLock().unlock();
        }
    }

    public HiredMerchant getMerchantForPlayer(String playerName) {
        merchantLock.readLock().lock();
        try {
            if (idLookup.containsKey(playerName)) {
                if (registry.containsKey(idLookup.get(playerName))) {
                    return registry.get(idLookup.get(playerName));
                }
            }
            return null;
        } finally {
            merchantLock.readLock().unlock();
        }
    }

    public HiredMerchant getMerchantForPlayer(int playerId) {
        merchantLock.readLock().lock();
        try {
            if (registry.containsKey(playerId)) {
                return registry.get(playerId);
            }
            return null;
        } finally {
            merchantLock.readLock().unlock();
        }
    }

    public void closeAndDeregisterAll() {
        merchantLock.writeLock().lock();
        try {
            for (HiredMerchant h : registry.values()) {
                h.closeShop();
            }
            registry.clear();
        } finally {
            merchantLock.writeLock().unlock();
        }
    }
}

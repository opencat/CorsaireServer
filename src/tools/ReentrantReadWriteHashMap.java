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

package tools;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @name        ReentrantReadWriteHashMap
 * @author      JVLaple
 */
@SuppressWarnings("element-type-mismatch")
public class ReentrantReadWriteHashMap<K, V>
extends java.util.HashMap<K, V> {
    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private Lock readLock = rwl.readLock(), writeLock = rwl.writeLock();

    public ReentrantReadWriteHashMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    public ReentrantReadWriteHashMap() {
    }

    public ReentrantReadWriteHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public ReentrantReadWriteHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            super.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        readLock.lock();
        try {
            return super.containsKey(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containsValue(Object value) {
        readLock.lock();
        try {
            return super.containsValue(value);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        readLock.lock();
        try {
            return super.entrySet();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public V get(Object key) {
        readLock.lock();
        try {
            return super.get(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        readLock.lock();
        try {
            return super.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<K> keySet() {
        readLock.lock();
        try {
            return super.keySet();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public V put(K key, V value) {
        writeLock.lock();
        try {
            return super.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        writeLock.lock();
        try {
            super.putAll(m);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V remove(Object key) {
        writeLock.lock();
        try {
            return super.remove(key);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int size() {
        readLock.lock();
        try {
            return super.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<V> values() {
        readLock.lock();
        try {
            return super.values();
        } finally {
            readLock.unlock();
        }
    }
}
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

import java.util.LinkedHashMap;
import java.util.ArrayList;
import tools.Pair;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @name        PropertiesTable
 * @author      Simon
 */
public class PropertiesTable {
    private LinkedHashMap<String, Object> PropertiesHashmap = new LinkedHashMap<String, Object>();
    private ReentrantReadWriteLock propsLock = new ReentrantReadWriteLock();
 
    public PropertiesTable() {
    }

    public PropertiesTable(Pair<String,Object>[] initialProps) {
        for(Pair<String, Object> p : initialProps) {
            this.setProperty(p.getLeft(), p.getRight());
        }
    }

    public ArrayList<String> getPropertyNames() {
        ArrayList<String> res = new ArrayList<String>();
        propsLock.readLock().lock();
        try {
            for(String s : this.PropertiesHashmap.keySet())
                res.add(s);
        } finally {
            propsLock.readLock().unlock();
        }
        return res;
    }

    public void setProperty(String propertyName, Object value) {
        propsLock.writeLock().lock();
        try {
            if(this.PropertiesHashmap.containsKey(propertyName)) {
                this.PropertiesHashmap.remove(propertyName);
            }
            this.PropertiesHashmap.put(propertyName, value);
        } finally {
            propsLock.writeLock().unlock();
        }
    }

    public Object getProperty(String propertyName) {
        propsLock.readLock().lock();
        try {
            if(this.PropertiesHashmap.containsKey(propertyName))
                return this.PropertiesHashmap.get(propertyName);
            return null;
        } finally {
            propsLock.readLock().unlock();
        }
    }
}

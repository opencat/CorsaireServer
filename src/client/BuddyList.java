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

package client;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.io.Serializable;
import tools.DatabaseConnection;
import tools.Pair;
import java.util.LinkedHashMap;
import tools.factory.BuddyFactory;

/**
 * @name        BuddyList
 * @author      Matze
 */
public class BuddyList implements Serializable {
    public enum BuddyOperation {
        ADDED, DELETED
    }

    public enum BuddyAddResult {
        BUDDYLIST_FULL, ALREADY_ON_LIST, OK
    }
    
    private static final long serialVersionUID = 1413738569L;
    private Map<Integer, BuddylistEntry> buddies = new LinkedHashMap<Integer, BuddylistEntry>();
    private int capacity;
    private Deque<CharacterNameAndId> pendingRequests = new LinkedList<CharacterNameAndId>();

    public BuddyList(int capacity) {
        this.capacity = capacity;
    }

    public boolean contains(int characterId) {
        return buddies.containsKey(Integer.valueOf(characterId));
    }

    public boolean containsVisible(int characterId) {
        BuddylistEntry ble = buddies.get(characterId);
        if (ble == null) {
            return false;
        }
        return ble.isVisible();
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public BuddylistEntry get(int characterId) {
        return buddies.get(Integer.valueOf(characterId));
    }

    public BuddylistEntry get(String characterName) {
        String lowerCaseName = characterName.toLowerCase();
        for (BuddylistEntry ble : buddies.values()) {
            if (ble.getName().toLowerCase().equals(lowerCaseName)) {
                return ble;
            }
        }
        return null;
    }

    public void put(BuddylistEntry entry) {
        buddies.put(Integer.valueOf(entry.getCharacterId()), entry);
    }

    public void remove(int characterId) {
        buddies.remove(Integer.valueOf(characterId));
    }

    public Collection<BuddylistEntry> getBuddies() {
        return buddies.values();
    }

    public boolean isFull() {
        return buddies.size() >= capacity;
    }

    public int[] getBuddyIds() {
        int buddyIds[] = new int[buddies.size()];
        int i = 0;
        for (BuddylistEntry ble : buddies.values()) {
            buddyIds[i++] = ble.getCharacterId();
        }
        return buddyIds;
    }

    public void loadFromDb(int characterId) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT b.buddyid, b.pending, b.group, c.name as buddyname FROM buddies as b, characters as c WHERE c.id = b.buddyid AND b.characterid = ?");
            ps.setInt(1, characterId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (!(rs.getInt("pending") == 1)) {
                    put(new BuddylistEntry(rs.getString("buddyname"), rs.getString("group"), rs.getInt("buddyid"), -1, true));
                }
            }
            rs.close();
            ps.close();
            ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM buddies WHERE pending = 1 AND characterid = ?");
            ps.setInt(1, characterId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void loadFromTransfer(final Map<Integer, Pair<String, Boolean>> data) {
        int buddyid;
        Pair pair;
        for (final Map.Entry<Integer, Pair<String, Boolean>> qs : data.entrySet()) {
            buddyid = qs.getKey();
            pair = qs.getValue();
            if (!((Boolean) pair.getRight())) {
                pendingRequests.push(new CharacterNameAndId(buddyid, (String) pair.getLeft()));
            } else {
                put(new BuddylistEntry((String) pair.getLeft(), "Default Group", buddyid, -1, true));
            }
        }
    }

    public CharacterNameAndId pollPendingRequest() {
        return pendingRequests.pollLast();
    }

    public void addBuddyRequest(MapleClient c, int cidFrom, String nameFrom, int channelFrom) {
        put(new BuddylistEntry(nameFrom, "Default Group", cidFrom, channelFrom, false));
        if (pendingRequests.isEmpty()) {
            c.announce(BuddyFactory.requestBuddylistAdd(cidFrom, c.getPlayer().getId(), nameFrom, 0, 0));
        } else {
            pendingRequests.push(new CharacterNameAndId(cidFrom, nameFrom));
        }
    }
}

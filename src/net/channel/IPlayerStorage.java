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

import java.util.Collection;
import client.MapleCharacter;
import net.world.CharacterTransfer;

/**
 * @name        IPlayerStorage
 * @author      Matze
 *              Modified by x711Li
 */
public interface IPlayerStorage {
    public MapleCharacter getCharacterByName(String name);
    public MapleCharacter getCharacterById(int id);
    Collection<MapleCharacter> getAllCharacters();
    public void registerPendingPlayer(final CharacterTransfer chr, final int playerid);
    public void deregisterPendingPlayer(final int charid);
    public CharacterTransfer getPendingCharacter(final int charid);
    public boolean isCharacterConnected(final String name);
    public void disconnectAll();
}

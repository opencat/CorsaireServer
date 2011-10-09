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

package net.world.remote;

import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

import net.world.CharacterTransfer;

/**
 * @name        WorldCashShopInterface
 * @author      Matze
 *              Modified by x711Li
 */
public interface WorldCashShopInterface extends Remote {
    public boolean isAvailable() throws RemoteException;
    public String getChannelIP(int channel) throws RemoteException;
    public boolean isCharacterListConnected(List<String> charName) throws RemoteException;
    public void channelChange(CharacterTransfer Data, int characterid, int toChannel) throws RemoteException;
}

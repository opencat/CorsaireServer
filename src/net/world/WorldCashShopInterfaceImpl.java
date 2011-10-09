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

package net.world;

import constants.ServerConstants;
import java.util.List;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import net.channel.remote.ChannelWorldInterface;
import net.world.guild.MapleGuildCharacter;
import net.world.remote.WorldCashShopInterface;

/**
 * @name        WorldCashShopInterfaceImpl
 * @author      Matze
 *              Modified by x711Li
 */
public class WorldCashShopInterfaceImpl extends UnicastRemoteObject implements WorldCashShopInterface {
    private static final long serialVersionUID = -4985323089596332908L;

    public WorldCashShopInterfaceImpl() throws RemoteException {
    super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

    public boolean isAvailable() throws RemoteException {
    return true;
    }

    public final String getChannelIP(int channel) throws RemoteException {
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                if (channel == i) {
                    final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                    try {
                        return cwi.getIP();
                    } catch (RemoteException e) {
                        //WorldRegistryImpl.getInstance().deregisterChannelServer(i);
                    }
                }
            }
        }
    return null;
    }

    public boolean isCharacterListConnected(List<String> charName) throws RemoteException {
        for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                try {
                    if (cwi.isCharacterListConnected(charName)) {
                        return true;
                    }
                } catch (RemoteException e) {
                    WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                }
            }
        }
    return false;
    }

    // Back to channel
    public void channelChange(CharacterTransfer data, int characterid, int toChannel) throws RemoteException {
    for (int j = 0; j < ServerConstants.NUM_WORLDS; j++) { // NUM_WORLDS
            for (int i : WorldRegistryImpl.getInstance().getChannelServer(j)) {
                if (i == toChannel) {
                    final ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, j);
                    try {
                        cwi.channelChange(data, characterid);
                    } catch (RemoteException e) {
                        WorldRegistryImpl.getInstance().deregisterChannelServer(i, j);
                    }
                }
            }
        }
    }
}

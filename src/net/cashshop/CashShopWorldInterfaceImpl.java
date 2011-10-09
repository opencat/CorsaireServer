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

package net.cashshop;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import net.cashshop.remote.CashShopWorldInterface;
import net.world.CharacterTransfer;

/**
 * @name        CashShopWorldInterfaceImpl
 * @author      Matze
 *              Modified by x711Li
 */
public class CashShopWorldInterfaceImpl extends UnicastRemoteObject implements CashShopWorldInterface {
    private static final long serialVersionUID = -3405666366539470037L;
    private CashShopServer cs;

    public CashShopWorldInterfaceImpl() throws RemoteException {
    super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

    public CashShopWorldInterfaceImpl(final CashShopServer cs) throws RemoteException {
    super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    this.cs = cs;
    }

    public void channelChange(CharacterTransfer transfer, int characterid) throws RemoteException {
    cs.getShopperStorage().registerPendingPlayer(transfer, characterid);
    }

    public final void shutdown() throws RemoteException {
    cs.shutdown();
    }
}
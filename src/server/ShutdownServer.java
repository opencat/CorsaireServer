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

import java.rmi.RemoteException;
import net.channel.ChannelServer;

/**
 * @name        ShutdownServer
 * @author      Frz
 */
public class ShutdownServer implements Runnable {
    private int myChannel;
    private int world;

    public ShutdownServer(int channel, int world) {
        myChannel = channel;
        this.world = world;
    }

    @Override
    public void run() {
        try {
            ChannelServer.getInstance(myChannel).shutdown();
        } catch (Exception t) {
            t.printStackTrace();
        }
        int c = 200;
        while (ChannelServer.getInstance(myChannel).getConnectedClients() > 0 && c > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            c--;
        }
        try {
            ChannelServer.getWorldRegistry().deregisterChannelServer(myChannel, world);
        } catch (RemoteException e) {
        }
        try {
            ChannelServer.getInstance(myChannel).unbind();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        boolean allShutdownFinished = true;
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            while (!cserv.hasFinishedShutdown()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.err.println("ERROR" + e);
                }
            }
        }
        if (allShutdownFinished) {
            TimerManager.getInstance().stop();
            System.exit(0);
        }
    }
}
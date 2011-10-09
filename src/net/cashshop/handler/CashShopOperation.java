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

package net.cashshop.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MaplePet;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import net.cashshop.CashShopServer;
import net.world.CharacterTransfer;
import net.world.remote.WorldCashShopInterface;
import server.CashItemFactory;
import server.CashItemInfo;
import server.MapleInventoryManipulator;
import tools.DatabaseConnection;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.factory.CashShopFactory;
import tools.factory.EffectFactory;
import tools.factory.InterServerFactory;
import tools.factory.IntraPersonalFactory;

/**
 * @name        CashShopOperation
 * @author      x711Li
 */
public class CashShopOperation {
    public static final void BuyHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.insideCashShop()) {
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        final int action = slea.readByte();
        if (action == 3) {
            slea.readByte();
            final int useNX = slea.readInt();
            final int snCS = slea.readInt();
            final CashItemInfo item = CashItemFactory.getInstance().getItem(snCS);
            if((int) (item.getId() / 10000) == 521 || (int) (item.getId() / 10000) == 536) {
                denyBuy(c);
                c.announce(IntraPersonalFactory.enableActions());
                return;
            }
            if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) (item.getId() / 1000000))).getNextFreeSlot() > -1) {
                if (item.getPrice() < 0) {
                    c.getPlayer().ban(c.getPlayer().getName() + " caught abusing Cash Shop glitch.", true);
                } else if (!item.getOnSale()) {
                    return;
                }
                if (c.getPlayer().getCSPoints(useNX) >= item.getPrice()) {
                    c.getPlayer().modifyCSPoints(useNX, -item.getPrice());
                } else {
                    c.getPlayer().ban(c.getPlayer().getName() + " caught buying without enough NX.", true);
                    c.announce(IntraPersonalFactory.enableActions());
                    return;
                }
                if (item.getId() >= 5000000 && item.getId() <= 5000100) {
                    final int petId = MaplePet.createPet(item.getId());
                    if (petId == -1) {
                        c.announce(IntraPersonalFactory.enableActions());
                        return;
                    }
                    MapleInventoryManipulator.addById(c, item.getId(), (short) 1, null, petId);
                } else {
                    MapleInventoryManipulator.addById(c, item.getId(), (short) item.getCount());
                }
                c.announce(CashShopFactory.showBoughtCSItem(c, item));
            } else {
                c.getPlayer().dropMessage(1, "Cash Inventory Full");
            }
        } else if (action == 4) { // Gifting, not GMS like without the cash inventories
            //if (checkBirthday(c, slea.readInt())) {
                final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
                String recipient = slea.readMapleAsciiString();
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                String message = slea.readMapleAsciiString();
                if (c.getPlayer().getCSPoints(1) < item.getPrice()) {
                    c.announce(IntraPersonalFactory.enableActions());
                    return;
                }
                if (victim != null) {
                    MapleInventoryManipulator.addById(victim.getClient(), item.getId(), (short) 1);
                    c.getPlayer().modifyCSPoints(4, -item.getPrice());
                    try {
                        victim.sendNote(victim.getName(), message);
                    } catch (SQLException s) {
                    }
                } else {
                    c.getPlayer().dropMessage(1, "Make sure the user you are gifting to is\r\n on the same channel.");
                }
            /*} else {
                c.getPlayer().dropMessage(1, "The birthday you entered was incorrect.");
            }*/
        } else if (action == 5) { // Modify wish list
            c.getPlayer().clearWishList();
            for (int i = 0; i < 10; i++) {
                final int sn = slea.readInt();
                if (sn != 0) {
                    c.getPlayer().addToWishList(sn);
                }
            }
            c.announce(CashShopFactory.sendWishList(c.getPlayer(), true));
        } else if (action == 7) {
            denyBuy(c);
            c.announce(IntraPersonalFactory.enableActions());
            return;
        } else if (action == 0x1C) { //crush ring (action 28)
            denyBuy(c);
            c.announce(IntraPersonalFactory.enableActions());
            return;
        } else if (action == 0x1D) { // Packages
            slea.readByte();
            int useNX = slea.readInt();
            int snCS = slea.readInt();
            CashItemInfo item = CashItemFactory.getInstance().getItem(snCS);
            if (c.getPlayer().getCSPoints(useNX) < item.getPrice()) {
                c.getPlayer().ban(c.getPlayer().getName() + " caught abusing Cash Shop glitch.", true);
                c.announce(IntraPersonalFactory.enableActions());
                return;
            }
            c.getPlayer().modifyCSPoints(useNX, -item.getPrice());
            for (int i : CashItemFactory.getInstance().getPackageItems(item.getId())) {
                i = CashItemFactory.getInstance().getItem(i).getId();
                if (i >= 5000000 && i <= 5000100) {
                    int petId = MaplePet.createPet(i);
                    if (petId == -1) {
                        c.announce(IntraPersonalFactory.enableActions());
                        return;
                    }
                    MapleInventoryManipulator.addById(c, i, (short) 1, null, petId);
                } else {
                    MapleInventoryManipulator.addById(c, i, (short) item.getCount());
                }
            }
            c.announce(CashShopFactory.showBoughtCSItem(c, item));
        } else if (action == 0x20) {
            CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            if (c.getPlayer().getMeso() > 0) {
                if (item.getOnSale() && item.getPrice() > 0) {
                    c.getPlayer().gainMeso(-item.getPrice(), false);
                    MapleInventoryManipulator.addById(c, item.getId(), (short) item.getCount());
                    c.announce(CashShopFactory.showBoughtCSQuestItem(item.getId()));
                }
            }
        } else if (action == 0x22) {
            denyBuy(c);
            c.announce(IntraPersonalFactory.enableActions());
            return;
        } else {
            System.out.println(slea);
            return;
        }
        showCS(c);
    }

    private static final void showCS(MapleClient c) {
        c.announce(CashShopFactory.showNXMapleTokens(c.getPlayer()));
        c.announce(CashShopFactory.enableCSUse0());
        c.announce(CashShopFactory.enableCSUse1());
        c.announce(CashShopFactory.enableCSUse2());
        c.announce(CashShopFactory.enableCSUse3());
        c.announce(IntraPersonalFactory.enableActions());
    }

    private static final void denyBuy(MapleClient c) {
        c.getPlayer().getClient().announce(EffectFactory.serverNotice(1, "This item has been disabled."));
        showCS(c);
    }

    public static final void CouponCodeHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(2);
        String code = slea.readMapleAsciiString();
        boolean validcode = false;
        int type = -1;
        int item = -1;
        validcode = getNXCodeValid(code.toUpperCase(), validcode);
        if (validcode) {
            type = getNXCode(code, "type");
            item = getNXCode(code, "item");
            if (type != 5) {
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("UPDATE nxcode SET `valid` = 0 WHERE code = " + code);
                    ps.executeUpdate();
                    ps.close();
                    ps = con.prepareStatement("UPDATE nxcode SET `user` = ? WHERE code = ?");
                    ps.setString(1, c.getPlayer().getName());
                    ps.setString(2, code);
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException e) {
                }
            }
            switch (type) {
                case 0:
                case 1:
                case 2:
                    c.getPlayer().modifyCSPoints(type, item);
                    break;
                case 3:
                    c.getPlayer().modifyCSPoints(0, item);
                    c.getPlayer().modifyCSPoints(2, (item / 5000));
                    break;
                case 4:
                    MapleInventoryManipulator.addById(c, item, (short) 1, null, -1);
                    c.announce(CashShopFactory.showCouponRedeemedItem(item));
                    break;
                case 5:
                    c.getPlayer().modifyCSPoints(0, item);
                    break;
            }
            c.announce(CashShopFactory.showNXMapleTokens(c.getPlayer()));
        } else {
            c.announce(CashShopFactory.wrongCouponCode());
        }
        c.announce(CashShopFactory.enableCSUse0());
        c.announce(CashShopFactory.enableCSUse1());
        c.announce(CashShopFactory.enableCSUse2());
    }

    private static final int getNXCode(String code, String type) {
        int item = -1;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `" + type + "` FROM nxcode WHERE code = ?");
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                item = rs.getInt(type);
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
        }
        return item;
    }

    private static final boolean getNXCodeValid(String code, boolean validcode) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `valid` FROM nxcode WHERE code = ?");
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                validcode = rs.getInt("valid") != 0;
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
        }
        return validcode;
    }

    public static final void ExitHandler(MapleClient c) {
        final CashShopServer cs = CashShopServer.getInstance();
        cs.getShopperStorage().deregisterPlayer(c.getPlayer());
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        try {
            final WorldCashShopInterface wci = cs.getCashShopInterface();
            wci.channelChange(new CharacterTransfer(c.getPlayer()), c.getPlayer().getId(), c.getChannel());
            final String ip = wci.getChannelIP(c.getChannel()); //temp fix
            final String[] socket = ip.split(":");
            try {
            c.announce(InterServerFactory.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
            } catch (Exception e) {
            }
            c.setInsideCashShop(false);
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        } finally {
            c.getPlayer().saveToDB(true);
            c.setPlayer(null);
        }
    }
}
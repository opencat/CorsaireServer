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

/**
 * @name        IItem
 * @author      Matze
 *              Modified by x711Li
 */
public interface IItem extends Comparable<IItem> {
    public final int ITEM = 2;
    public final int EQUIP = 1;
    public void setFlag(byte b);
    byte getFlag();
    byte getType();
    short getPosition();
    void setPosition(short position);
    int getId();
    short getQuantity();
    String getOwner();
    int getPetId();
    IItem copy();
    void setId(int id);
    void setOwner(String owner);
    void setQuantity(short quantity);
    long getExpiration();
    int getDBID();
    void setDBID(int dbid);
    void setPetId(int petid);
}

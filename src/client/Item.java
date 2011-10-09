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

import java.io.Serializable;

/**
 * @name        Item
 * @author      Matze
 *              Modified by x711Li
 */
public class Item implements IItem, Serializable {
    private int id;
    private short position;
    private short quantity;
    private int petid;
    private String owner = "";
    private byte flag;
    private long expiration = -1;
    private int DBID = -1;

    public Item(final int id, final short position, final short quantity) {
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.petid = -1;
        this.flag = 0;
    }

    public Item(int id, short position, short quantity, int petid) {
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.petid = petid;
        this.flag = 0;
    }

    public IItem copy() {
        final Item ret = new Item(id, position, quantity, petid);
        ret.owner = owner;
        return ret;
    }

    public final void setId(final int id) {
        this.id = id;
    }

    public final void setPosition(final short position) {
        this.position = position;
    }

    public void setQuantity(final short quantity) {
        this.quantity = quantity;
    }

    @Override
    public final int getId() {
        return id;
    }

    @Override
    public final short getPosition() {
        return position;
    }

    @Override
    public final short getQuantity() {
        return quantity;
    }

    @Override
    public byte getType() {
        return IItem.ITEM;
    }

    @Override
    public final String getOwner() {
        return owner;
    }

    public final void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public final int getPetId() {
        return petid;
    }

    @Override
    public int compareTo(final IItem other) {
        if (this.id < other.getId()) {
            return -1;
        } else if (this.id > other.getId()) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "Item: " + id + " quantity: " + quantity;
    }

    public byte getFlag() {
        return flag;
    }

    public void setFlag(byte b) {
        this.flag = b;
    }

    public final void setDBID(int dbid) {
        this.DBID = dbid;
    }

    public final void setPetId(int petid) {
        this.petid = petid;
    }
    
    public final int getDBID() {
        return this.DBID;
    }

    @Override
    public final long getExpiration() {
        return expiration;
    }

    public final void setExpiration(long expire) {
        this.expiration = expire;
    }
}


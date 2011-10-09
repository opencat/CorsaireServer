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

package net.channel.handler;

import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import client.MaplePet;
import client.PetCommand;
import client.PetDataFactory;
import client.SkillFactory;
import constants.ExpTable;
import java.awt.Point;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.factory.EffectFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.InventoryFactory;
import tools.factory.PetFactory;

/**
 * @name        PetOperation
 * @author      x711Li
 */
public class PetOperation {
    public static final void PetSpawnHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        byte slot = slea.readByte();
        slea.readByte();
        boolean lead = slea.readByte() == 1;
        MaplePet pet = MaplePet.loadFromDb(c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot).getId(), slot, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot).getPetId());
        if (pet != null) {
            if (c.getPlayer().getPetIndex(pet) != -1) {
                c.getPlayer().unequipPet(pet, true);
            } else {
                if (c.getPlayer().getSkillLevel(SkillFactory.getSkill(8)) == 0 && c.getPlayer().getPet(0) != null) {
                    c.getPlayer().unequipPet(c.getPlayer().getPet(0), false);
                }

                if (lead) {
                    c.getPlayer().shiftPetsRight();
                }

                Point pos = c.getPlayer().getPosition();
                pos.y -= 12;
                pet.setPos(pos);
                pet.setFh(c.getPlayer().getMap().getFootholds().findBelow(pet.getPos()).getId());
                pet.setStance(0);

                c.getPlayer().addPet(pet);
                c.getPlayer().getMap().broadcastMessage(PetFactory.showPet(c.getPlayer(), pet, false, false));
                c.announce(PetFactory.petStatUpdate(c.getPlayer()));
                c.announce(IntraPersonalFactory.enableActions());
            }
        }
    }

    public static final void PetChatHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int petId = slea.readInt();
        slea.readInt();
        slea.readByte();
        int act = slea.readByte();
        if (act > 0x15)
            return;
        String text = slea.readMapleAsciiString();
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PetFactory.petChat(c.getPlayer().getId(), c.getPlayer().getPetIndex(petId), act, text), true);
    }

    public static final void PetCommandHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int petId = slea.readInt();
        int petIndex = c.getPlayer().getPetIndex(petId);
        MaplePet pet = null;
        if (petIndex == -1) {
            return;
        } else {
            pet = c.getPlayer().getPet(petIndex);
        }
        slea.readInt();
        slea.readByte();
        byte command = slea.readByte();
        if(command > 0x15)
            return;
        PetCommand petCommand = PetDataFactory.getPetCommand(pet.getId(), (int) command);
        if (petCommand == null) {
            return;
        }
        boolean success = false;
        if (Randomizer.getInstance().nextInt(101) <= petCommand.getProbability()) {
            success = true;
            if (pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + petCommand.getIncrease();
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.announce(PetFactory.showOwnPetLevelUp(c.getPlayer().getPetIndex(pet)));
                    c.getPlayer().getMap().broadcastMessage(PetFactory.showPetLevelUp(c.getPlayer(), c.getPlayer().getPetIndex(pet)));
                }
                c.announce(InventoryFactory.updateSlot(pet));
            }
        }
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PetFactory.commandResponse(c.getPlayer().getId(), petIndex, command, success), true);
    }

    public static final void PetFoodHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int previousFullness = 100;
        int slot = 0;
        MaplePet[] pets = c.getPlayer().getPets();
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getFullness() < previousFullness) {
                    slot = i;
                    previousFullness = pets[i].getFullness();
                }
            }
        }
        MaplePet pet = c.getPlayer().getPet(slot);
        slea.readInt();
        slea.readShort();
        int Id = slea.readInt();
        if (Id % 1000000 == 2 && Id != 2120000) {
            return;
        }
        boolean gainCloseness = false;
        if (Randomizer.getInstance().nextInt(101) > 50) {
            gainCloseness = true;
        }
        if (pet.getFullness() < 100) {
            int newFullness = pet.getFullness() + 30;
            if (newFullness > 100) {
                newFullness = 100;
            }
            pet.setFullness(newFullness);
            if (gainCloseness && pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + 1;
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.announce(PetFactory.showOwnPetLevelUp(c.getPlayer().getPetIndex(pet)));
                    c.getPlayer().getMap().broadcastMessage(PetFactory.showPetLevelUp(c.getPlayer(), c.getPlayer().getPetIndex(pet)));
                }
            }
            c.announce(InventoryFactory.updateSlot(pet));
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PetFactory.commandResponse(c.getPlayer().getId(), slot, 1, true), true);
        } else {
            if (gainCloseness) {
                int newCloseness = pet.getCloseness() - 1;
                pet.setCloseness(newCloseness);
                if (newCloseness < ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                    pet.setLevel(pet.getLevel() - 1);
                }
            }
            c.announce(InventoryFactory.updateSlot(pet));
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PetFactory.commandResponse(c.getPlayer().getId(), slot, 1, false), true);
        }
        MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, Id, 1, true, false);
    }

    public static final void PetLootHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getNoPets() < 1 || c.getPlayer().getMap().getId() == 610030500 || //TODO DIP THIS
                c.getPlayer().getMap().getId() == 220080001 || c.getPlayer().getMap().getId() == 925100400 ||
                c.getPlayer().getMap().getId() == 240050101 || c.getPlayer().getMap().getId() == 240050102 ||
                c.getPlayer().getMap().getId() == 240050103 || c.getPlayer().getMap().getId() == 240050104) {
            return;
        }
        MaplePet pet = c.getPlayer().getPet(c.getPlayer().getPetIndex(slea.readInt()));
        slea.skip(13);
        int oid = slea.readInt();

        MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
        if (ob == null || pet == null) {
            c.announce(InventoryFactory.getInventoryFull());
            return;
        }
        if (ob instanceof MapleMapItem) {
            MapleMapItem mapitem = (MapleMapItem) ob;
            synchronized (mapitem) {
                if (mapitem.isPickedUp()) {
                    c.announce(InventoryFactory.getInventoryFull());
                    return;
                }
                if (mapitem.getMeso() > 0) {
                    if (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).findById(1812000) != null) {
                        c.getPlayer().gainMeso(mapitem.getMeso(), true, true, false);
                        c.getPlayer().getMap().broadcastMessage(EffectFactory.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, c.getPlayer().getPetIndex(pet)), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        mapitem.setPickedUp(false);
                        c.announce(IntraPersonalFactory.enableActions());
                        return;
                    }
                } else if (InventoryOperation.useItem(c, mapitem.getItem().getId())) {
                    if (mapitem.getItem().getId() / 10000 == 238) {
                        c.getPlayer().getMonsterBook().addCard(c, mapitem.getItem().getId());
                        c.getPlayer().setSaveBook();
                    }
                    mapitem.setPickedUp(true);
                    c.getPlayer().getMap().broadcastMessage(EffectFactory.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                    c.getPlayer().getMap().removeMapObject(ob);
                } else if (mapitem.getItem().getId() / 100 == 50000) {
                    if (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).findById(1812007) != null) {
                        for (int i : c.getPlayer().getExcluded()) {
                            if (mapitem.getItem().getId() == i) {
                                return;
                            }
                        }
                    } else if (MapleInventoryManipulator.addById(c, mapitem.getItem().getId(), mapitem.getItem().getQuantity(), null, -1)) {
                        c.getPlayer().getMap().broadcastMessage(EffectFactory.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, c.getPlayer().getPetIndex(pet)), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        return;
                    }
                } else if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
                    c.getPlayer().getMap().broadcastMessage(EffectFactory.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, c.getPlayer().getPetIndex(pet)), mapitem.getPosition());
                    c.getPlayer().getMap().removeMapObject(ob);
                } else {
                    return;
                }
                mapitem.setPickedUp(true);
            }
        }
        c.announce(IntraPersonalFactory.enableActions());
    }

    public static final void PetAutoPotHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        slea.readByte();
        slea.readLong();
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int Id = slea.readInt();
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0) {
            if (toUse.getId() != Id) {
                c.announce(IntraPersonalFactory.enableActions());
                return;
            }
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            MapleStatEffect stat = MapleItemInformationProvider.getInstance().getItemEffect(toUse.getId());
            stat.applyTo(c.getPlayer());
            if (stat.getMp() > 0) {
                c.announce(PetFactory.sendAutoMpPot(Id));
            }
            if (stat.getHp() > 0) {
                c.announce(PetFactory.sendAutoHpPot(Id));
            }
        }
    }

    public static final void PetExcludeItemHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readLong();
        byte amount = slea.readByte();
        for (int i = 0; i < amount; i++) {
            c.getPlayer().addExcluded(slea.readInt());
        }
    }
}
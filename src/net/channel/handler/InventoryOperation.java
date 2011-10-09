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

import client.Equip;
import client.IEquip;
import client.IEquip.ScrollResult;
import client.IItem;
import client.ISkill;
import client.Item;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleInventory;
import client.MapleInventoryType;
import client.MaplePet;
import client.MapleStat;
import client.SkillFactory;
import constants.ExpTable;
import constants.InventoryConstants;
import constants.ServerConstants;
import constants.skills.BlazeWizard;
import constants.skills.Brawler;
import constants.skills.Crusader;
import constants.skills.DawnWarrior;
import constants.skills.Magician;
import constants.skills.Swordsman;
import constants.skills.ThunderBreaker;
import constants.skills.WhiteKnight;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.channel.ChannelServer;
import net.world.MaplePartyCharacter;
import scripting.npc.NPCScriptManager;
import server.MakerItemFactory;
import server.MakerItemFactory.GemCreateEntry;
import server.MakerItemFactory.ItemMakerCreateEntry;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.shops.MaplePlayerShopItem;
import server.shops.MapleShopFactory;
import server.MapleStorage;
import server.MapleTreasure;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.FieldLimit;
import server.shops.HiredMerchant;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleTVEffect;
import tools.DataTool;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.factory.EffectFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.InventoryFactory;
import tools.factory.MarketFactory;
import tools.factory.PetFactory;

/**
 * @name        InventoryOperation
 * @author      x711Li
 */
public class InventoryOperation {
    public static final void ItemSortHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt(); // timestamp
        byte mode = slea.readByte();
        boolean sorted = false;
        MapleInventoryType pInvType = MapleInventoryType.getByType(mode);
        MapleInventory pInv = c.getPlayer().getInventory(pInvType);
        if (pInv.isFull()) {
            c.getPlayer().dropMessage(1, "You cannot use Item Sort with a full inventory.");
            return;
        } else {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            for (byte i = 1; i <= (c.getPlayer().gmLevel() == 0 ? 48 : 96); i++) {
                final IItem source = pInv.getItem(i);
                if (source != null && source.getQuantity() > ii.getSlotMax(c, source.getId())) {
                    c.getPlayer().dropMessage(1, "Unstack " + ii.getName(source.getId()) + ".");
                    return;
                }
            }
        }
        while (!sorted) {
            short freeSlot = pInv.getNextFreeSlot();
            if (freeSlot != -1) {
                short itemSlot = -1;
                for (short i = (short) (freeSlot + 1); i <= 100; i++) {
                    if (pInv.getItem(i) != null) {
                        itemSlot = i;
                        break;
                    }
                }
                if (itemSlot <= 100 && itemSlot > 0) {
                    MapleInventoryManipulator.move(c, pInvType, itemSlot, freeSlot);
                } else {
                    sorted = true;
                }
            }
        }
        c.announce(InventoryFactory.finishedSort(mode));
        c.announce(IntraPersonalFactory.enableActions());
    }

    public static final void ItemSortByIdHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt(); // timestamp
        byte mode = slea.readByte();
        if (mode < 0 || mode > 5) {
            return;
        }
        MapleInventory Inv = c.getPlayer().getInventory(MapleInventoryType.getByType(mode));
        ArrayList<Item> itemarray = new ArrayList<Item>();
        for (Iterator<IItem> it = Inv.iterator(); it.hasNext();) {
            Item item = (Item) it.next();
            IItem hm = item.copy();
            if (hm.getPetId() > 0) {
                hm.setPetId(item.getPetId());
            }
            itemarray.add((Item) hm);
        }
        Collections.sort(itemarray);
        for (IItem item : itemarray) {
            MapleInventoryManipulator.removeById(c, MapleInventoryType.getByType(mode), item.getId(), item.getQuantity(), false, false);
        }
        for (Item i : itemarray) {
            if (i.getId() > 5000000) {
                MapleInventoryManipulator.addById(c, i.getId(), i.getQuantity(), null, i.getPetId());
            } else {
                MapleInventoryManipulator.addFromDrop(c, i, false, false);
            }
        }
        c.announce(InventoryFactory.finishedSort2(mode));
    }

    public static final void ItemMoveHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(4); //?
        MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
        short src = slea.readShort();
        short action = slea.readShort();
        short quantity = slea.readShort();
        if (src < 0 && action > 0) {
            MapleInventoryManipulator.unequip(c, src, action);
        } else if (action < 0) {
            MapleInventoryManipulator.equip(c, src, action);
        } else if (action == 0) {
            MapleInventoryManipulator.drop(c, type, src, quantity);
        } else {
            MapleInventoryManipulator.move(c, type, src, action);
        }
    }

    public static final void MesoDropHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        slea.skip(4);
        int meso = slea.readInt();
        if (meso <= c.getPlayer().getMeso() && meso > 9 && meso < 50001) {
            c.getPlayer().gainMeso(-meso, false, true, false);
            c.getPlayer().getMap().spawnMesoDrop(meso, meso, c.getPlayer().getPosition(), c.getPlayer().getObjectId(), c.getPlayer().getPosition(), c.getPlayer(), false);
        }
    }

    public static final void useSkillBook(MapleClient c, IItem toUse) {
        Map<String, Integer> skilldata = MapleItemInformationProvider.getInstance().getSkillStats(toUse.getId(), c.getPlayer().getJob());
        boolean canuse = false;
        boolean success = false;
        int skill = 0;
        int maxlevel = 0;
        if (skilldata == null || c.getPlayer().getMasterLevel(SkillFactory.getSkill(skilldata.get("skillid"))) >= skilldata.get("masterLevel")) {
            return;
        }
        if (skilldata.get("skillid") == 0) {
            canuse = false;
        } else if (c.getPlayer().getMasterLevel(SkillFactory.getSkill(skilldata.get("skillid"))) >= skilldata.get("reqSkillLevel") || skilldata.get("reqSkillLevel") == 0) {
            canuse = true;
            if (Randomizer.getInstance().nextInt(101) <= skilldata.get("success") && skilldata.get("success") != 0) {
                success = true;
                ISkill skill2 = SkillFactory.getSkill(skilldata.get("skillid"));
                c.getPlayer().changeSkillLevel(skill2, c.getPlayer().getSkillLevel(skill2) + (toUse.getId() < 5620000 ? 0 : 1), Math.max(skilldata.get("masterLevel"), c.getPlayer().getMasterLevel(skill2)));
                c.getPlayer().dropMessage("The skill book lights up, and you feel a mysterious force streaming through you.");
            } else {
                success = false;
                c.getPlayer().dropMessage("The skill book lights up, but the skill winds up as if nothing happened.");
            }
            if (toUse.getId() < 5620000) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, toUse.getPosition(), (short) 1, false);
            }
        } else {
            canuse = false;
        }
        c.announce(EffectFactory.skillBookSuccess(c.getPlayer(), skill, maxlevel, canuse, success));
    }

    public static final void useTreasure(MapleClient c, IItem item) {
        int totalProb = 0;
        for (MapleTreasure treasure : MapleItemInformationProvider.getInstance().getTreasureReward(item.getId())) {
            totalProb += treasure.getProb();
        }
        int chance = Randomizer.getInstance().nextInt(totalProb) + 1; // HMM SO
        int probCounter = 0;
        for (MapleTreasure treasure : MapleItemInformationProvider.getInstance().getTreasureReward(item.getId())) {
            probCounter += treasure.getProb();
            if (chance >= totalProb - probCounter) {
                MapleInventoryManipulator.addById(c, treasure.getId(), (short) treasure.getCount());
                c.announce(EffectFactory.sendOpenTreasure(treasure.getId()));
                c.announce(IntraPersonalFactory.enableActions());
                return;
            }
        }
        c.announce(IntraPersonalFactory.enableActions());
    }

    public static final void UseCashItemHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        slea.skip(2);
        int itemId = slea.readInt();
        int itemType = itemId / 10000;
        IItem toUse = player.getInventory(MapleInventoryType.CASH).getItem(player.getInventory(MapleInventoryType.CASH).findById(itemId).getPosition());
        if (toUse == null || toUse.getId() != itemId || toUse.getQuantity() < 1 || player.gmLevel() == 1 || player.gmLevel() == 2) {
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        String medal = "";
        IItem medalItem = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -49);
        if (medalItem != null) {
            medal = "<" + ii.getName(medalItem.getId()) + "> ";
        }
        try {
            if (itemType == 505) { // AP/SP reset
                if (itemId > 5050000) {
                    int skill1 = slea.readInt();
                    int skill2 = slea.readInt();

                    ISkill skillSPTo = SkillFactory.getSkill(skill1);
                    ISkill skillSPFrom = SkillFactory.getSkill(skill2);

                    if (skillSPTo.isBeginnerSkill() || skillSPFrom.isBeginnerSkill()) {
                        c.announce(IntraPersonalFactory.enableActions());
                        return;
                    }
                    if ((c.getPlayer().getSkillLevel(skillSPTo) + 1 <= skillSPTo.getMaxLevel()) && (c.getPlayer().getSkillLevel(skillSPTo) + 1 <= skillSPTo.getMasterLevel()) && c.getPlayer().getSkillLevel(skillSPFrom) > 0) {
                        c.getPlayer().changeSkillLevel(skillSPFrom, (byte) (c.getPlayer().getSkillLevel(skillSPFrom) - 1), c.getPlayer().getMasterLevel(skillSPFrom));
                        c.getPlayer().changeSkillLevel(skillSPTo, (byte) (c.getPlayer().getSkillLevel(skillSPTo) + 1), c.getPlayer().getMasterLevel(skillSPTo));
                    }
                } else {

                    int job = c.getPlayer().getJob();
                    int jobtype = job / 100 % 10;
                    List<Pair<MapleStat, Integer>> statupdate = new ArrayList<Pair<MapleStat, Integer>>(2);
                    int APTo = slea.readInt();
                    int APFrom = slea.readInt();
                    ISkill improvingMaxHP;
                    ISkill improvingMaxMP;
                    switch (APFrom) {
                    case 64: // str
                        if (c.getPlayer().getStr() <= 4) {
                            return;
                        }
                        c.getPlayer().addStat(1, -1);
                        break;
                    case 128: // dex
                        if (c.getPlayer().getDex() <= 4) {
                            return;
                        }
                        c.getPlayer().addStat(2, -1);
                        break;
                    case 256: // int
                        if (c.getPlayer().getInt() <= 4) {
                            return;
                        }
                        c.getPlayer().addStat(3, -1);
                        break;
                    case 512: // luk
                        if (c.getPlayer().getLuk() <= 4) {
                            return;
                        }
                        c.getPlayer().addStat(4, -1);
                        break;
                    case 2048: // HP
                        int maxHP = c.getPlayer().getMaxHp();
                        if (jobtype == 0) {
                            maxHP -= 14;
                        } else if (jobtype == 1) {
                            if (c.getPlayer().isAran()) {
                                maxHP -= 51;
                            } else {
                                improvingMaxHP = c.getPlayer().isCygnus() ? SkillFactory.getSkill(DawnWarrior.MAX_HP_INCREASE) : SkillFactory.getSkill(Swordsman.IMPROVED_MAX_HP_INCREASE);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                if (improvingMaxHPLevel > 0) {
                                    maxHP -= improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
                                }
                                maxHP -= 26;
                            }
                        } else if (jobtype == 2) {
                            maxHP -= 12;
                        } else if (jobtype <= 4) {
                            maxHP -= 22;
                        } else if (jobtype == 5) {
                            improvingMaxHP = c.getPlayer().isCygnus() ? SkillFactory.getSkill(ThunderBreaker.IMPROVE_MAX_HP) : SkillFactory.getSkill(Brawler.IMPROVE_MAX_HP);
                            int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                            if (improvingMaxHPLevel > 0) {
                                maxHP -= improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
                            }
                            maxHP -= 22;
                        }
                        c.getPlayer().setHpApUsed(c.getPlayer().getHpApUsed() - 1);
                        c.getPlayer().setMaxHp(maxHP);
                        c.getPlayer().setHp(maxHP);
                        statupdate.add(new Pair<MapleStat, Integer>(MapleStat.HP, c.getPlayer().getMaxHp()));
                        statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, c.getPlayer().getMaxHp()));
                        break;
                    case 8192: // MP
                        int maxMP = c.getPlayer().getMaxMp();
                        if (jobtype == 0) {
                            maxMP -= 11;
                        } else if (jobtype == 1) {
                            improvingMaxMP = c.getPlayer().isCygnus() ? SkillFactory.getSkill(DawnWarrior.INCREASED_MP_RECOVERY) : job == Crusader.ID ? SkillFactory.getSkill(Crusader.IMPROVING_MP_RECOVERY) : SkillFactory.getSkill(WhiteKnight.IMPROVING_MP_RECOVERY);
                            int improvingMaxMPLevel = c.getPlayer().getSkillLevel(improvingMaxMP);
                            if (improvingMaxMPLevel > 0) {
                                maxMP -= improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
                            }
                            maxMP -= 5;
                        } else if (jobtype == 2) {
                            improvingMaxMP = c.getPlayer().isCygnus() ? SkillFactory.getSkill(BlazeWizard.INCREASING_MAX_MP) : SkillFactory.getSkill(Magician.IMPROVED_MAX_MP_INCREASE);
                            int improvingMaxMPLevel = c.getPlayer().getSkillLevel(improvingMaxMP);
                            if (improvingMaxMPLevel > 0) {
                                maxMP -= improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
                            }
                            maxMP -= 23;
                        } else if (jobtype <= 5) {
                            maxMP -= 15;
                        }
                        c.getPlayer().setMpApUsed(c.getPlayer().getMpApUsed() - 1);
                        c.getPlayer().setMaxMp(maxMP);
                        c.getPlayer().setMp(maxMP);
                        statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MP, c.getPlayer().getMaxMp()));
                        statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, c.getPlayer().getMaxMp()));
                        break;
                    default:
                        c.announce(IntraPersonalFactory.updatePlayerStats(DataTool.EMPTY_STATUPDATE, true));
                        return;
                    }
                    switch (APTo) {
                    case 64: // str
                        if (c.getPlayer().getStr() >= 999) {
                            return;
                        }
                        c.getPlayer().addStat(1, 1);
                        break;
                    case 128: // dex
                        if (c.getPlayer().getDex() >= 999) {
                            return;
                        }
                        c.getPlayer().addStat(2, 1);
                        break;
                    case 256: // int
                        if (c.getPlayer().getInt() >= 999) {
                            return;
                        }
                        c.getPlayer().addStat(3, 1);
                        break;
                    case 512: // luk
                        if (c.getPlayer().getLuk() >= 999) {
                            return;
                        }
                        c.getPlayer().addStat(4, 1);
                        break;
                    case 2048: // hp
                        int maxHP = c.getPlayer().getMaxHp();
                        if (c.getPlayer().getHpApUsed() == 10000 || maxHP >= 30000) {
                            c.announce(IntraPersonalFactory.updatePlayerStats(DataTool.EMPTY_STATUPDATE, true));
                            return;
                        }
                        if (jobtype == 0) {
                            maxHP += 8;
                        } else if (jobtype == 1) {
                            maxHP += 20;
                        } else if (jobtype == 2) {
                            maxHP += 6;
                        } else if (jobtype <= 4) {
                            maxHP += 16;
                        } else if (jobtype == 5) {
                            maxHP += 18;
                        }
                        maxHP = Math.min(30000, maxHP);
                        c.getPlayer().setHpApUsed(c.getPlayer().getHpApUsed() + 1);
                        c.getPlayer().setMaxHp(maxHP);
                        statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, c.getPlayer().getMaxHp()));
                        break;
                    case 8192: // mp
                        int maxMP = c.getPlayer().getMaxHp();
                        if (c.getPlayer().getMpApUsed() == 10000 || maxMP >= 30000) {
                            c.announce(IntraPersonalFactory.updatePlayerStats(DataTool.EMPTY_STATUPDATE, true));
                            c.announce(IntraPersonalFactory.enableActions());
                            return;
                        }
                        if (jobtype == 0) {
                            maxMP += 6;
                        } else if (jobtype == 1) {
                            maxMP += 2;
                        } else if (jobtype == 2) {
                            maxMP += 18;
                        } else if (jobtype <= 4) {
                            maxMP += 10;
                        } else if (jobtype == 5) {
                            maxMP += 14;
                        }
                        maxMP = Math.min(30000, maxMP);
                        c.getPlayer().setMpApUsed(c.getPlayer().getMpApUsed() + 1);
                        c.getPlayer().setMaxMp(maxMP);
                        statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, c.getPlayer().getMaxMp()));
                        break;
                    default:
                        c.announce(IntraPersonalFactory.updatePlayerStats(DataTool.EMPTY_STATUPDATE, true));
                        c.announce(IntraPersonalFactory.enableActions());
                        return;
                    }
                    c.announce(IntraPersonalFactory.updatePlayerStats(statupdate, true));
                }
                remove(c, itemId);
            } else if (itemType == 506) {
                int tagType = itemId % 10;
                IEquip eq;
                if (tagType == 0) { // Item Tag
                    int equipSlot = slea.readShort();
                    if (equipSlot == 0) {
                        return;
                    }
                    eq = (IEquip) player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) equipSlot);
                    eq.setOwner(player.getName());
                    c.announce(InventoryFactory.updateSlot(eq));
                    remove(c, itemId);
                } else if (tagType == 1) { // Sealing Lock
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    IItem item = player.getInventory(type).getItem((byte) slea.readInt());
                    if (item == null) {
                        return;
                    }
                    byte flag = item.getFlag();
                    flag |= InventoryConstants.LOCK;
                    item.setFlag(flag);
                    c.announce(InventoryFactory.updateSlot(item));
                    remove(c, itemId);
                } else if (tagType == 2) { // Incubator
                    byte inventory2 = (byte) slea.readInt();
                    byte slot2 = (byte) slea.readInt();
                    IItem item2 = player.getInventory(MapleInventoryType.getByType(inventory2)).getItem(slot2);
                    if (item2 == null) {
                        return;
                    }
                    NPCScriptManager.getInstance().start(c, 9010000, item2.getId());
                    remove(c, itemId);
                    return;
                } else if (tagType == 3) { // Peanut Machine
                    IItem peanut = player.getInventory(MapleInventoryType.ETC).findById(4170023);
                    if (peanut == null || peanut.getQuantity() <= 0) {
                        return;
                    }
                    NPCScriptManager.getInstance().start(c, 9010000, 4170023);
                    remove(c, itemId);
                }
                //slea.readInt(); // time stamp
            } else if (itemType == 507) {
                boolean whisper;
                long curTime = Calendar.getInstance().getTimeInMillis();
                if(c.getPlayer().getMegaLimit() > curTime) {
                    c.getPlayer().dropMessage("You may not use your megaphone yet. Please wait " + Math.floor((c.getPlayer().getMegaLimit() - curTime) / 1000) + " seconds.");
                    c.announce(IntraPersonalFactory.enableActions());
                    return;
                } else {
                    Calendar futureCal = Calendar.getInstance();
                    futureCal.set(Calendar.SECOND, Calendar.getInstance().get(Calendar.SECOND) + 30);
                    c.getPlayer().setMegaLimit(futureCal.getTimeInMillis());
                }
                switch (itemId / 1000 % 10) {
                case 1: // Megaphone
                    //player.dropMessage(1, "Megaphones are currently disabled. Please use Super Megaphones.");
                    if(System.currentTimeMillis() - c.getPlayer().getMuted() <= 90000)
                        c.getPlayer().dropMessage("You have been muted. You must wait " +
                        ((90000 - (System.currentTimeMillis() - c.getPlayer().getMuted())) / 1000) + " seconds before talking again.");
                    else {
                        c.getChannelServer().broadcastAnnouncementPacket(EffectFactory.serverNotice(3, c.getChannel() - c.getPlayer().getWorld() * ServerConstants.NUM_CHANNELS, medal + player.getName() + " : " + slea.readMapleAsciiString(), (slea.readByte() != 0)));
                    }
                    break;
                case 2: // Super megaphone
                    if(System.currentTimeMillis() - c.getPlayer().getMuted() <= 90000)
                        c.getPlayer().dropMessage("You have been muted. You must wait " +
                        ((90000 - (System.currentTimeMillis() - c.getPlayer().getMuted())) / 1000) + " seconds before talking again.");
                    else {
                        c.getChannelServer().getWorldInterface().broadcastAnnouncement(EffectFactory.serverNotice(3, c.getChannel() - c.getPlayer().getWorld() * ServerConstants.NUM_CHANNELS, medal + player.getName() + " : " + slea.readMapleAsciiString(), (slea.readByte() != 0)).getBytes());
                    }
                    break;
                case 5: // Maple TV TODO FIX
                    int tvType = itemId % 10;
                    boolean megassenger = false;
                    boolean ear = false;
                    MapleCharacter victim = null;
                    if (tvType != 1) {
                        if (tvType >= 3) {
                            megassenger = true;
                            if (tvType == 3) {
                                slea.readByte();
                            }
                            ear = 1 == slea.readByte();
                        } else if (tvType != 2) {
                            slea.readByte();
                        }
                        if (tvType != 4) {
                            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
                        }
                    }
                    List<String> messages = new LinkedList<String>();
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < 5; i++) {
                        String message = slea.readMapleAsciiString();
                        if (megassenger) {
                            builder.append(" " + message);
                        }
                        messages.add(message);
                    }
                    slea.readInt();
                    if (megassenger) {
                        c.getChannelServer().getWorldInterface().broadcastAnnouncement(EffectFactory.serverNotice(3, c.getChannel() - c.getPlayer().getWorld() * ServerConstants.NUM_CHANNELS, medal + player.getName() + " : " + builder.toString(), ear).getBytes());
                    }
                    if (!MapleTVEffect.isActive()) {
                        new MapleTVEffect(player, victim, messages, tvType);
                        remove(c, itemId);
                    } else {
                        player.dropMessage(1, "MapleTV is already in use.");
                        c.announce(IntraPersonalFactory.enableActions());
                        return;
                    }
                    break;
                case 6: //item megaphone
                    if(System.currentTimeMillis() - c.getPlayer().getMuted() <= 90000)
                        c.getPlayer().dropMessage("You have been muted. You must wait " +
                        ((90000 - (System.currentTimeMillis() - c.getPlayer().getMuted())) / 1000) + " seconds before talking again.");
                    else {
                        String msg = medal + player.getName() + " : " + slea.readMapleAsciiString();
                        whisper = slea.readByte() == 1;
                        IItem item = null;
                        if (slea.readByte() == 1) { //item
                            item = player.getInventory(MapleInventoryType.getByType((byte) slea.readInt())).getItem((byte) slea.readInt());
                            if (item == null) {
                                return;
                            } else if (ii.isDropRestricted(item.getId())) {
                                player.dropMessage(1, "You cannot trade this item.");
                                c.announce(IntraPersonalFactory.enableActions());
                                return;
                            }
                        }
                        c.getChannelServer().getWorldInterface().broadcastAnnouncement(EffectFactory.itemMegaphone(msg, whisper, c.getChannel() - c.getPlayer().getWorld() * ServerConstants.NUM_CHANNELS, item).getBytes());
                    }
                    break;
                case 7: //triple megaphone
                    int lines = slea.readByte();
                    if (lines < 1 || lines > 3) {
                        return;
                    }
                    String[] msg2 = new String[lines];
                    for (int i = 0; i < lines; i++) {
                        msg2[i] = medal + player.getName() + " : " + slea.readMapleAsciiString();
                    }
                    whisper = slea.readByte() == 1;
                    c.getChannelServer().getWorldInterface().broadcastAnnouncement(EffectFactory.getMultiMegaphone(msg2, c.getChannel() - c.getPlayer().getWorld() * ServerConstants.NUM_CHANNELS, whisper).getBytes());
                    break;
                }
                remove(c, itemId);
            } else if (itemType == 508) { // graduation banner
                slea.readMapleAsciiString();
                c.announce(IntraPersonalFactory.enableActions());
            } else if (itemType == 509) {
                String sendTo = slea.readMapleAsciiString();
                String msg = slea.readMapleAsciiString();
                try {
                    player.sendNote(sendTo, msg);
                } catch (SQLException e) {
                }
                remove(c, itemId);
            } else if (itemType == 510) {
                player.getMap().broadcastMessage(EffectFactory.musicChange("Jukebox/Congratulation"));
                remove(c, itemId);
            } else if (itemType == 512) {
                if (ii.getStateChangeItem(itemId) != 0) {
                    for (MapleCharacter mChar : player.getMap().getCharacters()) {
                        ii.getItemEffect(ii.getStateChangeItem(itemId)).applyTo(mChar);
                    }
                }
                player.getMap().startMapEffect(ii.getMsg(itemId).replaceFirst("%s", player.getName()).replaceFirst("%s", slea.readMapleAsciiString()), itemId);
                remove(c, itemId);
            } else if (itemType == 517) {
                MaplePet pet = player.getPet(0);
                String newName = slea.readMapleAsciiString();
                if (pet == null || newName.length() > 13 || newName.length() < 0) {
                    c.announce(IntraPersonalFactory.enableActions());
                    return;
                }
                pet.setName(newName);
                c.announce(InventoryFactory.updateSlot(pet));
                c.announce(IntraPersonalFactory.enableActions());
                player.getMap().broadcastMessage(player, PetFactory.changePetName(player, newName, 1), true);
                remove(c, itemId);
            } else if (itemType == 504) {
                // vip teleport rock
                String error1 = "Either the player could not be found or you were trying to teleport to an illegal location.";
                byte rocktype = slea.readByte();
                remove(c, itemId);
                c.announce(IntraPersonalFactory.refreshTeleportRockMapList(player, rocktype));
                if (rocktype == 0) {
                    int mapId = slea.readInt();
                    MapleMap map = c.getChannelServer().getMapFactory().getMap(mapId);
                    if (/*map.getForcedReturnId() == 999999999 && */FieldLimit.CANNOTVIPROCK.check(mapId)) {
                        player.changeMap(c.getChannelServer().getMapFactory().getMap(mapId));
                    } else {
                        MapleInventoryManipulator.addById(c, itemId, (short) 1);
                        player.dropMessage(1, error1);
                        c.announce(IntraPersonalFactory.enableActions());
                    }
                } else {
                    String name = slea.readMapleAsciiString();
                    MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                    boolean success = false;
                    if (victim != null) {
                        MapleMap target = victim.getMap();
                        if (c.getChannelServer().getMapFactory().getMap(c.getChannelServer().getWorldInterface().getLocation(name).map).getForcedReturnId() == 999999999 || victim.getMapId() < 100000000) {
                            if (!victim.isGM()) {
                                if (itemId == 5041000 || victim.getMapId() / player.getMapId() == 1) { //viprock & same continent
                                    player.changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
                                    success = true;
                                } else {
                                    player.dropMessage(1, error1);
                                }
                            } else {
                                player.dropMessage(1, error1);
                            }
                        } else {
                            player.dropMessage(1, "You cannot teleport to this map.");
                        }
                    } else {
                        player.dropMessage(1, "Player could not be found in this channel.");
                    }
                    if (!success) {
                        MapleInventoryManipulator.addById(c, itemId, (short) 1);
                        c.announce(IntraPersonalFactory.enableActions());
                    }
                }
            } else if (itemType == 520) {
                player.gainMeso(ii.getMeso(itemId), true, false, true);
                remove(c, itemId);
                c.announce(IntraPersonalFactory.enableActions());
            } else if (itemType == 523) { // Owl of Minerva
                int searchItemId = slea.readInt();
                List<MaplePlayerShopItem> itemList = new LinkedList<MaplePlayerShopItem>();
                List<HiredMerchant> list = new LinkedList<HiredMerchant>();
                for (int i = 910000001; i < 910000023; i++) {
                    MapleMap map = c.getChannelServer().getMapFactory().getMap(i);
                    for (HiredMerchant hm : map.getHiredMerchants()) {
                        for (MaplePlayerShopItem mpsi : hm.getItems()) {
                            if (mpsi.getItem().getId() == searchItemId && mpsi.isExist()) {
                                list.add(hm);
                                itemList.add(mpsi);
                            }
                        }
                    }
                }
                c.announce(MarketFactory.sendMinerva(list, itemList, searchItemId));
                remove(c, itemId);
            } else if (itemType == 524) {
                for (int i = 0; i < 3; i++) {
                    MaplePet pet = player.getPet(i);
                    if (pet != null) {
                        if (pet.canConsume(itemId)) {
                            pet.setFullness(100);
                            if (pet.getCloseness() + 100 > 30000) {
                                pet.setCloseness(30000);
                            } else {
                                pet.gainCloseness(100);
                            }
                            while (pet.getCloseness() >= ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                                pet.setLevel(pet.getLevel() + 1);
                                c.announce(PetFactory.showOwnPetLevelUp(player.getPetIndex(pet)));
                                player.getMap().broadcastMessage(PetFactory.showPetLevelUp(player, player.getPetIndex(pet)));
                            }
                            c.announce(InventoryFactory.updateSlot(pet));
                            player.getMap().broadcastMessage(player, PetFactory.commandResponse(player.getId(), 0, 1, true), true);
                            remove(c, itemId);
                            break;
                        }
                    } else {
                        break;
                    }
                }
            } else if (itemType == 530) {
                ii.getItemEffect(itemId).applyTo(player);
                remove(c, itemId);
            } else if (itemType == 533) {
                NPCScriptManager.getInstance().start(c, 9010009);
            } else if (itemType == 537) {
                player.setChalkboard(slea.readMapleAsciiString());
                player.getMap().broadcastMessage(EffectFactory.useChalkboard(player, false));
                player.getClient().announce(IntraPersonalFactory.enableActions());
            } else if (itemType == 539) {
                if(System.currentTimeMillis() - c.getPlayer().getMuted() <= 90000)
                    c.getPlayer().dropMessage("You have been muted. You must wait " +
                    ((90000 - (System.currentTimeMillis() - c.getPlayer().getMuted())) / 1000) + " seconds before talking again.");
                else {
                    List<String> lines = new LinkedList<String>();
                    for (int i = 0; i < 4; i++) {
                        lines.add(slea.readMapleAsciiString());
                    }
                    c.getChannelServer().getWorldInterface().broadcastMessage(null, EffectFactory.getAvatarMega(player, "", c.getChannel(), itemId, lines, (slea.readByte() != 0)).getBytes());
                    if (itemId == 5390006) {
                        c.getChannelServer().getWorldInterface().broadcastMessage(null, EffectFactory.trembleEffect(1, 15).getBytes());
                        c.getChannelServer().getWorldInterface().broadcastMessage(null, EffectFactory.removeTiger().getBytes());
                    }
                    remove(c, itemId);
                }
            } else if (itemType == 545) {// MiuMiu's travel store
                c.announce(IntraPersonalFactory.enableActions());
                MapleShopFactory.getInstance().getShop(9090000).sendShop(c);
                remove(c, itemId);
            } else if (itemType == 547) {// Store Remote Controller
                c.announce(IntraPersonalFactory.enableActions());
                HiredMerchant hm = c.getChannelServer().getHMRegistry().getMerchantForPlayer(c.getPlayer().getId());
                if (hm == null) {
                    c.getPlayer().dropMessage(1, "You do not have a merchant opened on your current channel.");
                } else {
                    c.announce(MarketFactory.getHiredMerchant(c.getPlayer(), hm, false));
                    c.announce(IntraPersonalFactory.enableActions());
                    remove(c, itemId);
                }
            } else if (itemType == 552) {
                MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                byte slot = (byte) slea.readInt();
                IItem item = player.getInventory(type).getItem(slot);
                if (item == null || item.getQuantity() <= 0 || (item.getFlag() & InventoryConstants.KARMA) > 0 && ii.isKarmaAble(item.getId())) {
                    c.announce(IntraPersonalFactory.enableActions());
                    return;
                }
                item.setFlag((byte) InventoryConstants.KARMA);
                c.announce(InventoryFactory.clearInventoryItem(type, item.getPosition(), false));
                c.announce(InventoryFactory.addInventorySlot(type, item, false));
                remove(c, itemId);
                c.announce(IntraPersonalFactory.enableActions());
            } else if (itemType == 553) {
                useTreasure(c, toUse);
                remove(c, itemId);
            } else if (itemType == 557) {
                slea.readInt();
                int itemSlot = slea.readInt();
                slea.readInt();
                final IEquip equip = (IEquip) player.getInventory(MapleInventoryType.EQUIP).getItem((byte) itemSlot);
                if (equip.getVicious() == 2 || player.getInventory(MapleInventoryType.CASH).findById(5570000) == null) {
                    return;
                }
                equip.setVicious((short) (equip.getVicious() + 1));
                equip.setUpgradeSlots((byte) (equip.getUpgradeSlots() + 1));
                remove(c, itemId);
                c.announce(EffectFactory.sendHammerData(equip.getVicious()));
                c.announce(EffectFactory.hammerItem(equip));
                c.announce(IntraPersonalFactory.enableActions()); // TODO TIME THIS
            } else if (itemType == 561) { // Vega Scrolls
                System.out.println("VEGA: " + itemType + "\n" + slea.toString());
            } else {
                System.out.println("NEW CASH ITEM: " + itemType + "\n" + slea.toString());
                c.announce(IntraPersonalFactory.enableActions());
            }
        } catch (RemoteException re) {
            c.getChannelServer().reconnectWorld();
        }
    }

    public static final void UseItemHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getId() == itemId) {
            if (itemId == 2022178 || itemId == 2022433 || itemId == 2050004) {
                c.getPlayer().dispelDebuffs();
                remove(c, slot);
                return;
            } else if (itemId == 2050000) {
                c.getPlayer().dispelDebuff(MapleDisease.POISON);
                remove(c, slot);
                return;
            } else if (itemId == 2050001) {
                c.getPlayer().dispelDebuff(MapleDisease.DARKNESS);
                remove(c, slot);
                return;
            } else if (itemId == 2050002) {
                c.getPlayer().dispelDebuff(MapleDisease.WEAKEN);
                remove(c, slot);
                return;
            } else if (itemId == 2050003) {
                c.getPlayer().dispelDebuff(MapleDisease.SEAL);
                c.getPlayer().dispelDebuff(MapleDisease.CURSE);
                remove(c, slot);
                return;
            } else if (isTownScroll(itemId)) {
                if (ii.getItemEffect(toUse.getId()).applyTo(c.getPlayer())) {
                    remove(c, slot);
                }
                return;
            } else if (itemId >= 2022266 && itemId <= 2022267) {
                NPCScriptManager.getInstance().start(c, 9000059 + itemId - 2022266);
                return;
            }

            if (isTownScroll(itemId)) {
                if (ii.getItemEffect(toUse.getId()).applyTo(c.getPlayer())) {
                    remove(c, slot);
                }
                c.announce(IntraPersonalFactory.enableActions());
                return;
            }
            remove(c, slot);
            ii.getItemEffect(toUse.getId()).applyTo(c.getPlayer());
            c.getPlayer().checkBerserk();
        }
    }

    private static final void remove(MapleClient c, int itemId) {
        MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, itemId, 1, true, false);
    }

    private static final void remove(MapleClient c, byte slot) {
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        c.announce(IntraPersonalFactory.enableActions());
    }

    private static final boolean isTownScroll(int itemId) {
        return itemId >= 2030000 && itemId < 2030021;
    }

    public static final void UseScrollHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if(c.getPlayer().gmLevel() == 1 || c.getPlayer().gmLevel() == 2) {
            return;
        }
        slea.readInt();
        byte slot = (byte) slea.readShort();
        byte dst = (byte) slea.readShort();
        byte ws = (byte) slea.readShort();
        int notice = 0;
        boolean whiteScroll = false;
        boolean legendarySpirit = false;
        if ((ws & 2) == 2) {
            whiteScroll = true;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IEquip toScroll = (IEquip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        ISkill LegendarySpirit = SkillFactory.getSkill(1003);
        if (c.getPlayer().getSkillLevel(LegendarySpirit) > 0 && dst >= 0) {
            legendarySpirit = true;
            toScroll = (IEquip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        byte oldLevel = toScroll.getLevel();
        byte oldSlots = toScroll.getUpgradeSlots();
        MapleInventory useInventory = c.getPlayer().getInventory(MapleInventoryType.USE);
        IItem scroll = useInventory.getItem(slot);
        if (ii.isException(scroll.getId()) && toScroll.getUpgradeSlots() < 1) {
            c.announce(InventoryFactory.getInventoryFull());
            return;
        }
        IItem wscroll = null;
        List<Integer> scrollReqs = ii.getScrollReqs(scroll.getId());
        if (scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getId())) {
            c.announce(InventoryFactory.getInventoryFull());
            return;
        }
        if (whiteScroll) {
            wscroll = useInventory.findById(2340000);
            if (wscroll == null || wscroll.getId() != 2340000) {
                whiteScroll = false;
            } else if (c.getPlayer().getItemQuantity(2340000, false) > 2) {
                notice = 2340000;
            }
        }
        if (scroll.getQuantity() < 1 || scroll.getId() / 100 != 20491 && ii.isException(scroll.getId()) && !((scroll.getId() / 100) % 100 == (toScroll.getId() / 10000) % 100)) { //TODO COLD PROTECTION/SPIKE EXPLOIT!
            return;
        } else if (scroll.getId() == 2049100 && c.getPlayer().getItemQuantity(2049100, false) > 2) {
            notice = 2049100;
        }
        IEquip scrolled = (IEquip) ii.scrollEquipWithId(toScroll, scroll.getId(), whiteScroll);
        ScrollResult scrollSuccess = IEquip.ScrollResult.FAIL;
        if (scrolled == null) {
            scrollSuccess = IEquip.ScrollResult.CURSE;
        } else if (scrolled.getLevel() > oldLevel || (ii.isCleanSlate(scroll.getId()) && scrolled.getUpgradeSlots() == oldSlots + (scroll.getId() > 2049005 ? 2 : 1))) {
            scrollSuccess = IEquip.ScrollResult.SUCCESS;
        }
        useInventory.removeItem(scroll.getPosition(), (short) 1, false);
        if (whiteScroll) {
            useInventory.removeItem(wscroll.getPosition(), (short) 1, false);
            if (wscroll.getQuantity() < 1) {
                c.announce(InventoryFactory.clearInventoryItem(MapleInventoryType.USE, wscroll.getPosition(), false));
            } else {
                c.announce(InventoryFactory.updateInventorySlot(MapleInventoryType.USE, (Item) wscroll));
            }
        }
        if (scrollSuccess == IEquip.ScrollResult.CURSE) {
            c.announce(InventoryFactory.scrolledItem(scroll, toScroll, true));
            if (dst < 0) {
                c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else {
            c.announce(InventoryFactory.scrolledItem(scroll, scrolled, false));
        }
        c.getPlayer().getMap().broadcastMessage(InventoryFactory.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit));
        if (scrollSuccess == IEquip.ScrollResult.SUCCESS || scrollSuccess == IEquip.ScrollResult.CURSE) {
            if (dst < 0) {
                c.getPlayer().equipChanged();
            } else {
                c.announce(InventoryFactory.updateSlot(toScroll));
            }
        }
        if (notice > 0) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.broadcastGMPacket
                (EffectFactory.serverNotice(6, MapleCharacter.makeMapleReadable
                (c.getPlayer().getName()) + " used a " + ii.getName(notice) + ". " + c.getPlayer().getItemQuantity(notice, false) + " left."));
            }
        }
        c.announce(IntraPersonalFactory.enableActions());
    }

    public static final void UseSummonBagHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive() || c.getPlayer().gmLevel() == 1 || c.getPlayer().gmLevel() == 2) {
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getId() == itemId) {
            if (System.currentTimeMillis() - c.getPlayer().latestUse > 100) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                final List<Pair<Integer, Integer>> toSpawn = MapleItemInformationProvider.getInstance().getSummonMobs(itemId);
                if (toSpawn == null) {
                    c.getSession().write(IntraPersonalFactory.enableActions());
                    return;
                }
                MapleMonster ht;
                for (int i = 0; i < toSpawn.size(); i++) {
                    if (Randomizer.nextInt(99) <= toSpawn.get(i).getRight()) {
                        ht = MapleLifeFactory.getInstance().getMonster(toSpawn.get(i).getLeft());
                        c.getPlayer().getMap().spawnMonsterOnGroudBelow(ht, c.getPlayer().getPosition());
                    }
                }
            } else {
                c.getPlayer().ban("Autoban '" + c.getPlayer().getName() + "': Packet Editing (Spamming summoningbags)", true);
                c.getPlayer().getMap().killAllMonsters();
            }
            c.announce(IntraPersonalFactory.enableActions());
        }
    }

    private static final int AMBIGUOUS_ITEMS[] = {4032096, 4032101, 4032120, 4032125 };
    private static final int HEROIC_ITEMS[] = { 4031343, 4031511, 4031514, 4031517, 4031860 };

    public static final void ItemPickUpHandler(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        if(c.getPlayer().gmLevel() == 1 || c.getPlayer().gmLevel() == 2) {
            return;
        }
        slea.skip(9);
        int oid = slea.readInt();
        MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
        if (c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(ob.getObjectId())).getNextFreeSlot() > -1) {
            if (c.getPlayer().getMapId() > 209000000 && c.getPlayer().getMapId() < 209000016) {//happyville trees
                MapleMapItem mapitem = (MapleMapItem) ob;
                if (mapitem.getDropperId() == c.getPlayer().getObjectId()) {
                    if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), false)) {
                        c.getPlayer().getMap().broadcastMessage(EffectFactory.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        return;
                    }
                    mapitem.setPickedUp(true);
                } else {
                    c.announce(InventoryFactory.getInventoryFull());
                    c.announce(InventoryFactory.getShowInventoryFull());
                    return;
                }
                return;
            }
            if (ob == null) {
                c.announce(InventoryFactory.getInventoryFull());
                c.announce(InventoryFactory.getShowInventoryFull());
                return;
            }
            if (ob instanceof MapleMapItem) {
                final MapleMapItem mapitem = (MapleMapItem) ob;
                synchronized (mapitem) {
                    if (mapitem.isPickedUp()) {
                        c.announce(InventoryFactory.getInventoryFull());
                        c.announce(InventoryFactory.getShowInventoryFull());
                        return;
                    }
                    if (mapitem.getItem() != null) {
                        if (mapitem.getItem().getId() == 4031343 || mapitem.getItem().getId() == 4031344) {
                            mapitem.getItem().setId(mapitem.getItem().getId() - 4031343 + HEROIC_ITEMS[c.getPlayer().getJob() % 1000 / 100 - 1]);
                        } else if (c.getPlayer().getMap().getId() >= 108000600 && c.getPlayer().getMap().getId() <= 108000602 ||
                                c.getPlayer().getMap().getId() >= 108010600 && c.getPlayer().getMap().getId() <= 108010640 ||
                                c.getPlayer().getMap().getId() >= 913010000 && c.getPlayer().getMap().getId() <= 913020300) {
                            for (int i = 0; i < AMBIGUOUS_ITEMS.length; i++) {
                                if (mapitem.getItem().getId() == AMBIGUOUS_ITEMS[i]) {
                                    mapitem.getItem().setId(mapitem.getItem().getId() + c.getPlayer().getJob() % 1000 / 100 - 1);
                                    break;
                                }
                            }
                        }
                    }
                    if (mapitem.getMeso() > 0) {
                        if (c.getPlayer().getParty() != null) {
                            int mesosamm = mapitem.getMeso();
                            if (mesosamm > 50000 * ServerConstants.MESO_RATE) {
                                c.announce(IntraPersonalFactory.enableActions());
                                return;
                            }
                            int partynum = 0;
                            for (MaplePartyCharacter partymem : c.getPlayer().getParty().getMembers()) {
                                if (partymem.isOnline() && partymem.getMapid() == c.getPlayer().getMap().getId() && partymem.getChannel() == c.getChannel()) {
                                    partynum++;
                                }
                            }
                            for (MaplePartyCharacter partymem : c.getPlayer().getParty().getMembers()) {
                                if (partymem.isOnline() && partymem.getMapid() == c.getPlayer().getMap().getId()) {
                                    MapleCharacter somecharacter = c.getChannelServer().getPlayerStorage().getCharacterById(partymem.getId());
                                    if (somecharacter != null) {
                                        somecharacter.gainMeso(mesosamm / partynum, true, true, false);
                                    }
                                }
                            }
                        } else {
                            c.getPlayer().gainMeso(mapitem.getMeso(), true, true, false);
                        }
                        c.getPlayer().getMap().broadcastMessage(EffectFactory.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else if (useItem(c, mapitem.getItem().getId())) {
                        if (mapitem.getItem().getId() / 10000 == 238) {
                            c.getPlayer().getMonsterBook().addCard(c, mapitem.getItem().getId());
                            c.announce(EffectFactory.showSpecialEffect((byte) 14));
                            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectFactory.showForeignEffect(c.getPlayer().getId(), 14), false);
                            c.getPlayer().setSaveBook();
                        }
                        mapitem.setPickedUp(true);
                        c.getPlayer().getMap().broadcastMessage(EffectFactory.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
                        c.getPlayer().getMap().broadcastMessage(EffectFactory.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        c.announce(IntraPersonalFactory.enableActions());
                        return;
                    }
                    mapitem.setPickedUp(true);
                }
            }
            c.announce(IntraPersonalFactory.enableActions());
        }
    }

    public static final boolean useItem(final MapleClient c, final int id) {
        if (id / 1000000 == 2) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final byte consumeval = ii.isConsumeOnPickup(id);
            if (consumeval > 0) {
                if (consumeval == 2) {
                    if (c.getPlayer().getParty() != null) {
                        for (final MaplePartyCharacter pc : c.getPlayer().getParty().getMembers()) {
                            final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(pc.getId());
                            if (chr != null) {
                                ii.getItemEffect(id).applyTo(chr);
                            }
                        }
                    } else {
                        ii.getItemEffect(id).applyTo(c.getPlayer());
                    }
                } else {
                    ii.getItemEffect(id).applyTo(c.getPlayer());
                }
                c.announce(EffectFactory.getShowItemGain(id, (byte) 1));
                return true;
            }
        }
        return false;
    }

    public static final void StorageHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if(c.getPlayer().gmLevel() == 1 || c.getPlayer().gmLevel() == 2) {
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte mode = slea.readByte();
        final MapleStorage storage = c.getPlayer().getStorage();
        if (mode == 4) { // take out
            final byte slot = storage.getSlot(MapleInventoryType.getByType(slea.readByte()), slea.readByte());
            final IItem item = storage.takeOut(slot);
            if (item != null) {
                if (ii.getOnly(item.getId()) == 1 && c.getPlayer().haveItem(item.getId())) {
                    storage.store(item);
                    c.getPlayer().dropMessage(1, "You cannot withdraw a One-of-a-Kind Item with one already in possession.");
                } else if (MapleInventoryManipulator.checkSpace(c, item.getId(), item.getQuantity(), item.getOwner())) {
                    MapleInventoryManipulator.addFromDrop(c, item, false);
                    c.getPlayer().setSaveStorage();
                } else {
                    storage.store(item);
                    c.getPlayer().dropMessage(1, "Your inventory is full.");
                }
                storage.sendTakenOut(c, ii.getInventoryType(item.getId()));
            }
        } else if (mode == 5) { // store
            final byte slot = (byte) slea.readShort();
            final int itemId = slea.readInt();
            short quantity = slea.readShort();
            final MapleInventory mi = c.getPlayer().getInventory(MapleInventoryType.getByType((byte) (itemId / 1000000)));
            if (quantity < 1 || mi.getItem(slot).getQuantity() < quantity || mi.getItem(slot).getId() != itemId) {
                if (!(quantity == 1 && InventoryConstants.isRechargable(itemId))) {
                    c.getPlayer().ban(c.getPlayer().getName() + " caught duping through storage.", true);
                    return;
                }
            }
            if (storage.isFull()) {
                c.announce(InventoryFactory.getStorageFull());
                return;
            }
            if (c.getPlayer().getMeso() < 100) {
                c.getPlayer().dropMessage(1, "You don't have enough mesos to store the item.");
            } else {
                MapleInventoryType type = ii.getInventoryType(itemId);
                IItem item = c.getPlayer().getInventory(type).getItem(slot).copy();
                if (item != null && item.getId() == itemId && (item.getQuantity() >= quantity || InventoryConstants.isRechargable(itemId))) {
                    if (InventoryConstants.isRechargable(itemId)) {
                        quantity = item.getQuantity();
                    }
                    c.getPlayer().gainMeso(c.getPlayer().getMap().getId() == 910000000 ? -500 : -100, false, true, false);
                    MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                    item.setQuantity(quantity);
                    storage.store(item);
                    c.getPlayer().setSaveStorage();
                } else {
                    c.getPlayer().ban(c.getPlayer().getName() + " caught duping through storage.", true);
                    return;
                }
            }
            storage.sendStored(c, ii.getInventoryType(itemId));
        } else if (mode == 7) { // meso
            int meso = slea.readInt();
            final int storageMesos = storage.getMeso();
            final int playerMesos = c.getPlayer().getMeso();
            if (playerMesos < 0) {
                return;
            }
            if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                if (meso < 0 && (storageMesos - meso) < 0) {
                    meso = -2147483648 + storageMesos;
                    if (meso < playerMesos) {
                        return;
                    }
                } else if (meso > 0 && (playerMesos + meso) < 0) {
                    meso = 2147483647 - playerMesos;
                    if (meso > storageMesos) {
                        return;
                    }
                }
                storage.setMeso(storageMesos - meso);
                c.getPlayer().gainMeso(meso, true);
                storage.sendMeso(c);
                c.getPlayer().setSaveStorage();
            }
        } else if (mode == 8) {// close
            storage.close();
        }
    }

    public static final void UseItemEffectHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int itemId = slea.readInt();
        if (c.getPlayer().getInventory((itemId == 4290001 || itemId == 4290000) ? MapleInventoryType.ETC : MapleInventoryType.CASH).findById(itemId) == null) {
            return;
        }
        c.getPlayer().setItemEffect(itemId);
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectFactory.itemEffect(c.getPlayer().getId(), itemId), false);
    }

    public static final void UseSkillBookHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() >= 1) {
            if (toUse.getId() != itemId) {
                return;
            }
            useSkillBook(c, toUse);
        }
    }

    public static final void UseMountFoodHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(6);
        if (c.getPlayer().getInventory(MapleInventoryType.USE).findById(2260000) != null && slea.readInt() == 2260000) {
            if (c.getPlayer().getMount() != null && c.getPlayer().getMount().getTiredness() > 0) {
                c.getPlayer().getMount().setTiredness(Math.max(c.getPlayer().getMount().getTiredness() - 30, 0));
                c.getPlayer().getMount().setExp((2 * c.getPlayer().getMount().getLevel() + 6) * Math.min(3, c.getPlayer().getMount().getTiredness() / 10) / 4 + c.getPlayer().getMount().getExp());
                int level = c.getPlayer().getMount().getLevel();
                boolean levelup = c.getPlayer().getMount().getExp() >= ExpTable.getMountExpNeededForLevel(level) && level < 31;
                if (levelup) {
                    c.getPlayer().getMount().setLevel(level + 1);
                }
                c.getPlayer().getMap().broadcastMessage(IntraPersonalFactory.updateMount(c.getPlayer().getId(), c.getPlayer().getMount(), levelup));
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, 2260000, 1, true, false);
            }
        }
    }

    public static final void MakerHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        final int makerType = slea.readInt();
        switch (makerType) {
        case 1: { // Gem/ETC
                final int toCreate = slea.readInt();
                if (toCreate >= 4250000 && toCreate <= 4251402) {
                    final GemCreateEntry gem = MakerItemFactory.getInstance().getGemInfo(toCreate);
                    if (!hasSkill(c, gem.getReqSkillLevel())) {
                        return; // H4x
                    }
                    if (c.getPlayer().getMeso() < gem.getCost()) {
                        return; // H4x
                    }
                    final int randGemGiven = getRandomGem(gem.getRandomReward());
                    if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) (randGemGiven / 1000000))).isFull()) {
                        return; // We'll do handling for this later
                    }
                    final int taken = check(c, gem.getReqRecipes());
                    if (taken == 0) {
                        return; // We'll do handling for this later
                    }
                    c.getPlayer().gainMeso(-gem.getCost(), false);
                    MapleInventoryManipulator.addById(c, randGemGiven, (byte) (taken == randGemGiven ? 9 : 1)); // Gem is always 1
                    c.announce(EffectFactory.showSpecialEffect((byte) 18));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectFactory.showForeignEffect(c.getPlayer().getId(), 18), false);
                } else {
                    final ItemMakerCreateEntry create = MakerItemFactory.getInstance().getCreateInfo(toCreate);
                    if (!hasSkill(c, create.getReqSkillLevel())) {
                        return; // H4x
                    }
                    if (c.getPlayer().getMeso() < create.getCost()) {
                        return; // H4x
                    }
                    if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) (toCreate / 1000000))).isFull()) {
                        return; // We'll do handling for this later
                    }
                    if (check(c, create.getReqItems()) == 0) {
                        return; // We'll do handling for this later
                    }
                    c.getPlayer().gainMeso(-create.getCost(), false);
                    if (toCreate < 4000000) {
                        final boolean stimulator = slea.readByte() > 0;
                        final int numEnchanter = slea.readInt();
                        if (numEnchanter > create.getTUC()) {
                            return; // h4x
                        }
                        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        final Equip toGive = (Equip) ii.getEquipById(toCreate);
                        if (stimulator || numEnchanter > 0) {
                            if (c.getPlayer().haveItem(create.getStimulator(), 1)) {
                                ii.randomizeStats(toGive);
                                MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, create.getStimulator(), 1, false, false);
                            }
                            for (int i = 0; i < numEnchanter; i++) {
                                final int enchant = slea.readInt();
                                if (c.getPlayer().haveItem(enchant, 1)) {
                                    final Map<String, Byte> stats = ii.getItemMakeStats(enchant);
                                    if (stats != null) {
                                        addEnchantStats(stats, toGive);
                                        MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, enchant, 1, false, false);
                                    }
                                }
                            }
                        }
                        MapleInventoryManipulator.addFromDrop(c, toGive);
                    } else {
                        MapleInventoryManipulator.addById(c, toCreate, (short) 1);
                    }
                    c.announce(EffectFactory.showSpecialEffect((byte) 18));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectFactory.showForeignEffect(c.getPlayer().getId(), 18), false);
                }
                break;
            }
        case 3: { // Making Crystals
                final int etc = slea.readInt();
                if (c.getPlayer().haveItem(etc, 100)) {
                    MapleInventoryManipulator.addById(c, getCreateCrystal(etc), (short) 1);
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, etc, 100, false, false);
                    c.announce(EffectFactory.showSpecialEffect((byte) 18));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectFactory.showForeignEffect(c.getPlayer().getId(), 18), false);
                }
                break;
            }
        case 4: { // Disassembling EQ.
                final int itemId = slea.readInt();
                slea.skip(4);
                final byte slot = (byte) slea.readInt();

                final IItem toUse = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(slot);
                if (toUse == null || toUse.getId() != itemId || toUse.getQuantity() < 1) {
                    return;
                }
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

                if (!ii.isDropRestricted(itemId)) {
                    final int[] toGive = getCrystal(itemId, ii.getReqLevel(itemId));
                    MapleInventoryManipulator.addById(c, toGive[0], (byte) toGive[1]);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIP, slot, (byte) 1, false);
                }
                c.announce(EffectFactory.showSpecialEffect((byte) 18));
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectFactory.showForeignEffect(c.getPlayer().getId(), 18), false);
                break;
            }
        }
    }

    private static final int getCreateCrystal(final int etc) {
        int itemid;
        final short level = MapleItemInformationProvider.getInstance().getItemMakeLevel(etc);

        if (level >= 31 && level <= 50) {
            itemid = 4260000;
        } else if (level >= 51 && level <= 60) {
            itemid = 4260001;
        } else if (level >= 61 && level <= 70) {
            itemid = 4260002;
        } else if (level >= 71 && level <= 80) {
            itemid = 4260003;
        } else if (level >= 81 && level <= 90) {
            itemid = 4260004;
        } else if (level >= 91 && level <= 100) {
            itemid = 4260005;
        } else if (level >= 101 && level <= 110) {
            itemid = 4260006;
        } else if (level >= 111 && level <= 120) {
            itemid = 4260007;
        } else if (level >= 121) {
            itemid = 4260008;
        } else {
            throw new RuntimeException("Invalid Item Maker id");
        }
        return itemid;
    }

    private static final int[] getCrystal(final int itemid, final int level) {
        int[] all = new int[2];
        all[0] = -1;
        if (level >= 31 && level <= 50) {
            all[0] = 4260000;
        } else if (level >= 51 && level <= 60) {
            all[0] = 4260001;
        } else if (level >= 61 && level <= 70) {
            all[0] = 4260002;
        } else if (level >= 71 && level <= 80) {
            all[0] = 4260003;
        } else if (level >= 81 && level <= 90) {
            all[0] = 4260004;
        } else if (level >= 91 && level <= 100) {
            all[0] = 4260005;
        } else if (level >= 101 && level <= 110) {
            all[0] = 4260006;
        } else if (level >= 111 && level <= 120) {
            all[0] = 4260007;
        } else if (level >= 121 && level <= 200) {
            all[0] = 4260008;
        } else {
            throw new RuntimeException("Invalid Item Maker type" + level);
        }
        if ((itemid >= 1302000 && itemid < 1492024) || (itemid >= 1050000 && itemid < 1060000)) {
            all[1] = Randomizer.rand(5, 11);
        } else {
            all[1] = Randomizer.rand(3, 7);
        }
        return all;
    }

    private static final void addEnchantStats(final Map<String, Byte> stats, final Equip item) {
        short s = stats.get("incPAD");
        if (s != 0) {
            item.setWatk((short) (item.getWatk() + s));
        }
        s = stats.get("incMAD");
        if (s != 0) {
            item.setMatk((short) (item.getMatk() + s));
        }
        s = stats.get("incACC");
        if (s != 0) {
            item.setAcc((short) (item.getAcc() + s));
        }
        s = stats.get("incEVA");
        if (s != 0) {
            item.setAvoid((short) (item.getAvoid() + s));
        }
        s = stats.get("incSpeed");
        if (s != 0) {
            item.setSpeed((short) (item.getSpeed() + s));
        }
        s = stats.get("incJump");
        if (s != 0) {
            item.setJump((short) (item.getJump() + s));
        }
        s = stats.get("incMaxHP");
        if (s != 0) {
            item.setHp((short) (item.getHp() + s));
        }
        s = stats.get("incMaxMP");
        if (s != 0) {
            item.setMp((short) (item.getMp() + s));
        }
        s = stats.get("incSTR");
        if (s != 0) {
            item.setStr((short) (item.getStr() + s));
        }
        s = stats.get("incDEX");
        if (s != 0) {
            item.setDex((short) (item.getDex() + s));
        }
        s = stats.get("incINT");
        if (s != 0) {
            item.setInt((short) (item.getInt() + s));
        }
        s = stats.get("incLUK");
        if (s != 0) {
            item.setLuk((short) (item.getLuk() + s));
        }
        s = stats.get("randOption");
        if (s > 0) {
            final boolean success = Randomizer.nextBoolean();
            final int ma = item.getMatk(), wa = item.getWatk();
            if (wa > 0) {
                item.setWatk((short) (success ? (wa + s) : (wa - s)));
            }
            if (ma > 0) {
                item.setMatk((short) (success ? (ma + s) : (ma - s)));
            }
        }
        s = stats.get("randStat");
        if (s > 0) {
            final boolean success = Randomizer.nextBoolean();
            final int str = item.getStr(), dex = item.getDex(), luk = item.getLuk(), int_ = item.getInt();
            if (str > 0) {
                item.setStr((short) (success ? (str + s) : (str - s)));
            }
            if (dex > 0) {
                item.setDex((short) (success ? (dex + s) : (dex - s)));
            }
            if (int_ > 0) {
                item.setInt((short) (success ? (int_ + s) : (int_ - s)));
            }
            if (luk > 0) {
                item.setLuk((short) (success ? (luk + s) : (luk - s)));
            }
        }
    }

    private static final int getRandomGem(final List<Pair<Integer, Integer>> rewards) {
        int itemid;
        final List<Integer> items = new ArrayList<Integer>();

        for (final Pair p : rewards) {
            itemid = (Integer) p.getLeft();
            for (int i = 0; i < (Integer) p.getRight(); i++) {
                items.add(itemid);
            }
        }
        return items.get(Randomizer.nextInt(items.size()));
    }


    private static final int check(final MapleClient c, final List<Pair<Integer, Integer>> recipe) {
        int itemid = 0, count;
        for (final Pair p : recipe) {
            itemid = (Integer) p.getLeft();
            count = (Integer) p.getRight();

            if (!c.getPlayer().haveItem(itemid, count)) {
                return 0;
            }
        }
        for (final Pair p : recipe) {
            itemid = (Integer) p.getLeft();
            count = (Integer) p.getRight();

            MapleInventoryManipulator.removeById(c, MapleInventoryType.getByType((byte) (itemid / 1000000)), itemid, count, false, false);
        }
        return itemid;
    }

    private static final boolean hasSkill(final MapleClient c, final int reqlvl) {
        if (c.getPlayer().isCygnus()) { // KoC Maker skill.
            return c.getPlayer().getSkillLevel(SkillFactory.getSkill(10001007)) >= reqlvl;
        } else if (c.getPlayer().isAran()) { // Aran skill.
            return c.getPlayer().getSkillLevel(SkillFactory.getSkill(20001007)) >= reqlvl;
        } else {
            return c.getPlayer().getSkillLevel(SkillFactory.getSkill(1007)) >= reqlvl;
        }
    }

    public static final void UseScriptedItemHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        slea.readInt();
        byte itemSlot = (byte) slea.readShort();
        int itemId = slea.readInt();
        int npcId = ii.getScriptedItemNpc(itemId);
        IItem item = c.getPlayer().getInventory(ii.getInventoryType(itemId)).getItem(itemSlot);
        if (item == null || item.getId() != itemId || item.getQuantity() < 1 || npcId == 0) {
            return;
        }
        NPCScriptManager.getInstance().start(c, npcId, itemId);
    }

    public static final void UseSolomonItemHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        /*slea.skip(4);
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem slotItem = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (slotItem.getQuantity() < 1 || slotItem.getId() != itemId || c.getPlayer().getLevel() > ii.getMaxLevelById(itemId)) {
            return;
        }
        c.getPlayer().gainExp(ii.getExpById(itemId), true, true);
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        c.announce(IntraPersonalFactory.enableActions());
    */}

    public static final void UseRemoteGachaponHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int type = slea.readInt();
        if (c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(type)).countById(type) < 1) {
            return;
        }
        int mode = slea.readInt();
        if (type == 5451000) {
            int npcId = 9100100;
            if (mode != 8 && mode != 9) {
                npcId += mode;
            } else {
                npcId = mode == 8 ? 9100109 : 9100117;
            }
            NPCScriptManager.getInstance().start(c, npcId);
        }
    }

    public static final void UseDeathItemHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int itemId = slea.readInt();
        if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) (itemId / 1000000))).findById(itemId) == null) {
            return;
        }
        c.getPlayer().setItemEffect(itemId);
        c.announce(EffectFactory.itemEffect(c.getPlayer().getId(), itemId));
    }

    public static final void UseCatchItemHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        short type = slea.readShort();
        int itemid = slea.readInt();
        int monsobid = slea.readInt();
        if (c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(itemid)).countById(itemid) <= 0 || c.getPlayer().getMap().getMonsterByOid(monsobid) == null) {
            return;
        }
        MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, true, true);
        Pair<Integer, Integer> catchMob = MapleItemInformationProvider.getInstance().getCatchMob(itemid);
        MapleMap map = c.getPlayer().getMap();
        MapleMonster caught = map.getMonsterByOid(monsobid);
        if (caught.getId() == catchMob.getLeft()) {
            c.announce(EffectFactory.catchMonster(monsobid, itemid, (byte) type));
            map.broadcastMessage(c.getPlayer(), EffectFactory.catchMonster(monsobid, itemid, (byte) 1));
            map.damageMonster(c.getPlayer(), caught, caught.getHp());
            MapleInventoryManipulator.addById(c, catchMob.getRight(), (short) 1);
        }
    }

    private static void doCapture(MapleClient c, int monsobid, int itemid, int type, int itemgain) {

    }

    public static final void UseTreasureHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt(); // will load from xml I don't care.
        if (itemId == 2022324) {
            NPCScriptManager.getInstance().start(c, 9010000, 2022324);
            return;
        } else if (c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot).getId() != itemId || c.getPlayer().getInventory(MapleInventoryType.USE).countById(itemId) <= 0) {
            return;
        }
        useTreasure(c, c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot));
        MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemId, 1, false, true);
    }

    public static final void UseChairHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int itemId = slea.readInt();
        if (c.getPlayer().getInventory(MapleInventoryType.SETUP).findById(itemId) == null) {
            return;
        }
        c.getPlayer().setChair(itemId);
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), InventoryFactory.showChair(c.getPlayer().getId(), itemId), false);
        c.announce(IntraPersonalFactory.enableActions());
    }

    public static final void CancelChairHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int id = slea.readShort();
        if (id == -1) { // Cancel Chair
            c.getPlayer().setChair(0);
            c.announce(InventoryFactory.cancelChair(-1));
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), InventoryFactory.showChair(c.getPlayer().getId(), 0), false);
        } else { // Use In-Map Chair
            c.announce(InventoryFactory.cancelChair(id));
        }
    }
}
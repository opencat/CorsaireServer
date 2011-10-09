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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import client.MapleMount;
import client.MapleCharacter;
import client.MapleQuestStatus;
import client.ISkill;
import client.SkillEntry;
import client.BuddylistEntry;
import server.quest.MapleQuest;
import tools.Pair;
import java.util.LinkedHashMap;

/**
 * @name        CharacterTransfer
 * @author      x711Li
 */
public class CharacterTransfer implements Externalizable {
    public int level, donorpts, dojopts, carnivalpts, fame, str, dex, int_, luk, exp, hp, maxhp, mp, maxmp, hpApUsed, mpApUsed, remainingSp, remainingAp, meso, gmLevel, skinColor, gender, job, hair, face, accountid, mapid, initialSpawnPoint, world, coupleId, reborns, rebornPts, mountexp, mountlevel, mounttiredness, omokwins, omoklosses, omokties, guildid, guildrank, alliancerank, bookCover, partyid, linkedlevel, nx, buddyslots, familyid;
    public boolean hasMerchant;
    public int characterid, channel, messengerid;
    public long lastFameTime, transfertime;
    public String name, partyquestitems, linkedname, accountname;
    public Object seedtime, skillmacro, monsterbook, inventories, marriageRing, keymap, savedLocations, rockMaps, vipRockMaps, lastMonthFameIds, wishList, storage, dojoBossCount, questinfo;
    public final Map<Integer, Pair<String, Boolean>> buddies = new LinkedHashMap<Integer, Pair<String, Boolean>>();
    public final Map<Integer, Object> quests = new LinkedHashMap<Integer, Object>();
    public final Map<Integer, Object> skills = new LinkedHashMap<Integer, Object>();

    public CharacterTransfer() {
    }

    public CharacterTransfer(final MapleCharacter chr) {
        this.characterid = chr.getId();
        this.accountid = chr.getAccountID();
        this.accountname = chr.getClient().getAccountName();
        this.channel = chr.getClient().getChannel();
        this.name = chr.getName();
        this.fame = chr.getFame();
        this.gender = chr.getGender();
        this.level = chr.getLevel();
        this.donorpts = chr.getDonorPts();
        this.dojopts = chr.getDojoPts();
        this.carnivalpts = chr.getCarnivalPts();
        this.coupleId = chr.getCoupleId();
        this.reborns = chr.getReborns();
        this.rebornPts = chr.getRebornPts();
        this.omokwins = chr.getMiniGamePoints("wins", true);
        this.omoklosses = chr.getMiniGamePoints("losses", true);
        this.omokties = chr.getMiniGamePoints("ties", true);
        this.hasMerchant = chr.hasMerchant();
        this.linkedlevel = chr.getLinkedLevel();
        this.linkedname = chr.getLinkedName();
        this.nx = chr.getCSPoints(1);
        this.partyquestitems = chr.getPartyQuestItems();
        this.str = chr.getStr();
        this.dex = chr.getDex();
        this.int_ = chr.getInt();
        this.luk = chr.getLuk();
        this.hp = chr.getHp();
        this.mp = chr.getMp();
        this.maxhp = chr.getMaxHp();
        this.maxmp = chr.getMaxMp();
        this.exp = chr.getExp();
        this.hpApUsed = chr.getHpApUsed();
        this.mpApUsed = chr.getMpApUsed();
        this.remainingAp = chr.getRemainingAp();
        this.remainingSp = chr.getRemainingSp();
        this.meso = chr.getMeso();
        this.skinColor = chr.getSkinColor();
        this.job = chr.getJob();
        this.hair = chr.getHair();
        this.face = chr.getFace();
        this.mapid = chr.getMapId();
        this.initialSpawnPoint = chr.getInitialSpawnpoint();
        this.world = chr.getWorld();
        this.guildid = chr.getGuildId();
        this.guildrank = chr.getGuildRank();
        this.alliancerank = chr.getAllianceRank();
        this.buddyslots = chr.getBuddylist().getCapacity();
        this.gmLevel = chr.gmLevel();
        for (final BuddylistEntry qs : chr.getBuddylist().getBuddies()) {
            this.buddies.put(qs.getCharacterId(), new tools.Pair<String, Boolean>(qs.getName(), qs.isVisible()));
        }
        this.partyid = chr.getPartyId();
        this.familyid = chr.getFamilyId();
        if (chr.getMessenger() != null) {
            this.messengerid = chr.getMessenger().getId();
        } else {
            this.messengerid = 0;
        }
        this.bookCover = chr.getMonsterBookCover();
        for (final Map.Entry<MapleQuest, MapleQuestStatus> qs : chr.getQuests().entrySet()) {
            this.quests.put(qs.getKey().getId(), qs.getValue());
        }
        this.monsterbook = chr.getMonsterBook();
        this.inventories = chr.getInventories();
        for (final Map.Entry<ISkill, SkillEntry> qs : chr.getSkills().entrySet()) {
            this.skills.put(qs.getKey().getId(), qs.getValue());
        }
        this.skillmacro = chr.getMacros();
        this.keymap = chr.getKeymap();
        this.savedLocations = chr.getSavedLocations();
        this.lastMonthFameIds = chr.getFamedCharacters();
        this.lastFameTime = chr.getLastFameTime();
        this.storage = chr.getStorage();
        this.rockMaps = chr.getTeleportRockMaps(0);
        this.vipRockMaps = chr.getTeleportRockMaps(1);
        this.wishList = chr.getWishList();
        this.marriageRing = chr.getMarriageRing();
        final MapleMount mount = chr.getMount();
        this.mounttiredness = mount.getTiredness();
        this.mountlevel = mount.getLevel();
        this.mountexp = mount.getExp();
        this.dojoBossCount = chr.getDojoBossCounts();
        this.questinfo = chr.getQuestInfo();
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        this.characterid = in.readInt();
        this.accountid = in.readInt();
        this.channel = in.readInt();
        this.fame = in.readInt();
        this.gender = in.readInt();
        this.level = in.readInt();
        this.donorpts = in.readInt();
        this.dojopts = in.readInt();
        this.carnivalpts = in.readInt();
        this.coupleId = in.readInt();
        this.reborns = in.readInt();
        this.rebornPts = in.readInt();
        this.omokwins = in.readInt();
        this.omoklosses = in.readInt();
        this.omokties = in.readInt();
        this.linkedlevel = in.readInt();
        this.nx = in.readInt();
        this.str = in.readInt();
        this.dex = in.readInt();
        this.int_ = in.readInt();
        this.luk = in.readInt();
        this.hp = in.readInt();
        this.mp = in.readInt();
        this.maxhp = in.readInt();
        this.maxmp = in.readInt();
        this.exp = in.readInt();
        this.hpApUsed = in.readInt();
        this.mpApUsed = in.readInt();
        this.remainingAp = in.readInt();
        this.remainingSp = in.readInt();
        this.meso = in.readInt();
        this.skinColor = in.readInt();
        this.job = in.readInt();
        this.hair = in.readInt();
        this.face = in.readInt();
        this.mapid = in.readInt();
        this.initialSpawnPoint = in.readInt();
        this.world = in.readInt();
        this.guildid = in.readInt();
        this.guildrank = in.readInt();
        this.alliancerank = in.readInt();
        this.buddyslots = in.readInt();
        this.gmLevel = in.readInt();
        this.partyid = in.readInt();
        this.familyid = in.readInt();
        this.messengerid = in.readInt();
        this.bookCover = in.readInt();
        this.mounttiredness = in.readInt();
        this.mountlevel = in.readInt();
        this.mountexp = in.readInt();
        this.hasMerchant = in.readBoolean();
        this.lastFameTime = in.readLong();
        this.monsterbook = in.readObject();
        this.inventories = in.readObject();
        this.skillmacro = in.readObject();
        this.keymap = in.readObject();
        this.savedLocations = in.readObject();
        this.lastMonthFameIds = in.readObject();
        this.storage = in.readObject();
        this.rockMaps = in.readObject();
        this.vipRockMaps = in.readObject();
        this.wishList = in.readObject();
        this.marriageRing = in.readObject();
        this.seedtime = in.readObject();
        this.dojoBossCount = in.readObject();
        this.questinfo = in.readObject();
        this.name = (String) in.readObject();
        this.accountname = (String) in.readObject();
        this.linkedname = (String) in.readObject();
        this.partyquestitems = (String) in.readObject();

        final short addedbuddysize = in.readShort();
        int buddyid;
        String buddyname;
        boolean visible;
        for (int i = 0; i < addedbuddysize; i++) {
            buddyid = in.readInt();
            buddyname = (String) in.readObject();
            visible = in.readBoolean();
            buddies.put(buddyid, new Pair(buddyname, visible));
        }

        final int questsize = in.readShort();
        int questid;
        Object queststatus;
        for (int i = 0; i < questsize; i++) {
            questid = in.readInt();
            queststatus = in.readObject();
            this.quests.put(questid, queststatus);
        }

        final int skillsize = in.readShort();
        int skillid;
        Object skill;
        for (int i = 0; i < skillsize; i++) {
            skillid = in.readInt();
            skill = in.readObject();
            this.skills.put(skillid, skill);
        }
        transfertime = System.currentTimeMillis();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(this.characterid);
        out.writeInt(this.accountid);
        out.writeInt(this.channel);
        out.writeInt(this.fame);
        out.writeInt(this.gender);
        out.writeInt(this.level);
        out.writeInt(this.donorpts);
        out.writeInt(this.dojopts);
        out.writeInt(this.carnivalpts);
        out.writeInt(this.coupleId);
        out.writeInt(this.reborns);
        out.writeInt(this.rebornPts);
        out.writeInt(this.omokwins);
        out.writeInt(this.omoklosses);
        out.writeInt(this.omokties);
        out.writeInt(this.linkedlevel);
        out.writeInt(this.nx);
        out.writeInt(this.str);
        out.writeInt(this.dex);
        out.writeInt(this.int_);
        out.writeInt(this.luk);
        out.writeInt(this.hp);
        out.writeInt(this.mp);
        out.writeInt(this.maxhp);
        out.writeInt(this.maxmp);
        out.writeInt(this.exp);
        out.writeInt(this.hpApUsed);
        out.writeInt(this.mpApUsed);
        out.writeInt(this.remainingAp);
        out.writeInt(this.remainingSp);
        out.writeInt(this.meso);
        out.writeInt(this.skinColor);
        out.writeInt(this.job);
        out.writeInt(this.hair);
        out.writeInt(this.face);
        out.writeInt(this.mapid);
        out.writeInt(this.initialSpawnPoint);
        out.writeInt(this.world);
        out.writeInt(this.guildid);
        out.writeInt(this.guildrank);
        out.writeInt(this.alliancerank);
        out.writeInt(this.buddyslots);
        out.writeInt(this.gmLevel);
        out.writeInt(this.partyid);
        out.writeInt(this.familyid);
        out.writeInt(this.messengerid);
        out.writeInt(this.bookCover);
        out.writeInt(this.mounttiredness);
        out.writeInt(this.mountlevel);
        out.writeInt(this.mountexp);
        out.writeBoolean(this.hasMerchant);
        out.writeLong(this.lastFameTime);
        out.writeObject(this.monsterbook);
        out.writeObject(this.inventories);
        out.writeObject(this.skillmacro);
        out.writeObject(this.keymap);
        out.writeObject(this.savedLocations);
        out.writeObject(this.lastMonthFameIds);
        out.writeObject(this.storage);
        out.writeObject(this.rockMaps);
        out.writeObject(this.vipRockMaps);
        out.writeObject(this.wishList);
        out.writeObject(this.marriageRing);
        out.writeObject(this.seedtime);
        out.writeObject(this.dojoBossCount);
        out.writeObject(this.questinfo);
        out.writeObject(this.name);
        out.writeObject(this.accountname);
        out.writeObject(this.linkedname);
        out.writeObject(this.partyquestitems);
        out.writeShort(this.buddies.size());
        for (final Map.Entry<Integer, Pair<String, Boolean>> qs : this.buddies.entrySet()) {
            out.writeInt(qs.getKey());
            out.writeObject(qs.getValue().getLeft());
            out.writeBoolean(qs.getValue().getRight());
        }
        out.writeShort(this.quests.size());
        for (final Map.Entry<Integer, Object> qs : this.quests.entrySet()) {
            out.writeInt(qs.getKey());
            out.writeObject(qs.getValue());
        }
        out.writeShort(this.skills.size());
        for (final Map.Entry<Integer, Object> qs : this.skills.entrySet()) {
            out.writeInt(qs.getKey());
            out.writeObject(qs.getValue());
        }
    }
}

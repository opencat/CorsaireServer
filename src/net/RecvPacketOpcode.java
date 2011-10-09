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

package net;

/**
 * @name        RecvPacketOpcode
 * @author      Matze
 *              Modified by x711Li
 */
public enum RecvPacketOpcode {
    LOGIN_PASSWORD(1, false),
    GUEST_LOGIN(2),
    SERVERLIST_REREQUEST(4),
    CHARLIST_REQUEST(5),
    SERVERSTATUS_REQUEST(6),
    SET_GENDER(8),
    AFTER_LOGIN(9),
    REGISTER_PIN(10),
    SERVERLIST_REQUEST(11),
    PLAYER_DC(12),
    VIEW_ALL_CHAR(13),
    PICK_ALL_CHAR(14),
    SEE_ALL_CHAR(15), // 13, 15, 14, 20
    PLAYER_LOGGEDIN(20, false),
    CHECK_CHAR_NAME(21),
    CREATE_CHAR(22),
    DELETE_CHAR(23),
    PONG(24),
    START(25),
    ERROR(26),
    C_ERROR(27),
    RELOG(28),
    REGISTER_PIC(29),
    PROMPT_PIC(30),
    VIEW_ALL_REGISTER_PIC(31),
    VIEW_ALL_PROMPT_PIC(32),
    CHANGE_MAP(38),
    CHANGE_CHANNEL(39),
    ENTER_CASH_SHOP(40),
    MOVE_PLAYER(41),
    CANCEL_CHAIR(42),
    USE_CHAIR(43),
    CLOSE_RANGE_ATTACK(44),
    RANGED_ATTACK(45),
    MAGIC_ATTACK(46),
    ENERGY_ORB_ATTACK(47),
    TAKE_DAMAGE(48),
    GENERAL_CHAT(49),
    CLOSE_CHALKBOARD(50),
    FACE_EXPRESSION(51),
    USE_ITEMEFFECT(52),
    USE_DEATHITEM(54),
    MONSTER_BOOK_COVER(57),
    NPC_TALK(58),
    REMOTE_SHOP(59),
    NPC_TALK_MORE(60),
    NPC_SHOP(61),
    STORAGE(62),
    HIRED_MERCHANT_REQUEST(63),
    FREDRICK_OPERATION(64),
    DUEY_ACTION(65),
    MINERVA(66),
    MINERVA_GO(67),
    ITEM_SORT(69),
    ITEM_SORT2(70),
    ITEM_MOVE(71),
    USE_ITEM(72),
    CANCEL_ITEM_EFFECT(73),
    USE_SUMMON_BAG(75),
    PET_FOOD(76),
    USE_MOUNT_FOOD(77),
    SCRIPTED_ITEM(78),
    USE_CASH_ITEM(79),
    USE_CATCH_ITEM(81),
    USE_SKILL_BOOK(82),
    USE_TELEPORT_ROCK(84),
    USE_RETURN_SCROLL(85),
    USE_UPGRADE_SCROLL(86),
    DISTRIBUTE_AP(87),
    AUTO_DISTRIBUTE_AP(88),
    HEAL_OVER_TIME(89),
    DISTRIBUTE_SP(90),
    SPECIAL_MOVE(91),
    CANCEL_BUFF(92),
    SKILL_EFFECT(93),
    MESO_DROP(94),
    GIVE_FAME(95),
    CHAR_INFO_REQUEST(97),
    SPAWN_PET(98),
    CANCEL_DEBUFF(99),
    CHANGE_MAP_SPECIAL(100),
    USE_INNER_PORTAL(101),
    TROCK_ADD_MAP(102),
    REPORT(106),
    QUEST_ACTION(107),
    END_COMBO(108),
    POISON_BOMB(109),
    SKILL_MACRO(110),
    OPEN_TREASURE(112),
    MAKER_SKILL(113),
    USE_REMOTE(116),
    PARTYCHAT(119),
    WHISPER(120),
    SPOUSE_CHAT(121),
    MESSENGER(122),
    PLAYER_INTERACTION(123),
    PARTY_OPERATION(124),
    DENY_PARTY_REQUEST(125),
    GUILD_OPERATION(126),
    DENY_GUILD_REQUEST(127),
    ADMIN_COMMAND(128),
    ADMIN_LOG(129),
    BUDDYLIST_MODIFY(130),
    NOTE_DELETE(131),
    NOTE_ACTION(132),
    USE_DOOR(133),
    CHANGE_KEYMAP(135),
    RING_ACTION(137),
    ALLIANCE_OPERATION(143),
    OPEN_FAMILY(145), // 146 - OPEN FAMILY, 145 - SHOW FAMILY, 147 - ADD
    ADD_FAMILY(146),
    ACCEPT_FAMILY(149),
    USE_FAMILY(150),
    ALLIANCE_DENIED(153),
    BBS_OPERATION(155),
    ENTER_MTS(156),
    USE_SOLOMON_ITEM(157),
    USE_GACHA_EXP(158),
    ARAN_COMBO(163),
    MOVE_PET(167),
    PET_CHAT(168),
    PET_COMMAND(169),
    PET_LOOT(170),
    PET_AUTO_POT(171),
    PET_EXCLUDE_ITEMS(172),
    MOVE_SUMMON(175),
    SUMMON_ATTACK(176),
    DAMAGE_SUMMON(177),
    BEHOLDER(178),
    QUICK_SLOT_CHANGE(183),
    MOVE_LIFE(188),
    AUTO_AGGRO(189),
    MOB_DAMAGE_MOB_FRIENDLY(192),
    MONSTER_EXPLOSION(193),
    MOB_DAMAGE_MOB(194),
    NPC_ACTION(197),
    ITEM_PICKUP(202),
    DAMAGE_REACTOR(205),
    TOUCHING_REACTOR(206),
    UPDATE_CHAR_2(207),
    MONSTER_CARNIVAL(218),
    PARTY_SEARCH_START(222),
    PLAYER_UPDATE(223),
    // TOUCHING_CS(236),
    BUY_CS_ITEM(229),
    COUPON_CODE(230),
    OPEN_ITEMUI(236),
    CLOSE_ITEMUI(237),
    USE_ITEMUI(238),
    ADMIN_MESSENGER(245),
    MTS_OP(251),
    USE_MAPLELIFE(256),
    USE_HAMMER(260);
    private int value;
    private boolean validate;

    private RecvPacketOpcode(final int value) {
        this.value = value;
        this.validate = true;
    }

    private RecvPacketOpcode(final int value, final boolean validate) {
        this.value = value;
        this.validate = validate;
    }

    public final boolean reqValidation() {
        return validate;
    }

    public final int getValue() {
        return value;
    }
}

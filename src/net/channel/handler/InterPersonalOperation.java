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

import client.BuddyList;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import client.BuddylistEntry;
import client.CharacterNameAndId;
import client.IItem;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MaplePet;
import client.MapleStat;
import client.messages.CommandProcessor;
import constants.InventoryConstants;
import constants.ServerConstants;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.PlayerInteraction;
import net.channel.ChannelServer;
import net.channel.remote.ChannelWorldInterface;
import net.world.MapleMessenger;
import net.world.MapleMessengerCharacter;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.guild.MapleAlliance;
import net.world.guild.MapleGuild;
import net.world.guild.MapleGuildResponse;
import net.world.remote.WorldChannelInterface;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleMiniGame;
import server.shops.MaplePlayerShopItem;
import server.MapleTrade;
import server.shops.HiredMerchant;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.DatabaseConnection;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.factory.BuddyFactory;
import tools.factory.EffectFactory;
import tools.factory.FamilyFactory;
import tools.factory.GameFactory;
import tools.factory.GuildFactory;
import tools.factory.InterPersonalFactory;
import tools.factory.IntraPersonalFactory;
import tools.factory.MarketFactory;
import tools.factory.PartyFactory;
import tools.factory.PetFactory;

/**
 * @name        InterPersonalOperation
 * @author      x711Li
 */
public class InterPersonalOperation {
    private static final void generalChat(MapleClient c, String text, SeekableLittleEndianAccessor slea) {
        if(c.getPlayer().gmLevel() == -1) {
            c.getPlayer().getMap().broadcastMessage(PartyFactory.multiChat(c.getPlayer().getName(), text, 3));
            c.getPlayer().getMap().broadcastMessage(EffectFactory.getChatText(c.getPlayer().getId(), text, false, 1));
        } else {
            c.getPlayer().getMap().broadcastMessage(EffectFactory.getChatText(c.getPlayer().getId(), text, c.getPlayer().getGMChat(), slea.readByte()));
        }
    }

    public static final void GeneralChatHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if(System.currentTimeMillis() - c.getPlayer().getMuted() <= 90000) {
            c.getPlayer().dropMessage("You have been muted. You must wait " +
            ((90000 - (System.currentTimeMillis() - c.getPlayer().getMuted())) / 1000) + " seconds before talking again.");
        } else {
            String text = slea.readMapleAsciiString();
            if (!CommandProcessor.getInstance().processCommand(c, text)) {
                if(c.getPlayer().getWordsTalked().equals(text)) {
                    if(c.getPlayer().getSpamCheck() > 1) {
                        c.getPlayer().dropMessage("Spam is currently disallowed.");
                        return;
                    } else {
                        c.getPlayer().setSpamCheck(c.getPlayer().getSpamCheck() + 1);
                        generalChat(c, text, slea);
                    }
                } else {
                    c.getPlayer().setWordsTalked(text);
                    c.getPlayer().setSpamCheck(0);
                    generalChat(c, text, slea);
                }
            }
        }
    }

    public static final void WhisperHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if(System.currentTimeMillis() - c.getPlayer().getMuted() <= 90000)
            c.getPlayer().dropMessage("You have been muted. You must wait " +
            ((90000 - (System.currentTimeMillis() - c.getPlayer().getMuted())) / 1000) + " seconds before talking again.");
        else {
            byte mode = slea.readByte();
            if (mode == 6) {
                String recipient = slea.readMapleAsciiString();
                String text = slea.readMapleAsciiString();
                if (!CommandProcessor.getInstance().processCommand(c, text)) {
                    MapleCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                    if (player != null) {
                        player.getClient().announce(InterPersonalFactory.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                        c.announce(InterPersonalFactory.getWhisperReply(recipient, (byte) 1));
                    } else {
                        try {
                            if (c.getChannelServer().getWorldInterface().isConnected(recipient)) {
                                c.getChannelServer().getWorldInterface().whisper(c.getPlayer().getName(), recipient, c.getChannel(), text);
                                c.announce(InterPersonalFactory.getWhisperReply(recipient, (byte) 1));
                            } else {
                                c.announce(InterPersonalFactory.getWhisperReply(recipient, (byte) 0));
                            }
                        } catch (RemoteException e) {
                            c.announce(InterPersonalFactory.getWhisperReply(recipient, (byte) 0));
                            c.getChannelServer().reconnectWorld();
                        }
                    }
                }
            } else if (mode == 5) {
                String recipient = slea.readMapleAsciiString();
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                if (victim != null && victim.gmLevel() < 1) {
                    if (victim.getClient().insideCashShop()) {
                        c.announce(InterPersonalFactory.getFindReply(victim.getName(), -1, 2));
                    } else {
                        c.announce(InterPersonalFactory.getFindReply(victim.getName(), victim.getMap().getId(), 1));
                    }
                } else if (victim != null && c.getPlayer().gmLevel() < victim.gmLevel()) {
                    c.announce(InterPersonalFactory.getWhisperReply(recipient, (byte) 0));
                } else {
                    try {
                        int channel = c.getChannelServer().getWorldInterface().find(recipient, true);
                        if (channel > -1) {
                            c.announce(InterPersonalFactory.getFindReply(recipient, channel, 3));
                        } else {
                            c.announce(InterPersonalFactory.getFindReply(recipient, -1, 0));
                        }
                    } catch (RemoteException ex) {
                        c.getChannelServer().reconnectWorld();
                    }
                }
            }
        }
    }

    public static final void FaceExpressionHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int emote = slea.readInt();
        if (emote > 7) {
            int emoteid = 5159992 + emote;
            MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(emoteid);
            if (c.getPlayer().getInventory(type).findById(emoteid) == null) {
                return;
            }
        }
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), InterPersonalFactory.facialExpression(c.getPlayer(), emote), false);
    }

    public static final void CharInfoRequestHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        int cid = slea.readInt();
        MapleCharacter player = (MapleCharacter) c.getPlayer().getMap().getMapObject(cid);
        if (player.gmLevel() < 1 || c.getPlayer().gmLevel() > 0) {
            c.announce(IntraPersonalFactory.charInfo(player, c.getPlayer().getId() == player.getId()));
        } else {
            c.announce(IntraPersonalFactory.enableActions());
        }
    }

    public static final void PlayerInteractionHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        byte mode = slea.readByte();
        if (mode == PlayerInteraction.CREATE.getCode()) {
            byte createType = slea.readByte();
            if (createType == 3) {// trade
                if (c.getPlayer().getTrade() == null && !(player.gmLevel() == 1 || player.gmLevel() == 2)) {
                    MapleTrade.startTrade(c.getPlayer());
                }
            } else if (createType == 1) { // omok mini game
                boolean ableToStart = false;

                for(int i = 4080001; i < 4080012; i++) {
                    if(c.getPlayer().getItemQuantity(i, false) > 0)  {
                        ableToStart = true;
                        break;
                    }
                }
                if (c.getPlayer().getChalkboard() != null || !ableToStart) {
                    return;
                }
                String desc = slea.readMapleAsciiString();
                slea.readByte(); // 20 6E 4E
                int type = slea.readByte(); // 20 6E 4E
                MapleMiniGame game = new MapleMiniGame(c.getPlayer(), desc);
                c.getPlayer().setMiniGame(game);
                game.setPieceType(type);
                game.setGameType("omok");
                c.getPlayer().getMap().addMapObject(game);
                c.getPlayer().getMap().broadcastMessage(GameFactory.addOmokBox(c.getPlayer(), 1, 0));
                game.sendOmok(c, type);
            } else if (createType == 2) { // matchcard
                if (c.getPlayer().getChalkboard() != null) {
                    return;
                }
                String desc = slea.readMapleAsciiString();
                slea.readByte(); // 20 6E 4E
                int type = slea.readByte(); // 20 6E 4E
                MapleMiniGame game = new MapleMiniGame(c.getPlayer(), desc);
                c.getPlayer().setMiniGame(game);
                game.setPieceType(type);
                if (type == 0) {
                    game.setMatchesToWin(6);
                } else if (type == 1) {
                    game.setMatchesToWin(10);
                } else {
                    game.setMatchesToWin(15);
                }
                game.setGameType("matchcard");
                c.getPlayer().getMap().addMapObject(game);
                c.getPlayer().getMap().broadcastMessage(GameFactory.addMatchCardBox(c.getPlayer(), 1, 0));
                game.sendMatchCard(c, type);
            } else {
                if (player.getMap().getMapObjectsInRange(player.getPosition(), 23000, Arrays.asList(MapleMapObjectType.HIRED_MERCHANT)).size() != 0 || player.getMapId() < 910000000 || player.getMapId() > 910000023) {
                    player.dropMessage(1, "You may not establish a store here.");
                    return;
                }
                String desc = slea.readMapleAsciiString();
                slea.skip(3);
                int itemId = slea.readInt();
                if (player.getInventory(MapleInventoryType.CASH).countById(itemId) < 1) {
                    player.ban(c.getPlayer().getName() + " caught trying to set up Hired Merchant without cash item.", true);
                    return;
                }
                if (createType == 4) { // shop
                    player.dropMessage(1, "Player shops are currently disabled.");
                    return;
                } else if (createType == 5) {
                    if (player.hasMerchant()) {
                        player.dropMessage(5, "You already have another merchant open!");
                        return;
                    }
                    HiredMerchant merchant = new HiredMerchant(player, itemId, desc);
                    player.setHiredMerchant(merchant);
                    c.announce(MarketFactory.getHiredMerchant(player, merchant, true));
                }
            }
        } else if (mode == PlayerInteraction.INVITE.getCode()) {
            int otherPlayer = slea.readInt();
            MapleCharacter trader = player.getMap().getCharacterById(otherPlayer);
            if (trader.getHiredMerchant() != null || trader.getTrade() != null || trader.getMiniGame() != null) {
                player.message(trader.getName() + " is busy at the moment.");
                return;
            }
            MapleTrade.inviteTrade(player, player.getMap().getCharacterById(otherPlayer));
        } else if (mode == PlayerInteraction.DECLINE.getCode()) {
            MapleTrade.declineTrade(player);
        } else if (mode == PlayerInteraction.VISIT.getCode()) {
            if (player.getTrade() != null && player.getTrade().getPartner() != null && !(player.gmLevel() == 1 || player.gmLevel() == 2)) {
                MapleTrade.visitTrade(player, player.getTrade().getPartner().getChr());
            } else {
                int oid = slea.readInt();
                MapleMapObject ob = player.getMap().getMapObject(oid);
                if (ob.getType().equals(MapleMapObjectType.MINI_GAME)) {
                    MapleMiniGame game = (MapleMiniGame) ob;
                    if (game.hasFreeSlot() && !game.isVisitor(player)) {
                        game.addVisitor(player);
                        player.setMiniGame(game);
                        if (game.getGameType().equals("omok")) {
                            game.sendOmok(c, game.getPieceType());
                        } else {
                            game.sendMatchCard(c, game.getPieceType());
                        }
                    } else {
                        player.getClient().announce(GameFactory.getMiniGameFull());
                    }
                } else if (ob.getType().equals(MapleMapObjectType.HIRED_MERCHANT) && player.getHiredMerchant() == null) {
                    HiredMerchant merchant = (HiredMerchant) ob;
                    player.setHiredMerchant(merchant);
                    if (merchant.isOwner(player)) {
                        merchant.setOpen(false);
                        merchant.removeAllVisitors();
                        c.announce(MarketFactory.getHiredMerchant(player, merchant, false));
                    } else if (!merchant.isOpen()) {
                        player.dropMessage(1, "This shop is in maintenance, please come by later.");
                        player.setHiredMerchant(null);
                    } else if (merchant.getFreeSlot() == -1) {
                        player.dropMessage(1, "This shop has reached it's maximum capacity, please come by later.");
                        player.setHiredMerchant(null);
                    } else {
                        merchant.addVisitor(player);
                        c.announce(MarketFactory.getHiredMerchant(player, merchant, false));
                    }
                }
            }
        } else if (mode == PlayerInteraction.CHAT.getCode()) {
            HiredMerchant merchant = player.getHiredMerchant();
            if (player.getTrade() != null) {
                player.getTrade().chat(slea.readMapleAsciiString());
            } else if (player.getMiniGame() != null) {
                MapleMiniGame game = player.getMiniGame();
                if (game != null) {
                    game.chat(c, slea.readMapleAsciiString());
                }
            } else if (merchant != null) {
                merchant.broadcastToVisitors(MarketFactory.hiredMerchantChat(player.getName() + " : " + slea.readMapleAsciiString(), merchant.getVisitorSlot(player)));
            }
        } else if (mode == PlayerInteraction.EXIT.getCode()) {
            if (player.getTrade() != null) {
                MapleTrade.cancelTrade(player);
            } else {
                MapleMiniGame game = player.getMiniGame();
                HiredMerchant merchant = player.getHiredMerchant();
                if (game != null) {
                    player.setMiniGame(null);
                    if (game.isOwner(player)) {
                        if(game.isStarted()) {
                            player.setMiniGamePoints(game.getVisitor(), 2, true);
                        }
                        player.getMap().broadcastMessage(GameFactory.removeCharBox(player));
                        game.broadcastToVisitor(GameFactory.getMiniGameClose((byte) 0));
                    } else {
                        if(game.isStarted()) {
                            player.setMiniGamePoints(game.getOwner(), 2, true);
                        }
                        game.removeVisitor(c.getPlayer());
                    }
                } else if (merchant != null) {
                    if (!merchant.isOwner(player)) {
                        merchant.removeVisitor(player);
                    } else {
                        c.announce(MarketFactory.hiredMerchantVisitorLeave(0, true));
                    }
                    player.setHiredMerchant(null);
                }
            }
        } else if (mode == PlayerInteraction.OPEN.getCode()) {
            if (player.getMap().getMapObjectsInRange(player.getPosition(), 23000, Arrays.asList(MapleMapObjectType.HIRED_MERCHANT)).size() != 0 || player.getMapId() < 910000000 && player.getMapId() > 910000023) {
                player.dropMessage(1, "You may not establish a store here.");
                return;
            }
            HiredMerchant merchant = player.getHiredMerchant();
            if (merchant != null && merchant.isOwner(player)) {
                player.setHasMerchant(true);
                merchant.setOpen(true);
                player.getMap().addMapObject(merchant);
                player.getMap().broadcastMessage(MarketFactory.spawnHiredMerchant(merchant));
                slea.readByte();
            }
        } else if (mode == PlayerInteraction.READY.getCode()) {
            MapleMiniGame game = player.getMiniGame();
            game.broadcast(GameFactory.getMiniGameReady(game));
        } else if (mode == PlayerInteraction.UN_READY.getCode()) {
            MapleMiniGame game = player.getMiniGame();
            game.broadcast(GameFactory.getMiniGameUnReady(game));
        } else if (mode == PlayerInteraction.START.getCode()) {
            MapleMiniGame game = player.getMiniGame();
            if (game.getGameType().equals("omok")) {
                game.broadcast(GameFactory.getMiniGameStart(game, game.getLoser()));
                player.getMap().broadcastMessage(GameFactory.addOmokBox(game.getOwner(), 2, 1));
            } else {
                game.shuffleList();
                game.broadcast(GameFactory.getMatchCardStart(game, game.getLoser()));
                player.getMap().broadcastMessage(GameFactory.addMatchCardBox(game.getOwner(), 2, 1));
            }
        } else if (mode == PlayerInteraction.GIVE_UP.getCode()) {
            MapleMiniGame game = player.getMiniGame();
            if (game.getGameType().equals("omok")) {
                if (game.isOwner(player)) {
                    game.broadcast(GameFactory.getMiniGameOwnerForfeit(game));
                } else {
                    game.broadcast(GameFactory.getMiniGameVisitorForfeit(game));
                }
            } else {
                if (game.isOwner(player)) {
                    game.broadcast(GameFactory.getMatchCardVisitorWin(game));
                } else {
                    game.broadcast(GameFactory.getMatchCardOwnerWin(game));
                }
            }
        } else if (mode == PlayerInteraction.REQUEST_TIE.getCode()) {
            MapleMiniGame game = player.getMiniGame();
            if (game.isOwner(player)) {
                game.broadcastToVisitor(GameFactory.getMiniGameRequestTie(game));
            } else {
                game.getOwner().getClient().announce(GameFactory.getMiniGameRequestTie(game));
            }
        } else if (mode == PlayerInteraction.ANSWER_TIE.getCode()) {
            MapleMiniGame game = player.getMiniGame();
            slea.readByte();
            if (game.getGameType().equals("omok")) {
                game.broadcast(GameFactory.getMiniGameTie(game));
            } else {
                game.broadcast(GameFactory.getMatchCardTie(game));
            }
        } else if (mode == PlayerInteraction.SKIP.getCode()) {
            MapleMiniGame game = player.getMiniGame();
            if (game.isOwner(player)) {
                game.broadcast(GameFactory.getMiniGameSkipOwner(game));
            } else {
                game.broadcast(GameFactory.getMiniGameSkipVisitor(game));
            }
        } else if (mode == PlayerInteraction.MOVE_OMOK.getCode()) {
            player.getMiniGame().setPiece(slea.readInt(), slea.readInt(), slea.readByte(), player);
        } else if (mode == PlayerInteraction.SELECT_CARD.getCode()) {
            int turn = slea.readByte(); // 1st turn = 1; 2nd turn = 0
            int slot = slea.readByte(); // slot
            MapleMiniGame game = c.getPlayer().getMiniGame();
            int firstslot = game.getFirstSlot();
            if (turn == 1) {
                game.setFirstSlot(slot);
                if (game.isOwner(c.getPlayer())) {
                    game.broadcastToVisitor(GameFactory.getMatchCardSelect(game, turn, slot, firstslot, turn));
                } else {
                    game.getOwner().getClient().announce(GameFactory.getMatchCardSelect(game, turn, slot, firstslot, turn));
                }
            } else if ((game.getCardId(firstslot + 1)) == (game.getCardId(slot + 1))) {
                if (game.isOwner(c.getPlayer())) {
                    game.broadcast(GameFactory.getMatchCardSelect(game, turn, slot, firstslot, 2));
                    game.setOwnerPoints();
                } else {
                    game.broadcast(GameFactory.getMatchCardSelect(game, turn, slot, firstslot, 3));
                    game.setVisitorPoints();
                }
            } else if (game.isOwner(c.getPlayer())) {
                game.broadcast(GameFactory.getMatchCardSelect(game, turn, slot, firstslot, 0));
            } else {
                game.broadcast(GameFactory.getMatchCardSelect(game, turn, slot, firstslot, 1));
            }
        } else if (mode == PlayerInteraction.SET_MESO.getCode()) {
            if (player.getTrade() != null) {
                player.getTrade().setMeso(slea.readInt());
            }
        } else if (mode == PlayerInteraction.SET_ITEMS.getCode()) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MapleInventoryType ivType = MapleInventoryType.getByType(slea.readByte());
            IItem item = player.getInventory(ivType).getItem((byte) slea.readShort());
            short quantity = slea.readShort();
            byte targetSlot = slea.readByte();
            if (player.getTrade() != null && item != null) {
                if ((quantity <= item.getQuantity() && quantity > 0) || InventoryConstants.isRechargable(item.getId())) {

                    if (item.getFlag() == InventoryConstants.UNTRADEABLE || item.getFlag() == InventoryConstants.LOCK) {
                        c.announce(IntraPersonalFactory.enableActions());
                        return;
                    }
                    if (ii.isDropRestricted(item.getId()) && item.getFlag() != InventoryConstants.KARMA) { // ensure that undroppable items do not make it to the trade window
                        c.announce(IntraPersonalFactory.enableActions());
                        return;
                    }
                    IItem tradeItem = item.copy();
                    if (InventoryConstants.isRechargable(item.getId())) {
                        tradeItem.setQuantity(item.getQuantity());
                        MapleInventoryManipulator.removeFromSlot(c, ivType, item.getPosition(), item.getQuantity(), true);
                    } else {
                        tradeItem.setQuantity(quantity);
                        MapleInventoryManipulator.removeFromSlot(c, ivType, item.getPosition(), quantity, true);
                    }
                    tradeItem.setPosition(targetSlot);
                    player.getTrade().addItem(tradeItem);
                    return;
                }
            }
        } else if (mode == PlayerInteraction.CONFIRM.getCode() && !(player.gmLevel() == 1 || player.gmLevel() == 2)) {
            if (player.getTrade() != null) {
                MapleTrade.completeTrade(player);
            }
        } else if (mode == PlayerInteraction.TRANSACTION.getCode()) {
        } else if (mode == PlayerInteraction.ADD_ITEM.getCode() || mode == PlayerInteraction.PUT_ITEM.getCode()) {
            MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
            byte slot = (byte) slea.readShort();
            IItem ivItem = player.getInventory(type).getItem(slot);
            short bundles = slea.readShort();
            if (ivItem.getFlag() == InventoryConstants.UNTRADEABLE) {
                return;
            }
            if (ivItem != null) {
                if (c.getPlayer().getItemQuantity(ivItem.getId(), false) < bundles || ivItem.getFlag() == InventoryConstants.UNTRADEABLE) {
                    return;
                }
                short perBundle = slea.readShort();
                int price = slea.readInt();
                if (ivItem.getQuantity() < bundles) {
                    return;
                } else if (perBundle < 1) {
                    player.ban(c.getPlayer().getName() + " caught duping through Hired Merchant.", true);
                    return;
                } else if (bundles < 1) {
                    player.ban(c.getPlayer().getName() + " caught duping through Hired Merchant.", true);
                    return;
                } else if (ivItem.getId() >= 5000000) {
                    return;
                } else if (price < 1) {
                    player.ban(c.getPlayer().getName() + " caught duping through Hired Merchant.", true);
                    return;
                } else if (ivItem.getFlag() == InventoryConstants.UNTRADEABLE || ivItem.getFlag() == InventoryConstants.LOCK) {
                    c.announce(IntraPersonalFactory.enableActions());
                    return;
                }
                IItem sellItem = ivItem.copy();
                sellItem.setQuantity(bundles);
                MaplePlayerShopItem item = new MaplePlayerShopItem(sellItem, bundles, price);
                HiredMerchant merchant = c.getPlayer().getHiredMerchant();
                if (merchant != null && merchant.isOwner(c.getPlayer())) {
                    if(merchant.getItems().size() >= 18) {
                        c.getPlayer().dropMessage(1, "You cannot store more than 18 items in your store at one time!");
                        return;
                    }
                    merchant.addItem(item);
                    c.announce(MarketFactory.updateHiredMerchant(merchant));
                }
                if (InventoryConstants.isRechargable(ivItem.getId())) {
                    MapleInventoryManipulator.removeFromSlot(c, type, slot, ivItem.getQuantity(), true);
                } else {
                    MapleInventoryManipulator.removeFromSlot(c, type, slot, (short) (bundles), true);
                }
                c.getPlayer().saveItems();
            }
        } else if (mode == PlayerInteraction.REMOVE_ITEM.getCode()) {
        } else if (mode == PlayerInteraction.BUY.getCode() || mode == PlayerInteraction.MERCHANT_BUY.getCode()) {
            int item = slea.readByte();
            short quantity = slea.readShort();
            if (quantity < 0) {
                player.ban(c.getPlayer().getName() + " caught duping through Hired Merchant.", true);
                return;
            }
            HiredMerchant merchant = player.getHiredMerchant();
            if (merchant != null && !merchant.getOwner().equals(player.getName())) {
                if (merchant.getOwnerId() == player.getId()) {
                    player.ban(c.getPlayer().getName() + " caught buying from own Hired Merchant.", true);
                } else if (player.getInventory(MapleItemInformationProvider.getInstance().getInventoryType(merchant.getItems().get(item).getItem().getId())).getNextFreeSlot() < 0) {
                } else {
                    merchant.buy(c, item, quantity);
                    merchant.broadcastToVisitors(MarketFactory.updateHiredMerchant(merchant));
                }
            }
        } else if (mode == PlayerInteraction.TAKE_ITEM_BACK.getCode()) {
            HiredMerchant merchant = player.getHiredMerchant();
            if (merchant != null && merchant.isOwner(player)) {
                short slot = slea.readShort();
                MaplePlayerShopItem item = merchant.getItems().get(slot);
                if (player.getInventory(MapleItemInformationProvider.getInstance().getInventoryType(item.getItem().getId())).getNextFreeSlot() > -1) {
                    if (item.getBundles() > 0) {
                        IItem iitem = item.getItem();
                        iitem.setQuantity((short) item.getBundles());
                        MapleInventoryManipulator.addFromDrop(c, iitem, true);
                    }
                    merchant.removeFromSlot(slot);
                    c.announce(MarketFactory.updateHiredMerchant(merchant));
                } else {
                    player.dropMessage(1, "Your inventory is currently full.");
                }
            }
        } else if (mode == PlayerInteraction.CLOSE_MERCHANT.getCode()) {
            HiredMerchant merchant = c.getPlayer().getHiredMerchant();
            synchronized (merchant) {
                if (!c.getPlayer().hasMerchant()) {
                    return;
                }
                if (merchant != null && merchant.isOwner(c.getPlayer())) {
                    c.announce(MarketFactory.hiredMerchantOwnerLeave());
                    c.announce(MarketFactory.leaveHiredMerchant(0x00, 0x03));
                    merchant.closeShop();
                    c.getPlayer().setHasMerchant(false);
                }
            }
        } else if (mode == PlayerInteraction.MAINTENANCE_OFF.getCode()) {
            HiredMerchant merchant = player.getHiredMerchant();
            if (merchant != null && merchant.isOwner(player)) {
                merchant.setOpen(true);
            }
            c.announce(IntraPersonalFactory.enableActions());
        } else if (mode == PlayerInteraction.BAN_PLAYER.getCode()) {
        } else if (mode == PlayerInteraction.OPEN_STORE.getCode()) {
        } else if (mode == PlayerInteraction.VIEW_VISITORS.getCode()) {
        } else if (mode == PlayerInteraction.BLACKLIST.getCode()) {
            if (player.getHiredMerchant() != null && player.getHiredMerchant().isOwner(player)) {
            }
        } else if (mode == PlayerInteraction.MERCHANT_ORGANIZE.getCode()) {
            HiredMerchant merch = player.getHiredMerchant();
            if (merch != null && merch.isOwner(player)) {
                int i = 0;
                for (Iterator<MaplePlayerShopItem> iterator = merch.getItems().iterator(); iterator.hasNext(); ) {
                    MaplePlayerShopItem curItem = iterator.next();
                    if (!curItem.isExist()) {
                        merch.removeFromSlot(i);
                        iterator.remove();
                    } else {
                        i++;
                    }
                }
                c.announce(MarketFactory.updateHiredMerchant(merch));
            }
        } else if (mode == PlayerInteraction.REAL_CLOSE_MERCHANT.getCode()) {
        } else if (mode == PlayerInteraction.CHAT_THING.getCode()) {
            c.disconnect(true, false);
        }
    }

    public static final void GiveFameHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter target = (MapleCharacter) c.getPlayer().getMap().getMapObject(slea.readInt());
        int mode = slea.readByte();
        int famechange = mode == 0 ? -1 : 1;
        MapleCharacter player = c.getPlayer();

        if (target == c.getPlayer()) { // faming self
            c.getPlayer().ban(c.getPlayer().getName() + " caught faming self.", true);//getCheatTracker().registerOffense(CheatingOffense.FAMING_SELF);
            return;
        } else if (c.getPlayer().getLevel() < 15) {
            c.getPlayer().ban(c.getPlayer().getName() + " caught faming under level 15.", true);//getCheatTracker().registerOffense(CheatingOffense.FAMING_UNDER_15);
            return;
        }
        switch (player.canGiveFame(target)) {
        case OK:
            if (Math.abs(target.getFame() + famechange) < 30001) {
                target.addFame(famechange);
                target.updateSingleStat(MapleStat.FAME, target.getFame());
            }
            player.hasGivenFame(target);
            c.announce(InterPersonalFactory.giveFameResponse(mode, target.getName(), target.getFame()));
            target.getClient().announce(InterPersonalFactory.receiveFame(mode, player.getName()));
            break;
        case NOT_TODAY:
            c.announce(InterPersonalFactory.giveFameErrorResponse(3));
            break;
        case NOT_THIS_MONTH:
            c.announce(InterPersonalFactory.giveFameErrorResponse(4));
            break;
        }
    }

    public static final void PartyOperationHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int operation = slea.readByte();
        MapleCharacter player = c.getPlayer();
        WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
        MapleParty party = player.getParty();
        MaplePartyCharacter partyplayer = new MaplePartyCharacter(player);
        switch (operation) {
        case 1: { // create
                if (c.getPlayer().getParty() == null) {
                    try {
                        party = wci.createParty(partyplayer);
                        player.setParty(party);
                    } catch (RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                    c.announce(PartyFactory.partyCreated());
                } else {
                    c.getPlayer().dropMessage(5, "You can't create a party as you are already in one");
                }
                break;
            }
        case 2: { // leave
                if (party != null) {
                    try {
                        if (partyplayer.equals(party.getLeader())) {
                            wci.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
                            if (player.getEventInstance() != null) {
                                player.getEventInstance().disbandParty();
                            }
                        } else {
                            wci.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
                            if (player.getEventInstance() != null) {
                                player.getEventInstance().leftParty(player);
                            }
                        }
                    } catch (RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                    player.setParty(null);
                }
                break;
            }
        case 3: { // accept invitation
                int partyid = slea.readInt();
                if (c.getPlayer().getParty() == null) {
                    try {
                        party = wci.getParty(partyid);
                        if (party != null) {
                            if (party.getMembers().size() < 6) {
                                wci.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                                player.receivePartyMemberHP();
                                player.updatePartyMemberHP();
                            } else {
                                c.announce(PartyFactory.partyStatusMessage(17));
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "The party you are trying to join does not exist");
                        }
                    } catch (RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                } else {
                    c.getPlayer().dropMessage(5, "You can't join the party as you are already in one");
                }
                break;
            }
        case 4: { // invite
                String name = slea.readMapleAsciiString();
                MapleCharacter invited = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                if (invited != null) {
                    if (invited.getParty() == null) {
                        if (party.getMembers().size() < 6) {
                            invited.getClient().announce(PartyFactory.partyInvite(player));
                        }
                    } else {
                        c.announce(PartyFactory.partyStatusMessage(16));
                    }
                } else {
                    c.announce(PartyFactory.partyStatusMessage(18));
                }
                break;
            }
        case 5: { // expel
                int cid = slea.readInt();
                if (partyplayer.equals(party.getLeader())) {
                    MaplePartyCharacter expelled = party.getMemberById(cid);
                    if (expelled != null && !expelled.getName().equals(partyplayer.getName())) {
                        try {
                            wci.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                            if (player.getEventInstance() != null) {
                                if (expelled.isOnline()) {
                                    player.getEventInstance().disbandParty();
                                }
                            }
                        } catch (RemoteException e) {
                            c.getChannelServer().reconnectWorld();
                        }
                    }
                }
                break;
            }
        case 6:
            int newLeader = slea.readInt();
            MaplePartyCharacter newLeadr = party.getMemberById(newLeader);
            try {
                if (newLeadr != null) {
                    party.setLeader(newLeadr);
                    wci.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newLeadr);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                c.getChannelServer().reconnectWorld();
            }
            break;
        default:
            System.out.println("Unhandled Party function." + operation + "");
            break;
        }
    }

    public static final void DenyPartyRequestHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte action = slea.readByte();
        int fromCID = slea.readInt();
        MapleParty party;
        MapleCharacter player = c.getPlayer();
        WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
        MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterById(fromCID);
        MaplePartyCharacter partyplayer = new MaplePartyCharacter(player);
        if(cfrom != null) {
            switch(action) {
                case 26: //deny
                    cfrom.getClient().announce(PartyFactory.partyStatusMessage(23, c.getPlayer().getName()));
                    break;
                case 27: { // accept invitation
                    int partyid = cfrom.getPartyId();
                    if (c.getPlayer().getParty() == null) {
                        try {
                            party = wci.getParty(partyid);
                            if (party != null) {
                                if (party.getMembers().size() < 6) {
                                    wci.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                                    player.receivePartyMemberHP();
                                    player.updatePartyMemberHP();
                                } else {
                                    c.announce(PartyFactory.partyStatusMessage(17));
                                }
                            } else {
                                c.getPlayer().dropMessage(5, "The party you are trying to join does not exist");
                            }
                        } catch (Exception e) {
                            c.getChannelServer().reconnectWorld();
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "You can't join the party as you are already in one");
                    }
                    break;
                }
            }
        }
    }

    private static final void partyChat(MapleClient c, MapleCharacter player, int type, int[] recipients, String chattext) {
        try {
            if (type == 0) {
                c.getChannelServer().getWorldInterface().buddyChat(recipients, player.getId(), player.getName(), chattext);
            } else if (type == 1 && player.getParty() != null) {
                c.getChannelServer().getWorldInterface().partyChat(player.getParty().getId(), chattext, player.getName());
            } else if (type == 2 && player.getGuildId() > 0) {
                c.getChannelServer().getWorldInterface().guildChat(player.getGuildId(), player.getName(), player.getId(), chattext);
            } else if (type == 3 && player.getGuild() != null) {
                int allianceId = player.getGuild().getAllianceId();
                if (allianceId > 0) {
                    c.getChannelServer().getWorldInterface().allianceMessage(allianceId, PartyFactory.multiChat(player.getName(), chattext, 3), player.getId(), -1);
                }
            }
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
    }

    public static final void PartyChatHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if(System.currentTimeMillis() - c.getPlayer().getMuted() <= 90000)
        c.getPlayer().dropMessage("You have been muted. You must wait " +
        ((90000 - (System.currentTimeMillis() - c.getPlayer().getMuted())) / 1000) + " seconds before talking again.");
        else {
            MapleCharacter player = c.getPlayer();
            int type = slea.readByte(); // 0 for buddys, 1 for partys
            int numRecipients = slea.readByte();
            int recipients[] = new int[numRecipients];
            for (int i = 0; i < numRecipients; i++) {
                recipients[i] = slea.readInt();
            }
            String chattext = slea.readMapleAsciiString();
            if (!CommandProcessor.getInstance().processCommand(c, chattext)) {
                if (c.getPlayer().getWordsTalked().equals(chattext)) {
                    if(c.getPlayer().getSpamCheck() > 1) {
                        c.getPlayer().dropMessage("Spam is currently disallowed.");
                        return;
                    } else {
                        c.getPlayer().setSpamCheck(c.getPlayer().getSpamCheck() + 1);
                        partyChat(c, player, type, recipients, chattext);
                    }
                } else {
                    c.getPlayer().setWordsTalked(chattext);
                    c.getPlayer().setSpamCheck(0);
                    partyChat(c, player, type, recipients, chattext);
                }
            }
        }
    }

    public static final void UseDoorHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        boolean mode = (slea.readByte() == 0); // specifies if backwarp or not, 1 town to target, 0 target to town
        for (MapleMapObject obj : c.getPlayer().getMap().getMapObjects()) {
            if (obj instanceof MapleDoor) {
                MapleDoor door = (MapleDoor) obj;
                if (door.getOwner().getId() == oid) {
                    door.warp(c.getPlayer(), mode);
                    return;
                }
            }
        }
    }

    private static class CharacterIdNameBuddyCapacity extends CharacterNameAndId {
        private int buddyCapacity;

        public CharacterIdNameBuddyCapacity(int id, String name, int buddyCapacity) {
            super(id, name);
            this.buddyCapacity = buddyCapacity;
        }

        public int getBuddyCapacity() {
            return buddyCapacity;
        }
    }

    private static void nextPendingRequest(MapleClient c) {
        CharacterNameAndId pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.announce(BuddyFactory.requestBuddylistAdd(pendingBuddyRequest.getId(), c.getPlayer().getId(), pendingBuddyRequest.getName(), 0, 0));//todo: real offline levels / jobids
        }
    }

    private static CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(String name) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT id, buddyCapacity FROM characters WHERE name LIKE ?");
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        CharacterIdNameBuddyCapacity ret = null;
        if (rs.next()) {
            ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), name, rs.getInt("buddyCapacity"));
        }
        rs.close();
        ps.close();
        return ret;
    }

    public static void BuddyListModifyHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().setSaveBuddies();
        int mode = slea.readByte();
        MapleCharacter player = c.getPlayer();
        WorldChannelInterface worldInterface = c.getChannelServer().getWorldInterface();
        BuddyList buddylist = player.getBuddylist();
        if (mode == 1) { // add
            String addName = slea.readMapleAsciiString();
            String group = slea.readMapleAsciiString();
            if (group.length() > 16 || addName.length() < 4 || addName.length() > 13) {
                return; //hax.
            }
            BuddylistEntry ble = buddylist.get(addName);
            if (ble != null && !ble.isVisible() && group.equals(ble.getGroup())) {
                c.announce(EffectFactory.serverNotice(1, "You already have \"" + ble.getName() + "\" on your Buddylist"));
            } else if (buddylist.isFull() && ble == null) {
                c.announce(EffectFactory.serverNotice(1, "Your buddylist is already full"));
            } else if (ble == null) {
                try {
                    CharacterIdNameBuddyCapacity charWithId = null;
                    int channel;
                    MapleCharacter otherChar = c.getChannelServer().getPlayerStorage().getCharacterByName(addName);
                    if (otherChar != null) {
                        channel = c.getChannel();
                        charWithId = new CharacterIdNameBuddyCapacity(otherChar.getId(), otherChar.getName(), otherChar.getBuddylist().getCapacity());
                    } else {
                        channel = worldInterface.find(addName, false);
                        charWithId = getCharacterIdAndNameFromDatabase(addName);
                    }
                    if (charWithId != null) {
                        BuddyAddResult buddyAddResult = null;
                        if (channel != -1) {
                            ChannelWorldInterface channelInterface = worldInterface.getChannelInterface(channel);
                            buddyAddResult = channelInterface.requestBuddyAdd(addName, c.getChannel(), player.getId(), player.getName());
                        } else {
                            Connection con = DatabaseConnection.getConnection();
                            PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = 0");
                            ps.setInt(1, charWithId.getId());
                            ResultSet rs = ps.executeQuery();
                            if (!rs.next()) {
                                ps.close();
                                rs.close();
                                throw new RuntimeException("Result set expected");
                            } else {
                                if (rs.getInt("buddyCount") >= charWithId.getBuddyCapacity()) {
                                    buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
                                }
                            }
                            rs.close();
                            ps.close();
                            ps = con.prepareStatement("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?");
                            ps.setInt(1, charWithId.getId());
                            ps.setInt(2, player.getId());
                            rs = ps.executeQuery();
                            if (rs.next()) {
                                buddyAddResult = BuddyAddResult.ALREADY_ON_LIST;
                            }
                            rs.close();
                            ps.close();
                        }
                        if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) {
                            c.announce(EffectFactory.serverNotice(1, "\"" + addName + "\"'s Buddylist is full"));
                        } else {
                            int displayChannel = -1;
                            int otherCid = charWithId.getId();
                            if (buddyAddResult == BuddyAddResult.ALREADY_ON_LIST && channel != -1) {
                                displayChannel = channel;
                                notifyRemoteChannel(c, channel, otherCid, BuddyList.BuddyOperation.ADDED);
                            } else if (buddyAddResult != BuddyAddResult.ALREADY_ON_LIST && channel == -1) {
                                Connection con = DatabaseConnection.getConnection();
                                PreparedStatement ps = con.prepareStatement("INSERT INTO buddies (`id`, characterid, `buddyid`, `pending`) VALUES (DEFAULT, ?, ?, 1)");
                                ps.setInt(1, charWithId.getId());
                                ps.setInt(2, player.getId());
                                ps.executeUpdate();
                                ps.close();
                            }
                            buddylist.put(new BuddylistEntry(charWithId.getName(), group, otherCid, displayChannel, true));
                            c.announce(BuddyFactory.updateBuddylist(buddylist.getBuddies()));
                        }
                    } else {
                        c.announce(EffectFactory.serverNotice(1, "A character called \"" + addName + "\" does not exist"));
                    }
                } catch (RemoteException e) {
                } catch (SQLException e) {
                }
            } else {
                ble.changeGroup(group);
                c.announce(BuddyFactory.updateBuddylist(buddylist.getBuddies()));
            }
        } else if (mode == 2) { // accept buddy
            int otherCid = slea.readInt();
            if (!buddylist.isFull()) {
                try {
                    int channel = worldInterface.find(otherCid, false);
                    String otherName = null;
                    MapleCharacter otherChar = c.getChannelServer().getPlayerStorage().getCharacterById(otherCid);
                    if (otherChar == null) {
                        Connection con = DatabaseConnection.getConnection();
                        PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ?");
                        ps.setInt(1, otherCid);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            otherName = rs.getString("name");
                        }
                        rs.close();
                        ps.close();
                    } else {
                        otherName = otherChar.getName();
                    }
                    if (otherName != null) {
                        buddylist.put(new BuddylistEntry(otherName, "Default Group", otherCid, channel, true));
                        c.announce(BuddyFactory.updateBuddylist(buddylist.getBuddies()));
                        notifyRemoteChannel(c, channel, otherCid, BuddyList.BuddyOperation.ADDED);
                    }
                } catch (RemoteException e) {
                } catch (SQLException e) {
                }
            }
            nextPendingRequest(c);
        } else if (mode == 3) { // delete
            int otherCid = slea.readInt();
            if (buddylist.containsVisible(otherCid)) {
                try {
                    notifyRemoteChannel(c, worldInterface.find(otherCid, false), otherCid, BuddyList.BuddyOperation.DELETED);
                } catch (RemoteException e) {
                }
            }
            buddylist.remove(otherCid);
            c.announce(BuddyFactory.updateBuddylist(player.getBuddylist().getBuddies()));
            nextPendingRequest(c);
        }
    }

    private static void notifyRemoteChannel(MapleClient c, int remoteChannel, int otherCid, BuddyOperation operation) throws RemoteException {
        WorldChannelInterface worldInterface = c.getChannelServer().getWorldInterface();
        MapleCharacter player = c.getPlayer();
        if (remoteChannel != -1) {
            ChannelWorldInterface channelInterface = worldInterface.getChannelInterface(remoteChannel);
            channelInterface.buddyChanged(otherCid, player.getId(), player.getName(), c.getChannel(), operation);
        }
    }

    private static final boolean isGuildNameAcceptable(String name) {
        if (name.length() < 3 || name.length() > 12) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            if (!Character.isLowerCase(name.charAt(i)) && !Character.isUpperCase(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static void respawnPlayer(MapleCharacter mc) {
        mc.getMap().broadcastMessage(mc, InterPersonalFactory.removePlayerFromMap(mc.getId()), false);
        mc.getMap().broadcastMessage(mc, InterPersonalFactory.spawnPlayerMapobject(mc), false);
        if (mc.getNoPets() > 0) {
            for (MaplePet pet : mc.getPets()) {
                if (pet != null) {
                    mc.getMap().broadcastMessage(mc, PetFactory.showPet(mc, pet, false, false), false);
                }
            }
        }
    }

    private static class Invited {
        public String name;
        public int gid;
        public long expiration;

        public Invited(String n, int id) {
            name = n.toLowerCase();
            gid = id;
            expiration = System.currentTimeMillis() + 60 * 60 * 1000;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Invited)) {
                return false;
            }
            Invited oth = (Invited) other;
            return (gid == oth.gid && name.equals(oth));
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 83 * hash + (this.name != null ? this.name.hashCode() : 0);
            hash = 83 * hash + this.gid;
            return hash;
        }
    }
    private static List<Invited> invited = new LinkedList<Invited>();
    private static long nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;

    public static final void GuildOperationHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (System.currentTimeMillis() >= nextPruneTime) {
            Iterator<Invited> itr = invited.iterator();
            Invited inv;
            while (itr.hasNext()) {
                inv = itr.next();
                if (System.currentTimeMillis() >= inv.expiration) {
                    itr.remove();
                }
            }
            nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;
        }
        MapleCharacter mc = c.getPlayer();
        byte type = slea.readByte();
        switch (type) {
            case 0x02: //CREATE
                if (mc.getGuildId() > 0 || mc.getMapId() != 200000301) {
                    c.getPlayer().dropMessage(1, "You cannot create a new Guild while in one.");
                    return;
                }
                if (mc.getMeso() < 1500000) { //CREATE_GUILD_COST
                    c.getPlayer().dropMessage(1, "You do not have enough mesos to create a Guild.");
                    return;
                }
                String guildName = slea.readMapleAsciiString();
                if (!isGuildNameAcceptable(guildName)) {
                    c.getPlayer().dropMessage(1, "The Guild name you have chosen is not accepted.");
                    return;
                }
                int gid;
                try {
                    gid = c.getChannelServer().getWorldInterface().createGuild(mc.getId(), guildName);
                } catch (java.rmi.RemoteException re) {
                    System.out.println("RemoteException occurred " + re);
                    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
                    return;
                }
                if (gid == 0) {
                    c.announce(GuildFactory.genericGuildMessage((byte) 0x1c));
                    return;
                }
                mc.gainMeso(-1500000, true, false, true); //CREATE_GUILD_COST
                mc.setGuildId(gid);
                mc.setGuildRank(1);
                mc.saveGuildStatus();
                c.announce(GuildFactory.showGuildInfo(mc));
                c.getPlayer().dropMessage(1, "You have successfully created a Guild.");
                respawnPlayer(mc);
                break;
            case 0x05: //INVITE
                if (mc.getGuildId() <= 0 || mc.getGuildRank() > 2) {
                    return;
                }
                String name = slea.readMapleAsciiString();
                MapleGuildResponse mgr = MapleGuild.sendInvite(c, name);
                if (mgr != null) {
                    c.announce(mgr.getPacket());
                } else {
                    Invited inv = new Invited(name, mc.getGuildId());
                    if (!invited.contains(inv)) {
                        invited.add(inv);
                    }
                }
                break;
            case 0x06: //JOIN
                if (mc.getGuildId() > 0) {
                    System.out.println("[hax] " + mc.getName() + " attempted to join a guild when s/he is already in one.");
                    return;
                }
                gid = slea.readInt();
                int cid = slea.readInt();
                if (cid != mc.getId()) {
                    System.out.println("[hax] " + mc.getName() + " attempted to join a guild with a different character id.");
                    return;
                }
                name = mc.getName().toLowerCase();
                Iterator<Invited> itr = invited.iterator();
                boolean bOnList = false;
                while (itr.hasNext()) {
                    Invited inv = itr.next();
                    if (gid == inv.gid && name.equals(inv.name)) {
                        bOnList = true;
                        itr.remove();
                        break;
                    }
                }
                if (!bOnList) {
                    System.out.println("[hax] " + mc.getName() + " is trying to join a guild that never invited him/her (or that the invitation has expired)");
                    return;
                }
                mc.setGuildId(gid); // joins the guild
                mc.setGuildRank(5); // start at lowest rank
                int s;
                try {
                    s = c.getChannelServer().getWorldInterface().addGuildMember(mc.getMGC());
                } catch (java.rmi.RemoteException e) {
                    System.out.println("RemoteException occurred while attempting to add character to guild " + e);
                    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
                    mc.setGuildId(0);
                    return;
                }
                if (s == 0) {
                    c.getPlayer().dropMessage(1, "The Guild you are trying to join is already full.");
                    mc.setGuildId(0);
                    return;
                }
                c.announce(GuildFactory.showGuildInfo(mc));
                mc.saveGuildStatus(); // update database
                respawnPlayer(mc);
                break;
            case 0x07: //QUIT
                cid = slea.readInt();
                name = slea.readMapleAsciiString();
                if (cid != mc.getId() || !name.equals(mc.getName()) || mc.getGuildId() <= 0) {
                    System.out.println("[hax] " + mc.getName() + " tried to quit guild under the name \"" + name + "\" and current guild id of " + mc.getGuildId() + ".");
                    return;
                }
                try {
                    c.getChannelServer().getWorldInterface().leaveGuild(mc.getMGC());
                } catch (java.rmi.RemoteException re) {
                    System.out.println("RemoteException occurred while attempting to leave guild " + re);
                    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
                    return;
                }
                c.announce(GuildFactory.showGuildInfo(null));
                mc.setGuildId(0);
                mc.saveGuildStatus();
                respawnPlayer(mc);
                break;
            case 0x08: //EXPEL TODO FIX
                cid = slea.readInt();
                name = slea.readMapleAsciiString();
                if (mc.getGuildRank() > 2 || mc.getGuildId() <= 0) {
                    System.out.println("[hax] " + mc.getName() + " is trying to expel without rank 1 or 2.");
                    return;
                }
                try {
                    c.getChannelServer().getWorldInterface().expelMember(mc.getMGC(), name, cid);
                } catch (java.rmi.RemoteException re) {
                    System.out.println("RemoteException occurred while attempting to change rank " + re);
                    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
                    return;
                }
                break;
            case 0x0d: //RANK CHANGE - LEADER
                if (mc.getGuildId() <= 0 || mc.getGuildRank() != 1) {
                    System.out.println("[hax] " + mc.getName() + " tried to change guild rank titles when s/he does not have permission.");
                    return;
                }
                String ranks[] = new String[5];
                for (int i = 0; i < 5; i++) {
                    ranks[i] = slea.readMapleAsciiString();
                }
                try {
                    c.getChannelServer().getWorldInterface().changeRankTitle(mc.getGuildId(), ranks);
                } catch (java.rmi.RemoteException re) {
                    System.out.println("RemoteException occurred " + re);
                    c.announce(EffectFactory.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                    return;
                }
                break;
            case 0x0e: //RANK CHANGE - JR.
                cid = slea.readInt();
                byte newRank = slea.readByte();
                if (mc.getGuildRank() > 2 || (newRank <= 2 && mc.getGuildRank() != 1) || mc.getGuildId() <= 0) {
                    System.out.println("[hax] " + mc.getName() + " is trying to change rank outside of his/her permissions.");
                    return;
                }
                if (newRank <= 1 || newRank > 5) {
                    return;
                }
                try {
                    c.getChannelServer().getWorldInterface().changeRank(mc.getGuildId(), cid, newRank);
                } catch (java.rmi.RemoteException re) {
                    System.out.println("RemoteException occurred while attempting to change rank " + re);
                    c.announce(EffectFactory.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                    return;
                }
                break;
            case 0x0f:
                if (mc.getGuildId() <= 0 || mc.getGuildRank() != 1 || mc.getMapId() != 200000301) {
                    System.out.println("[hax] " + mc.getName() + " tried to change guild emblem without being the guild leader.");
                    return;
                }
                if (mc.getMeso() < 5000000) { //EMBLEM_COST
                    c.announce(EffectFactory.serverNotice(1, "You do not have enough mesos to create a Guild."));
                    return;
                }
                short bg = slea.readShort();
                byte bgcolor = slea.readByte();
                short logo = slea.readShort();
                byte logocolor = slea.readByte();
                try {
                    c.getChannelServer().getWorldInterface().setGuildEmblem(mc.getGuildId(), bg, bgcolor, logo, logocolor);
                } catch (java.rmi.RemoteException re) {
                    System.out.println("RemoteException occurred " + re);
                    c.announce(EffectFactory.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                    return;
                }
                mc.gainMeso(-5000000, true, false, true); //EMBLEM_COST
                respawnPlayer(mc);
                break;
            case 0x10: //notice
                if (mc.getGuildId() <= 0 || mc.getGuildRank() > 2) {
                    System.out.println("[hax] " + mc.getName() + " tried to change guild notice while not in a guild.");
                    return;
                }
                String notice = slea.readMapleAsciiString();
                if (notice.length() > 100) {
                    return;
                }
                try {
                    c.getChannelServer().getWorldInterface().setGuildNotice(mc.getGuildId(), notice);
                } catch (java.rmi.RemoteException re) {
                    System.out.println("RemoteException occurred " + re);
                    c.announce(EffectFactory.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                    return;
                }
                break;
            default:
                System.out.println("Unhandled GUILD_OPERATION packet: \n" + slea.toString());
        }
    }

    public static final void DenyGuildRequestHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
        if (cfrom != null) {
            cfrom.getClient().announce(GuildFactory.denyGuildInvitation(c.getPlayer().getName()));
        }
    }

    private static final String correctLength(String in, int maxSize) {
        return in.length() > maxSize ? in.substring(0, maxSize) : in;
    }

    public static final void BBSOperationHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getGuildId() < 1) {
            return;
        }
        byte mode = slea.readByte();
        int localthreadid = 0;
        switch (mode) {
            case 0:
                boolean bEdit = slea.readByte() == 1;
                if (bEdit) {
                    localthreadid = slea.readInt();
                }
                boolean bNotice = slea.readByte() == 1;
                String title = correctLength(slea.readMapleAsciiString(), 25);
                String text = correctLength(slea.readMapleAsciiString(), 600);
                int icon = slea.readInt();
                if (icon >= 0x64 && icon <= 0x6a) {
                    if (c.getPlayer().getItemQuantity(5290000 + icon - 0x64, false) > 0) {
                        return;
                    }
                } else if (icon < 0 || icon > 3) {
                    return;
                }
                if (!bEdit) {
                    newBBSThread(c, title, text, icon, bNotice);
                } else {
                    editBBSThread(c, title, text, icon, localthreadid);
                }
                break;
            case 1:
                localthreadid = slea.readInt();
                deleteBBSThread(c, localthreadid);
                break;
            case 2:
                listBBSThreads(c, slea.readInt() * 10);
                break;
            case 3: // list thread + reply, followed by id (int)
                localthreadid = slea.readInt();
                displayThread(c, localthreadid);
                break;
            case 4: // reply
                localthreadid = slea.readInt();
                text = correctLength(slea.readMapleAsciiString(), 25);
                newBBSReply(c, localthreadid, text);
                break;
            case 5: // delete reply
                localthreadid = slea.readInt(); // we don't use this
                int replyid = slea.readInt();
                deleteBBSReply(c, replyid);
                break;
            default:
                System.out.println("Unhandled BBS mode: " + slea.toString());
        }
    }

    private static final void listBBSThreads(MapleClient c, int start) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? ORDER BY localthreadid DESC");
            ps.setInt(1, c.getPlayer().getGuildId());
            ResultSet rs = ps.executeQuery();
            c.announce(GuildFactory.bbsThreadList(rs, start));
            rs.close();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private static final void newBBSReply(MapleClient c, int localthreadid, String text) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT threadid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?");
            ps.setInt(1, c.getPlayer().getGuildId());
            ps.setInt(2, localthreadid);
            ResultSet threadRS = ps.executeQuery();
            if (!threadRS.next()) {
                threadRS.close();
                ps.close();
                return;
            }
            int threadid = threadRS.getInt("threadid");
            threadRS.close();
            ps.close();
            ps = con.prepareStatement("INSERT INTO bbs_replies " + "(`threadid`, `postercid`, `timestamp`, `content`) VALUES " + "(?, ?, ?, ?)");
            ps.setInt(1, threadid);
            ps.setInt(2, c.getPlayer().getId());
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, text);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("UPDATE bbs_threads SET replycount = replycount + 1 WHERE threadid = ?");
            ps.setInt(1, threadid);
            ps.executeUpdate();
            ps.close();
            displayThread(c, localthreadid);
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private static final void editBBSThread(MapleClient client, String title, String text, int icon, int localthreadid) {
        MapleCharacter c = client.getPlayer();
        if (c.getGuildId() < 1) {
            return;
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE bbs_threads SET `name` = ?, `timestamp` = ?, " + "`icon` = ?, " + "`startpost` = ? WHERE guildid = ? AND localthreadid = ? AND (postercid = ? OR ?)");
            ps.setString(1, title);
            ps.setLong(2, System.currentTimeMillis());
            ps.setInt(3, icon);
            ps.setString(4, text);
            ps.setInt(5, c.getGuildId());
            ps.setInt(6, localthreadid);
            ps.setInt(7, c.getId());
            ps.setBoolean(8, c.getGuildRank() < 3);
            ps.executeUpdate();
            ps.close();
            displayThread(client, localthreadid);
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private static final void newBBSThread(MapleClient client, String title, String text, int icon, boolean bNotice) {
        MapleCharacter c = client.getPlayer();
        if (c.getGuildId() <= 0) {
            return;
        }
        int nextId = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (!bNotice) {
                ps = con.prepareStatement("SELECT MAX(localthreadid) AS lastLocalId FROM bbs_threads WHERE guildid = ?");
                ps.setInt(1, c.getGuildId());
                ResultSet rs = ps.executeQuery();
                rs.next();
                nextId = rs.getInt("lastLocalId") + 1;
                rs.close();
                ps.close();
            }
            ps = con.prepareStatement("INSERT INTO bbs_threads " + "(`postercid`, `name`, `timestamp`, `icon`, `startpost`, " + "`guildid`, `localthreadid`) " + "VALUES(?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, c.getId());
            ps.setString(2, title);
            ps.setLong(3, System.currentTimeMillis());
            ps.setInt(4, icon);
            ps.setString(5, text);
            ps.setInt(6, c.getGuildId());
            ps.setInt(7, nextId);
            ps.execute();
            ps.close();
            displayThread(client, nextId);
        } catch (SQLException se) {
            se.printStackTrace();
        }

    }

    public static final void deleteBBSThread(MapleClient client, int localthreadid) {
        MapleCharacter mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT threadid, postercid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?");
            ps.setInt(1, mc.getGuildId());
            ps.setInt(2, localthreadid);
            ResultSet threadRS = ps.executeQuery();
            if (!threadRS.next()) {
                threadRS.close();
                ps.close();
                return;
            }
            if (mc.getId() != threadRS.getInt("postercid") && mc.getGuildRank() > 2) {
                threadRS.close();
                ps.close();
                return;
            }
            int threadid = threadRS.getInt("threadid");
            ps.close();
            ps = con.prepareStatement("DELETE FROM bbs_replies WHERE threadid = ?");
            ps.setInt(1, threadid);
            ps.execute();
            ps.close();
            ps = con.prepareStatement("DELETE FROM bbs_threads WHERE threadid = ?");
            ps.setInt(1, threadid);
            ps.execute();
            threadRS.close();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public static final void deleteBBSReply(MapleClient client, int replyid) {
        MapleCharacter mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        int threadid;
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT postercid, threadid FROM bbs_replies WHERE replyid = ?");
            ps.setInt(1, replyid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return;
            }
            if (mc.getId() != rs.getInt("postercid") && mc.getGuildRank() > 2) {
                rs.close();
                ps.close();
                return;
            }
            threadid = rs.getInt("threadid");
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM bbs_replies WHERE replyid = ?");
            ps.setInt(1, replyid);
            ps.execute();
            ps.close();
            ps = con.prepareStatement("UPDATE bbs_threads SET replycount = replycount - 1 WHERE threadid = ?");
            ps.setInt(1, threadid);
            ps.execute();
            ps.close();
            displayThread(client, threadid, false);
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public static final void displayThread(MapleClient client, int threadid) {
        displayThread(client, threadid, true);
    }

    public static final void displayThread(MapleClient client, int threadid, boolean bIsThreadIdLocal) {
        MapleCharacter mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? AND " + (bIsThreadIdLocal ? "local" : "") + "threadid = ?");
            ps.setInt(1, mc.getGuildId());
            ps.setInt(2, threadid);
            ResultSet threadRS = ps.executeQuery();
            if (!threadRS.next()) {
                threadRS.close();
                ps.close();
                return;
            }
            ResultSet repliesRS = null;
            PreparedStatement ps2 = null;
            if (threadRS.getInt("replycount") >= 0) {
                ps2 = con.prepareStatement("SELECT * FROM bbs_replies WHERE threadid = ?");
                ps2.setInt(1, !bIsThreadIdLocal ? threadid : threadRS.getInt("threadid"));
                repliesRS = ps2.executeQuery();
            }
            client.announce(GuildFactory.showThread(bIsThreadIdLocal ? threadid : threadRS.getInt("localthreadid"), threadRS, repliesRS));
            repliesRS.close();
            ps.close();
            if (ps2 != null) {
                ps2.close();
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } catch (RuntimeException re) {//btw we get this everytime for some reason, but replies work!
            re.printStackTrace();
            System.out.println("The number of reply rows does not match the replycount in thread.");
        }
    }

    public static final void MessengerHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        String input;
        byte mode = slea.readByte();
        MapleCharacter player = c.getPlayer();
        WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
        MapleMessenger messenger = player.getMessenger();
        switch (mode) {
            case 0x00:
                if (messenger == null) {
                    int messengerid = slea.readInt();
                    if (messengerid == 0) {
                        try {
                            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
                            messenger = wci.createMessenger(messengerplayer);
                            player.setMessenger(messenger);
                        } catch (RemoteException e) {
                            c.getChannelServer().reconnectWorld();
                        }
                    } else {
                        try {
                            messenger = wci.getMessenger(messengerid);
                            int position = messenger.getLowestPosition();
                            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player, position);
                            if (messenger.getMembers().size() < 3) {
                                player.setMessenger(messenger);
                                wci.joinMessenger(messenger.getId(), messengerplayer, player.getName(), messengerplayer.getChannel());
                            }
                        } catch (RemoteException e) {
                            c.getChannelServer().reconnectWorld();
                        }
                    }
                }
                break;
            case 0x02:
                if (messenger != null) {
                    MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
                    try {
                        wci.leaveMessenger(messenger.getId(), messengerplayer);
                    } catch (RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                    player.setMessenger(null);
                }
                break;
            case 0x03:
                if (messenger.getMembers().size() < 3) {
                    input = slea.readMapleAsciiString();
                    MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(input);
                    if (target != null) {
                        if (target.getMessenger() == null) {
                            target.getClient().announce(InterPersonalFactory.messengerInvite(c.getPlayer().getName(), messenger.getId()));
                            c.announce(InterPersonalFactory.messengerNote(input, 4, 1));
                        } else {
                            c.announce(InterPersonalFactory.messengerChat(player.getName() + " : " + input + " is already using Maple Messenger"));
                        }
                    } else {
                        try {
                            if (c.getChannelServer().getWorldInterface().isConnected(input)) {
                                c.getChannelServer().getWorldInterface().messengerInvite(c.getPlayer().getName(), messenger.getId(), input, c.getChannel());
                            } else {
                                c.announce(InterPersonalFactory.messengerNote(input, 4, 0));
                            }
                        } catch (RemoteException e) {
                            c.getChannelServer().reconnectWorld();
                        }
                    }
                } else {
                    c.announce(InterPersonalFactory.messengerChat(player.getName() + " : You cannot have more than 3 people in the Maple Messenger"));
                }
                break;
            case 0x05:
                String targeted = slea.readMapleAsciiString();
                MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(targeted);
                if (target != null) {
                    if (target.getMessenger() != null) {
                        target.getClient().announce(InterPersonalFactory.messengerNote(player.getName(), 5, 0));
                    }
                } else {
                    try {
                        wci.declineChat(targeted, player.getName());
                    } catch (RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                }
                break;
            case 0x06:
                if (messenger != null) {
                    MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
                    input = slea.readMapleAsciiString();
                    try {
                        wci.messengerChat(messenger.getId(), input, messengerplayer.getName());
                    } catch (RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                }
                break;
        }
    }

    public static final void ReportHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        String victim = slea.readMapleAsciiString();
        slea.readByte();
        String description = slea.readMapleAsciiString();
        if (c.getPlayer().getMeso() > 299) { // you don't actually lose mesos, but it checks
            c.announce(EffectFactory.reportResponse((byte) 2, 10));
        } else {
            return;
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO `reports` (`reporterid`, " +
                    "`victimid`, `victimname`, `reason`, `status`) VALUES (?,(select id from characters where name = ?), ?, ?, 0)");
            ps.setInt(1, c.getPlayer().getId());
            ps.setString(2, victim);
            ps.setString(3, victim);
            ps.setString(4, description);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            System.out.println("Error updating report log.");
            e.printStackTrace();
        }
        c.getChannelServer().broadcastGMPacket(EffectFactory.serverNotice(6, victim + " was reported for: " + description));
    }

    public static final void FamilyAddHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        String toAdd = slea.readMapleAsciiString();
        MapleCharacter addChr = c.getChannelServer().getPlayerStorage().getCharacterByName(toAdd);
        if (addChr != null) {
            addChr.getClient().announce(FamilyFactory.sendFamilyInvite(c.getPlayer().getId(), toAdd));
            c.getPlayer().dropMessage("The invite has been sent.");
        } else {
            c.getPlayer().dropMessage("The player cannot be found!");
        }
        c.announce(IntraPersonalFactory.enableActions());
    }

    public static final void FamilyOperationHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int[] repCost = {3, 5, 7, 8, 10, 12, 15, 20, 25, 40, 50};
        final int type = slea.readInt();
        if (c.getPlayer().getFamily().getReputation() > repCost[type]) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
            if (type == 0 || type == 1) {
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
                if (victim != null) {
                    if (type == 0) {
                        c.getPlayer().changeMap(victim.getMap(), victim.getMap().getPortal(0));
                    } else {
                        victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().getPortal(0));
                    }
                } else {
                    return;
                }
            } else {
                int erate = type == 3 ? 150 : (type == 4 || type == 6 || type == 8 || type == 10 ? 200 : 100);
                int drate = type == 2 ? 150 : (type == 4 || type == 5 || type == 7 || type == 9 ? 200 : 100);
                if (type > 8) {
                } else {
                    c.announce(FamilyFactory.useRep(drate == 100 ? 2 : (erate == 100 ? 3 : 4), type, erate, drate, ((type > 5 || type == 4) ? 2 : 1) * 15 * 60 * 1000));
                }
            }
            c.getPlayer().getFamily().gainReputation(repCost[type]);
        }
    }

    public static final void AllianceOperationHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleAlliance alliance = null;
        if (c.getPlayer().getGuild() != null && c.getPlayer().getGuild().getAllianceId() > 0) {
            try {
                alliance = c.getChannelServer().getWorldInterface().getAlliance(c.getPlayer().getGuild().getAllianceId());
            } catch (RemoteException re) {
                c.getChannelServer().reconnectWorld();
            }
        }
        if (alliance == null) {
            c.getPlayer().dropMessage("You are not in an alliance.");
            c.announce(IntraPersonalFactory.enableActions());
            return;
        } else if (c.getPlayer().getMGC().getAllianceRank() > 2 || !alliance.getGuilds().contains(c.getPlayer().getGuildId())) {
            c.announce(IntraPersonalFactory.enableActions());
            return;
        }
        try {
            switch (slea.readByte()) {
                case 0x01:
                    c.getChannelServer().getWorldInterface().allianceMessage(alliance.getId(), GuildFactory.sendShowInfo(c.getPlayer().getGuild().getAllianceId(), c.getPlayer().getId()), -1, -1);
                    break;
                case 0x02: { // LEAVE, DOESN'T WORK
                    if (c.getPlayer().getGuild().getAllianceId() == 0 || c.getPlayer().getGuildId() < 1 || c.getPlayer().getGuildRank() != 1) {
                        return;
                    }
                    c.getChannelServer().getWorldInterface().allianceMessage(alliance.getId(), GuildFactory.sendChangeGuild(c.getPlayer().getGuildId(), c.getPlayer().getId(), c.getPlayer().getGuildId(), 2), -1, -1);
                    break;
                }
                case 0x03: // SEND, DETECTED, DOESN'T WORK
                    String charName = slea.readMapleAsciiString();
                    int channel = c.getChannelServer().getWorldInterface().find(charName, false);
                    if (channel == -1) {
                        c.getPlayer().dropMessage("The player is not online.");
                    } else {
                        MapleCharacter victim = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(charName);
                        if (victim.getGuildId() == 0) {
                            c.getPlayer().dropMessage("The person you are trying to invite does not have a guild.");
                        } else if (victim.getGuildRank() != 1) {
                            c.getPlayer().dropMessage("The player is not the leader of his/her guild.");
                        } else {
                            c.getChannelServer().getWorldInterface().allianceMessage(alliance.getId(), GuildFactory.sendInvitation(c.getPlayer().getGuild().getAllianceId(), c.getPlayer().getId(), slea.readMapleAsciiString()), -1, -1);
                        }
                    }
                    break;
                case 0x04: { // IDK
                    int guildid = slea.readInt();
                    if (c.getPlayer().getGuild().getAllianceId() != 0 || c.getPlayer().getGuildRank() != 1 || c.getPlayer().getGuildId() < 1) {
                        return;
                    }
                    c.getChannelServer().getWorldInterface().allianceMessage(alliance.getId(), GuildFactory.sendChangeGuild(guildid, c.getPlayer().getId(), c.getPlayer().getGuildId(), 0), -1, -1);
                    break;
                }
                case 0x06: { // EXPEL, DOESN'T WORK
                    int guildid = slea.readInt();
                    int allianceid = slea.readInt();
                    if (c.getPlayer().getGuild().getAllianceId() == 0 || c.getPlayer().getGuild().getAllianceId() != allianceid) {
                        return;
                    }
                    c.getChannelServer().getWorldInterface().allianceMessage(alliance.getId(), GuildFactory.sendChangeGuild(allianceid, c.getPlayer().getId(), guildid, 1), -1, -1);
                    break;
                }
                case 0x07: { // Change Alliance Leader
                    if (c.getPlayer().getGuild().getAllianceId() == 0 || c.getPlayer().getGuildId() < 1) {
                        return;
                    }
                    c.getChannelServer().getWorldInterface().allianceMessage(alliance.getId(), GuildFactory.sendChangeLeader(c.getPlayer().getGuild().getAllianceId(), c.getPlayer().getId(), slea.readInt()), -1, -1);
                    break;
                }
                case 0x08: // WORKS, DOESN'T SAVE
                    String ranks[] = new String[5];
                    for (int i = 0; i < 5; i++) {
                        ranks[i] = slea.readMapleAsciiString();
                    }
                    c.getChannelServer().getWorldInterface().setAllianceRanks(alliance.getId(), ranks);
                    c.getChannelServer().getWorldInterface().allianceMessage(alliance.getId(), GuildFactory.changeAllianceRankTitle(alliance.getId(), ranks), -1, -1);
                    break;
                case 0x09: { // CHANGE RANK, DOESN'T WORK?
                    int int1 = slea.readInt();
                    byte byte1 = slea.readByte();
                    c.getChannelServer().getWorldInterface().allianceMessage(alliance.getId(), GuildFactory.sendChangeRank(c.getPlayer().getGuild().getAllianceId(), c.getPlayer().getId(), int1, byte1), -1, -1);
                    break;
                }
                case 0x0A: // WORKS, DOESN'T SAVE
                    String notice = slea.readMapleAsciiString();
                    c.getChannelServer().getWorldInterface().setAllianceNotice(alliance.getId(), notice);
                    c.getChannelServer().getWorldInterface().allianceMessage(alliance.getId(), GuildFactory.allianceNotice(alliance.getId(), notice), -1, -1);
                    break;
                default:
                    c.getPlayer().dropMessage("Feature not available");
            }
            alliance.saveToDB();
        } catch (RemoteException r) {
            c.getChannelServer().reconnectWorld();
        }
    }

    public static final void FamilyAcceptHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int inviterId = slea.readInt();
        MapleCharacter inviter = ChannelServer.getCharacterFromAllServers(inviterId);
        if (inviter != null) {
            inviter.getClient().announce(FamilyFactory.sendFamilyJoinResponse(true, c.getPlayer().getName()));
        }
        c.announce(FamilyFactory.sendFamilyMessage());
    }

    public static void PartySearchStartHandler(SeekableLittleEndianAccessor slea, MapleClient c) {
        int min = slea.readInt();
        int max = slea.readInt();
        slea.readInt(); // members
        int jobs = slea.readInt();
        MapleCharacter chr = c.getPlayer();
        MapleMap map = chr.getMap();
        Collection<MapleMapObject> mapobjs = map.getAllObjects(MapleMapObjectType.PLAYER);
        for (MapleMapObject mapobj : mapobjs) {
            if (chr.getParty().getMembers().size() > 5) {
                break;
            }
            if (mapobj instanceof MapleCharacter) {
                MapleCharacter tchar = (MapleCharacter) mapobj;
                int charlvl = tchar.getLevel();
                if (charlvl >= min && charlvl <= max && isValidJob(tchar.getJob(), jobs)) {
                    if (c.getPlayer().getParty() == null) {
                        try {
                            WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
                            MapleParty party = c.getPlayer().getParty();
                            int partyid = party.getId();
                            party = wci.getParty(partyid);
                            if (party != null) {
                                if (party.getMembers().size() < 6) {
                                    MaplePartyCharacter partyplayer = new MaplePartyCharacter(tchar);
                                    wci.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                                    c.getPlayer().receivePartyMemberHP();
                                    c.getPlayer().updatePartyMemberHP();
                                } else {
                                    c.announce(PartyFactory.partyStatusMessage(17));
                                }
                            }
                        } catch (Exception e) {
                            c.getChannelServer().reconnectWorld();
                        }
                    }
                }
            }
        }
    }

    private static boolean isValidJob(int thejob, int jobs) {
        int jobid = thejob;
        if (jobid == 0) {
            return ((jobs & 2) > 0);
        } else if (jobid == 100) {
            return ((jobs & 4) > 0);
        } else if (jobid > 100 && jobid < 113) {
            return ((jobs & 8) > 0);
        } else if (jobid > 110 && jobid < 123) {
            return ((jobs & 16) > 0);
        } else if (jobid > 120 && jobid < 133) {
            return ((jobs & 32) > 0);
        } else if (jobid == 200) {
            return ((jobs & 64) > 0);
        } else if (jobid > 209 && jobid < 213) {
            return ((jobs & 128) > 0);
        } else if (jobid > 219 && jobid < 223) {
            return ((jobs & 256) > 0);
        } else if (jobid > 229 && jobid < 233) {
            return ((jobs & 512) > 0);
        } else if (jobid == 500) {
            return ((jobs & 1024) > 0);
        } else if (jobid > 509 && jobid < 513) {
            return ((jobs & 2048) > 0);
        } else if (jobid > 519 && jobid < 523) {
            return ((jobs & 4096) > 0);
        } else if (jobid == 400) {
            return ((jobs & 8192) > 0);
        } else if (jobid > 400 && jobid < 413) {
            return ((jobs & 16384) > 0);
        } else if (jobid > 419 && jobid < 423) {
            return ((jobs & 32768) > 0);
        } else if (jobid == 300) {
            return ((jobs & 65536) > 0);
        } else if (jobid > 300 && jobid < 313) {
            return ((jobs & 131072) > 0);
        } else if (jobid > 319 && jobid < 323) {
            return ((jobs & 262144) > 0);
        }
        return false;
    }
}
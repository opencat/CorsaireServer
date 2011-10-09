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

package scripting.npc;

import java.util.Map;
import javax.script.Invocable;
import client.MapleClient;
import client.MapleCharacter;
import java.lang.reflect.UndeclaredThrowableException;
import scripting.AbstractScriptManager;
import java.util.HashMap;
import tools.factory.IntraPersonalFactory;

/**
 * @name        NPCScriptManager
 * @author      Matze
 *              Modified by x711Li
 */
public class NPCScriptManager extends AbstractScriptManager {
    private Map<MapleClient, NPCConversationManager> cms = new HashMap<MapleClient, NPCConversationManager>();
    private Map<MapleClient, NPCScript> scripts = new HashMap<MapleClient, NPCScript>();
    private static NPCScriptManager instance = new NPCScriptManager();

    public synchronized static NPCScriptManager getInstance() {
        return instance;
    }

    public void start(MapleClient c, int npc) {
        start(c, npc, "" + npc, c.getPlayer(), -1);
    }

    public void start(MapleClient c, int npc, int item) {
        start(c, npc, "" + npc, c.getPlayer(), item);
    }

    public void start(MapleClient c, int npc, String filename, MapleCharacter chr, int item) {
        try {
            NPCConversationManager cm = new NPCConversationManager(c, npc, -1);
            if (npc == 1202000) {
                action(c, (byte) 1, (byte) 0, 0);
            }
            if (cms.containsKey(c)) {
                dispose(c);
                return;
            }
            cms.put(c, cm);
            Invocable iv = null;
            if (filename != null) {
                iv = getInvocable("npc/" + filename + ".js", c);
            }
            if (iv == null) {
                iv = getInvocable("npc/" + npc + ".js", c);
            }
            if (iv == null || NPCScriptManager.getInstance() == null) {
                dispose(c);
                notice(c, npc);
                return;
            }
            engine.put("cm", cm);
            NPCScript ns = iv.getInterface(NPCScript.class);
            scripts.put(c, ns);
            if (item > 0 && chr != null) {
                ns.start(chr, item);
            } else if (chr == null) {
                ns.start();
            } else {
                ns.start(chr);
            }
        } catch (UndeclaredThrowableException ute) {
            ute.printStackTrace();
            dispose(c);
            cms.remove(c);
            notice(c, npc);
        } catch (Exception e) {
            e.printStackTrace();
            dispose(c);
            cms.remove(c);
            notice(c, npc);
        }
    }

    public void action(MapleClient c, byte mode, byte type, int selection) {
        NPCScript ns = scripts.get(c);
        if (ns != null) {
            try {
                ns.action(mode, type, selection);
            } catch (UndeclaredThrowableException ute) {
                ute.printStackTrace();
                dispose(c);
                notice(c, getCM(c).getNpc());
            } catch (Exception e) {
                e.printStackTrace();
                dispose(c);
                notice(c, getCM(c).getNpc());
            }
        }
    }

    public void dispose(NPCConversationManager cm) {
        MapleClient client = cm.getClient();
        cm.getPlayer().setNpcId(-1);
        cms.remove(client);
        scripts.remove(client);
        resetContext("npc/" + cm.getNpc() + ".js", cm.getClient());
        cm = null;
    }

    public void dispose(MapleClient c) {
        if (cms.get(c) != null) {
            dispose(cms.get(c));
        }
        c.announce(IntraPersonalFactory.enableActions());
    }

    public NPCConversationManager getCM(MapleClient c) {
        return cms.get(c);
    }
}

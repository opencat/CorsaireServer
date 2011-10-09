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

package scripting;

import java.io.File;
import java.io.FileReader;
import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import client.MapleClient;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import server.maps.MapleReactor;

/**
 * @name        AbstractCachedScriptManager
 * @author      x711Li
 */
public abstract class AbstractCachedScriptManager {
    private Map<String, AbstractCachedScript> scripts = new HashMap<String, AbstractCachedScript>();
    private ScriptEngineFactory sef;

    protected AbstractCachedScriptManager() {
        ScriptEngineManager sem = new ScriptEngineManager();
        sef = sem.getEngineByName("javascript").getFactory();
    }

    public AbstractCachedScript load(String scriptName) {
        File scriptFile = new File("scripts/" + scriptName + ".js");
        if (!scriptFile.exists()) {
            return null;
        }
        FileReader fr = null;
        ScriptEngine scripteng = sef.getScriptEngine();
        try {
            fr = new FileReader(scriptFile);
            CompiledScript compiled = ((Compilable) scripteng).compile(fr);
            compiled.eval();
        } catch (UndeclaredThrowableException ute) {
            ute.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    System.err.println("ERROR CLOSING" + e);
                }
            }
        }
        AbstractCachedScript script = ((Invocable) scripteng).getInterface(AbstractCachedScript.class);
        scripts.put(scriptName, script);
        return script;
    }

    public void start(MapleClient c, MapleReactor reactor, String scriptName) {
        if (scripts.containsKey(scriptName)) {
            scripts.get(scriptName).start(new AbstractCachedScriptMethods(c, reactor));
            return;
        }
        AbstractCachedScript script = load(scriptName);
        if (script != null) {
            script.start(new AbstractCachedScriptMethods(c, reactor));
        }
    }

    public void end(MapleClient c, String scriptName) {
        if (scripts.containsKey(scriptName)) {
            scripts.get(scriptName).end(new AbstractCachedScriptMethods(c, null));
            return;
        }
        AbstractCachedScript script = load(scriptName);
        if (script != null) {
            script.end(new AbstractCachedScriptMethods(c, null));
        }
    }

    public void clearScripts() {
        scripts.clear();
    }
}
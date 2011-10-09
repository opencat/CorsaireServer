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

package tools;

import java.io.FileWriter;
import java.io.File;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;

/**
 * @name        PrimitiveLogger
 * @author      Simon
 *              Modified by x711Li
 */
public class PrimitiveLogger {
    public synchronized static void log(String filename, String message) {
        try {
            File file = new File(filename);
            file.createNewFile(); // only creates if it's not there already
            FileWriter fstream = new FileWriter(filename, true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(message + "\n");
            out.close();
            fstream.close();
        } catch (Exception e) {
            e.printStackTrace(); // try -> catch it! if it's just caught normally then it'll be logged during this... infinite loop
        }
    }

    public static void logTime(String filename, String message) {
       Date now = new Date();
       log(filename, DateFormat.getDateTimeInstance().format(now) + ":" + message);
    }

    public static void logException(String filename, String header, Exception e) {
       log(filename, header + "\n" + getStackTrace(e));
    }

    public static void logException(String filename, String header, Throwable t) {
       log(filename, header + "\n" + getStackTrace(t));
    }

    private static String getStackTrace(Exception e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            pw.close();
            sw.close();
            return sw.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static String getStackTrace(Throwable t) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw, true);
            t.printStackTrace(pw);
            pw.flush();
            sw.flush();
            pw.close();
            sw.close();
            return sw.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}

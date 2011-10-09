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

import java.security.SecureRandom;
import java.util.Random;

/**
 * @name        Randomizer
 * @author      HalfDemi
 */
public class Randomizer {
    private static Randomizer instance = new Randomizer();
    private static SecureRandom secureRandom;
    private static Random rand;
    private static int callCount;

    public static Randomizer getInstance() {
        return instance;
    }

    private Randomizer() {
        secureRandom = new SecureRandom();
        rand = new Random(secureRandom.nextLong());
        callCount = 0;
    }

    private static void callRandom() {
        if (callCount > 9) {
            secureRandom.setSeed(rand.nextLong());
            rand.setSeed(secureRandom.nextLong());
        } else {
            callCount++;
        }
    }

    public static final boolean nextBoolean() {
        return rand.nextBoolean();
    }

    public static final int nextInt() {
        return rand.nextInt();
    }

    public static final int nextInt(int i) {
        callRandom();
        return rand.nextInt(i);
    }

    public static final double nextDouble() {
        return rand.nextDouble();
    }

    public static final int rand(final int lbound, final int ubound) {
        return (int) ((rand.nextDouble() * (ubound - lbound + 1)) + lbound);
    }
    
}

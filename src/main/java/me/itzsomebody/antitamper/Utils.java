/*
 * Copyright (C) 2018 ItzSomebody
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package me.itzsomebody.antitamper;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class Utils {
    private static final char[] ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789".toCharArray();

    public static String encrypt(String msg, String className, String methodName, int cpSize) {
        int key1 = className.hashCode();
        int key2 = methodName.hashCode();
        char[] chars = msg.toCharArray();
        char[] encrypted = new char[chars.length];

        for (int i = 0; i < encrypted.length; i++) {
            switch (i % 2) {
                case 0:
                    encrypted[i] = (char) (cpSize ^ key1 ^ chars[i]);
                    break;
                case 1:
                    encrypted[i] = (char) (cpSize ^ key2 ^ chars[i]);
                    break;
            }
        }

        return new String(encrypted);
    }

    public static boolean hasInstructions(MethodNode methodNode) {
        return methodNode.instructions != null && methodNode.instructions.size() != 0;
    }

    public static boolean isString(AbstractInsnNode insn) {
        return insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String;
    }

    public static String randomClassName(List<String> classNames) {
        String first = classNames.get(randomInt(classNames.size()));
        String second = classNames.get(randomInt(classNames.size()));

        return first + '$' + second.substring(second.lastIndexOf("/") + 1);
    }

    public static String randomString() {
        int length = randomInt(8) + 8;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(ALPHA[randomInt(ALPHA.length)]);
        }

        return sb.toString();
    }

    public static int randomInt(int bounds) {
        return ThreadLocalRandom.current().nextInt(bounds);
    }
}

package me.itzsomebody.antitamper;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
    public static final char[] ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789".toCharArray();

    public static String encrypt(String msg, String className, String methodName, int cpSize) {
        int key1 = className.hashCode();
        int key2 = methodName.hashCode();
        char[] chars = msg.toCharArray();
        char[] encrypted = new char[chars.length];

        for (int i = 0; i < encrypted.length; i++) {
            switch (i % 5) {
                case 0:
                    encrypted[i] = (char) ((chars[i] ^ cpSize) << 4);
                    break;
                case 1:
                    encrypted[i] = (char) (cpSize ^ key1 ^ chars[i]);
                    break;
                case 2:
                    encrypted[i] = (char) (cpSize ^ key2 ^ chars[i]);
                    break;
                case 3:
                    encrypted[i] = (char) (cpSize ^ chars[i]);
                    break;
                case 4:
                    encrypted[i] = (char) ((chars[i] ^ cpSize) << 2);
                    break;
            }
        }

        return new String(encrypted);
    }

    public static String randomClassName(List<String> classNames) {
        String first = classNames.get(randomInt(classNames.size()));
        String second = classNames.get(randomInt(classNames.size()));

        return first + '$' + second.substring(second.lastIndexOf("/") + 1, second.length());
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

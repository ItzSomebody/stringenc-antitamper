package me.itzsomebody.antitamper.templates;

import sun.misc.SharedSecrets;

public class StringEncryption {
    private static int[] indexGrab = new int[1];
    private static int one;

    public static String decrypt(Object msg) throws Throwable {
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        int magicKey1 = SharedSecrets.getJavaLangAccess().getConstantPool(Class.forName(stes[indexGrab[one - 1]].getClassName())).getSize();
        int magicKey2 = stes[indexGrab[one - 1]].getClassName().hashCode();
        int magicKey3 = stes[indexGrab[one - 1]].getMethodName().hashCode();
        String castedMsg = (String) msg;
        char[] decrypted = new char[castedMsg.length()];
        char[] encrypted = castedMsg.toCharArray();
        int arrayLength = castedMsg.length();

        int i = one >> indexGrab[one - 1] >> one; // 0
        while (true) {
            if (i >= arrayLength) {
                break;
            }

            switch (i % 2) {
                case 0:
                    decrypted[i] = (char) (encrypted[i] ^ magicKey2 ^ magicKey1);
                    break;
                case 1:
                    decrypted[i] = (char) (encrypted[i] ^ magicKey3 ^ magicKey1);
                    break;
            }

            i++;
        }

        return new String(decrypted);
    }

    static {
        StringEncryption.one = 1;
        indexGrab[0] = 2;
    }
}

package com.itsonlybinary.lcdbot;

/**
 * Created by ItsOnlyBinary
 */
public class Util {

    public static boolean isValidInt(String intString) {
        if (intString.length() == 0) return false;
        for (int i = 0; i < intString.length(); i++) {
            char c = intString.charAt(i);
            if ((c < 0x30 && (c == 0x2d && i != 0)) || c > 0x39) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isValidInt(String intString, int min, int max) {
        if (isValidInt(intString)) {
            int testInt = Integer.parseInt(intString);
            if (testInt >= min && testInt <= max) {
                return true;
            }
        }
        return false;
    }
}

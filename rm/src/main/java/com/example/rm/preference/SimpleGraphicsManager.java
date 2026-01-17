package com.example.rm.preference;

public final class SimpleGraphicsManager {

    private static volatile boolean einkMode = false;

    private SimpleGraphicsManager() {}

    public static boolean isEinkMode() {
        return einkMode;
    }

    public static void setEinkMode(boolean enabled) {
        einkMode = enabled;
    }
}

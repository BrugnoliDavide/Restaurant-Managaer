package com.example.rm.preference;

public final class DemoModeManager {

    private DemoModeManager(){

    }

    private static volatile boolean demoMode = false;

    public static boolean isDemoMode() {
        return demoMode;
    }

    public static void setDemoMode(boolean enabled) {
        demoMode = enabled;
    }
}

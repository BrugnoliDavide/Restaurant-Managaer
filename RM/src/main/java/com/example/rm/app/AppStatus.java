package com.example.rm.app;

public final class AppStatus {

    private static boolean dbConnectionOk = false;

    private AppStatus() {}

    public static boolean isDbConnectionOk() {
        return dbConnectionOk;
    }

    public static void setDbConnectionOk(boolean ok) {
        dbConnectionOk = ok;
    }
}

package com.example.rm.controller;

import com.example.rm.preference.KitchenPreferences;

public interface KitchenPreferencesUseCase {
    KitchenPreferences load(String username);
    boolean save(KitchenPreferences preferences);
    boolean reset(String username);
}

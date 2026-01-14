package com.example.rm.dao;

import com.example.rm.preference.KitchenPreferences;

public interface KitchenPreferencesDAO {

    KitchenPreferences loadByUsername(String username);

    boolean save(KitchenPreferences preferences);

    boolean deleteByUsername(String username);
}

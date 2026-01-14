package com.example.rm.dao;

import com.example.rm.preference.KitchenPreferences;
import com.example.rm.service.DatabaseService;

public class DatabaseKitchenPreferencesDAO implements KitchenPreferencesDAO {

    @Override
    public KitchenPreferences loadByUsername(String username) {
        // Usa i metodi esistenti del tuo DatabaseService
        return DatabaseService.getKitchenPreferences(username);
    }

    @Override
    public boolean save(KitchenPreferences preferences) {
        return DatabaseService.saveKitchenPreferences(preferences);
    }

    @Override
    public boolean deleteByUsername(String username) {
        return DatabaseService.deleteKitchenPreferences(username);
    }
}

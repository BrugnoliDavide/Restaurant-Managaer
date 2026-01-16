package com.example.rm.dao;

import com.example.rm.preference.DemoModeManager;
import com.example.rm.preference.KitchenPreferences;
import com.example.rm.service.DatabaseService;

import java.util.logging.Level;

import static com.example.rm.view.KitchenController.logger;

public class DatabaseKitchenPreferencesDAO implements KitchenPreferencesDAO {

    @Override
    public KitchenPreferences loadByUsername(String username) {
        // Usa i metodi esistenti del tuo DatabaseService
        return DatabaseService.getKitchenPreferences(username);
    }

    @Override
    public boolean save(KitchenPreferences preferences) {
        if (DemoModeManager.isDemoMode()) {
            logger.log(Level.INFO,"modalità DEMO attiva: preferenze cucina saranno valide fino alla chiusura del sistema");
            return true;
        } else {
            return DatabaseService.saveKitchenPreferences(preferences);
        }
    }

    @Override
    public boolean deleteByUsername(String username) {
        return DatabaseService.deleteKitchenPreferences(username);
    }
}

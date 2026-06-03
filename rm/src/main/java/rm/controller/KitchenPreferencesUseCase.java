package rm.controller;

import rm.preference.KitchenPreferences;

public interface KitchenPreferencesUseCase {
    KitchenPreferences load(String username);
    boolean save(KitchenPreferences preferences);
    boolean reset(String username);
}

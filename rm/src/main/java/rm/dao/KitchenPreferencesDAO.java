package rm.dao;

import rm.preference.KitchenPreferences;

public interface KitchenPreferencesDAO {

    KitchenPreferences loadByUsername(String username);

    boolean save(KitchenPreferences preferences);

    boolean deleteByUsername(String username);
}

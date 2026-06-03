package rm.controller;

import rm.dao.KitchenPreferencesDAO;
import rm.dao.DatabaseKitchenPreferencesDAO;
import rm.preference.KitchenPreferences;

public class KitchenPreferencesController implements KitchenPreferencesUseCase {

    private final KitchenPreferencesDAO dao;

    // costruttore default usato in produzione
    public KitchenPreferencesController() {
        this.dao = new DatabaseKitchenPreferencesDAO();
    }

    // costruttore per test / injection
    public KitchenPreferencesController(KitchenPreferencesDAO dao) {
        this.dao = dao;
    }

    @Override
    public KitchenPreferences load(String username) {
        return dao.loadByUsername(username);
    }

    @Override
    public boolean save(KitchenPreferences preferences) {
        return dao.save(preferences);
    }

    @Override
    public boolean reset(String username) {
        return dao.deleteByUsername(username);
    }
}

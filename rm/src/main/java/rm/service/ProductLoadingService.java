package rm.service;

import rm.controller.MenuUseCase;
import rm.model.MenuProduct;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service asincrono per il caricamento dei prodotti del menu.
 * Gestisce automaticamente il ciclo di vita del thread.
 */
public class ProductLoadingService extends Service<List<MenuProduct>> {

    private static final Logger logger = Logger.getLogger(ProductLoadingService.class.getName());
    private final MenuUseCase menuUseCase;

    public ProductLoadingService(MenuUseCase menuUseCase) {
        this.menuUseCase = menuUseCase;
    }

    @Override
    protected Task<List<MenuProduct>> createTask() {
        return new Task<>() {
            @Override
            protected List<MenuProduct> call() throws Exception {
                logger.info("Caricamento prodotti in background...");

                try {
                    return menuUseCase.loadAllProducts();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Errore caricamento prodotti", e);
                    return Collections.emptyList();
                }
            }
        };
    }
}
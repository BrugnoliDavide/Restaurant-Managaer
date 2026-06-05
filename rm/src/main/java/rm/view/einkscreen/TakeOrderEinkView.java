package rm.view.einkscreen;

import rm.view.TakeOrderViewCallback;
import rm.view.View;
import rm.view.TakeOrderController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
public class TakeOrderEinkView implements View, TakeOrderViewCallback {

    private final Parent root;
    private final TakeOrderController controller;
    public final int numeroTavolo;

    public TakeOrderEinkView(int numeroTavolo) {
        this.numeroTavolo = numeroTavolo;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/einkBN/TakeOrderView.fxml"));
            this.root = loader.load();
            this.controller = loader.getController();
            controller.init(numeroTavolo, this);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Errore caricamento TakeOrderEinkView.fxml", e);
        }
    }

    @Override
    public Parent getRoot() { return root; }

    @Override
    public void onProductsLoaded(int count) { /* noop o log */ }

    @Override
    public void onCartUpdated(int totalItems) { /* noop o log */ }

    @Override
    public void onOrderSuccess() { /* noop o log */ }

    @Override
    public void onOrderFailure(String message) { /* noop o log */ }
}
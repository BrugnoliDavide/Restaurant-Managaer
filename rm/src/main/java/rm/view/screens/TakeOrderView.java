package rm.view.screens;

import rm.view.TakeOrderViewCallback;
import rm.view.View;
import rm.view.TakeOrderController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class TakeOrderView implements View, TakeOrderViewCallback {

    private final Parent root;
    private final TakeOrderController controller;
    public final int numeroTavolo;

    public TakeOrderView(int numeroTavolo) {
        this.numeroTavolo = numeroTavolo;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/TakeOrderView.fxml"));
            this.root = loader.load();
            this.controller = loader.getController();
            controller.init(numeroTavolo, this);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Errore caricamento TakeOrderView.fxml", e);
        }
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    /** Callback: prodotti caricati con successo. */
    public void onProductsLoaded(int count) {
        // La view viene notificata. Il rendering reale
        // è già eseguito dal controller (che ha i @FXML).
        // Qui la view potrebbe loggare, aggiornare un badge, ecc.
    }

    /** Callback: il carrello è stato modificato. */
    public void onCartUpdated(int totalItems) {
        // Idem: notifica pura. Il controller ha già aggiornato
        // btnSend internamente prima di invocare questo metodo.
    }

    /** Callback: ordine creato con successo. */
    public void onOrderSuccess() {
        // La view riceve la notifica. L'alert è mostrato
        // dal controller che ha accesso allo Stage.
    }

    /** Callback: ordine fallito. */
    public void onOrderFailure(String message) {
        // Notifica di fallimento.
    }
}
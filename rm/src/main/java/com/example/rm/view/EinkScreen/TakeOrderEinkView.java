package com.example.rm.view.EinkScreen;

import com.example.rm.view.View;
import com.example.rm.view.TakeOrderController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class TakeOrderEinkView implements View {

    private final Parent root;

    public final int numeroTavolo;

    public TakeOrderEinkView(int numeroTavolo) {
        this.numeroTavolo = numeroTavolo;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/einkBN//TakeOrderView.fxml")
            );
            this.root = loader.load();

            TakeOrderController controller = loader.getController();
            controller.init(numeroTavolo);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Errore caricamento TakeOrderEinkView.fxml", e
            );
        }
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
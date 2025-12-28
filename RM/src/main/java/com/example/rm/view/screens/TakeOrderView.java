package com.example.rm.view.screens;

import com.example.rm.view.View;
import com.example.rm.view.TakeOrderController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class TakeOrderView implements View {

    private final Parent root;
    private final int numeroTavolo;

    public TakeOrderView(int numeroTavolo) {
        this.numeroTavolo = numeroTavolo;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/TakeOrderView.fxml")
            );
            this.root = loader.load();

            TakeOrderController controller = loader.getController();
            controller.init(numeroTavolo);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Errore caricamento TakeOrderView.fxml", e
            );
        }
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
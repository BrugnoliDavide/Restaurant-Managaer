package com.example.RM.view.screens;

import com.example.RM.view.View;
import com.example.RM.view.TakeOrderController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class TakeOrderView implements View {

    private final int numeroTavolo;

    public TakeOrderView(int numeroTavolo) {
        this.numeroTavolo = numeroTavolo;
    }

    @Override
    public Parent getRoot() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/TakeOrderView.fxml")
            );
            Parent root = loader.load();

            TakeOrderController controller = loader.getController();
            controller.init(numeroTavolo);

            return root;

        } catch (Exception e) {
            throw new RuntimeException("Errore caricamento TakeOrderView", e);
        }
    }
}

package com.example.demo.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Objects;

public abstract class AbstractView implements View {

    private final Parent root;

    protected AbstractView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource(fxmlPath))
            );
            this.root = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Impossibile caricare la view: " + fxmlPath, e
            );
        }
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}

package com.example.rm.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

public abstract class AbstractView implements View {

    private final Parent root;

    protected AbstractView(String fxmlPath) {
        // CORRETTO: Gestione esplicita del caso null prima di requireNonNull
        URL resource = getClass().getResource(fxmlPath);

        if (resource == null) {
            throw new IllegalStateException(
                    "Impossibile trovare il file FXML: " + fxmlPath
            );
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
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
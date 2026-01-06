package com.example.rm.view.component;

import com.example.rm.model.OrderItem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;

public class OrderReviewDialog {

    private static final Logger logger = Logger.getLogger(OrderReviewDialog.class.getName());

    @FXML
    private Label lblTavolo;

    @FXML
    private Label lblTotale;

    @FXML
    private VBox vboxProducts;

    @FXML
    private TextArea txtNote;

    @FXML
    private Button btnConfirm;

    @FXML
    private Button btnCancel;

    private Stage stage;
    private Integer tavolo;
    private List<OrderItem> items;
    private String initialNote;
    private Consumer<OrderReviewResult> onComplete;


    public static class OrderReviewResult {
        public boolean confirmed;
        public String note;

        public OrderReviewResult(boolean confirmed, String note) {
            this.confirmed = confirmed;
            this.note = note;
        }
    }

    public static void show(Stage owner, Integer tavolo, List<OrderItem> items,
                            String initialNote, Consumer<OrderReviewResult> onComplete) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    OrderReviewDialog.class.getResource("/OrderReviewDialog.fxml")
            );
            VBox root = loader.load();

            OrderReviewDialog controller = loader.getController();
            controller.initialize(tavolo, items, initialNote, onComplete);

            Stage dialog = new Stage();
            dialog.initStyle(StageStyle.UTILITY);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(owner);
            dialog.setTitle("Revisione Ordine");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.setWidth(600);
            dialog.setHeight(700);

            controller.stage = dialog;
            dialog.showAndWait();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento OrderReviewDialog.fxml", e);
        }
    }

    private void initialize(Integer tavolo, List<OrderItem> items, String initialNote,
                            Consumer<OrderReviewResult> onComplete) {
        this.tavolo = tavolo;
        this.items = items;
        this.initialNote = initialNote != null ? initialNote : "";
        this.onComplete = onComplete;

        // Setta tavolo
        if (tavolo != null && tavolo > 0) {
            lblTavolo.setText("Tavolo N° " + tavolo);
        } else {
            lblTavolo.setText("Asporto / Delivery");
        }

        // Popola prodotti
        populateProducts();

        // Setta note iniziali
        txtNote.setText(this.initialNote);

        // Event handlers
        btnConfirm.setOnAction(e -> handleConfirm());
        btnCancel.setOnAction(e -> handleCancel());
    }


    private void populateProducts() {
        vboxProducts.getChildren().clear();
        double totale = 0;

        for (OrderItem item : items) {
            String nomeProdotto = item.getProduct().getNome();
            int quantita = item.getQuantita();
            double prezzoProdotto = item.getPrezzoSnapshot();
            double totaleProdotto = quantita * prezzoProdotto;

            totale += totaleProdotto;

            // Crea riga per il prodotto
            HBox rowProduct = new HBox(10);
            rowProduct.setStyle("-fx-border-color: #EEE; -fx-border-radius: 4; -fx-padding: 10; -fx-background-color: #FAFAFA;");
            rowProduct.setPadding(new Insets(10));

            Label lblQta = new Label(quantita + "x");
            lblQta.setStyle("-fx-font-weight: bold; -fx-min-width: 30; -fx-text-alignment: center;");

            Label lblNome = new Label(nomeProdotto);
            lblNome.setStyle("-fx-font-size: 13px;");
            HBox.setHgrow(lblNome, javafx.scene.layout.Priority.ALWAYS);

            Label lblPrezzo = new Label(String.format("€ %.2f", totaleProdotto));
            lblPrezzo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");

            rowProduct.getChildren().addAll(lblQta, lblNome, lblPrezzo);
            vboxProducts.getChildren().add(rowProduct);
        }

        // Setta totale
        lblTotale.setText(String.format("€ %.2f", totale));
    }

    @FXML
    private void handleConfirm() {
        String note = txtNote.getText().trim();

        if (onComplete != null) {
            onComplete.accept(new OrderReviewResult(true, note));
        }
        stage.close();
    }

    @FXML
    private void handleCancel() {
        if (onComplete != null) {
            onComplete.accept(new OrderReviewResult(false, ""));
        }
        stage.close();
    }
}

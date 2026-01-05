package com.example.rm.view;

import com.example.rm.model.Order;
import com.example.rm.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderDetailController {

    @FXML private Label lblBack;
    @FXML private Label lblTitle;
    @FXML private VBox contentBox;

    private Order order;

    private static final Logger logger =
            Logger.getLogger(OrderDetailController.class.getName());

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'alle' HH:mm");


    public void setOrder(Order order) {
        if (order == null) {
            logger.log(Level.WARNING, "Tentativo di impostare ordine NULL");
            return;
        }

        this.order = order;
        render();
    }

    @FXML
    public void initialize() {
        setupBackHover();
    }


    private void render() {
        if (order == null) {
            logger.log(Level.WARNING, "render() chiamato ma order è null");
            return;
        }

        // === TITOLO ===
        lblTitle.setText("Ordine #" + order.getId());

        // === PULISCI CONTAINER ===
        contentBox.getChildren().clear();

        // === SEZIONE INFO GENERALI ===
        VBox infoSection = createInfoSection();

        // === SEZIONE ARTICOLI ===
        VBox itemsSection = createItemsSection();

        // === SEZIONE TOTALI ===
        VBox totalsSection = createTotalsSection();

        // === SEZIONE STATO ===
        VBox statusSection = createStatusSection();

        // === ASSEMBLA ===
        contentBox.getChildren().addAll(
                infoSection,
                new Separator(),
                itemsSection,
                new Separator(),
                totalsSection,
                new Separator(),
                statusSection
        );
    }


    private VBox createInfoSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(0, 0, 10, 0));

        // Data e ora
        Label lblDateLabel = new Label("Data e Ora:");
        lblDateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        String formattedDate = order.getDataOra()
                .format(DATE_FORMATTER)
                .substring(0, 1).toUpperCase() +
                order.getDataOra().format(DATE_FORMATTER).substring(1);

        Label lblDateValue = new Label(formattedDate);
        lblDateValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        // Tavolo
        Label lblTableLabel = new Label("Tavolo:");
        lblTableLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label lblTableValue = new Label(
                String.valueOf(order.getTavolo())
        );
        lblTableValue.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-text-fill: #2196F3; " +
                        "-fx-font-weight: bold;"
        );

        // Operatore
        Label lblUserLabel = new Label("Gestito da:");
        lblUserLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label lblUserValue = new Label(order.getUsername());
        lblUserValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        // Note (se presenti)
        if (order.hasNote()) {
            Label lblNoteLabel = new Label("Note:");
            lblNoteLabel.setStyle(
                    "-fx-font-weight: bold; " +
                            "-fx-font-size: 14px; " +
                            "-fx-text-fill: #D32F2F;"
            );

            Label lblNoteValue = new Label(order.getNote());
            lblNoteValue.setWrapText(true);
            lblNoteValue.setStyle(
                    "-fx-font-size: 14px; " +
                            "-fx-text-fill: #D32F2F; " +
                            "-fx-font-style: italic;"
            );

            section.getChildren().addAll(
                    lblDateLabel, lblDateValue,
                    lblTableLabel, lblTableValue,
                    lblUserLabel, lblUserValue,
                    lblNoteLabel, lblNoteValue
            );
        } else {
            section.getChildren().addAll(
                    lblDateLabel, lblDateValue,
                    lblTableLabel, lblTableValue,
                    lblUserLabel, lblUserValue
            );
        }

        return section;
    }


    private VBox createItemsSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10, 0, 10, 0));

        // Header
        Label lblHeader = new Label("Articoli Ordinati");
        lblHeader.setStyle(
                "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #333;"
        );

        section.getChildren().add(lblHeader);

        // Recupera articoli dal database
        List<String> items = DatabaseService.getOrderItemsForDisplay(
                order.getId()
        );

        if (items.isEmpty()) {
            Label empty = new Label("Nessun articolo trovato");
            empty.setStyle(
                    "-fx-font-size: 13px; " +
                            "-fx-text-fill: #999; " +
                            "-fx-font-style: italic;"
            );
            section.getChildren().add(empty);
        } else {
            // Lista articoli
            VBox itemsList = new VBox(8);
            itemsList.setPadding(new Insets(5, 0, 0, 10));

            for (String item : items) {
                HBox itemRow = new HBox(10);
                itemRow.setAlignment(Pos.CENTER_LEFT);

                Label bullet = new Label("•");
                bullet.setStyle(
                        "-fx-font-size: 18px; " +
                                "-fx-text-fill: #2ecc71;"
                );

                Label lblItem = new Label(item);
                lblItem.setStyle(
                        "-fx-font-size: 14px; " +
                                "-fx-text-fill: #444;"
                );

                itemRow.getChildren().addAll(bullet, lblItem);
                itemsList.getChildren().add(itemRow);
            }

            section.getChildren().add(itemsList);
        }

        return section;
    }


    private VBox createTotalsSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(10, 0, 10, 0));
        section.setStyle(
                "-fx-background-color: #F5F5F5; " +
                        "-fx-padding: 15; " +
                        "-fx-background-radius: 8;"
        );

        // Totale
        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_RIGHT);

        Label lblTotalLabel = new Label("TOTALE:");
        lblTotalLabel.setStyle(
                "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #333;"
        );

        Label lblTotalValue = new Label(
                String.format("€%.2f", order.getTotale())
        );
        lblTotalValue.setStyle(
                "-fx-font-size: 32px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #2ecc71; " +
                        "-fx-padding: 0 0 0 20;"
        );

        totalRow.getChildren().addAll(lblTotalLabel, lblTotalValue);

        section.getChildren().add(totalRow);

        return section;
    }


    private VBox createStatusSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10, 0, 0, 0));

        Label lblHeader = new Label("Stato Ordine");
        lblHeader.setStyle(
                "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #333;"
        );

        // Badge stato
        HBox statusBadge = new HBox();
        statusBadge.setAlignment(Pos.CENTER_LEFT);
        statusBadge.setPadding(new Insets(10));
        statusBadge.setMaxWidth(200);

        String statusText;
        String badgeStyle;

        switch (order.getStatus()) {
            case "to-do":
                statusText = "⏳ In Preparazione";
                badgeStyle =
                        "-fx-background-color: #FFF3E0; " +
                                "-fx-border-color: #FF9800; " +
                                "-fx-border-width: 2; " +
                                "-fx-background-radius: 5; " +
                                "-fx-border-radius: 5;";
                break;
            case "ready":
                statusText = "✓ Pronto";
                badgeStyle =
                        "-fx-background-color: #E8F8F5; " +
                                "-fx-border-color: #0eda07; " +
                                "-fx-border-width: 2; " +
                                "-fx-background-radius: 5; " +
                                "-fx-border-radius: 5;";
                break;
            case "closed":
                statusText = "✓ Pagato e Chiuso";
                badgeStyle =
                        "-fx-background-color: #E3F2FD; " +
                                "-fx-border-color: #2196F3; " +
                                "-fx-border-width: 2; " +
                                "-fx-background-radius: 5; " +
                                "-fx-border-radius: 5;";
                break;

            case "delivered":
                statusText = "da pagare";
                badgeStyle =
                        "-fx-background-color: #FFF3E0; " +
                                "-fx-border-color: #a823bc; " +
                                "-fx-border-width: 2; " +
                                "-fx-background-radius: 5; " +
                                "-fx-border-radius: 5;";
                break;
            default:
                statusText = "❓ Sconosciuto";
                badgeStyle =
                        "-fx-background-color: #F5F5F5; " +
                                "-fx-border-color: #999; " +
                                "-fx-border-width: 2; " +
                                "-fx-background-radius: 5; " +
                                "-fx-border-radius: 5;";
        }

        statusBadge.setStyle(badgeStyle);

        Label lblStatus = new Label(statusText);
        lblStatus.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold;"
        );

        statusBadge.getChildren().add(lblStatus);

        section.getChildren().addAll(lblHeader, statusBadge);

        return section;
    }


    private void setupBackHover() {
        if (lblBack == null) return;

        String normal =
                "-fx-text-fill: #888; " +
                        "-fx-cursor: hand; " +
                        "-fx-font-size: 14px;";

        String hover =
                "-fx-text-fill: #333; " +
                        "-fx-underline: true; " +
                        "-fx-cursor: hand; " +
                        "-fx-font-size: 14px;";

        lblBack.setStyle(normal);
        lblBack.setOnMouseEntered(e -> lblBack.setStyle(hover));
        lblBack.setOnMouseExited(e -> lblBack.setStyle(normal));
    }


    @FXML
    private void goBack() {
        logger.log(Level.INFO, "Ritorno a Financial View");
        View financialView = ViewFactory.forRole("financial");
        lblBack.getScene().setRoot(financialView.getRoot());
    }
}
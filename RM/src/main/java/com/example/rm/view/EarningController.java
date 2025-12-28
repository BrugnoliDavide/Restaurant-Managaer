package com.example.rm.view;

import com.example.rm.app.UserSession;
import com.example.rm.model.Order;
import com.example.rm.model.User;
import com.example.rm.service.DatabaseService;
import com.example.rm.view.component.OrderRowItemController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EarningController {

    @FXML private VBox ordersContainer;
    @FXML private VBox detailsPane;
    @FXML private TextField searchField;

    @FXML private StackPane profileBtn;
    @FXML private Circle profileCircle;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderRole;

    /* ======================
       STATE
       ====================== */

    private List<Order> openOrders = new ArrayList<>();
    private Order selectedOrder;
    private OrderRowItemController selectedRowController;

    /* ======================
       INIT
       ====================== */

    @FXML
    public void initialize() {

        UserSession session = UserSession.getInstance();
        User u = session.getUser();

        lblHeaderName.setText(u.getUsername());
        lblHeaderRole.setText(u.getRole().toUpperCase());

        profileBtn.setOnMouseEntered(e -> profileCircle.setStrokeWidth(3));
        profileBtn.setOnMouseExited(e -> profileCircle.setStrokeWidth(0));

        loadOpenOrders();
    }

    /* ======================
       SEARCH
       ====================== */

    @FXML
    private void onSearch() {

        String query = searchField.getText().toLowerCase().trim();

        ordersContainer.getChildren().clear();

        if (query.isEmpty()) {
            renderOrders(openOrders);
            return;
        }

        List<Order> filtered = openOrders.stream()
                .filter(order ->
                        order.getUsername().toLowerCase().contains(query)
                                || String.valueOf(order.getTavolo()).contains(query)
                )
                .toList();

        renderOrders(filtered);
    }

    private void renderOrders(List<Order> orders) {

        ordersContainer.getChildren().clear();
        selectedRowController = null;

        for (Order order : orders) {
            try {
                FXMLLoader loader =
                        new FXMLLoader(getClass().getResource("/OrderRowItem.fxml"));
                Parent row = loader.load();

                OrderRowItemController rowController = loader.getController();

                rowController.setOrder(order, selected -> {

                    if (selectedRowController != null) {
                        selectedRowController.setSelected(false);
                    }

                    rowController.setSelected(true);
                    selectedRowController = rowController;

                    showDetails(selected);
                });

                ordersContainer.getChildren().add(row);

            } catch (IOException e) {
                throw new RuntimeException("Errore caricamento OrderRowItem.fxml", e);
            }
        }
    }

    /* ======================
       DETAILS
       ====================== */

    private void showDetails(Order order) {
        selectedOrder = order;

        detailsPane.getChildren().clear();
        detailsPane.getChildren().add(
                OrderDetailsFactory.create(order, this::markAsPaid)
        );
    }

    /* ======================
       ACTIONS
       ====================== */

    private void markAsPaid(double discount) {

        if (selectedOrder == null) {
            return;
        }

        DatabaseService.markOrderAsPaid(selectedOrder.getId());

        selectedOrder = null;
        detailsPane.getChildren().clear();

        loadOpenOrders();
    }

    /* ======================
       PROFILE MENU
       ====================== */

    @FXML
    private void handleProfileMenu(MouseEvent event) {

        ContextMenu menu = new ContextMenu();

        MenuItem logout = new MenuItem("Logout");
        logout.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        logout.setOnAction(e -> {

            UserSession.cleanUserSession();

            try {
                Parent login =
                        FXMLLoader.load(getClass().getResource("/LoginView.fxml"));
                profileBtn.getScene().setRoot(login);

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        menu.getItems().add(logout);
        menu.show(profileBtn, Side.BOTTOM, 0, 0);
    }

    /* ======================
       LOAD DATA
       ====================== */

    private void loadOpenOrders() {

        detailsPane.getChildren().clear();
        selectedRowController = null;

        openOrders = DatabaseService.getOrdersToPay();

        renderOrders(openOrders);
    }
}

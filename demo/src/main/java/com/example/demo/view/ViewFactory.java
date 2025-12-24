package com.example.demo.view;

import com.example.demo.view.screens.FinancialView;
import com.example.demo.view.screens.*;
import com.example.demo.model.MenuProduct;
import com.example.demo.view.ProductDetailController;

import static java.lang.System.load;

public final class ViewFactory {

    private ViewFactory() {}

    public static View forRole(String role) {

        if (role == null) {
            throw new IllegalArgumentException("Role nullo");
        }

        return switch (role.toLowerCase()) {

            case "manager"                  -> new ManagerView();
            case "cameriere"                -> new WaiterView();
            case "cucina"                   -> new KitchenView();
            case "users"                    -> new UsersView();
            case "financial"                -> new FinancialView();
            case "menu"                     -> new MenuView();

            default -> throw new IllegalArgumentException(
                    "Ruolo non supportato: " + role
            );
        };
    }

}

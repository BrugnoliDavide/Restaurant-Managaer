package com.example.demo.view;

import com.example.demo.view.screens.*;

public final class ViewFactory {

    private ViewFactory() {}

    public static View forRole(String role) {

        if (role == null) {
            throw new IllegalArgumentException("Role nullo");
        }

        return switch (role.toLowerCase()) {
            case "manager"   -> new ManagerView();
            case "cameriere" -> new WaiterView();
            case "cucina"    -> new KitchenView();
            case "users" -> new UsersView();

            default -> throw new IllegalArgumentException(
                    "Ruolo non supportato: " + role
            );
        };
    }
}

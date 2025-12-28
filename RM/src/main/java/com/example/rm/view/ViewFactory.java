package com.example.rm.view;

import com.example.rm.view.screens.*;

public final class ViewFactory {

    private ViewFactory() {}

    /**
     * Crea una view basata sul ruolo (view senza parametri)
     */
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
            case "cassiere"                 -> new EarningView();

            default -> throw new IllegalArgumentException(
                    "Ruolo non supportato: " + role
            );
        };
    }


    public static View forTakeOrder(int numeroTavolo) {
        if (numeroTavolo <= 0) {
            throw new IllegalArgumentException(
                    "Numero tavolo non valido: " + numeroTavolo
            );
        }
        return new TakeOrderView(numeroTavolo);
    }


    public static View create(String viewType, Object... params) {

        if (viewType == null) {
            throw new IllegalArgumentException("ViewType nullo");
        }

        return switch (viewType.toLowerCase()) {

            case "takeorder" -> {
                if (params.length != 1 || !(params[0] instanceof Integer)) {
                    throw new IllegalArgumentException(
                            "TakeOrderView richiede un parametro Integer (numeroTavolo)"
                    );
                }
                yield new TakeOrderView((Integer) params[0]);
            }

            // Altre view parametriche future possono essere aggiunte qui

            default -> throw new IllegalArgumentException(
                    "View type non supportato: " + viewType
            );
        };
    }
}
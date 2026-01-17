package com.example.rm.view;

import com.example.rm.exception.ViewNotFoundException;
import com.example.rm.preference.SimpleGraphicsManager;
import com.example.rm.view.EinkScreen.KitchenEinkView;
import com.example.rm.view.EinkScreen.WaiterEinkView;
import com.example.rm.view.screens.*;

public final class ViewFactory {

    private ViewFactory() {}

    public static View forRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role nullo o vuoto");
        }

        boolean eink = SimpleGraphicsManager.isEinkMode();

        return switch (role.toLowerCase()) {
            case "manager"   -> new ManagerView();
            case "cameriere" -> eink ? new WaiterEinkView() : new WaiterView();
            case "cucina"    -> eink ? new KitchenEinkView() : new KitchenView();
            case "users"     -> new UsersView();
            case "financial" -> new FinancialView();
            case "menu"      -> new MenuView();
            case "cassiere"  -> new EarningView();
            default -> throw new ViewNotFoundException("Nessuna view associata al ruolo: " + role);
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

        if (viewType == null || viewType.isBlank()) {
            throw new IllegalArgumentException("ViewType nullo o vuoto");
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

            default -> throw new ViewNotFoundException(
                    "View type non supportato: " + viewType
            );
        };
    }

    public static View getLoginView() {
        return new LoginView();
    }
}

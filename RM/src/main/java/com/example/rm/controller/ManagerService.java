package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.service.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManagerService implements ManagerUseCase {

    private static final Logger logger = Logger.getLogger(ManagerService.class.getName());

    @Override
    public List<Order> loadAllOrders() {
        try {
            // Sostituisci con il tuo metodo reale
            // ad esempio: return DatabaseService.getAllOrders();
            return DatabaseService.getKitchenActiveOrders(); // TODO: usa un metodo più adatto se esiste
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento di tutti gli ordini (manager).", e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean updateOrderStatus(int orderId, String newStatus) {
        try {
            // Molte tue classi usano già setOrderStatus(id, status)
            DatabaseService.setOrderStatus(orderId, newStatus);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Errore durante l'aggiornamento dello stato ordine id=" + orderId +
                            " a '" + newStatus + "'", e);
            return false;
        }
    }

    @Override
    public boolean deleteOrder(int orderId) {
        try {
            // Se hai un vero metodo delete:
            // return DatabaseService.deleteOrder(orderId);
            // In alternativa puoi marcare come "canceled"
            DatabaseService.setOrderStatus(orderId, "canceled");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Errore durante la cancellazione ordine id=" + orderId, e);
            return false;
        }
    }

    @Override
    public Order findOrderById(int orderId) {
        try {
            // Se esiste un metodo specifico:
            // return DatabaseService.getOrderById(orderId);
            // Altrimenti ricarica la lista e filtra
            return loadAllOrders().stream()
                    .filter(o -> o.getId() == orderId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Errore durante la ricerca ordine id=" + orderId, e);
            return null;
        }
    }
}

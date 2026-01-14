package com.example.rm.controller;

import com.example.rm.model.Order;
import com.example.rm.model.User;
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
            return DatabaseService.getKitchenActiveOrders();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento di tutti gli ordini (manager).", e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean updateOrderStatus(int orderId, String newStatus) {
        try {
            DatabaseService.setOrderStatus(orderId, newStatus);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Errore durante aggiornamento dello stato ordine id={0} a {1}, {2} ",
                    new Object[]{orderId, newStatus , e});
            return false;
        }
    }

    @Override
    public boolean deleteOrder(int orderId) {
        try {
            DatabaseService.setOrderStatus(orderId, "canceled");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Errore durante la cancellazione ordine id={0}, {1}",new Object[]{orderId, e});
            return false;
        }
    }

    @Override
    public Order findOrderById(int orderId) {
        try {
            return loadAllOrders().stream()
                    .filter(o -> o.getId() == orderId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Errore durante la ricerca ordine id={0},{1}", new Object[] {orderId, e});
            return null;
        }
    }


    @Override
    public List<User> loadAllUsers() {
        return DatabaseService.getAllUsers();
    }

    @Override
    public boolean deleteUser(String userId) {
        return DatabaseService.deleteUser(userId);
    }


}

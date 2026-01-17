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
    public List<User> loadAllUsers() {
        return DatabaseService.getAllUsers();
    }

    @Override
    public boolean deleteUser(String userId) {
        return DatabaseService.deleteUser(userId);
    }


}

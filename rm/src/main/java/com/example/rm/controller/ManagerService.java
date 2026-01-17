package com.example.rm.controller;


import com.example.rm.model.User;
import com.example.rm.service.DatabaseService;
import java.util.List;

public class ManagerService implements ManagerUseCase {



    @Override
    public List<User> loadAllUsers() {
        return DatabaseService.getAllUsers();
    }

    @Override
    public boolean deleteUser(String userId) {
        return DatabaseService.deleteUser(userId);
    }


}

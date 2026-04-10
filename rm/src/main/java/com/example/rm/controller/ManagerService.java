package com.example.rm.controller;


import com.example.rm.model.User;
import java.util.List;
import com.example.rm.dao.DatabaseUserDAO;
import com.example.rm.dao.UserDAO;


public class ManagerService implements ManagerUseCase {


    private final UserDAO userDAO = new DatabaseUserDAO();


    @Override
    public List<User> loadAllUsers() {
        return userDAO.findAll();
    }

    @Override
    public boolean deleteUser(String userId) {
        return userDAO.delete(userId);
    }


}

package com.example.rm.controller;


import com.example.rm.model.User;

import java.util.List;

public interface ManagerUseCase {

    List<User> loadAllUsers();

    boolean deleteUser(String userId);

}

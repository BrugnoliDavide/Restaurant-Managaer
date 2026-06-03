package rm.controller;


import rm.model.User;

import java.util.List;

public interface ManagerUseCase {

    List<User> loadAllUsers();

    boolean deleteUser(String userId);

}

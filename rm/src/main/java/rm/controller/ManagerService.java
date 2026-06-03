package rm.controller;

import rm.model.User;
import java.util.List;
import rm.dao.DatabaseUserDAO;
import rm.dao.UserDAO;
import rm.util.BeanMapper;


public class ManagerService implements ManagerUseCase {


    private final UserDAO userDAO = new DatabaseUserDAO();


    @Override
    public List<User> loadAllUsers() {
        return BeanMapper.toUserModels(userDAO.findAll());
    }

    @Override
    public boolean deleteUser(String userId) {
        return userDAO.delete(userId);
    }


}

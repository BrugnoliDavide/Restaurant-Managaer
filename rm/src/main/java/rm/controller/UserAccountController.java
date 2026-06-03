package rm.controller;

import rm.service.SecurityService;

public class UserAccountController implements UserAccountUseCase {

    @Override
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return false;
        }
        return SecurityService.changePassword(username, currentPassword, newPassword);
    }
}

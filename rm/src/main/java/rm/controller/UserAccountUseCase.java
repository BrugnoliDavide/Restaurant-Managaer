package rm.controller;

public interface UserAccountUseCase {
    boolean changePassword(String username, String currentPassword, String newPassword);
}

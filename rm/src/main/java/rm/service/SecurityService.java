package rm.service;

import rm.dao.UserDAO;
import rm.dao.UserDAO.UserCredentials;
import rm.dao.DatabaseUserDAO;
import rm.exception.AuthenticationException;
import org.mindrot.jbcrypt.BCrypt;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class SecurityService {

    private static final Logger logger =
            Logger.getLogger(SecurityService.class.getName());

    private static final UserDAO userDAO = new DatabaseUserDAO();

    private SecurityService() {
        throw new AssertionError("Classe di utilità non istanziabile");
    }

    /**
     * Autentica un utente confrontando la password con l'hash salvato.
     *
     * @param username          username
     * @param candidatePassword password in chiaro
     * @return ruolo se autenticato
     * @throws AuthenticationException se le credenziali non sono valide
     */
    public static String authenticate(String username, String candidatePassword) {
        if (username == null || username.isBlank()
                || candidatePassword == null || candidatePassword.isBlank()) {
            throw new IllegalArgumentException("Username o password non validi");
        }

        UserCredentials credentials = userDAO.findCredentials(username);

        if (credentials == null) {
            throw new AuthenticationException("Credenziali non valide");
        }

        if (!BCrypt.checkpw(candidatePassword, credentials.hashedPassword())) {
            throw new AuthenticationException("Credenziali non valide");
        }

        return credentials.role();
    }

    public static boolean registerUser(String username, String plainPassword, String role) {
        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        return userDAO.insert(username, hashed, role);
    }

    public static boolean changePassword(String username,
                                         String currentPassword,
                                         String newPassword) {
        try {
            authenticate(username, currentPassword);
        } catch (AuthenticationException e) {
            logger.log(Level.WARNING,
                    "Cambio password fallito per {0}: password corrente errata", username);
            return false;
        }

        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        return userDAO.updatePassword(username, newHash);
    }
}
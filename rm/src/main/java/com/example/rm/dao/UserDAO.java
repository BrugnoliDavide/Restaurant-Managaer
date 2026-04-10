package com.example.rm.dao;

import com.example.rm.model.User;
import java.util.List;

public interface UserDAO {

    /**
     * Recupera tutti gli utenti ordinati per ruolo e username.
     */
    List<User> findAll();

    /**
     * Elimina un utente per username.
     * @return true se almeno una riga è stata eliminata
     */
    boolean delete(String username);

    /**
     * Inserisce un nuovo utente con password già hashata.
     * @return true se l'inserimento è riuscito
     */
    boolean insert(String username, String hashedPassword, String role);

    /**
     * Recupera hash e ruolo per un dato username.
     * @return un record con hash e ruolo, oppure null se l'utente non esiste
     */
    UserCredentials findCredentials(String username);

    /**
     * Aggiorna la password (già hashata) di un utente.
     * @return true se almeno una riga è stata aggiornata
     */
    boolean updatePassword(String username, String newHashedPassword);

    /**
     * Record immutabile per restituire le credenziali senza esporre il ResultSet.
     */
    record UserCredentials(String hashedPassword, String role) {}
}
package com.example.rm.controller;

import com.example.rm.service.DBConfigStore;
import com.example.rm.service.DatabaseService;

public class DBConfigController implements DBConfigUseCase {

    @Override
    public DBConfig loadConfig() {
        // Usa API esistenti di DatabaseService
        String host = DatabaseService.getDBHost();
        String port = DatabaseService.getDBPort();
        String dbName = DatabaseService.getDBName();
        String user = DatabaseService.getDBUser();
        boolean hasPassword = DatabaseService.hasPassword(); // esiste nel tuo codice

        return new DBConfig(host, port, dbName, user, hasPassword);
    }

    @Override
    public boolean saveConfig(String host, String port, String dbName, String username, String password) {
        // Validazioni base
        if (isBlank(host) || isBlank(port) || isBlank(dbName) || isBlank(username)) {
            return false;
        }

        // Se password è vuota, recupera quella esistente dal DBConfigStore di
        // modo che per non cambiare la password basti non inserirla
        String finalPassword;
        if (password == null || password.isBlank()) {
            finalPassword = DBConfigStore.getPassword();
            if (finalPassword == null || finalPassword.isBlank()) {
                // finalPassword == null significa che non c'è password salvata
                return false;
            }
        } else {
            finalPassword = password.trim();
        }

        // Salva nelle preferenze cifrate
        DBConfigStore.save(
                host.trim(),
                port.trim(),
                dbName.trim(),
                username.trim(),
                finalPassword
        );

        // Aggiorna la configurazione runtime di DatabaseService
        DatabaseService.setConnectionConfig(
                host.trim(),
                port.trim(),
                dbName.trim(),
                username.trim(),
                finalPassword
        );

        return true;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

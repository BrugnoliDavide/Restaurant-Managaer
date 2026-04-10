package com.example.rm.controller;

import com.example.rm.service.ConnectionManager;
import com.example.rm.service.DBConfigStore;

public class DBConfigController implements DBConfigUseCase {

    @Override
    public DBConfig loadConfig() {

        String host =           ConnectionManager.getHost();
        String port =           ConnectionManager.getPort();
        String dbName =         ConnectionManager.getDbName();
        String user =           ConnectionManager.getUser();
        boolean hasPassword =   ConnectionManager.hasPassword();

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

        ConnectionManager.configure(
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

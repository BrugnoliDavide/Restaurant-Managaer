/*package com.example.rm.dao;

import com.example.rm.service.ConnectionManager;
import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConnection {

    protected static Connection connection;

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = ConnectionManager.getConnection();
        }
        return connection;
    }
}*/
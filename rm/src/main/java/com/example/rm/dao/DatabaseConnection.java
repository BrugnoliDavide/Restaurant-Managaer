package com.example.rm.dao;

import com.example.rm.service.DatabaseService;

import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConnection {

    protected static Connection connection;

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DatabaseService.getConnection();
        }
        return connection;
    }
}

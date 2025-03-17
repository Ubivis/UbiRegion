package com.ubivismedia.ubiregions;

import java.sql.*;

public class DatabaseManager {

    private Connection connection;

    public DatabaseManager() {
        setupDatabase();
    }

    private void setupDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:plugins/UbiRegions/regions.db");
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS regions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "biome TEXT,"
                    + "min_x INTEGER,"
                    + "min_z INTEGER,"
                    + "max_x INTEGER,"
                    + "max_z INTEGER);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS region_chunks ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "region_id INTEGER NOT NULL,"
                    + "chunk_x INTEGER NOT NULL,"
                    + "chunk_z INTEGER NOT NULL,"
                    + "FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE CASCADE);");
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

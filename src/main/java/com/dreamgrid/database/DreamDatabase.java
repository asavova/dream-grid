package com.dreamgrid.database;

import com.dreamgrid.config.AppConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DreamDatabase {
  private static AppConfig config = AppConfig.load();
  private static Connection connection;
  private static final Logger logger = Logger.getLogger(DreamDatabase.class.getName());

  public static void initialize() {
    initialize(AppConfig.load());
  }

  public static void initialize(AppConfig appConfig) {
    try {
      config = appConfig;
      Path databasePath = Paths.get(config.getDatabasePath());
      Path parent = databasePath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      connection = DriverManager.getConnection(config.getDatabaseUrl());
      logger.info("Connected to database: " + config.getDatabaseUrl());

      try (Statement stmt = connection.createStatement()) {
        String createTableSQL =
            """
CREATE TABLE IF NOT EXISTS dreams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    dream_date TEXT,
    timestamp INTEGER,
    symbol_tags TEXT,
    dream_type TEXT,
    analyzed INTEGER DEFAULT 0,
    analysis_result TEXT,
    analyzed_at INTEGER,
    analysis_version TEXT,
    analysis_status TEXT NOT NULL DEFAULT 'PENDING'
);
""";

        stmt.execute(createTableSQL);
        migrateDreamsTable(stmt);
        logger.info("Dreams table ensured.");
      }

    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error during initialization", e);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Unexpected error during DB setup", e);
    }
  }

  public static Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
      connection = DriverManager.getConnection(config.getDatabaseUrl());
    }
    return connection;
  }

  private static void migrateDreamsTable(Statement stmt) throws SQLException {
    addColumnIfMissing(stmt, "analysis_result", "TEXT");
    addColumnIfMissing(stmt, "analyzed_at", "INTEGER");
    addColumnIfMissing(stmt, "analysis_version", "TEXT");
    addColumnIfMissing(stmt, "analysis_status", "TEXT NOT NULL DEFAULT 'PENDING'");
  }

  private static void addColumnIfMissing(Statement stmt, String columnName, String columnDefinition)
      throws SQLException {
    if (!columnExists(stmt, columnName)) {
      stmt.executeUpdate("ALTER TABLE dreams ADD COLUMN " + columnName + " " + columnDefinition);
    }
  }

  private static boolean columnExists(Statement stmt, String columnName) throws SQLException {
    try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(dreams)")) {
      while (rs.next()) {
        if (columnName.equalsIgnoreCase(rs.getString("name"))) {
          return true;
        }
      }
    }
    return false;
  }

  public static void close() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
        logger.info("Database connection closed.");
      }
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Error while closing DB connection", e);
    }
  }
}

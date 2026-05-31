package com.dreamgrid.database;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DreamDatabase {
  private static final String DB_URL = "jdbc:sqlite:data/dreams.db";
  private static Connection connection;
  private static final Logger logger = Logger.getLogger(DreamDatabase.class.getName());

  public static void initialize() {
    try {
      Files.createDirectories(Paths.get("data"));
      connection = DriverManager.getConnection(DB_URL);
      logger.info("Connected to database: " + DB_URL);

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
      connection = DriverManager.getConnection(DB_URL);
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

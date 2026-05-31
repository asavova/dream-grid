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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
      enableForeignKeys(connection);
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
    analysis_status TEXT NOT NULL DEFAULT 'PENDING',
    user_classification TEXT,
    inferred_classification TEXT,
    effective_classification TEXT NOT NULL DEFAULT 'UNKNOWN',
    classification_source TEXT NOT NULL DEFAULT 'UNKNOWN',
    classification_reason TEXT,
    classification_updated_at INTEGER
);
""";

        stmt.execute(createTableSQL);
        migrateDreamsTable(stmt);
        createTagTables(stmt);
        createAnalysisRunTable(stmt);
        migrateLegacySymbolTags(stmt);
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
      enableForeignKeys(connection);
    }
    return connection;
  }

  private static void enableForeignKeys(Connection connection) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("PRAGMA foreign_keys = ON");
    }
  }

  private static void migrateDreamsTable(Statement stmt) throws SQLException {
    addColumnIfMissing(stmt, "analysis_result", "TEXT");
    addColumnIfMissing(stmt, "analyzed_at", "INTEGER");
    addColumnIfMissing(stmt, "analysis_version", "TEXT");
    addColumnIfMissing(stmt, "analysis_status", "TEXT NOT NULL DEFAULT 'PENDING'");
    addColumnIfMissing(stmt, "user_classification", "TEXT");
    addColumnIfMissing(stmt, "inferred_classification", "TEXT");
    addColumnIfMissing(stmt, "effective_classification", "TEXT NOT NULL DEFAULT 'UNKNOWN'");
    addColumnIfMissing(stmt, "classification_source", "TEXT NOT NULL DEFAULT 'UNKNOWN'");
    addColumnIfMissing(stmt, "classification_reason", "TEXT");
    addColumnIfMissing(stmt, "classification_updated_at", "INTEGER");
    stmt.executeUpdate(
        """
UPDATE dreams
SET effective_classification = CASE
    WHEN dream_type IN ('LUCID', 'NIGHTMARE', 'RECURRING', 'NEUTRAL') THEN dream_type
    ELSE 'UNKNOWN'
END,
classification_source = CASE
    WHEN dream_type IN ('LUCID', 'NIGHTMARE', 'RECURRING', 'NEUTRAL') THEN 'USER'
    ELSE 'UNKNOWN'
END,
user_classification = CASE
    WHEN dream_type IN ('LUCID', 'NIGHTMARE', 'RECURRING', 'NEUTRAL') THEN dream_type
    ELSE user_classification
END,
classification_updated_at = COALESCE(classification_updated_at, timestamp)
WHERE effective_classification IS NULL OR effective_classification = 'UNKNOWN'
""");
  }

  private static void createTagTables(Statement stmt) throws SQLException {
    stmt.execute(
        """
CREATE TABLE IF NOT EXISTS dream_tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    normalized_name TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL
);
""");
    stmt.execute(
        """
CREATE TABLE IF NOT EXISTS dream_tag_links (
    dream_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    source TEXT NOT NULL,
    confidence_score REAL,
    created_at INTEGER NOT NULL,
    PRIMARY KEY (dream_id, tag_id, source),
    FOREIGN KEY (dream_id) REFERENCES dreams(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES dream_tags(id) ON DELETE CASCADE
);
""");
  }

  private static void createAnalysisRunTable(Statement stmt) throws SQLException {
    stmt.execute(
        """
CREATE TABLE IF NOT EXISTS analysis_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dream_id INTEGER NOT NULL,
    requested_at INTEGER NOT NULL,
    completed_at INTEGER,
    status TEXT NOT NULL,
    analysis_version TEXT,
    analysis_result TEXT,
    failure_reason TEXT,
    FOREIGN KEY (dream_id) REFERENCES dreams(id) ON DELETE CASCADE
);
""");
  }

  private static void migrateLegacySymbolTags(Statement stmt) throws SQLException {
    if (!columnExists(stmt, "symbol_tags")) {
      return;
    }

    List<LegacyTagLink> links = new ArrayList<>();
    try (ResultSet rs =
        stmt.executeQuery(
            "SELECT id, symbol_tags FROM dreams WHERE symbol_tags IS NOT NULL AND TRIM(symbol_tags) <> ''")) {
      while (rs.next()) {
        int dreamId = rs.getInt("id");
        for (String normalizedName : parseLegacyTags(rs.getString("symbol_tags"))) {
          links.add(new LegacyTagLink(dreamId, normalizedName));
        }
      }
    }

    for (LegacyTagLink link : links) {
      long createdAt = System.currentTimeMillis();
      stmt.executeUpdate(
          "INSERT OR IGNORE INTO dream_tags (name, normalized_name, created_at) VALUES ('"
              + escapeSql(link.normalizedName())
              + "', '"
              + escapeSql(link.normalizedName())
              + "', "
              + createdAt
              + ")");
      stmt.executeUpdate(
          "INSERT OR IGNORE INTO dream_tag_links (dream_id, tag_id, source, confidence_score, created_at) "
              + "SELECT "
              + link.dreamId()
              + ", id, 'MANUAL', NULL, "
              + createdAt
              + " FROM dream_tags WHERE normalized_name = '"
              + escapeSql(link.normalizedName())
              + "'");
    }
  }

  private static Set<String> parseLegacyTags(String value) {
    Set<String> tags = new LinkedHashSet<>();
    if (value == null || value.isBlank()) {
      return tags;
    }

    for (String rawTag : value.split(",")) {
      String normalized = normalizeLegacyTag(rawTag);
      if (!normalized.isBlank()) {
        tags.add(normalized);
      }
    }
    return tags;
  }

  private static String normalizeLegacyTag(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFKC)
        .toLowerCase(Locale.ROOT)
        .replaceAll("[_\\-]+", " ")
        .replaceAll("[^\\p{Alnum}\\s']", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private static String escapeSql(String value) {
    return value.replace("'", "''");
  }

  private record LegacyTagLink(int dreamId, String normalizedName) {}

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

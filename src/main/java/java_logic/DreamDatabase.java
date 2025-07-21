package java_logic;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
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
        String sql =
            """
                    CREATE TABLE IF NOT EXISTS dreams (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        symbol_tag TEXT,
                        mood_tag TEXT,
                        timestamp INTEGER
                    );
                """;
        stmt.execute(sql);
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
      connection = DriverManager.getConnection("jdbc:sqlite:dreams.db");
    }
    return connection;
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

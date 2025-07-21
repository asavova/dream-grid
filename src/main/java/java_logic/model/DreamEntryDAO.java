package java_logic.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DreamEntryDAO {
  private final Connection conn;

  public DreamEntryDAO(Connection connection) {
    this.conn = connection;
  }

  public void insert(DreamEntry entry) throws SQLException {
    String sql =
        "INSERT INTO dreams (title, content, symbol_tag, mood_tag, timestamp) VALUES (?, ?, ?, ?, ?)";

    try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setString(1, entry.getTitle());
      pstmt.setString(2, entry.getContent());
      pstmt.setString(3, entry.getSymbolTag());
      pstmt.setString(4, entry.getMoodTag());
      pstmt.setLong(5, entry.getTimestamp());
      pstmt.executeUpdate();

      int affectedRows = pstmt.executeUpdate();

      if (affectedRows == 0) {
        throw new SQLException("Inserting dream failed, no rows affected.");
      }

      try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          entry.setId(generatedKeys.getInt(1)); // тук връщаме id обратно в обекта
        }
      }
    }
  }

  public List<DreamEntry> findAll() {
    List<DreamEntry> dreams = new ArrayList<>();

    try (PreparedStatement statement = conn.prepareStatement("SELECT * FROM dreams");
        ResultSet rs = statement.executeQuery()) {

      while (rs.next()) {
        DreamEntry entry =
            new DreamEntry(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("symbol_tag"),
                rs.getString("mood_tag"),
                rs.getLong("timestamp"));
        dreams.add(entry);
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }

    return dreams;
  }

  public List<DreamEntry> getAll() {
    List<DreamEntry> entries = new ArrayList<>();
    try {
      String sql = "SELECT * FROM dreams";
      try (Statement stmt = conn.createStatement();
          ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
          DreamEntry entry =
              new DreamEntry(
                  rs.getInt("id"),
                  rs.getString("title"),
                  rs.getString("content"),
                  rs.getString("symbol_tag"),
                  rs.getString("mood_tag"),
                  rs.getLong("timestamp"));
          entries.add(entry);
        }
      }
    } catch (SQLException e) {
      System.err.println("Failed to retrieve dream entries: " + e.getMessage());
    }
    return entries;
  }
}

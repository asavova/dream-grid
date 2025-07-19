package java_logic;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DreamEntryDAO {
  private final Connection conn;

  public DreamEntryDAO(Connection conn) {
    this.conn = conn;
  }

  public void insert(DreamEntry entry) {
    try {
      if (conn.isClosed()) {
        System.out.println("Connection is closed!");
        return;
      }
      String sql =
          "INSERT INTO dreams (title, content, symbol_tag, mood_tag, timestamp) VALUES (?, ?, ?, ?, ?)";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, entry.getTitle());
        pstmt.setString(2, entry.getContent());
        pstmt.setString(3, entry.getSymbolTag());
        pstmt.setString(4, entry.getMoodTag());
        pstmt.setLong(5, entry.getTimestamp());
        pstmt.executeUpdate();
      }
    } catch (SQLException e) {
      System.err.println("Failed to insert dream entry: " + e.getMessage());
    }
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

package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.DreamEntry;
import model.DreamType;
import model.SymbolTag;

public class DreamEntryDAO {
  private final Connection connection;

  public DreamEntryDAO(Connection connection) {
    this.connection = connection;
  }

  public void insert(DreamEntry entry) throws SQLException {
    String sql =
        "INSERT INTO dreams (title, content, dream_date, timestamp, symbol_tags, dream_type, analyzed) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement stmt =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, entry.getTitle());
      stmt.setString(2, entry.getContent());
      stmt.setString(3, entry.getDreamDate());
      stmt.setLong(4, entry.getTimestamp());

      stmt.setString(5, SymbolTag.serialize(entry.getSymbolTags()));

      DreamType type = entry.getDreamType();
      stmt.setString(6, type != null ? type.name() : null);

      stmt.executeUpdate();

      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          entry.setId(generatedKeys.getInt(1));
        }
      }
    }
  }

  public void update(DreamEntry entry) throws SQLException {
    String sql =
        "UPDATE dreams SET title = ?, content = ?, dream_date = ?, timestamp = ?, symbol_tags = ?, dream_type = ?, analyzed = ? WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, entry.getTitle());
      stmt.setString(2, entry.getContent());
      stmt.setString(3, entry.getDreamDate());
      stmt.setLong(4, entry.getTimestamp());

      List<SymbolTag> tags = entry.getSymbolTags();
      String tagStr = tags != null ? String.join(",", tags.stream().map(Enum::name).toList()) : "";
      stmt.setString(5, tagStr);

      DreamType type = entry.getDreamType();
      stmt.setString(6, type != null ? type.name() : null);
      stmt.setInt(7, entry.isAnalyzed() ? 1 : 0);

      stmt.setInt(8, entry.getId());

      stmt.executeUpdate();
    }
  }

  public List<DreamEntry> getAll() throws SQLException {
    List<DreamEntry> dreams = new ArrayList<>();
    String sql = "SELECT * FROM dreams";

    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        DreamEntry entry =
            new DreamEntry(
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("dream_date"),
                rs.getLong("timestamp"),
                SymbolTag.deserialize(rs.getString("symbol_tags")),
                DreamType.valueOf(rs.getString("dream_type")));
        entry.setId(rs.getInt("id"));
        entry.setAnalyzed(rs.getInt("analyzed") == 1);
        dreams.add(entry);
      }
    }
    return dreams;
  }
}

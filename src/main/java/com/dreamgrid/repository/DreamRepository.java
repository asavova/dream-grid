package com.dreamgrid.repository;

import com.dreamgrid.model.AnalysisStatus;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamSymbol;
import com.dreamgrid.model.DreamType;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DreamRepository {
  private final Connection connection;

  public DreamRepository(Connection connection) {
    this.connection = connection;
  }

  public void insert(DreamEntry entry) throws SQLException {
    String sql =
        "INSERT INTO dreams (title, content, dream_date, timestamp, symbol_tags, dream_type, analyzed, analysis_result, analyzed_at, analysis_version, analysis_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement stmt =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, entry.getTitle());
      stmt.setString(2, entry.getContent());
      stmt.setString(3, entry.getDreamDate());
      stmt.setLong(4, entry.getTimestamp());

      stmt.setString(5, DreamSymbol.serialize(entry.getSymbolTags()));

      DreamType type = entry.getDreamType();
      stmt.setString(6, type != null ? type.name() : null);

      stmt.setInt(7, entry.isAnalyzed() ? 1 : 0);
      stmt.setString(8, entry.getAnalysisResult());
      setNullableLong(stmt, 9, entry.getAnalyzedAt());
      stmt.setString(10, entry.getAnalysisVersion());
      stmt.setString(11, normalizeAnalysisStatus(entry.getAnalysisStatus()).name());

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
        "UPDATE dreams SET title = ?, content = ?, dream_date = ?, timestamp = ?, symbol_tags = ?, dream_type = ?, analyzed = ?, analysis_result = ?, analyzed_at = ?, analysis_version = ?, analysis_status = ? WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, entry.getTitle());
      stmt.setString(2, entry.getContent());
      stmt.setString(3, entry.getDreamDate());
      stmt.setLong(4, entry.getTimestamp());

      List<DreamSymbol> tags = entry.getSymbolTags();
      String tagStr = tags != null ? String.join(",", tags.stream().map(Enum::name).toList()) : "";
      stmt.setString(5, tagStr);

      DreamType type = entry.getDreamType();
      stmt.setString(6, type != null ? type.name() : null);
      stmt.setInt(7, entry.isAnalyzed() ? 1 : 0);
      stmt.setString(8, entry.getAnalysisResult());
      setNullableLong(stmt, 9, entry.getAnalyzedAt());
      stmt.setString(10, entry.getAnalysisVersion());
      stmt.setString(11, normalizeAnalysisStatus(entry.getAnalysisStatus()).name());

      stmt.setInt(12, entry.getId());

      stmt.executeUpdate();
    }
  }

  public void updateAnalysisFields(DreamEntry entry) throws SQLException {
    String sql =
        "UPDATE dreams SET analyzed = ?, analysis_result = ?, analyzed_at = ?, analysis_version = ?, analysis_status = ? WHERE id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setInt(1, entry.isAnalyzed() ? 1 : 0);
      stmt.setString(2, entry.getAnalysisResult());
      setNullableLong(stmt, 3, entry.getAnalyzedAt());
      stmt.setString(4, entry.getAnalysisVersion());
      stmt.setString(5, normalizeAnalysisStatus(entry.getAnalysisStatus()).name());
      stmt.setInt(6, entry.getId());

      stmt.executeUpdate();
    }
  }

  public List<DreamEntry> getAll() throws SQLException {
    List<DreamEntry> dreams = new ArrayList<>();
    String sql = "SELECT * FROM dreams";

    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        dreams.add(mapDreamEntry(rs));
      }
    }
    return dreams;
  }

  public DreamEntry findById(int id) throws SQLException {
    String sql = "SELECT * FROM dreams WHERE id = ?";

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setInt(1, id);
      ResultSet rs = stmt.executeQuery();

      if (rs.next()) {
        return mapDreamEntry(rs);
      }
    }
    return null;
  }

  private DreamEntry mapDreamEntry(ResultSet rs) throws SQLException {
    DreamEntry entry =
        new DreamEntry(
            rs.getString("title"),
            rs.getString("content"),
            rs.getString("dream_date"),
            rs.getLong("timestamp"),
            DreamSymbol.deserialize(rs.getString("symbol_tags")),
            parseDreamType(rs.getString("dream_type")),
            rs.getString("analysis_result"),
            getNullableLong(rs, "analyzed_at"),
            rs.getString("analysis_version"),
            parseAnalysisStatus(rs.getString("analysis_status"), rs.getInt("analyzed") == 1));
    entry.setId(rs.getInt("id"));
    return entry;
  }

  private DreamType parseDreamType(String value) {
    return value != null && !value.isBlank() ? DreamType.valueOf(value) : DreamType.NONE;
  }

  private AnalysisStatus parseAnalysisStatus(String value, boolean legacyAnalyzed) {
    if (value != null && !value.isBlank()) {
      return AnalysisStatus.valueOf(value);
    }
    return legacyAnalyzed ? AnalysisStatus.COMPLETED : AnalysisStatus.PENDING;
  }

  private AnalysisStatus normalizeAnalysisStatus(AnalysisStatus status) {
    return status != null ? status : AnalysisStatus.PENDING;
  }

  private void setNullableLong(PreparedStatement stmt, int index, Long value) throws SQLException {
    if (value == null) {
      stmt.setNull(index, Types.INTEGER);
    } else {
      stmt.setLong(index, value);
    }
  }

  private Long getNullableLong(ResultSet rs, String columnName) throws SQLException {
    long value = rs.getLong(columnName);
    return rs.wasNull() ? null : value;
  }
}

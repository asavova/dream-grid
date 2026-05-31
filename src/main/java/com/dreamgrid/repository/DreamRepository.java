package com.dreamgrid.repository;

import com.dreamgrid.model.AnalysisStatus;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamSymbol;
import com.dreamgrid.model.DreamType;
import java.sql.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

  public List<DreamEntry> findByTag(DreamSymbol tag) throws SQLException {
    return findByFilters(null, null, null, tag);
  }

  public List<DreamEntry> findByDreamType(DreamType dreamType) throws SQLException {
    return findByFilters(null, dreamType, null, null);
  }

  public List<DreamEntry> findByAnalysisStatus(AnalysisStatus analysisStatus) throws SQLException {
    return findByFilters(null, null, analysisStatus, null);
  }

  public List<DreamEntry> findByKeyword(String keyword) throws SQLException {
    return findByFilters(keyword, null, null, null);
  }

  public List<DreamEntry> findByFilters(
      String keyword, DreamType dreamType, AnalysisStatus analysisStatus, DreamSymbol tag)
      throws SQLException {
    StringBuilder sql = new StringBuilder("SELECT * FROM dreams WHERE 1 = 1");
    List<SqlParameter> parameters = new ArrayList<>();

    if (keyword != null && !keyword.isBlank()) {
      sql.append(" AND (LOWER(title) LIKE ? OR LOWER(content) LIKE ?)");
      String pattern = "%" + keyword.trim().toLowerCase() + "%";
      parameters.add(stmt -> stmt.setString(pattern));
      parameters.add(stmt -> stmt.setString(pattern));
    }

    if (dreamType != null) {
      sql.append(" AND dream_type = ?");
      parameters.add(stmt -> stmt.setString(dreamType.name()));
    }

    if (analysisStatus != null) {
      sql.append(" AND analysis_status = ?");
      parameters.add(stmt -> stmt.setString(analysisStatus.name()));
    }

    if (tag != null) {
      sql.append(" AND (',' || COALESCE(symbol_tags, '') || ',') LIKE ?");
      parameters.add(stmt -> stmt.setString("%," + tag.name() + ",%"));
    }

    sql.append(" ORDER BY timestamp DESC, id DESC");

    try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
      for (int i = 0; i < parameters.size(); i++) {
        parameters.get(i).apply(stmt, i + 1);
      }

      try (ResultSet rs = stmt.executeQuery()) {
        return mapDreamEntries(rs);
      }
    }
  }

  public Map<DreamSymbol, Integer> getTagUsageCounts() throws SQLException {
    Map<DreamSymbol, Integer> counts = new EnumMap<>(DreamSymbol.class);
    for (DreamSymbol symbol : DreamSymbol.values()) {
      counts.put(symbol, 0);
    }

    String sql = "SELECT symbol_tags FROM dreams";
    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        for (DreamSymbol symbol : DreamSymbol.deserialize(rs.getString("symbol_tags"))) {
          counts.put(symbol, counts.getOrDefault(symbol, 0) + 1);
        }
      }
    }
    return counts;
  }

  public List<DreamEntry> getAll() throws SQLException {
    String sql = "SELECT * FROM dreams";

    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      return mapDreamEntries(rs);
    }
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

  private List<DreamEntry> mapDreamEntries(ResultSet rs) throws SQLException {
    List<DreamEntry> dreams = new ArrayList<>();
    while (rs.next()) {
      dreams.add(mapDreamEntry(rs));
    }
    return dreams;
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

  @FunctionalInterface
  private interface SqlParameter {
    void apply(IndexedStatement stmt) throws SQLException;

    default void apply(PreparedStatement stmt, int index) throws SQLException {
      apply(new IndexedStatement(stmt, index));
    }
  }

  private record IndexedStatement(PreparedStatement stmt, int index) {
    void setString(String value) throws SQLException {
      stmt.setString(index, value);
    }
  }
}

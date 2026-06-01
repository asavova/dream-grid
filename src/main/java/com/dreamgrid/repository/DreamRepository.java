package com.dreamgrid.repository;

import com.dreamgrid.dto.TagCoOccurrenceResponse;
import com.dreamgrid.dto.TagUsage;
import com.dreamgrid.model.AnalysisStatus;
import com.dreamgrid.model.ClassificationSource;
import com.dreamgrid.model.DreamClassification;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamTag;
import com.dreamgrid.model.TagSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DreamRepository {
  private final Connection connection;

  public DreamRepository(Connection connection) {
    this.connection = connection;
  }

  public Connection getConnection() {
    return connection;
  }

  public void insert(DreamEntry entry) throws SQLException {
    String sql =
        """
INSERT INTO dreams (
  title, content, dream_date, timestamp, symbol_tags, dream_type,
  analyzed, analysis_result, analyzed_at, analysis_version, analysis_status,
  user_classification, inferred_classification, effective_classification,
  classification_source, classification_reason, type_confidence,
  classification_updated_at
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""";
    try (PreparedStatement stmt =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, entry.getTitle());
      stmt.setString(2, entry.getContent());
      stmt.setString(3, entry.getDreamDate());
      stmt.setLong(4, entry.getTimestamp());

      stmt.setString(5, "");

      stmt.setString(6, entry.getEffectiveClassification().name());

      stmt.setInt(7, entry.isAnalyzed() ? 1 : 0);
      stmt.setString(8, entry.getAnalysisResult());
      setNullableLong(stmt, 9, entry.getAnalyzedAt());
      stmt.setString(10, entry.getAnalysisVersion());
      stmt.setString(11, normalizeAnalysisStatus(entry.getAnalysisStatus()).name());
      setNullableClassification(stmt, 12, entry.getUserClassification());
      setNullableClassification(stmt, 13, entry.getInferredClassification());
      stmt.setString(14, entry.getEffectiveClassification().name());
      stmt.setString(15, entry.getClassificationSource().name());
      stmt.setString(16, entry.getClassificationReason());
      setNullableDouble(stmt, 17, entry.getTypeConfidence());
      setNullableLong(stmt, 18, entry.getClassificationUpdatedAt());

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
        """
UPDATE dreams SET
  title = ?, content = ?, dream_date = ?, timestamp = ?, symbol_tags = ?, dream_type = ?,
  analyzed = ?, analysis_result = ?, analyzed_at = ?, analysis_version = ?, analysis_status = ?,
  user_classification = ?, inferred_classification = ?, effective_classification = ?,
  classification_source = ?, classification_reason = ?, type_confidence = ?,
  classification_updated_at = ?
WHERE id = ?
""";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, entry.getTitle());
      stmt.setString(2, entry.getContent());
      stmt.setString(3, entry.getDreamDate());
      stmt.setLong(4, entry.getTimestamp());

      stmt.setString(5, "");

      stmt.setString(6, entry.getEffectiveClassification().name());
      stmt.setInt(7, entry.isAnalyzed() ? 1 : 0);
      stmt.setString(8, entry.getAnalysisResult());
      setNullableLong(stmt, 9, entry.getAnalyzedAt());
      stmt.setString(10, entry.getAnalysisVersion());
      stmt.setString(11, normalizeAnalysisStatus(entry.getAnalysisStatus()).name());
      setNullableClassification(stmt, 12, entry.getUserClassification());
      setNullableClassification(stmt, 13, entry.getInferredClassification());
      stmt.setString(14, entry.getEffectiveClassification().name());
      stmt.setString(15, entry.getClassificationSource().name());
      stmt.setString(16, entry.getClassificationReason());
      setNullableDouble(stmt, 17, entry.getTypeConfidence());
      setNullableLong(stmt, 18, entry.getClassificationUpdatedAt());

      stmt.setInt(19, entry.getId());

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

  public void updateClassificationFields(DreamEntry entry) throws SQLException {
    String sql =
        """
UPDATE dreams SET
  user_classification = ?,
  inferred_classification = ?,
  effective_classification = ?,
  classification_source = ?,
  classification_reason = ?,
  type_confidence = ?,
  classification_updated_at = ?,
  dream_type = ?
WHERE id = ?
""";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      setNullableClassification(stmt, 1, entry.getUserClassification());
      setNullableClassification(stmt, 2, entry.getInferredClassification());
      stmt.setString(3, entry.getEffectiveClassification().name());
      stmt.setString(4, entry.getClassificationSource().name());
      stmt.setString(5, entry.getClassificationReason());
      setNullableDouble(stmt, 6, entry.getTypeConfidence());
      setNullableLong(stmt, 7, entry.getClassificationUpdatedAt());
      stmt.setString(8, entry.getEffectiveClassification().name());
      stmt.setInt(9, entry.getId());
      stmt.executeUpdate();
    }
  }

  public List<DreamEntry> findByTag(String normalizedTag) throws SQLException {
    return findByFilters(null, null, null, normalizedTag);
  }

  public List<DreamEntry> findByAnalysisStatus(AnalysisStatus analysisStatus) throws SQLException {
    return findByFilters(null, null, analysisStatus, null);
  }

  public List<DreamEntry> findByKeyword(String keyword) throws SQLException {
    return findByFilters(keyword, null, null, null);
  }

  public List<DreamEntry> findByFilters(
      String keyword,
      DreamClassification classification,
      AnalysisStatus analysisStatus,
      String normalizedTag)
      throws SQLException {
    StringBuilder sql = new StringBuilder("SELECT * FROM dreams WHERE 1 = 1");
    List<SqlParameter> parameters = new ArrayList<>();

    if (keyword != null && !keyword.isBlank()) {
      sql.append(" AND (LOWER(title) LIKE ? OR LOWER(content) LIKE ?)");
      String pattern = "%" + keyword.trim().toLowerCase() + "%";
      parameters.add(stmt -> stmt.setString(pattern));
      parameters.add(stmt -> stmt.setString(pattern));
    }

    if (classification != null) {
      sql.append(" AND effective_classification = ?");
      parameters.add(stmt -> stmt.setString(classification.name()));
    }

    if (analysisStatus != null) {
      sql.append(" AND analysis_status = ?");
      parameters.add(stmt -> stmt.setString(analysisStatus.name()));
    }

    if (normalizedTag != null && !normalizedTag.isBlank()) {
      sql.append(
          " AND id IN (SELECT dream_id FROM dream_tag_links l JOIN dream_tags t ON t.id = l.tag_id WHERE t.normalized_name = ?)");
      parameters.add(stmt -> stmt.setString(normalizedTag));
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

  public List<TagUsage> getTagUsageCounts() throws SQLException {
    String sql =
        """
WITH dream_tag_presence AS (
  SELECT DISTINCT l.dream_id, t.id AS tag_id, t.name, t.normalized_name
  FROM dream_tag_links l
  JOIN dream_tags t ON t.id = l.tag_id
)
SELECT name, normalized_name, COUNT(DISTINCT dream_id) AS usage_count
FROM dream_tag_presence
GROUP BY tag_id, name, normalized_name
ORDER BY usage_count DESC, normalized_name ASC
""";
    List<TagUsage> usages = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        usages.add(
            new TagUsage(
                rs.getString("name"), rs.getString("normalized_name"), rs.getInt("usage_count")));
      }
    }
    return usages;
  }

  public List<TagUsage> getRecurringTagUsageCounts() throws SQLException {
    String sql =
        """
WITH dream_tag_presence AS (
  SELECT DISTINCT l.dream_id, t.id AS tag_id, t.name, t.normalized_name
  FROM dream_tag_links l
  JOIN dream_tags t ON t.id = l.tag_id
)
SELECT name, normalized_name, COUNT(DISTINCT dream_id) AS usage_count
FROM dream_tag_presence
GROUP BY tag_id, name, normalized_name
HAVING COUNT(DISTINCT dream_id) > 1
ORDER BY usage_count DESC, normalized_name ASC
""";
    List<TagUsage> usages = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        usages.add(
            new TagUsage(
                rs.getString("name"), rs.getString("normalized_name"), rs.getInt("usage_count")));
      }
    }
    return usages;
  }

  public List<TagCoOccurrenceResponse> getTagCoOccurrences() throws SQLException {
    String sql =
        """
WITH dream_tag_presence AS (
  SELECT DISTINCT l.dream_id, t.normalized_name
  FROM dream_tag_links l
  JOIN dream_tags t ON t.id = l.tag_id
)
SELECT p1.normalized_name AS first_tag,
       p2.normalized_name AS second_tag,
       COUNT(DISTINCT p1.dream_id) AS pair_count
FROM dream_tag_presence p1
JOIN dream_tag_presence p2
  ON p1.dream_id = p2.dream_id
 AND p1.normalized_name < p2.normalized_name
GROUP BY first_tag, second_tag
ORDER BY pair_count DESC, first_tag ASC, second_tag ASC
""";
    List<TagCoOccurrenceResponse> pairs = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        pairs.add(
            new TagCoOccurrenceResponse(
                rs.getString("first_tag"), rs.getString("second_tag"), rs.getInt("pair_count")));
      }
    }
    return pairs;
  }

  public List<TagCoOccurrenceResponse> getRelatedTagCoOccurrences(String normalizedTag)
      throws SQLException {
    String sql =
        """
WITH dream_tag_presence AS (
  SELECT DISTINCT l.dream_id, t.normalized_name
  FROM dream_tag_links l
  JOIN dream_tags t ON t.id = l.tag_id
),
tag_pairs AS (
  SELECT p1.normalized_name AS first_tag,
         p2.normalized_name AS second_tag,
         COUNT(DISTINCT p1.dream_id) AS pair_count
  FROM dream_tag_presence p1
  JOIN dream_tag_presence p2
    ON p1.dream_id = p2.dream_id
   AND p1.normalized_name < p2.normalized_name
  GROUP BY first_tag, second_tag
)
SELECT first_tag, second_tag, pair_count
FROM tag_pairs
WHERE first_tag = ? OR second_tag = ?
ORDER BY pair_count DESC, first_tag ASC, second_tag ASC
""";
    List<TagCoOccurrenceResponse> pairs = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, normalizedTag);
      stmt.setString(2, normalizedTag);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          pairs.add(
              new TagCoOccurrenceResponse(
                  rs.getString("first_tag"), rs.getString("second_tag"), rs.getInt("pair_count")));
        }
      }
    }
    return pairs;
  }

  public List<Integer> findRecentDreamIdsByTag(String normalizedTag, int limit)
      throws SQLException {
    String sql =
        """
SELECT DISTINCT d.id
FROM dreams d
JOIN dream_tag_links l ON l.dream_id = d.id
JOIN dream_tags t ON t.id = l.tag_id
WHERE t.normalized_name = ?
ORDER BY d.timestamp DESC, d.id DESC
LIMIT ?
""";
    List<Integer> dreamIds = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, normalizedTag);
      stmt.setInt(2, limit);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          dreamIds.add(rs.getInt("id"));
        }
      }
    }
    return dreamIds;
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

  public void deleteById(int id) throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM dreams WHERE id = ?")) {
      stmt.setInt(1, id);
      stmt.executeUpdate();
    }
  }

  public void deleteUnlinkedTags() throws SQLException {
    try (PreparedStatement stmt =
        connection.prepareStatement(
            "DELETE FROM dream_tags WHERE id NOT IN (SELECT DISTINCT tag_id FROM dream_tag_links)")) {
      stmt.executeUpdate();
    }
  }

  public DreamTag createOrFindTag(DreamTag tag) throws SQLException {
    DreamTag existing = findTagByNormalizedName(tag.getNormalizedName());
    if (existing != null) {
      return existing;
    }

    String sql = "INSERT INTO dream_tags (name, normalized_name, created_at) VALUES (?, ?, ?)";
    long createdAt = tag.getCreatedAt() > 0 ? tag.getCreatedAt() : System.currentTimeMillis();
    try (PreparedStatement stmt =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, tag.getName());
      stmt.setString(2, tag.getNormalizedName());
      stmt.setLong(3, createdAt);
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          tag.setId(rs.getInt(1));
        }
      }
    }
    tag.setCreatedAt(createdAt);
    return tag;
  }

  public DreamTag findTagByNormalizedName(String normalizedName) throws SQLException {
    String sql =
        "SELECT id, name, normalized_name, created_at FROM dream_tags WHERE normalized_name = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, normalizedName);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return new DreamTag(
              rs.getInt("id"),
              rs.getString("name"),
              rs.getString("normalized_name"),
              null,
              null,
              rs.getLong("created_at"));
        }
      }
    }
    return null;
  }

  public void linkTagToDream(int dreamId, DreamTag tag, TagSource source, Double confidenceScore)
      throws SQLException {
    DreamTag persisted = createOrFindTag(tag);
    String sql =
        """
INSERT INTO dream_tag_links (dream_id, tag_id, source, confidence_score, created_at)
VALUES (?, ?, ?, ?, ?)
ON CONFLICT(dream_id, tag_id, source)
DO UPDATE SET confidence_score = excluded.confidence_score
""";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setInt(1, dreamId);
      stmt.setInt(2, persisted.getId());
      stmt.setString(3, source.name());
      if (confidenceScore == null) {
        stmt.setNull(4, Types.REAL);
      } else {
        stmt.setDouble(4, confidenceScore);
      }
      stmt.setLong(5, System.currentTimeMillis());
      stmt.executeUpdate();
    }
  }

  public void replaceAnalysisTags(int dreamId, List<DreamTag> tags) throws SQLException {
    try (PreparedStatement stmt =
        connection.prepareStatement(
            "DELETE FROM dream_tag_links WHERE dream_id = ? AND source = ?")) {
      stmt.setInt(1, dreamId);
      stmt.setString(2, TagSource.ANALYSIS.name());
      stmt.executeUpdate();
    }

    for (DreamTag tag : tags) {
      linkTagToDream(dreamId, tag, TagSource.ANALYSIS, tag.getConfidenceScore());
    }
  }

  public List<DreamTag> listTagsForDream(int dreamId) throws SQLException {
    String sql =
        """
SELECT t.id, t.name, t.normalized_name, l.source, l.confidence_score, l.created_at
FROM dream_tag_links l
JOIN dream_tags t ON t.id = l.tag_id
WHERE l.dream_id = ?
ORDER BY t.normalized_name ASC, l.source ASC
""";
    List<DreamTag> tags = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setInt(1, dreamId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          tags.add(mapDreamTag(rs));
        }
      }
    }
    return tags;
  }

  public int countDreamsSharingAtLeastTags(
      int dreamId, List<String> normalizedTags, int minimumShared) throws SQLException {
    if (normalizedTags == null || normalizedTags.isEmpty()) {
      return 0;
    }

    String placeholders = String.join(",", normalizedTags.stream().map(tag -> "?").toList());
    String sql =
        """
SELECT COUNT(*) AS matching_dreams
FROM (
  SELECT l.dream_id
  FROM dream_tag_links l
  JOIN dream_tags t ON t.id = l.tag_id
  WHERE l.dream_id <> ? AND t.normalized_name IN (
"""
            + placeholders
            + """
)
  GROUP BY l.dream_id
  HAVING COUNT(DISTINCT t.normalized_name) >= ?
)
""";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setInt(1, dreamId);
      for (int i = 0; i < normalizedTags.size(); i++) {
        stmt.setString(i + 2, normalizedTags.get(i));
      }
      stmt.setInt(normalizedTags.size() + 2, minimumShared);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? rs.getInt("matching_dreams") : 0;
      }
    }
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
            List.of(),
            resolveLegacyClassification(
                rs.getString("effective_classification"), rs.getString("dream_type")),
            rs.getString("analysis_result"),
            getNullableLong(rs, "analyzed_at"),
            rs.getString("analysis_version"),
            parseAnalysisStatus(rs.getString("analysis_status"), rs.getInt("analyzed") == 1));
    entry.setId(rs.getInt("id"));
    entry.setUserClassification(parseNullableClassification(rs.getString("user_classification")));
    entry.setInferredClassification(
        parseNullableClassification(rs.getString("inferred_classification")));
    entry.setEffectiveClassification(parseClassification(rs.getString("effective_classification")));
    entry.setClassificationSource(parseClassificationSource(rs.getString("classification_source")));
    entry.setClassificationReason(rs.getString("classification_reason"));
    entry.setTypeConfidence(getNullableDouble(rs, "type_confidence"));
    entry.setClassificationUpdatedAt(getNullableLong(rs, "classification_updated_at"));
    entry.setSymbolTags(listTagsForDream(entry.getId()));
    return entry;
  }

  private DreamTag mapDreamTag(ResultSet rs) throws SQLException {
    double confidenceScore = rs.getDouble("confidence_score");
    return new DreamTag(
        rs.getInt("id"),
        rs.getString("name"),
        rs.getString("normalized_name"),
        TagSource.valueOf(rs.getString("source")),
        rs.wasNull() ? null : confidenceScore,
        rs.getLong("created_at"));
  }

  private DreamClassification parseClassification(String value) {
    if (value == null || value.isBlank()) {
      return DreamClassification.UNKNOWN;
    }
    try {
      return DreamClassification.valueOf(value);
    } catch (IllegalArgumentException e) {
      return DreamClassification.UNKNOWN;
    }
  }

  private DreamClassification resolveLegacyClassification(
      String effectiveClassification, String legacyClassificationValue) {
    DreamClassification parsed = parseClassification(effectiveClassification);
    if (parsed != DreamClassification.UNKNOWN) {
      return parsed;
    }
    return parseClassification(legacyClassificationValue);
  }

  private DreamClassification parseNullableClassification(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return DreamClassification.valueOf(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private ClassificationSource parseClassificationSource(String value) {
    if (value == null || value.isBlank()) {
      return ClassificationSource.UNKNOWN;
    }
    if ("ANALYSIS".equals(value) || "PATTERN_ENGINE".equals(value)) {
      return ClassificationSource.INFERRED;
    }
    try {
      return ClassificationSource.valueOf(value);
    } catch (IllegalArgumentException e) {
      return ClassificationSource.UNKNOWN;
    }
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

  private void setNullableDouble(PreparedStatement stmt, int index, Double value)
      throws SQLException {
    if (value == null) {
      stmt.setNull(index, Types.REAL);
    } else {
      stmt.setDouble(index, value);
    }
  }

  private void setNullableClassification(
      PreparedStatement stmt, int index, DreamClassification value) throws SQLException {
    if (value == null) {
      stmt.setNull(index, Types.VARCHAR);
    } else {
      stmt.setString(index, value.name());
    }
  }

  private Long getNullableLong(ResultSet rs, String columnName) throws SQLException {
    long value = rs.getLong(columnName);
    return rs.wasNull() ? null : value;
  }

  private Double getNullableDouble(ResultSet rs, String columnName) throws SQLException {
    double value = rs.getDouble(columnName);
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

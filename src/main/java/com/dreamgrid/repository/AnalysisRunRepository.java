package com.dreamgrid.repository;

import com.dreamgrid.model.AnalysisRun;
import com.dreamgrid.model.AnalysisStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AnalysisRunRepository {
  private final Connection connection;

  public AnalysisRunRepository(Connection connection) {
    this.connection = connection;
  }

  public Connection getConnection() {
    return connection;
  }

  public AnalysisRun createPendingRun(int dreamId, long requestedAt) throws SQLException {
    AnalysisRun run = new AnalysisRun(dreamId, requestedAt, AnalysisStatus.PENDING);
    String sql =
        """
INSERT INTO analysis_runs (
  dream_id, requested_at, completed_at, status, analysis_version, analysis_result, failure_reason
) VALUES (?, ?, NULL, ?, NULL, NULL, NULL)
""";
    try (PreparedStatement stmt =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setInt(1, dreamId);
      stmt.setLong(2, requestedAt);
      stmt.setString(3, run.getStatus().name());
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          run.setId(rs.getInt(1));
        }
      }
    }
    return run;
  }

  public void markCompleted(
      int runId, long completedAt, String analysisVersion, String analysisResult)
      throws SQLException {
    String sql =
        """
UPDATE analysis_runs
SET completed_at = ?, status = ?, analysis_version = ?, analysis_result = ?, failure_reason = NULL
WHERE id = ?
""";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, completedAt);
      stmt.setString(2, AnalysisStatus.COMPLETED.name());
      stmt.setString(3, analysisVersion);
      stmt.setString(4, analysisResult);
      stmt.setInt(5, runId);
      stmt.executeUpdate();
    }
  }

  public void markFailed(int runId, long completedAt, String failureReason) throws SQLException {
    String sql =
        """
UPDATE analysis_runs
SET completed_at = ?, status = ?, failure_reason = ?
WHERE id = ?
""";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, completedAt);
      stmt.setString(2, AnalysisStatus.FAILED.name());
      stmt.setString(3, failureReason);
      stmt.setInt(4, runId);
      stmt.executeUpdate();
    }
  }

  public List<AnalysisRun> findByDreamId(int dreamId) throws SQLException {
    String sql =
        "SELECT * FROM analysis_runs WHERE dream_id = ? ORDER BY requested_at DESC, id DESC";
    List<AnalysisRun> runs = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setInt(1, dreamId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          runs.add(mapRun(rs));
        }
      }
    }
    return runs;
  }

  public AnalysisRun findLatestByDreamId(int dreamId) throws SQLException {
    String sql =
        "SELECT * FROM analysis_runs WHERE dream_id = ? ORDER BY requested_at DESC, id DESC LIMIT 1";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setInt(1, dreamId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return mapRun(rs);
        }
      }
    }
    return null;
  }

  private AnalysisRun mapRun(ResultSet rs) throws SQLException {
    return new AnalysisRun(
        rs.getInt("id"),
        rs.getInt("dream_id"),
        rs.getLong("requested_at"),
        getNullableLong(rs, "completed_at"),
        parseStatus(rs.getString("status")),
        rs.getString("analysis_version"),
        rs.getString("analysis_result"),
        rs.getString("failure_reason"));
  }

  private AnalysisStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return AnalysisStatus.PENDING;
    }
    return AnalysisStatus.valueOf(status);
  }

  private Long getNullableLong(ResultSet rs, String columnName) throws SQLException {
    long value = rs.getLong(columnName);
    return rs.wasNull() ? null : value;
  }
}

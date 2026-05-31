package com.dreamgrid.repository;

import com.dreamgrid.model.DreamQuestion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DreamQuestionRepository {
  private final Connection connection;

  public DreamQuestionRepository(Connection connection) {
    this.connection = connection;
  }

  public DreamQuestion insert(int dreamId, Integer analysisRunId, String question, String answer)
      throws SQLException {
    long createdAt = System.currentTimeMillis();
    String sql =
        """
INSERT INTO dream_questions (dream_id, analysis_run_id, question, answer, created_at)
VALUES (?, ?, ?, ?, ?)
""";
    try (PreparedStatement stmt =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setInt(1, dreamId);
      if (analysisRunId == null) {
        stmt.setNull(2, Types.INTEGER);
      } else {
        stmt.setInt(2, analysisRunId);
      }
      stmt.setString(3, question);
      stmt.setString(4, answer);
      stmt.setLong(5, createdAt);
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          return new DreamQuestion(
              rs.getInt(1), dreamId, analysisRunId, question, answer, createdAt);
        }
      }
    }
    throw new SQLException("Failed to insert dream question");
  }

  public List<DreamQuestion> findByDreamId(int dreamId) throws SQLException {
    String sql =
        "SELECT * FROM dream_questions WHERE dream_id = ? ORDER BY created_at DESC, id DESC";
    List<DreamQuestion> questions = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setInt(1, dreamId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          questions.add(map(rs));
        }
      }
    }
    return questions;
  }

  public DreamQuestion findByIdAndDreamId(int id, int dreamId) throws SQLException {
    String sql = "SELECT * FROM dream_questions WHERE id = ? AND dream_id = ?";
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setInt(1, id);
      stmt.setInt(2, dreamId);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? map(rs) : null;
      }
    }
  }

  private DreamQuestion map(ResultSet rs) throws SQLException {
    int analysisRunId = rs.getInt("analysis_run_id");
    Integer nullableRun = rs.wasNull() ? null : analysisRunId;
    return new DreamQuestion(
        rs.getInt("id"),
        rs.getInt("dream_id"),
        nullableRun,
        rs.getString("question"),
        rs.getString("answer"),
        rs.getLong("created_at"));
  }
}

package com.dreamgrid.testsupport;

import java.sql.Connection;
import java.sql.Statement;

public final class TestSchema {
  private TestSchema() {}

  public static void createCurrentSchema(Connection connection) throws Exception {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("PRAGMA foreign_keys = ON");
      stmt.execute(
          """
CREATE TABLE dreams (
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
    type_confidence REAL,
    classification_updated_at INTEGER
);
""");
      stmt.execute(
          """
CREATE TABLE dream_tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    normalized_name TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL
);
""");
      stmt.execute(
          """
CREATE TABLE dream_tag_links (
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
      stmt.execute(
          """
CREATE TABLE analysis_runs (
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
      stmt.execute(
          """
CREATE TABLE dream_questions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dream_id INTEGER NOT NULL,
    analysis_run_id INTEGER,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (dream_id) REFERENCES dreams(id) ON DELETE CASCADE,
    FOREIGN KEY (analysis_run_id) REFERENCES analysis_runs(id)
);
""");
    }
  }

  public static void createLegacyDreamsOnlySchema(Connection connection) throws Exception {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(
          """
CREATE TABLE dreams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    dream_date TEXT,
    timestamp INTEGER,
    symbol_tags TEXT,
    dream_type TEXT,
    analyzed INTEGER DEFAULT 0
);
""");
    }
  }
}

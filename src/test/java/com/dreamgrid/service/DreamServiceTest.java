package com.dreamgrid.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.dreamgrid.client.DreamAnalysisClient;
import com.dreamgrid.model.AnalysisStatus;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamSymbol;
import com.dreamgrid.model.DreamType;
import com.dreamgrid.repository.DreamRepository;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class DreamServiceTest {
  private Connection connection;
  private DreamRepository repository;
  private FakeAnalysisClient analysisClient;
  private DreamService dreamService;

  @Before
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    createSchema(connection);
    repository = new DreamRepository(connection);
    analysisClient = new FakeAnalysisClient();
    dreamService = new DreamService(repository, analysisClient, "v1");
  }

  @Test
  public void completedAnalysisReturnsCachedResultWithoutCallingClient() throws Exception {
    DreamEntry dream = completedDream("cached analysis", 100L, "v1");
    repository.insert(dream);

    String analysis = dreamService.analyzeDream(dream.getId());

    assertEquals("cached analysis", analysis);
    assertEquals(0, analysisClient.calls);
  }

  @Test
  public void forcedReanalysisCallsClientAndReplacesAnalysis() throws Exception {
    DreamEntry dream = completedDream("old analysis", 100L, "v1");
    repository.insert(dream);
    analysisClient.nextResult = "new analysis";

    String analysis = dreamService.reanalyzeDream(dream.getId());
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals("new analysis", analysis);
    assertEquals(1, analysisClient.calls);
    assertEquals("new analysis", reloaded.getAnalysisResult());
    assertEquals(AnalysisStatus.COMPLETED, reloaded.getAnalysisStatus());
    assertEquals("v1", reloaded.getAnalysisVersion());
  }

  @Test
  public void failedReanalysisPreservesPreviousSuccessfulResult() throws Exception {
    DreamEntry dream = completedDream("old analysis", 100L, "v1");
    repository.insert(dream);
    analysisClient.nextFailure = new IOException("analysis service unavailable");

    assertThrows(IOException.class, () -> dreamService.reanalyzeDream(dream.getId()));
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals(1, analysisClient.calls);
    assertEquals("old analysis", reloaded.getAnalysisResult());
    assertEquals(Long.valueOf(100L), reloaded.getAnalyzedAt());
    assertEquals("v1", reloaded.getAnalysisVersion());
    assertEquals(AnalysisStatus.FAILED, reloaded.getAnalysisStatus());
  }

  @Test
  public void missingDreamFailsBeforeCallingClient() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> dreamService.analyzeDream(999));

    assertEquals("Dream with ID 999 not found.", exception.getMessage());
    assertEquals(0, analysisClient.calls);
  }

  @Test
  public void staleAnalysisVersionCallsClient() throws Exception {
    DreamEntry dream = completedDream("cached analysis", 100L, "v0");
    repository.insert(dream);
    analysisClient.nextResult = "fresh analysis";

    String analysis = dreamService.analyzeDream(dream.getId());
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals("fresh analysis", analysis);
    assertEquals(1, analysisClient.calls);
    assertEquals("fresh analysis", reloaded.getAnalysisResult());
    assertEquals("v1", reloaded.getAnalysisVersion());
  }

  private DreamEntry completedDream(String analysis, long analyzedAt, String version) {
    DreamEntry dream =
        new DreamEntry(
            "Dream",
            "I crossed a bright sky portal.",
            "2026-05-31",
            123L,
            List.of(DreamSymbol.SKY, DreamSymbol.PORTAL),
            DreamType.VISION);
    dream.completeAnalysis(analysis, analyzedAt, version);
    return dream;
  }

  private void createSchema(Connection connection) throws Exception {
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
    analyzed INTEGER DEFAULT 0,
    analysis_result TEXT,
    analyzed_at INTEGER,
    analysis_version TEXT,
    analysis_status TEXT NOT NULL DEFAULT 'PENDING'
);
""");
    }
  }

  private static class FakeAnalysisClient extends DreamAnalysisClient {
    private int calls;
    private String nextResult = "generated analysis";
    private IOException nextFailure;

    @Override
    public String analyzeDream(String dream) throws IOException {
      calls++;
      if (nextFailure != null) {
        throw nextFailure;
      }
      return nextResult;
    }
  }
}

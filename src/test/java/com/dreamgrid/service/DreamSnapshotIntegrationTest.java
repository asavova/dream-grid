package com.dreamgrid.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dreamgrid.client.DreamAnalysisClient;
import com.dreamgrid.model.DreamClassification;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamSnapshot;
import com.dreamgrid.repository.DreamRepository;
import com.dreamgrid.testsupport.TestSchema;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class DreamSnapshotIntegrationTest {
  private DreamService dreamService;
  private DreamRepository repository;
  private TrackingAnalysisClient analysisClient;

  @Before
  public void setUp() throws Exception {
    Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    TestSchema.createCurrentSchema(connection);
    repository = new DreamRepository(connection);
    analysisClient = new TrackingAnalysisClient();
    dreamService = new DreamService(repository, analysisClient, "test-version");
  }

  @Test
  public void analyzeUsesSnapshotReadOnlyStateForClientCall() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Snapshot",
            "Original content",
            "2026-06-01",
            DreamClassification.UNKNOWN,
            List.of("manual"));
    analysisClient.nextResult = "{\"summary\":\"ok\",\"modelVersion\":\"v1\"}";

    dreamService.analyzeDream(dream.getId());

    DreamSnapshot snapshot = DreamSnapshot.from(repository.findById(dream.getId()));
    assertEquals("Original content", analysisClient.lastAnalyzedDream);
    assertTrue(snapshot.hasUsableAnalysis());
    assertEquals("v1", snapshot.getAnalysisVersion());
  }

  @Test
  public void askQuestionUsesSnapshotAnalysisContext() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Snapshot",
            "Question content",
            "2026-06-01",
            DreamClassification.UNKNOWN,
            List.of("manual"));
    analysisClient.nextResult =
        "{\"summary\":\"analysis for question\",\"modelVersion\":\"test-version\"}";
    dreamService.analyzeDream(dream.getId());
    analysisClient.nextAnswer = "answer";

    dreamService.askQuestionAboutDream(dream.getId(), "What does it mean?");

    assertEquals("Question content", analysisClient.lastQuestionDream);
    assertTrue(analysisClient.lastQuestionAnalysis.contains("analysis for question"));
  }

  private static class TrackingAnalysisClient extends DreamAnalysisClient {
    private String nextResult = "analysis";
    private String nextAnswer = "answer";
    private String lastAnalyzedDream;
    private String lastQuestionDream;
    private String lastQuestionAnalysis;

    @Override
    public String analyzeDream(String dream) {
      lastAnalyzedDream = dream;
      return nextResult;
    }

    @Override
    public String askQuestion(String dream, String analysis, String question) {
      lastQuestionDream = dream;
      lastQuestionAnalysis = analysis;
      return nextAnswer;
    }
  }
}

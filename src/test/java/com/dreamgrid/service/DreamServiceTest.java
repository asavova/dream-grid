package com.dreamgrid.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dreamgrid.api.ApiErrorCode;
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
  private static final String EXPECTED_ANALYSIS_VERSION = "test-analysis-version";

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
    dreamService = new DreamService(repository, analysisClient, EXPECTED_ANALYSIS_VERSION);
  }

  @Test
  public void completedAnalysisReturnsCachedResultWithoutCallingClient() throws Exception {
    DreamEntry dream = completedDream("cached analysis", 100L, EXPECTED_ANALYSIS_VERSION);
    repository.insert(dream);

    String analysis = dreamService.analyzeDream(dream.getId());

    assertEquals("cached analysis", analysis);
    assertEquals(0, analysisClient.calls);
  }

  @Test
  public void forcedReanalysisCallsClientAndReplacesAnalysis() throws Exception {
    DreamEntry dream = completedDream("old analysis", 100L, EXPECTED_ANALYSIS_VERSION);
    repository.insert(dream);
    analysisClient.nextResult = "new analysis";

    String analysis = dreamService.reanalyzeDream(dream.getId());
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals("new analysis", analysis);
    assertEquals(1, analysisClient.calls);
    assertEquals("new analysis", reloaded.getAnalysisResult());
    assertEquals(AnalysisStatus.COMPLETED, reloaded.getAnalysisStatus());
    assertEquals(EXPECTED_ANALYSIS_VERSION, reloaded.getAnalysisVersion());
  }

  @Test
  public void failedReanalysisPreservesPreviousSuccessfulResult() throws Exception {
    DreamEntry dream = completedDream("old analysis", 100L, EXPECTED_ANALYSIS_VERSION);
    repository.insert(dream);
    analysisClient.nextFailure = new IOException("analysis service unavailable");

    assertThrows(IOException.class, () -> dreamService.reanalyzeDream(dream.getId()));
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals(1, analysisClient.calls);
    assertEquals("old analysis", reloaded.getAnalysisResult());
    assertEquals(Long.valueOf(100L), reloaded.getAnalyzedAt());
    assertEquals(EXPECTED_ANALYSIS_VERSION, reloaded.getAnalysisVersion());
    assertEquals(AnalysisStatus.FAILED, reloaded.getAnalysisStatus());
  }

  @Test
  public void missingDreamFailsBeforeCallingClient() {
    DreamGridException exception =
        assertThrows(DreamGridException.class, () -> dreamService.analyzeDream(999));

    assertEquals(ApiErrorCode.NOT_FOUND, exception.getErrorCode());
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
    assertEquals(EXPECTED_ANALYSIS_VERSION, reloaded.getAnalysisVersion());
  }

  @Test
  public void storesModelVersionReturnedByAnalysisService() throws Exception {
    DreamEntry dream =
        new DreamEntry(
            "Dream",
            "I crossed a bright sky portal.",
            "2026-05-31",
            123L,
            List.of(DreamSymbol.SKY, DreamSymbol.PORTAL),
            DreamType.VISION);
    repository.insert(dream);
    analysisClient.nextResult =
        "{\"summary\":\"A transition dream.\",\"detectedSymbols\":[\"SKY\"],\"modelVersion\":\"python-model-2026-05\"}";

    dreamService.analyzeDream(dream.getId());
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals("python-model-2026-05", reloaded.getAnalysisVersion());
  }

  @Test
  public void questionAboutAnalyzedDreamUsesStoredAnalysisContext() throws Exception {
    DreamEntry dream =
        completedDream("{\"summary\":\"A transition dream.\"}", 100L, EXPECTED_ANALYSIS_VERSION);
    repository.insert(dream);
    analysisClient.nextAnswer = "The portal points to transition.";

    String answer = dreamService.askQuestionAboutDream(dream.getId(), "What does the portal mean?");

    assertEquals("The portal points to transition.", answer);
    assertEquals(1, analysisClient.questionCalls);
    assertEquals("I crossed a bright sky portal.", analysisClient.lastQuestionDream);
    assertEquals("{\"summary\":\"A transition dream.\"}", analysisClient.lastQuestionAnalysis);
    assertEquals("What does the portal mean?", analysisClient.lastQuestion);
  }

  @Test
  public void questionBeforeAnalysisFailsCleanly() throws Exception {
    DreamEntry dream =
        new DreamEntry(
            "Dream",
            "I crossed a bright sky portal.",
            "2026-05-31",
            123L,
            List.of(DreamSymbol.SKY, DreamSymbol.PORTAL),
            DreamType.VISION);
    repository.insert(dream);

    DreamGridException exception =
        assertThrows(
            DreamGridException.class,
            () -> dreamService.askQuestionAboutDream(dream.getId(), "What does it mean?"));

    assertEquals(ApiErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    assertEquals("Dream must be analyzed before asking questions.", exception.getMessage());
    assertEquals(0, analysisClient.questionCalls);
  }

  @Test
  public void saveDreamNormalizesTagsAndRemovesDuplicates() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Tagged Dream",
            "A dream about fire.",
            "2026-05-31",
            DreamType.ORDINARY,
            List.of(" fire ", "FIRE", "", "sky", "not-a-symbol"));

    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals(List.of(DreamSymbol.FIRE, DreamSymbol.SKY), reloaded.getSymbolTags());
  }

  @Test
  public void searchByTagReturnsMatchingDreams() throws Exception {
    DreamEntry fireDream =
        dreamService.saveDream(
            "Fire Dream", "The horizon burned.", "2026-05-31", DreamType.VISION, List.of("fire"));
    dreamService.saveDream(
        "Water Dream",
        "Rain covered the road.",
        "2026-05-31",
        DreamType.ORDINARY,
        List.of("water"));

    List<DreamEntry> results = dreamService.getDreamsByTag(" FIRE ");

    assertEquals(1, results.size());
    assertEquals(fireDream.getId(), results.get(0).getId());
  }

  @Test
  public void keywordSearchChecksTitleAndContent() throws Exception {
    DreamEntry titleMatch =
        dreamService.saveDream(
            "Ocean Door", "I was in a quiet room.", "2026-05-31", DreamType.ORDINARY, null);
    DreamEntry contentMatch =
        dreamService.saveDream(
            "Plain Dream", "The ocean was dark.", "2026-05-31", DreamType.ORDINARY, null);
    dreamService.saveDream(
        "Sky Dream", "Birds crossed the sky.", "2026-05-31", DreamType.ORDINARY, null);

    List<Integer> resultIds =
        dreamService.searchDreams("ocean").stream().map(DreamEntry::getId).toList();

    assertEquals(2, resultIds.size());
    assertTrue(resultIds.contains(titleMatch.getId()));
    assertTrue(resultIds.contains(contentMatch.getId()));
  }

  @Test
  public void filteringByAnalysisStatusWorks() throws Exception {
    DreamEntry completed = completedDream("cached analysis", 100L, EXPECTED_ANALYSIS_VERSION);
    repository.insert(completed);
    dreamService.saveDream(
        "Pending Dream", "No analysis yet.", "2026-05-31", DreamType.ORDINARY, null);

    List<DreamEntry> results = dreamService.filterDreams(null, "completed", null);

    assertEquals(1, results.size());
    assertEquals(completed.getId(), results.get(0).getId());
  }

  @Test
  public void unknownTagReturnsEmptyResult() throws Exception {
    dreamService.saveDream(
        "Fire Dream", "The horizon burned.", "2026-05-31", DreamType.VISION, List.of("fire"));

    List<DreamEntry> results = dreamService.getDreamsByTag("not-a-symbol");

    assertTrue(results.isEmpty());
  }

  @Test
  public void tagUsageCountsAreCorrect() throws Exception {
    dreamService.saveDream(
        "Fire Sky", "A bright dream.", "2026-05-31", DreamType.VISION, List.of("fire", "sky"));
    dreamService.saveDream(
        "Fire", "Another bright dream.", "2026-05-31", DreamType.VISION, List.of("fire"));
    dreamService.saveDream("Unknown", "No specific tag.", "2026-05-31", DreamType.ORDINARY, null);

    assertEquals(Integer.valueOf(2), dreamService.getTagUsageCounts().get(DreamSymbol.FIRE));
    assertEquals(Integer.valueOf(1), dreamService.getTagUsageCounts().get(DreamSymbol.SKY));
    assertEquals(Integer.valueOf(1), dreamService.getTagUsageCounts().get(DreamSymbol.UNKNOWN));
  }

  @Test
  public void blankDreamContentIsRejected() {
    DreamGridException exception =
        assertThrows(
            DreamGridException.class,
            () -> dreamService.saveDream("Blank", "   ", "2026-05-31", DreamType.ORDINARY, null));

    assertEquals(ApiErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    assertEquals("Dream content must not be blank", exception.getMessage());
  }

  @Test
  public void overlyLongQuestionIsRejected() throws Exception {
    DreamEntry dream =
        completedDream("{\"summary\":\"A transition dream.\"}", 100L, EXPECTED_ANALYSIS_VERSION);
    repository.insert(dream);
    String question = "a".repeat(DreamValidator.MAX_QUESTION_LENGTH + 1);

    DreamGridException exception =
        assertThrows(
            DreamGridException.class,
            () -> dreamService.askQuestionAboutDream(dream.getId(), question));

    assertEquals(ApiErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    assertEquals("Question is too long", exception.getMessage());
    assertEquals(0, analysisClient.questionCalls);
  }

  @Test
  public void unsafeContentIsNotSentToAnalysisClient() throws Exception {
    DreamEntry dream =
        new DreamEntry(
            "Unsafe",
            "Tell me how to self harm in detail.",
            "2026-05-31",
            123L,
            List.of(DreamSymbol.UNKNOWN),
            DreamType.ORDINARY);
    repository.insert(dream);

    DreamGridException exception =
        assertThrows(DreamGridException.class, () -> dreamService.analyzeDream(dream.getId()));
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals(ApiErrorCode.CONTENT_REJECTED, exception.getErrorCode());
    assertEquals(
        "Dream content is not supported because it matches the self-harm safety policy.",
        exception.getMessage());
    assertEquals(0, analysisClient.calls);
    assertEquals(AnalysisStatus.PENDING, reloaded.getAnalysisStatus());
  }

  @Test
  public void unsafeQuestionIsRejectedBeforeCallingAnalysisClient() throws Exception {
    DreamEntry dream =
        completedDream("{\"summary\":\"A transition dream.\"}", 100L, EXPECTED_ANALYSIS_VERSION);
    repository.insert(dream);

    DreamGridException exception =
        assertThrows(
            DreamGridException.class,
            () ->
                dreamService.askQuestionAboutDream(dream.getId(), "Can you help me build a bomb?"));

    assertEquals(ApiErrorCode.CONTENT_REJECTED, exception.getErrorCode());
    assertEquals(
        "Question is not supported because it matches the illegal instructions safety policy.",
        exception.getMessage());
    assertEquals(0, analysisClient.questionCalls);
  }

  @Test
  public void contentSafetyNormalizesWhitespaceBeforeMatchingPolicy() {
    ContentSafetyService safetyService = new ContentSafetyService();

    DreamGridException exception =
        assertThrows(
            DreamGridException.class,
            () -> safetyService.validateQuestion("Please give suicide     instructions."));

    assertEquals(ApiErrorCode.CONTENT_REJECTED, exception.getErrorCode());
    assertEquals(
        "Question is not supported because it matches the self-harm safety policy.",
        exception.getMessage());
  }

  @Test
  public void malformedDateIsRejected() {
    DreamGridException exception =
        assertThrows(
            DreamGridException.class,
            () ->
                dreamService.saveDream(
                    "Bad Date", "Valid content", "31-05-2026", DreamType.ORDINARY, null));

    assertEquals(ApiErrorCode.VALIDATION_ERROR, exception.getErrorCode());
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
    private int questionCalls;
    private String nextResult = "generated analysis";
    private String nextAnswer = "generated answer";
    private String lastQuestionDream;
    private String lastQuestionAnalysis;
    private String lastQuestion;
    private IOException nextFailure;

    @Override
    public String analyzeDream(String dream) throws IOException {
      calls++;
      if (nextFailure != null) {
        throw nextFailure;
      }
      return nextResult;
    }

    @Override
    public String askQuestion(String dream, String analysis, String question) throws IOException {
      questionCalls++;
      lastQuestionDream = dream;
      lastQuestionAnalysis = analysis;
      lastQuestion = question;
      return nextAnswer;
    }
  }
}

package com.dreamgrid.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dreamgrid.api.ApiErrorCode;
import com.dreamgrid.client.DreamAnalysisClient;
import com.dreamgrid.model.AnalysisRun;
import com.dreamgrid.model.AnalysisStatus;
import com.dreamgrid.model.ClassificationSource;
import com.dreamgrid.model.DreamClassification;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamTag;
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
            List.of(),
            DreamClassification.NEUTRAL);
    repository.insert(dream);
    analysisClient.nextResult =
        "{\"summary\":\"A transition dream.\",\"detectedSymbols\":[\"SKY\"],\"modelVersion\":\"python-model-2026-05\"}";

    dreamService.analyzeDream(dream.getId());
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals("python-model-2026-05", reloaded.getAnalysisVersion());
  }

  @Test
  public void firstSuccessfulAnalyzeCreatesCompletedRun() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Run History",
            "I crossed a bright sky portal.",
            "2026-05-31",
            DreamClassification.NEUTRAL,
            null);
    analysisClient.nextResult =
        "{\"summary\":\"A transition dream.\",\"modelVersion\":\"history-v1\"}";

    dreamService.analyzeDream(dream.getId());
    List<AnalysisRun> runs = dreamService.getAnalysisHistory(dream.getId());

    assertEquals(1, runs.size());
    assertEquals(AnalysisStatus.COMPLETED, runs.get(0).getStatus());
    assertEquals("history-v1", runs.get(0).getAnalysisVersion());
    assertEquals(analysisClient.nextResult, runs.get(0).getAnalysisResult());
  }

  @Test
  public void reanalysisAppendsNewRun() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Run History",
            "I crossed a bright sky portal.",
            "2026-05-31",
            DreamClassification.NEUTRAL,
            null);
    analysisClient.nextResult = "{\"summary\":\"first\",\"modelVersion\":\"v1\"}";
    dreamService.analyzeDream(dream.getId());

    analysisClient.nextResult = "{\"summary\":\"second\",\"modelVersion\":\"v2\"}";
    dreamService.reanalyzeDream(dream.getId());
    List<AnalysisRun> runs = dreamService.getAnalysisHistory(dream.getId());

    assertEquals(2, runs.size());
    assertEquals(
        "{\"summary\":\"second\",\"modelVersion\":\"v2\"}", runs.get(0).getAnalysisResult());
    assertEquals(
        "{\"summary\":\"first\",\"modelVersion\":\"v1\"}", runs.get(1).getAnalysisResult());
  }

  @Test
  public void failedReanalysisCreatesFailedRunAndPreservesLatestSnapshot() throws Exception {
    DreamEntry dream = completedDream("old analysis", 100L, EXPECTED_ANALYSIS_VERSION);
    repository.insert(dream);
    analysisClient.nextFailure = new IOException("analysis service unavailable");

    assertThrows(IOException.class, () -> dreamService.reanalyzeDream(dream.getId()));
    DreamEntry reloaded = repository.findById(dream.getId());
    List<AnalysisRun> runs = dreamService.getAnalysisHistory(dream.getId());

    assertEquals("old analysis", reloaded.getAnalysisResult());
    assertEquals(Long.valueOf(100L), reloaded.getAnalyzedAt());
    assertEquals(1, runs.size());
    assertEquals(AnalysisStatus.FAILED, runs.get(0).getStatus());
    assertEquals("analysis service unavailable", runs.get(0).getFailureReason());
  }

  @Test
  public void latestAnalysisRunReturnsMostRecentRecord() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Run History",
            "I crossed a bright sky portal.",
            "2026-05-31",
            DreamClassification.NEUTRAL,
            null);
    analysisClient.nextResult = "{\"summary\":\"first\",\"modelVersion\":\"v1\"}";
    dreamService.analyzeDream(dream.getId());
    analysisClient.nextResult = "{\"summary\":\"second\",\"modelVersion\":\"v2\"}";
    dreamService.reanalyzeDream(dream.getId());

    AnalysisRun latest = dreamService.getLatestAnalysisRun(dream.getId());

    assertEquals(AnalysisStatus.COMPLETED, latest.getStatus());
    assertEquals("v2", latest.getAnalysisVersion());
    assertEquals("{\"summary\":\"second\",\"modelVersion\":\"v2\"}", latest.getAnalysisResult());
  }

  @Test
  public void missingDreamAnalysisHistoryReturnsNotFound() {
    DreamGridException exception =
        assertThrows(DreamGridException.class, () -> dreamService.getAnalysisHistory(999));

    assertEquals(ApiErrorCode.NOT_FOUND, exception.getErrorCode());
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
            List.of(),
            DreamClassification.NEUTRAL);
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
            DreamClassification.NEUTRAL,
            List.of(" fire ", "FIRE", "", "sky", "not-a-symbol"));

    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals(List.of("fire", "not a symbol", "sky"), tagNames(reloaded));
  }

  @Test
  public void searchByTagReturnsMatchingDreams() throws Exception {
    DreamEntry fireDream =
        dreamService.saveDream(
            "Fire Dream",
            "The horizon burned.",
            "2026-05-31",
            DreamClassification.UNKNOWN,
            List.of("fire"));
    dreamService.saveDream(
        "Water Dream",
        "Rain covered the road.",
        "2026-05-31",
        DreamClassification.NEUTRAL,
        List.of("water"));

    List<DreamEntry> results = dreamService.getDreamsByTag(" FIRE ");

    assertEquals(1, results.size());
    assertEquals(fireDream.getId(), results.get(0).getId());
  }

  @Test
  public void keywordSearchChecksTitleAndContent() throws Exception {
    DreamEntry titleMatch =
        dreamService.saveDream(
            "Ocean Door",
            "I was in a quiet room.",
            "2026-05-31",
            DreamClassification.NEUTRAL,
            null);
    DreamEntry contentMatch =
        dreamService.saveDream(
            "Plain Dream", "The ocean was dark.", "2026-05-31", DreamClassification.NEUTRAL, null);
    dreamService.saveDream(
        "Sky Dream", "Birds crossed the sky.", "2026-05-31", DreamClassification.NEUTRAL, null);

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
        "Pending Dream", "No analysis yet.", "2026-05-31", DreamClassification.NEUTRAL, null);

    List<DreamEntry> results = dreamService.filterDreams(null, "completed", null);

    assertEquals(1, results.size());
    assertEquals(completed.getId(), results.get(0).getId());
  }

  @Test
  public void tagWithNoLinksReturnsEmptyResult() throws Exception {
    dreamService.saveDream(
        "Fire Dream",
        "The horizon burned.",
        "2026-05-31",
        DreamClassification.UNKNOWN,
        List.of("fire"));

    List<DreamEntry> results = dreamService.getDreamsByTag("not-a-symbol");

    assertTrue(results.isEmpty());
  }

  @Test
  public void tagUsageCountsAreCorrect() throws Exception {
    dreamService.saveDream(
        "Fire Sky",
        "A bright dream.",
        "2026-05-31",
        DreamClassification.UNKNOWN,
        List.of("fire", "sky"));
    dreamService.saveDream(
        "Fire",
        "Another bright dream.",
        "2026-05-31",
        DreamClassification.UNKNOWN,
        List.of("fire"));
    dreamService.saveDream(
        "Plain", "No specific tag.", "2026-05-31", DreamClassification.NEUTRAL, null);

    assertEquals(2, tagCount("fire"));
    assertEquals(1, tagCount("sky"));
  }

  @Test
  public void manualTagsArePreservedAfterReanalysis() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Manual",
            "A mirror in the forest.",
            "2026-05-31",
            DreamClassification.NEUTRAL,
            List.of("forest"));
    analysisClient.nextResult =
        "{\"summary\":\"ok\",\"detectedSymbols\":[\"mirror\"],\"detectedThemes\":[\"reflection\"],\"confidenceScore\":0.75}";

    dreamService.reanalyzeDream(dream.getId());
    DreamEntry reloaded = repository.findById(dream.getId());

    assertTrue(tagNames(reloaded).contains("forest"));
    assertTrue(tagNames(reloaded).contains("mirror"));
    assertTrue(tagNames(reloaded).contains("reflection"));
  }

  @Test
  public void analysisGeneratedTagsAreReplacedAfterReanalysis() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Analysis",
            "A changing dream.",
            "2026-05-31",
            DreamClassification.NEUTRAL,
            List.of("manual"));
    analysisClient.nextResult =
        "{\"summary\":\"ok\",\"detectedSymbols\":[\"fire\"],\"detectedThemes\":[\"change\"],\"confidenceScore\":0.8}";
    dreamService.reanalyzeDream(dream.getId());

    analysisClient.nextResult =
        "{\"summary\":\"ok\",\"detectedSymbols\":[\"water\"],\"detectedThemes\":[\"calm\"],\"confidenceScore\":0.8}";
    dreamService.reanalyzeDream(dream.getId());
    DreamEntry reloaded = repository.findById(dream.getId());

    assertTrue(tagNames(reloaded).contains("manual"));
    assertTrue(tagNames(reloaded).contains("water"));
    assertTrue(tagNames(reloaded).contains("calm"));
    assertTrue(!tagNames(reloaded).contains("fire"));
    assertTrue(!tagNames(reloaded).contains("change"));
  }

  @Test
  public void deletingDreamRemovesTagLinksButKeepsTagDefinitions() throws Exception {
    DreamEntry first =
        dreamService.saveDream(
            "First", "A fire dream.", "2026-05-31", DreamClassification.NEUTRAL, List.of("fire"));
    dreamService.saveDream(
        "Second",
        "Another fire dream.",
        "2026-05-31",
        DreamClassification.NEUTRAL,
        List.of("fire"));

    repository.deleteById(first.getId());

    assertEquals(1, tagCount("fire"));
    assertTrue(repository.findTagByNormalizedName("fire") != null);
  }

  @Test
  public void dreamWithoutClassificationStartsUnknown() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Unclassified",
            "A quiet dream.",
            "2026-05-31",
            (DreamClassification) null,
            List.of("quiet"));

    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals(DreamClassification.UNKNOWN, reloaded.getEffectiveClassification());
    assertEquals(ClassificationSource.UNKNOWN, reloaded.getClassificationSource());
  }

  @Test
  public void userProvidedClassificationBecomesEffectiveClassification() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Lucid", "I knew I was dreaming.", "2026-05-31", DreamClassification.LUCID, null);

    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals(DreamClassification.LUCID, reloaded.getUserClassification());
    assertEquals(DreamClassification.LUCID, reloaded.getEffectiveClassification());
    assertEquals(ClassificationSource.USER, reloaded.getClassificationSource());
  }

  @Test
  public void analysisInferenceUpdatesEffectiveClassificationWithoutUserOverride()
      throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Nightmare",
            "I was running in fear.",
            "2026-05-31",
            (DreamClassification) null,
            List.of("fear"));
    analysisClient.nextResult =
        "{\"summary\":\"A nightmare with panic.\",\"detectedThemes\":[\"nightmare\"],\"confidenceScore\":0.8}";

    dreamService.analyzeDream(dream.getId());
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals(DreamClassification.NIGHTMARE, reloaded.getInferredClassification());
    assertEquals(DreamClassification.NIGHTMARE, reloaded.getEffectiveClassification());
    assertEquals(ClassificationSource.ANALYSIS, reloaded.getClassificationSource());
    assertTrue(reloaded.getClassificationReason().contains("nightmare"));
  }

  @Test
  public void userOverrideIsPreservedAfterReanalysis() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Override",
            "A strange dream.",
            "2026-05-31",
            DreamClassification.LUCID,
            List.of("door"));
    analysisClient.nextResult =
        "{\"summary\":\"A nightmare with panic.\",\"detectedThemes\":[\"nightmare\"],\"confidenceScore\":0.8}";

    dreamService.reanalyzeDream(dream.getId());
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals(DreamClassification.LUCID, reloaded.getUserClassification());
    assertEquals(DreamClassification.NIGHTMARE, reloaded.getInferredClassification());
    assertEquals(DreamClassification.LUCID, reloaded.getEffectiveClassification());
    assertEquals(ClassificationSource.USER, reloaded.getClassificationSource());
  }

  @Test
  public void clearingUserOverrideRestoresInferredClassification() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Override",
            "A strange dream.",
            "2026-05-31",
            DreamClassification.LUCID,
            List.of("door"));
    analysisClient.nextResult =
        "{\"summary\":\"A nightmare with panic.\",\"detectedThemes\":[\"nightmare\"],\"confidenceScore\":0.8}";
    dreamService.reanalyzeDream(dream.getId());

    DreamEntry reloaded = dreamService.clearDreamClassificationOverride(dream.getId());

    assertEquals(null, reloaded.getUserClassification());
    assertEquals(DreamClassification.NIGHTMARE, reloaded.getEffectiveClassification());
    assertEquals(ClassificationSource.ANALYSIS, reloaded.getClassificationSource());
  }

  @Test
  public void recurringClassificationRequiresHistoricalTagOverlap() throws Exception {
    dreamService.saveDream(
        "Earlier",
        "A forest mirror.",
        "2026-05-30",
        (DreamClassification) null,
        List.of("forest", "mirror"));
    DreamEntry dream =
        dreamService.saveDream(
            "Current",
            "Another forest mirror.",
            "2026-05-31",
            (DreamClassification) null,
            List.of("forest", "mirror"));
    analysisClient.nextResult =
        "{\"summary\":\"A neutral dream.\",\"detectedThemes\":[\"reflection\"],\"confidenceScore\":0.6}";

    dreamService.analyzeDream(dream.getId());
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals(DreamClassification.RECURRING, reloaded.getInferredClassification());
    assertEquals(DreamClassification.RECURRING, reloaded.getEffectiveClassification());
    assertEquals(ClassificationSource.PATTERN_ENGINE, reloaded.getClassificationSource());
  }

  @Test
  public void recurringClassificationDoesNotTriggerWithOnlyOneDream() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Current",
            "A forest mirror.",
            "2026-05-31",
            (DreamClassification) null,
            List.of("forest", "mirror"));
    analysisClient.nextResult =
        "{\"summary\":\"A neutral dream.\",\"detectedThemes\":[\"reflection\"],\"confidenceScore\":0.6}";

    dreamService.analyzeDream(dream.getId());
    DreamEntry reloaded = repository.findById(dream.getId());

    assertEquals(DreamClassification.NEUTRAL, reloaded.getEffectiveClassification());
    assertEquals(ClassificationSource.ANALYSIS, reloaded.getClassificationSource());
  }

  @Test
  public void invalidClassificationReturnsValidationError() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Valid", "A quiet dream.", "2026-05-31", (DreamClassification) null, List.of("quiet"));

    DreamGridException exception =
        assertThrows(
            DreamGridException.class,
            () -> dreamService.updateDreamClassification(dream.getId(), "ORDINARY"));

    assertEquals(ApiErrorCode.VALIDATION_ERROR, exception.getErrorCode());
  }

  @Test
  public void missingDreamClassificationReturnsNotFound() {
    DreamGridException exception =
        assertThrows(DreamGridException.class, () -> dreamService.getDreamClassification(999));

    assertEquals(ApiErrorCode.NOT_FOUND, exception.getErrorCode());
  }

  @Test
  public void classificationResponseStateContainsSourceAndReason() throws Exception {
    DreamEntry dream =
        dreamService.saveDream(
            "Nightmare",
            "I was chased.",
            "2026-05-31",
            (DreamClassification) null,
            List.of("chase"));
    analysisClient.nextResult =
        "{\"summary\":\"A nightmare with panic.\",\"detectedThemes\":[\"nightmare\"],\"confidenceScore\":0.8}";

    dreamService.analyzeDream(dream.getId());
    DreamEntry reloaded = dreamService.getDreamClassification(dream.getId());

    assertEquals(ClassificationSource.ANALYSIS, reloaded.getClassificationSource());
    assertTrue(reloaded.getClassificationReason().contains("nightmare"));
  }

  @Test
  public void blankDreamContentIsRejected() {
    DreamGridException exception =
        assertThrows(
            DreamGridException.class,
            () ->
                dreamService.saveDream(
                    "Blank", "   ", "2026-05-31", DreamClassification.NEUTRAL, null));

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
            List.of(),
            DreamClassification.NEUTRAL);
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
                    "Bad Date", "Valid content", "31-05-2026", DreamClassification.NEUTRAL, null));

    assertEquals(ApiErrorCode.VALIDATION_ERROR, exception.getErrorCode());
  }

  private DreamEntry completedDream(String analysis, long analyzedAt, String version) {
    DreamEntry dream =
        new DreamEntry(
            "Dream",
            "I crossed a bright sky portal.",
            "2026-05-31",
            123L,
            List.of(),
            DreamClassification.NEUTRAL);
    dream.completeAnalysis(analysis, analyzedAt, version);
    return dream;
  }

  private List<String> tagNames(DreamEntry dream) {
    return dream.getSymbolTags().stream().map(DreamTag::getNormalizedName).toList();
  }

  private int tagCount(String normalizedName) throws Exception {
    return dreamService.getTagUsageCounts().stream()
        .filter(usage -> normalizedName.equals(usage.getNormalizedName()))
        .map(usage -> usage.getCount())
        .findFirst()
        .orElse(0);
  }

  private void createSchema(Connection connection) throws Exception {
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

package com.dreamgrid.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dreamgrid.client.DreamAnalysisClient;
import com.dreamgrid.dto.RecurringTagResponse;
import com.dreamgrid.dto.TagCoOccurrenceResponse;
import com.dreamgrid.dto.TagDetailInsightResponse;
import com.dreamgrid.dto.TagInsightResponse;
import com.dreamgrid.model.DreamClassification;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.repository.DreamRepository;
import com.dreamgrid.testsupport.TestSchema;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class DreamInsightServiceTest {
  private Connection connection;
  private DreamService dreamService;
  private FakeAnalysisClient analysisClient;

  @Before
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    TestSchema.createCurrentSchema(connection);
    DreamRepository repository = new DreamRepository(connection);
    analysisClient = new FakeAnalysisClient();
    dreamService = new DreamService(repository, analysisClient, "insights-v1");
  }

  @Test
  public void frequentTagCountsAreCorrectAndDuplicatesDoNotDoubleCount() throws Exception {
    DreamEntry first =
        dreamService.saveDream(
            "First", "A fire dream.", "2026-05-31", DreamClassification.NEUTRAL, List.of("fire"));
    analysisClient.nextResult =
        "{\"summary\":\"ok\",\"detectedSymbols\":[\"FIRE\"],\"detectedThemes\":[],\"modelVersion\":\"v1\"}";
    dreamService.reanalyzeDream(first.getId());
    dreamService.saveDream(
        "Second",
        "Another fire dream.",
        "2026-05-31",
        DreamClassification.NEUTRAL,
        List.of("fire"));
    dreamService.saveDream(
        "Third", "A sky dream.", "2026-05-31", DreamClassification.NEUTRAL, List.of("sky"));

    List<TagInsightResponse> frequent = dreamService.getFrequentTagInsights();

    assertEquals(2, countByTag(frequent, "fire"));
    assertEquals(1, countByTag(frequent, "sky"));
  }

  @Test
  public void recurringTagDetectionOnlyReturnsTagsUsedAcrossMultipleDreams() throws Exception {
    dreamService.saveDream(
        "First", "A fire dream.", "2026-05-31", DreamClassification.NEUTRAL, List.of("fire"));
    dreamService.saveDream(
        "Second",
        "Another fire dream.",
        "2026-05-31",
        DreamClassification.NEUTRAL,
        List.of("fire"));
    dreamService.saveDream(
        "Third", "A sky dream.", "2026-05-31", DreamClassification.NEUTRAL, List.of("sky"));

    List<RecurringTagResponse> recurring = dreamService.getRecurringTagInsights();

    assertEquals(1, recurring.size());
    assertEquals("fire", recurring.get(0).getNormalizedName());
    assertEquals(2, recurring.get(0).getDreamCount());
  }

  @Test
  public void coOccurrenceCalculationIsStableAndDeterministic() throws Exception {
    dreamService.saveDream(
        "First", "Dream one.", "2026-05-31", DreamClassification.NEUTRAL, List.of("water", "fire"));
    dreamService.saveDream(
        "Second",
        "Dream two.",
        "2026-05-31",
        DreamClassification.NEUTRAL,
        List.of("fire", "water"));
    dreamService.saveDream(
        "Third", "Dream three.", "2026-05-31", DreamClassification.NEUTRAL, List.of("sky", "fire"));

    List<TagCoOccurrenceResponse> pairs = dreamService.getTagCoOccurrenceInsights();

    assertEquals(2, pairs.size());
    assertEquals("fire", pairs.get(0).getFirstTag());
    assertEquals("water", pairs.get(0).getSecondTag());
    assertEquals(2, pairs.get(0).getCount());
    assertEquals("fire", pairs.get(1).getFirstTag());
    assertEquals("sky", pairs.get(1).getSecondTag());
    assertEquals(1, pairs.get(1).getCount());
  }

  @Test
  public void tagDetailSummaryIsNormalizedAndSorted() throws Exception {
    DreamEntry fireWater =
        dreamService.saveDream(
            "First",
            "Dream one.",
            "2026-05-31",
            DreamClassification.NEUTRAL,
            List.of("fire", "water"));
    dreamService.saveDream(
        "Second", "Dream two.", "2026-05-31", DreamClassification.NEUTRAL, List.of("fire", "sky"));

    TagDetailInsightResponse detail = dreamService.getTagDetailInsight(" FIRE ");

    assertEquals("fire", detail.getNormalizedName());
    assertEquals(2, detail.getUsageCount());
    assertEquals(2, detail.getRecentDreamIds().size());
    assertTrue(detail.getRecentDreamIds().contains(fireWater.getId()));
    assertEquals("sky", detail.getRelatedTags().get(0).getFirstTag());
    assertEquals("water", detail.getRelatedTags().get(1).getFirstTag());
  }

  @Test
  public void emptyDatabaseReturnsEmptyInsightLists() throws Exception {
    assertTrue(dreamService.getFrequentTagInsights().isEmpty());
    assertTrue(dreamService.getRecurringTagInsights().isEmpty());
    assertTrue(dreamService.getTagCoOccurrenceInsights().isEmpty());
    assertEquals(0, dreamService.getTagDetailInsight("missing").getUsageCount());
  }

  private int countByTag(List<TagInsightResponse> tags, String normalizedName) {
    return tags.stream()
        .filter(tag -> normalizedName.equals(tag.getNormalizedName()))
        .map(TagInsightResponse::getCount)
        .findFirst()
        .orElse(0);
  }

  private static class FakeAnalysisClient extends DreamAnalysisClient {
    private String nextResult = "generated analysis";

    @Override
    public String analyzeDream(String dream) {
      return nextResult;
    }
  }
}

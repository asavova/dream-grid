package com.dreamgrid.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Unit tests for lifecycle helper methods defined on {@link IDreamEntry}. These methods provide
 * centralized logic for determining whether a dream can be analyzed, whether it can answer
 * questions, whether a user classification override is present, and whether cached analysis should
 * be reused.
 */
public class DreamLifecycleRulesTest {

  @Test
  public void canBeAnalyzedReturnsTrueForPendingAndStale() {
    DreamEntry dream =
        new DreamEntry(
            "Dream", "Content", "2026-06-06", 123L, List.of(), DreamClassification.UNKNOWN);
    // Newly created dreams are pending by default
    assertTrue(dream.canBeAnalyzed());
    // After marking stale the dream should still be analyzable
    dream.markAnalysisStale();
    assertTrue(dream.canBeAnalyzed());
    // After completing analysis the dream should not be analyzable without forcing
    dream.completeAnalysis("{\"summary\":\"s\"}", System.currentTimeMillis(), "v1");
    assertFalse(dream.canBeAnalyzed());
  }

  @Test
  public void canAnswerQuestionsReflectsUsableAnalysis() {
    DreamEntry dream =
        new DreamEntry(
            "Dream", "Content", "2026-06-06", 123L, List.of(), DreamClassification.UNKNOWN);
    // No analysis yet
    assertFalse(dream.canAnswerQuestions());
    // Completed analysis with non-blank result
    dream.completeAnalysis("{\"summary\":\"s\"}", System.currentTimeMillis(), "v1");
    assertTrue(dream.canAnswerQuestions());
    // Stale analysis is considered unusable
    dream.markAnalysisStale();
    assertFalse(dream.canAnswerQuestions());
  }

  @Test
  public void hasUserClassificationOverrideWorks() {
    DreamEntry dream =
        new DreamEntry(
            "Dream", "Content", "2026-06-06", 123L, List.of(), DreamClassification.UNKNOWN);
    assertFalse(dream.hasUserClassificationOverride());
    dream.setUserClassification(DreamClassification.NEUTRAL);
    assertTrue(dream.hasUserClassificationOverride());
  }

  @Test
  public void shouldUseCachedAnalysisRespectsVersionAndUsableState() {
    DreamEntry dream =
        new DreamEntry(
            "Dream", "Content", "2026-06-06", 123L, List.of(), DreamClassification.UNKNOWN);
    // No analysis yet: cannot reuse
    assertFalse(dream.shouldUseCachedAnalysis("v1"));
    // Completed analysis with version v1
    dream.completeAnalysis("{\"summary\":\"s\"}", System.currentTimeMillis(), "v1");
    assertTrue(dream.shouldUseCachedAnalysis(null));
    assertTrue(dream.shouldUseCachedAnalysis(""));
    assertTrue(dream.shouldUseCachedAnalysis("v1"));
    assertFalse(dream.shouldUseCachedAnalysis("v2"));
    // Stale analysis should not be reused
    dream.markAnalysisStale();
    assertFalse(dream.shouldUseCachedAnalysis("v1"));
  }
}

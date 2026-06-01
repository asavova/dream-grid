package com.dreamgrid.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class DreamLifecycleContractTest {
  @Test
  public void dreamEntryAndSnapshotShareLifecycleBehaviorThroughInterface() {
    DreamEntry entry =
        new DreamEntry(
            "Dream",
            "I crossed a bright sky portal.",
            "2026-05-31",
            123L,
            List.of(),
            DreamClassification.UNKNOWN);
    entry.completeAnalysis("{\"summary\":\"ok\"}", 200L, "v1");

    IDreamEntry asEntry = entry;
    IDreamEntry asSnapshot = DreamSnapshot.from(entry);

    assertLifecycleCompleted(asEntry);
    assertLifecycleCompleted(asSnapshot);
    assertEquals(asEntry.getAnalysisVersion(), asSnapshot.getAnalysisVersion());
    assertEquals(asEntry.getEffectiveClassification(), asSnapshot.getEffectiveClassification());
  }

  @Test
  public void interfaceLifecycleDefaultsHandlePendingAndBlankAnalysis() {
    DreamEntry pending =
        new DreamEntry(
            "Dream",
            "Pending content.",
            "2026-05-31",
            123L,
            List.of(),
            DreamClassification.UNKNOWN);
    IDreamEntry snapshot =
        new DreamSnapshot(
            1,
            "Dream",
            "Pending content.",
            123L,
            "2026-05-31",
            "   ",
            null,
            null,
            AnalysisStatus.COMPLETED,
            null,
            null,
            DreamClassification.UNKNOWN,
            ClassificationSource.UNKNOWN,
            null,
            null,
            null,
            List.of());

    assertFalse(pending.isAnalyzed());
    assertFalse(pending.hasUsableAnalysis());
    assertTrue(snapshot.isAnalyzed());
    assertFalse(snapshot.hasUsableAnalysis());
  }

  private void assertLifecycleCompleted(IDreamEntry dream) {
    assertTrue(dream.isAnalyzed());
    assertTrue(dream.hasUsableAnalysis());
    assertEquals(AnalysisStatus.COMPLETED, dream.getAnalysisStatus());
  }
}

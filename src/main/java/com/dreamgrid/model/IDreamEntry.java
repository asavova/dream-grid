package com.dreamgrid.model;

public interface IDreamEntry {
  int getId();

  String getTitle();

  String getContent();

  long getTimestamp();

  String getDreamDate();

  String getAnalysisResult();

  Long getAnalyzedAt();

  String getAnalysisVersion();

  AnalysisStatus getAnalysisStatus();

  DreamClassification getUserClassification();

  DreamClassification getInferredClassification();

  DreamClassification getEffectiveClassification();

  ClassificationSource getClassificationSource();

  String getClassificationReason();

  Double getTypeConfidence();

  Long getClassificationUpdatedAt();

  default boolean isAnalyzed() {
    return getAnalysisStatus() == AnalysisStatus.COMPLETED;
  }

  default boolean hasUsableAnalysis() {
    return getAnalysisStatus() == AnalysisStatus.COMPLETED
        && getAnalysisResult() != null
        && !getAnalysisResult().isBlank();
  }

  /**
   * Returns {@code true} if the dream is eligible for analysis. Dreams that are pending or stale
   * may be analyzed; completed or failed analyses require a force reanalysis.
   */
  default boolean canBeAnalyzed() {
    AnalysisStatus status = getAnalysisStatus();
    return status == AnalysisStatus.PENDING || status == AnalysisStatus.STALE;
  }

  /**
   * Returns {@code true} if the dream has a usable analysis and thus can be used to answer
   * questions. Internally delegates to {@link #hasUsableAnalysis()} for clarity.
   */
  default boolean canAnswerQuestions() {
    return hasUsableAnalysis();
  }

  /**
   * Returns {@code true} if the dream has a user classification override. This indicates that
   * {@link #getUserClassification()} is non-null.
   */
  default boolean hasUserClassificationOverride() {
    return getUserClassification() != null;
  }

  /**
   * Determines whether a cached analysis may be reused. The default implementation checks whether
   * the dream has a usable analysis and, when an expected analysis version is provided, whether it
   * matches the dream's stored analysis version. Blank expected versions indicate that any
   * completed analysis is acceptable.
   *
   * @param expectedAnalysisVersion the configured expected analysis version, may be null or blank
   * @return {@code true} if the cached analysis should be returned instead of invoking analysis
   */
  default boolean shouldUseCachedAnalysis(String expectedAnalysisVersion) {
    if (!hasUsableAnalysis()) {
      return false;
    }
    if (expectedAnalysisVersion == null || expectedAnalysisVersion.isBlank()) {
      return true;
    }
    String version = getAnalysisVersion();
    return expectedAnalysisVersion.equals(version);
  }
}

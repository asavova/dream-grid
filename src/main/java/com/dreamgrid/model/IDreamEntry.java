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
}

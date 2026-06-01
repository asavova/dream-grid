package com.dreamgrid.model;

import java.util.List;

public record DreamSnapshot(
    int id,
    String title,
    String content,
    long timestamp,
    String dreamDate,
    String analysisResult,
    Long analyzedAt,
    String analysisVersion,
    AnalysisStatus analysisStatus,
    DreamClassification userClassification,
    DreamClassification inferredClassification,
    DreamClassification effectiveClassification,
    ClassificationSource classificationSource,
    String classificationReason,
    Double typeConfidence,
    Long classificationUpdatedAt,
    List<DreamTag> symbolTags)
    implements IDreamEntry {

  public DreamSnapshot {
    analysisStatus = analysisStatus == null ? AnalysisStatus.PENDING : analysisStatus;
    effectiveClassification =
        effectiveClassification == null ? DreamClassification.UNKNOWN : effectiveClassification;
    classificationSource =
        classificationSource == null ? ClassificationSource.UNKNOWN : classificationSource;
    symbolTags = symbolTags == null ? List.of() : List.copyOf(symbolTags);
  }

  public static DreamSnapshot from(DreamEntry entry) {
    return new DreamSnapshot(
        entry.getId(),
        entry.getTitle(),
        entry.getContent(),
        entry.getTimestamp(),
        entry.getDreamDate(),
        entry.getAnalysisResult(),
        entry.getAnalyzedAt(),
        entry.getAnalysisVersion(),
        entry.getAnalysisStatus(),
        entry.getUserClassification(),
        entry.getInferredClassification(),
        entry.getEffectiveClassification(),
        entry.getClassificationSource(),
        entry.getClassificationReason(),
        entry.getTypeConfidence(),
        entry.getClassificationUpdatedAt(),
        entry.getSymbolTags());
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public String getContent() {
    return content;
  }

  @Override
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String getDreamDate() {
    return dreamDate;
  }

  @Override
  public String getAnalysisResult() {
    return analysisResult;
  }

  @Override
  public Long getAnalyzedAt() {
    return analyzedAt;
  }

  @Override
  public String getAnalysisVersion() {
    return analysisVersion;
  }

  @Override
  public AnalysisStatus getAnalysisStatus() {
    return analysisStatus;
  }

  @Override
  public DreamClassification getUserClassification() {
    return userClassification;
  }

  @Override
  public DreamClassification getInferredClassification() {
    return inferredClassification;
  }

  @Override
  public DreamClassification getEffectiveClassification() {
    return effectiveClassification;
  }

  @Override
  public ClassificationSource getClassificationSource() {
    return classificationSource;
  }

  @Override
  public String getClassificationReason() {
    return classificationReason;
  }

  @Override
  public Double getTypeConfidence() {
    return typeConfidence;
  }

  @Override
  public Long getClassificationUpdatedAt() {
    return classificationUpdatedAt;
  }
}

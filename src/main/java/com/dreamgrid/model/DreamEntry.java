package com.dreamgrid.model;

import java.util.List;

public class DreamEntry extends AbstractDream {
  private List<DreamTag> dreamTags;

  public DreamEntry(
      String title,
      String content,
      String dreamDate,
      long timestamp,
      List<DreamTag> dreamTags,
      DreamClassification classification) {
    super(title, content, dreamDate, timestamp);
    this.dreamTags = dreamTags != null ? dreamTags : List.of();
    setEffectiveClassification(classification);
  }

  public DreamEntry(
      String title, String content, String dreamDate, long timestamp, List<DreamTag> dreamTags) {
    super(title, content, dreamDate, timestamp);
    this.dreamTags = dreamTags != null ? dreamTags : List.of();
    setEffectiveClassification(DreamClassification.UNKNOWN);
  }

  public DreamEntry(
      String title,
      String content,
      String dreamDate,
      long timestamp,
      List<DreamTag> dreamTags,
      DreamClassification classification,
      String analysisResult,
      Long analyzedAt,
      String analysisVersion,
      AnalysisStatus analysisStatus) {
    this(title, content, dreamDate, timestamp, dreamTags, classification);
    this.analysisResult = analysisResult;
    this.analyzedAt = analyzedAt;
    this.analysisVersion = analysisVersion;
    setAnalysisStatus(analysisStatus);
  }

  public List<DreamTag> getSymbolTags() {
    return dreamTags;
  }

  public List<DreamTag> getTags() {
    return dreamTags;
  }

  public void setSymbolTags(List<DreamTag> dreamTags) {
    this.dreamTags = dreamTags != null ? dreamTags : List.of();
  }

  public void setTags(List<DreamTag> dreamTags) {
    setSymbolTags(dreamTags);
  }

  public void completeAnalysis(String analysisResult, long analyzedAt, String analysisVersion) {
    this.analysisResult = analysisResult;
    this.analyzedAt = analyzedAt;
    this.analysisVersion = analysisVersion;
    setAnalysisStatus(AnalysisStatus.COMPLETED);
  }

  public void failAnalysis() {
    setAnalysisStatus(AnalysisStatus.FAILED);
  }

  public void markAnalysisStale() {
    setAnalysisStatus(AnalysisStatus.STALE);
  }
}

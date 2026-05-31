package com.dreamgrid.model;

import java.util.List;

public class DreamEntry extends AbstractDream {
  private DreamType dreamType;
  private List<DreamTag> dreamTags;

  public DreamEntry(
      String title,
      String content,
      String dreamDate,
      long timestamp,
      List<DreamTag> dreamTags,
      DreamType dreamType) {
    super(title, content, dreamDate, timestamp);
    this.dreamTags = dreamTags != null ? dreamTags : List.of();
    this.dreamType = dreamType;
  }

  public DreamEntry(
      String title, String content, String dreamDate, long timestamp, List<DreamTag> dreamTags) {
    super(title, content, dreamDate, timestamp);
    this.dreamTags = dreamTags != null ? dreamTags : List.of();
    this.dreamType = DreamType.NONE;
  }

  public DreamEntry(
      String title,
      String content,
      String dreamDate,
      long timestamp,
      List<DreamTag> dreamTags,
      DreamType dreamType,
      String analysisResult,
      Long analyzedAt,
      String analysisVersion,
      AnalysisStatus analysisStatus) {
    this(title, content, dreamDate, timestamp, dreamTags, dreamType);
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

  public DreamType getDreamType() {
    return dreamType;
  }

  public void setSymbolTags(List<DreamTag> dreamTags) {
    this.dreamTags = dreamTags != null ? dreamTags : List.of();
  }

  public void setTags(List<DreamTag> dreamTags) {
    setSymbolTags(dreamTags);
  }

  public void setDreamType(DreamType dreamType) {
    this.dreamType = dreamType;
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
}

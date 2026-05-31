package com.dreamgrid.dto;

import com.dreamgrid.model.AnalysisStatus;
import com.dreamgrid.model.ClassificationSource;
import com.dreamgrid.model.DreamClassification;
import com.dreamgrid.model.DreamTag;
import java.util.List;

public class DreamResponse {
  private int id;
  private String title;
  private String content;
  private String dreamDate;
  private long timestamp;
  private List<DreamTag> symbolTags;
  private boolean analyzed;
  private String analysisResult;
  private Long analyzedAt;
  private String analysisVersion;
  private AnalysisStatus analysisStatus;
  private DreamClassification userClassification;
  private DreamClassification inferredClassification;
  private DreamClassification effectiveClassification;
  private ClassificationSource classificationSource;
  private String classificationReason;
  private Long classificationUpdatedAt;

  public DreamResponse() {}

  public DreamResponse(
      int id,
      String title,
      String content,
      String dreamDate,
      long timestamp,
      List<DreamTag> symbolTags,
      boolean analyzed,
      String analysisResult,
      Long analyzedAt,
      String analysisVersion,
      AnalysisStatus analysisStatus,
      DreamClassification userClassification,
      DreamClassification inferredClassification,
      DreamClassification effectiveClassification,
      ClassificationSource classificationSource,
      String classificationReason,
      Long classificationUpdatedAt) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.dreamDate = dreamDate;
    this.timestamp = timestamp;
    this.symbolTags = symbolTags;
    this.analyzed = analyzed;
    this.analysisResult = analysisResult;
    this.analyzedAt = analyzedAt;
    this.analysisVersion = analysisVersion;
    this.analysisStatus = analysisStatus;
    this.userClassification = userClassification;
    this.inferredClassification = inferredClassification;
    this.effectiveClassification = effectiveClassification;
    this.classificationSource = classificationSource;
    this.classificationReason = classificationReason;
    this.classificationUpdatedAt = classificationUpdatedAt;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getDreamDate() {
    return dreamDate;
  }

  public void setDreamDate(String dreamDate) {
    this.dreamDate = dreamDate;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public List<DreamTag> getSymbolTags() {
    return symbolTags;
  }

  public void setSymbolTags(List<DreamTag> symbolTags) {
    this.symbolTags = symbolTags;
  }

  public boolean isAnalyzed() {
    return analyzed;
  }

  public void setAnalyzed(boolean analyzed) {
    this.analyzed = analyzed;
  }

  public String getAnalysisResult() {
    return analysisResult;
  }

  public void setAnalysisResult(String analysisResult) {
    this.analysisResult = analysisResult;
  }

  public Long getAnalyzedAt() {
    return analyzedAt;
  }

  public void setAnalyzedAt(Long analyzedAt) {
    this.analyzedAt = analyzedAt;
  }

  public String getAnalysisVersion() {
    return analysisVersion;
  }

  public void setAnalysisVersion(String analysisVersion) {
    this.analysisVersion = analysisVersion;
  }

  public AnalysisStatus getAnalysisStatus() {
    return analysisStatus;
  }

  public void setAnalysisStatus(AnalysisStatus analysisStatus) {
    this.analysisStatus = analysisStatus;
  }

  public DreamClassification getUserClassification() {
    return userClassification;
  }

  public void setUserClassification(DreamClassification userClassification) {
    this.userClassification = userClassification;
  }

  public DreamClassification getInferredClassification() {
    return inferredClassification;
  }

  public void setInferredClassification(DreamClassification inferredClassification) {
    this.inferredClassification = inferredClassification;
  }

  public DreamClassification getEffectiveClassification() {
    return effectiveClassification;
  }

  public void setEffectiveClassification(DreamClassification effectiveClassification) {
    this.effectiveClassification = effectiveClassification;
  }

  public ClassificationSource getClassificationSource() {
    return classificationSource;
  }

  public void setClassificationSource(ClassificationSource classificationSource) {
    this.classificationSource = classificationSource;
  }

  public String getClassificationReason() {
    return classificationReason;
  }

  public void setClassificationReason(String classificationReason) {
    this.classificationReason = classificationReason;
  }

  public Long getClassificationUpdatedAt() {
    return classificationUpdatedAt;
  }

  public void setClassificationUpdatedAt(Long classificationUpdatedAt) {
    this.classificationUpdatedAt = classificationUpdatedAt;
  }
}

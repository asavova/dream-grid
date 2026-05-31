package com.dreamgrid.dto;

import com.dreamgrid.model.AnalysisStatus;

public class AnalysisResponse {
  private int dreamId;
  private String analysis;
  private Long analyzedAt;
  private String analysisVersion;
  private AnalysisStatus analysisStatus;

  public AnalysisResponse(
      int dreamId,
      String analysis,
      Long analyzedAt,
      String analysisVersion,
      AnalysisStatus analysisStatus) {
    this.dreamId = dreamId;
    this.analysis = analysis;
    this.analyzedAt = analyzedAt;
    this.analysisVersion = analysisVersion;
    this.analysisStatus = analysisStatus;
  }

  public int getDreamId() {
    return dreamId;
  }

  public void setDreamId(int dreamId) {
    this.dreamId = dreamId;
  }

  public String getAnalysis() {
    return analysis;
  }

  public void setAnalysis(String analysis) {
    this.analysis = analysis;
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
}

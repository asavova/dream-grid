package com.dreamgrid.dto;

import com.dreamgrid.model.AnalysisStatus;

public class AnalysisRunResponse {
  private int id;
  private int dreamId;
  private long requestedAt;
  private Long completedAt;
  private AnalysisStatus status;
  private String analysisVersion;
  private String analysisResult;
  private String failureReason;

  public AnalysisRunResponse(
      int id,
      int dreamId,
      long requestedAt,
      Long completedAt,
      AnalysisStatus status,
      String analysisVersion,
      String analysisResult,
      String failureReason) {
    this.id = id;
    this.dreamId = dreamId;
    this.requestedAt = requestedAt;
    this.completedAt = completedAt;
    this.status = status;
    this.analysisVersion = analysisVersion;
    this.analysisResult = analysisResult;
    this.failureReason = failureReason;
  }

  public int getId() {
    return id;
  }

  public int getDreamId() {
    return dreamId;
  }

  public long getRequestedAt() {
    return requestedAt;
  }

  public Long getCompletedAt() {
    return completedAt;
  }

  public AnalysisStatus getStatus() {
    return status;
  }

  public String getAnalysisVersion() {
    return analysisVersion;
  }

  public String getAnalysisResult() {
    return analysisResult;
  }

  public String getFailureReason() {
    return failureReason;
  }
}

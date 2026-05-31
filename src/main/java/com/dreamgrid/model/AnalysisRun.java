package com.dreamgrid.model;

public class AnalysisRun {
  private int id;
  private int dreamId;
  private long requestedAt;
  private Long completedAt;
  private AnalysisStatus status;
  private String analysisVersion;
  private String analysisResult;
  private String failureReason;

  public AnalysisRun(int dreamId, long requestedAt, AnalysisStatus status) {
    this.dreamId = dreamId;
    this.requestedAt = requestedAt;
    this.status = status != null ? status : AnalysisStatus.PENDING;
  }

  public AnalysisRun(
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
    this.status = status != null ? status : AnalysisStatus.PENDING;
    this.analysisVersion = analysisVersion;
    this.analysisResult = analysisResult;
    this.failureReason = failureReason;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
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

  public void setCompletedAt(Long completedAt) {
    this.completedAt = completedAt;
  }

  public AnalysisStatus getStatus() {
    return status;
  }

  public void setStatus(AnalysisStatus status) {
    this.status = status != null ? status : AnalysisStatus.PENDING;
  }

  public String getAnalysisVersion() {
    return analysisVersion;
  }

  public void setAnalysisVersion(String analysisVersion) {
    this.analysisVersion = analysisVersion;
  }

  public String getAnalysisResult() {
    return analysisResult;
  }

  public void setAnalysisResult(String analysisResult) {
    this.analysisResult = analysisResult;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }
}

package com.dreamgrid.model;

public abstract class AbstractDream implements IDreamEntry {
  protected String title;
  protected String content;
  protected long timestamp;
  protected String dreamDate;
  protected boolean analyzed;
  protected int id;
  protected String analysisResult;
  protected Long analyzedAt;
  protected String analysisVersion;
  protected AnalysisStatus analysisStatus;
  protected DreamClassification userClassification;
  protected DreamClassification inferredClassification;
  protected DreamClassification effectiveClassification;
  protected ClassificationSource classificationSource;
  protected String classificationReason;
  protected Long classificationUpdatedAt;

  public AbstractDream(String title, String content, String dreamDate, long timestamp) {
    this.title = title;
    this.content = content;
    this.dreamDate = dreamDate;
    this.timestamp = timestamp;
    this.analyzed = false;
    this.analysisStatus = AnalysisStatus.PENDING;
    this.effectiveClassification = DreamClassification.UNKNOWN;
    this.classificationSource = ClassificationSource.UNKNOWN;
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

  public String getContent() {
    return content;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getDreamDate() {
    return dreamDate;
  }

  public boolean isAnalyzed() {
    return analysisStatus == AnalysisStatus.COMPLETED;
  }

  public void setAnalyzed(boolean analyzed) {
    this.analyzed = analyzed;
    this.analysisStatus = analyzed ? AnalysisStatus.COMPLETED : AnalysisStatus.PENDING;
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
    this.analysisStatus = analysisStatus != null ? analysisStatus : AnalysisStatus.PENDING;
    this.analyzed = this.analysisStatus == AnalysisStatus.COMPLETED;
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
    return effectiveClassification != null ? effectiveClassification : DreamClassification.UNKNOWN;
  }

  public void setEffectiveClassification(DreamClassification effectiveClassification) {
    this.effectiveClassification =
        effectiveClassification != null ? effectiveClassification : DreamClassification.UNKNOWN;
  }

  public ClassificationSource getClassificationSource() {
    return classificationSource != null ? classificationSource : ClassificationSource.UNKNOWN;
  }

  public void setClassificationSource(ClassificationSource classificationSource) {
    this.classificationSource =
        classificationSource != null ? classificationSource : ClassificationSource.UNKNOWN;
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

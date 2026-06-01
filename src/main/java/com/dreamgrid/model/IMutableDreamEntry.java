package com.dreamgrid.model;

public interface IMutableDreamEntry extends IDreamEntry {
  void setId(int id);

  void setTitle(String title);

  void setContent(String content);

  void setTimestamp(long timestamp);

  void setDreamDate(String dreamDate);

  void setAnalyzed(boolean analyzed);

  void setAnalysisResult(String analysisResult);

  void setAnalyzedAt(Long analyzedAt);

  void setAnalysisVersion(String analysisVersion);

  void setAnalysisStatus(AnalysisStatus analysisStatus);

  void setUserClassification(DreamClassification userClassification);

  void setInferredClassification(DreamClassification inferredClassification);

  void setEffectiveClassification(DreamClassification effectiveClassification);

  void setClassificationSource(ClassificationSource classificationSource);

  void setClassificationReason(String classificationReason);

  void setTypeConfidence(Double typeConfidence);

  void setClassificationUpdatedAt(Long classificationUpdatedAt);
}

package com.dreamgrid.model;

public interface IDreamEntry {
  int getId();

  void setId(int id);

  String getTitle();

  String getContent();

  long getTimestamp();

  String getDreamDate();

  boolean isAnalyzed();

  void setAnalyzed(boolean analyzed);

  String getAnalysisResult();

  void setAnalysisResult(String analysisResult);

  Long getAnalyzedAt();

  void setAnalyzedAt(Long analyzedAt);

  String getAnalysisVersion();

  void setAnalysisVersion(String analysisVersion);

  AnalysisStatus getAnalysisStatus();

  void setAnalysisStatus(AnalysisStatus analysisStatus);
}

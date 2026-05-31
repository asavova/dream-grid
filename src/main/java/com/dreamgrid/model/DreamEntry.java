package com.dreamgrid.model;

import java.util.List;

public class DreamEntry extends AbstractDream {
  private DreamType dreamType;
  private List<DreamSymbol> dreamSymbols;

  public DreamEntry(
      String title,
      String content,
      String dreamDate,
      long timestamp,
      List<DreamSymbol> dreamSymbols,
      DreamType dreamType) {
    super(title, content, dreamDate, timestamp);
    this.dreamSymbols = dreamSymbols;
    this.dreamType = dreamType;
  }

  public DreamEntry(
      String title,
      String content,
      String dreamDate,
      long timestamp,
      List<DreamSymbol> dreamSymbols) {
    super(title, content, dreamDate, timestamp);
    this.dreamSymbols = dreamSymbols;
    this.dreamType = DreamType.NONE;
  }

  public DreamEntry(
      String title,
      String content,
      String dreamDate,
      long timestamp,
      List<DreamSymbol> dreamSymbols,
      DreamType dreamType,
      String analysisResult,
      Long analyzedAt,
      String analysisVersion,
      AnalysisStatus analysisStatus) {
    this(title, content, dreamDate, timestamp, dreamSymbols, dreamType);
    this.analysisResult = analysisResult;
    this.analyzedAt = analyzedAt;
    this.analysisVersion = analysisVersion;
    setAnalysisStatus(analysisStatus);
  }

  public List<DreamSymbol> getSymbolTags() {
    return dreamSymbols;
  }

  public DreamType getDreamType() {
    return dreamType;
  }

  public void setSymbolTags(List<DreamSymbol> dreamSymbols) {
    this.dreamSymbols = dreamSymbols;
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

package com.dreamgrid.dto;

import com.dreamgrid.model.AnalysisStatus;
import com.dreamgrid.model.DreamSymbol;
import com.dreamgrid.model.DreamType;
import java.util.List;

public class DreamResponse {
  private int id;
  private String title;
  private String content;
  private String dreamDate;
  private long timestamp;
  private List<DreamSymbol> symbolTags;
  private DreamType dreamType;
  private boolean analyzed;
  private String analysisResult;
  private Long analyzedAt;
  private String analysisVersion;
  private AnalysisStatus analysisStatus;

  public DreamResponse() {}

  public DreamResponse(
      int id,
      String title,
      String content,
      String dreamDate,
      long timestamp,
      List<DreamSymbol> symbolTags,
      DreamType dreamType,
      boolean analyzed,
      String analysisResult,
      Long analyzedAt,
      String analysisVersion,
      AnalysisStatus analysisStatus) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.dreamDate = dreamDate;
    this.timestamp = timestamp;
    this.symbolTags = symbolTags;
    this.dreamType = dreamType;
    this.analyzed = analyzed;
    this.analysisResult = analysisResult;
    this.analyzedAt = analyzedAt;
    this.analysisVersion = analysisVersion;
    this.analysisStatus = analysisStatus;
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

  public List<DreamSymbol> getSymbolTags() {
    return symbolTags;
  }

  public void setSymbolTags(List<DreamSymbol> symbolTags) {
    this.symbolTags = symbolTags;
  }

  public DreamType getDreamType() {
    return dreamType;
  }

  public void setDreamType(DreamType dreamType) {
    this.dreamType = dreamType;
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
}

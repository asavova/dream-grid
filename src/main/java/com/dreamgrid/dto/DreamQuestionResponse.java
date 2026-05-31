package com.dreamgrid.dto;

public class DreamQuestionResponse {
  private int id;
  private int dreamId;
  private Integer analysisRunId;
  private String question;
  private String answer;
  private long createdAt;

  public DreamQuestionResponse(
      int id, int dreamId, Integer analysisRunId, String question, String answer, long createdAt) {
    this.id = id;
    this.dreamId = dreamId;
    this.analysisRunId = analysisRunId;
    this.question = question;
    this.answer = answer;
    this.createdAt = createdAt;
  }

  public int getId() {
    return id;
  }

  public int getDreamId() {
    return dreamId;
  }

  public Integer getAnalysisRunId() {
    return analysisRunId;
  }

  public String getQuestion() {
    return question;
  }

  public String getAnswer() {
    return answer;
  }

  public long getCreatedAt() {
    return createdAt;
  }
}

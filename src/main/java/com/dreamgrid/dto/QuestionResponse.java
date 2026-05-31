package com.dreamgrid.dto;

public class QuestionResponse {
  private int dreamId;
  private String question;
  private String answer;

  public QuestionResponse(int dreamId, String question, String answer) {
    this.dreamId = dreamId;
    this.question = question;
    this.answer = answer;
  }

  public int getDreamId() {
    return dreamId;
  }

  public void setDreamId(int dreamId) {
    this.dreamId = dreamId;
  }

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }
}

package com.dreamgrid.dto;

import java.util.List;

public class DreamRequest {
  private String title;
  private String content;
  private String date;
  private String type;
  private List<String> tags;

  public DreamRequest() {}

  public DreamRequest(String title, String content, String date, String type) {
    this.title = title;
    this.content = content;
    this.date = date;
    this.type = type;
  }

  public DreamRequest(String title, String content, String date, String type, List<String> tags) {
    this(title, content, date, type);
    this.tags = tags;
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

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }
}

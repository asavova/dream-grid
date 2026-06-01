package com.dreamgrid.dto;

public class TagCoOccurrenceResponse {
  private final String firstTag;
  private final String secondTag;
  private final int count;

  public TagCoOccurrenceResponse(String firstTag, String secondTag, int count) {
    this.firstTag = firstTag;
    this.secondTag = secondTag;
    this.count = count;
  }

  public String getFirstTag() {
    return firstTag;
  }

  public String getSecondTag() {
    return secondTag;
  }

  public int getCount() {
    return count;
  }
}

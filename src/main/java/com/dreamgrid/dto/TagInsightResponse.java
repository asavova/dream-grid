package com.dreamgrid.dto;

public class TagInsightResponse {
  private final String tag;
  private final String normalizedName;
  private final int count;

  public TagInsightResponse(String tag, String normalizedName, int count) {
    this.tag = tag;
    this.normalizedName = normalizedName;
    this.count = count;
  }

  public String getTag() {
    return tag;
  }

  public String getNormalizedName() {
    return normalizedName;
  }

  public int getCount() {
    return count;
  }
}

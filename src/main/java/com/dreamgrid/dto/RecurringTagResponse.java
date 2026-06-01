package com.dreamgrid.dto;

public class RecurringTagResponse {
  private final String tag;
  private final String normalizedName;
  private final int dreamCount;

  public RecurringTagResponse(String tag, String normalizedName, int dreamCount) {
    this.tag = tag;
    this.normalizedName = normalizedName;
    this.dreamCount = dreamCount;
  }

  public String getTag() {
    return tag;
  }

  public String getNormalizedName() {
    return normalizedName;
  }

  public int getDreamCount() {
    return dreamCount;
  }
}

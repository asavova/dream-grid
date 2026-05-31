package com.dreamgrid.dto;

public class TagResponse {
  private String tag;
  private String normalizedName;
  private int count;

  public TagResponse(String tag, int count) {
    this.tag = tag;
    this.normalizedName = tag;
    this.count = count;
  }

  public TagResponse(String tag, String normalizedName, int count) {
    this.tag = tag;
    this.normalizedName = normalizedName;
    this.count = count;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getNormalizedName() {
    return normalizedName;
  }

  public void setNormalizedName(String normalizedName) {
    this.normalizedName = normalizedName;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }
}

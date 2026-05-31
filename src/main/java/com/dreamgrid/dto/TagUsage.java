package com.dreamgrid.dto;

public class TagUsage {
  private final String name;
  private final String normalizedName;
  private final int count;

  public TagUsage(String name, String normalizedName, int count) {
    this.name = name;
    this.normalizedName = normalizedName;
    this.count = count;
  }

  public String getName() {
    return name;
  }

  public String getNormalizedName() {
    return normalizedName;
  }

  public int getCount() {
    return count;
  }
}

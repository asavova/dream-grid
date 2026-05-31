package com.dreamgrid.model;

public class DreamTag {
  private int id;
  private String name;
  private String normalizedName;
  private TagSource source;
  private Double confidenceScore;
  private long createdAt;

  public DreamTag(
      int id,
      String name,
      String normalizedName,
      TagSource source,
      Double confidenceScore,
      long createdAt) {
    this.id = id;
    this.name = name;
    this.normalizedName = normalizedName;
    this.source = source;
    this.confidenceScore = confidenceScore;
    this.createdAt = createdAt;
  }

  public DreamTag(String name, String normalizedName, TagSource source) {
    this(0, name, normalizedName, source, null, System.currentTimeMillis());
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNormalizedName() {
    return normalizedName;
  }

  public void setNormalizedName(String normalizedName) {
    this.normalizedName = normalizedName;
  }

  public TagSource getSource() {
    return source;
  }

  public void setSource(TagSource source) {
    this.source = source;
  }

  public Double getConfidenceScore() {
    return confidenceScore;
  }

  public void setConfidenceScore(Double confidenceScore) {
    this.confidenceScore = confidenceScore;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }
}

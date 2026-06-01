package com.dreamgrid.dto;

import java.util.List;

public class TagDetailInsightResponse {
  private final String tag;
  private final String normalizedName;
  private final int usageCount;
  private final List<TagCoOccurrenceResponse> relatedTags;
  private final List<Integer> recentDreamIds;

  public TagDetailInsightResponse(
      String tag,
      String normalizedName,
      int usageCount,
      List<TagCoOccurrenceResponse> relatedTags,
      List<Integer> recentDreamIds) {
    this.tag = tag;
    this.normalizedName = normalizedName;
    this.usageCount = usageCount;
    this.relatedTags = relatedTags;
    this.recentDreamIds = recentDreamIds;
  }

  public String getTag() {
    return tag;
  }

  public String getNormalizedName() {
    return normalizedName;
  }

  public int getUsageCount() {
    return usageCount;
  }

  public List<TagCoOccurrenceResponse> getRelatedTags() {
    return relatedTags;
  }

  public List<Integer> getRecentDreamIds() {
    return recentDreamIds;
  }
}

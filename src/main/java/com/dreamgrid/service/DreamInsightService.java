package com.dreamgrid.service;

import com.dreamgrid.dto.RecurringTagResponse;
import com.dreamgrid.dto.TagCoOccurrenceResponse;
import com.dreamgrid.dto.TagDetailInsightResponse;
import com.dreamgrid.dto.TagInsightResponse;
import com.dreamgrid.dto.TagUsage;
import com.dreamgrid.repository.DreamRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DreamInsightService {
  private static final int DEFAULT_RECENT_DREAM_LIMIT = 10;

  private final DreamRepository dreamRepository;
  private final TagNormalizationService tagNormalizationService;

  public DreamInsightService(
      DreamRepository dreamRepository, TagNormalizationService tagNormalizationService) {
    this.dreamRepository = dreamRepository;
    this.tagNormalizationService = tagNormalizationService;
  }

  public List<TagInsightResponse> getFrequentTags() throws SQLException {
    return toTagInsights(dreamRepository.getTagUsageCounts());
  }

  public List<RecurringTagResponse> getRecurringTags() throws SQLException {
    List<RecurringTagResponse> responses = new ArrayList<>();
    for (TagUsage usage : dreamRepository.getRecurringTagUsageCounts()) {
      responses.add(
          new RecurringTagResponse(usage.getName(), usage.getNormalizedName(), usage.getCount()));
    }
    return responses;
  }

  public List<TagCoOccurrenceResponse> getTagCoOccurrences() throws SQLException {
    return dreamRepository.getTagCoOccurrences();
  }

  public TagDetailInsightResponse getTagDetailSummary(String tag) throws SQLException {
    String normalizedTag = tagNormalizationService.normalizeName(tag);
    if (normalizedTag.isBlank()) {
      return new TagDetailInsightResponse(tag, "", 0, List.of(), List.of());
    }

    TagUsage usage =
        dreamRepository.getTagUsageCounts().stream()
            .filter(item -> normalizedTag.equals(item.getNormalizedName()))
            .findFirst()
            .orElse(null);

    if (usage == null) {
      return new TagDetailInsightResponse(normalizedTag, normalizedTag, 0, List.of(), List.of());
    }

    List<TagCoOccurrenceResponse> relatedTags =
        dreamRepository.getRelatedTagCoOccurrences(normalizedTag).stream()
            .map(
                pair ->
                    new TagCoOccurrenceResponse(
                        normalizedTag.equals(pair.getFirstTag())
                            ? pair.getSecondTag()
                            : pair.getFirstTag(),
                        normalizedTag,
                        pair.getCount()))
            .toList();

    List<Integer> recentDreamIds =
        dreamRepository.findRecentDreamIdsByTag(normalizedTag, DEFAULT_RECENT_DREAM_LIMIT);

    return new TagDetailInsightResponse(
        usage.getName(), usage.getNormalizedName(), usage.getCount(), relatedTags, recentDreamIds);
  }

  private List<TagInsightResponse> toTagInsights(List<TagUsage> usages) {
    List<TagInsightResponse> responses = new ArrayList<>();
    for (TagUsage usage : usages) {
      responses.add(
          new TagInsightResponse(usage.getName(), usage.getNormalizedName(), usage.getCount()));
    }
    return responses;
  }
}

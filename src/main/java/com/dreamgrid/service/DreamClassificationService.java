package com.dreamgrid.service;

import com.dreamgrid.api.ApiErrorCode;
import com.dreamgrid.model.ClassificationSource;
import com.dreamgrid.model.DreamClassification;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamTag;
import com.dreamgrid.repository.DreamRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DreamClassificationService {
  private static final Path DEFAULT_RULE_PATH =
      Path.of("python", "rules", "classification_rules.json");
  private static final int DEFAULT_MIN_SHARED_TAGS = 2;
  private static final int DEFAULT_MIN_MATCHING_DREAMS = 1;

  private final DreamRepository dreamRepository;
  private final List<TypeRule> rules;
  private final int minSharedTagsForRecurring;
  private final int minMatchingDreamsForRecurring;

  public DreamClassificationService(DreamRepository dreamRepository) {
    this(dreamRepository, DEFAULT_RULE_PATH, DEFAULT_MIN_SHARED_TAGS, DEFAULT_MIN_MATCHING_DREAMS);
  }

  DreamClassificationService(DreamRepository dreamRepository, Path rulePath) {
    this(dreamRepository, rulePath, DEFAULT_MIN_SHARED_TAGS, DEFAULT_MIN_MATCHING_DREAMS);
  }

  DreamClassificationService(
      DreamRepository dreamRepository,
      Path rulePath,
      int minSharedTagsForRecurring,
      int minMatchingDreamsForRecurring) {
    this.dreamRepository = dreamRepository;
    this.rules = parseRules(new RuleFileLoader().load(rulePath, "classifications"));
    this.minSharedTagsForRecurring = Math.max(1, minSharedTagsForRecurring);
    this.minMatchingDreamsForRecurring = Math.max(1, minMatchingDreamsForRecurring);
  }

  public ClassificationResult inferFromDreamText(String title, String content) {
    String evidence =
        ((title == null ? "" : title) + " " + (content == null ? "" : content))
            .toLowerCase(Locale.ROOT);
    long now = System.currentTimeMillis();

    TypeRule fallback = null;
    for (TypeRule rule : rules) {
      if (rule.classification() == DreamClassification.NEUTRAL) {
        fallback = rule;
      }
      if (!rule.keywords().isEmpty() && containsAny(evidence, rule.keywords())) {
        return toResult(rule, now);
      }
    }

    if (fallback != null) {
      return toResult(fallback, now);
    }

    return new ClassificationResult(
        DreamClassification.UNKNOWN,
        ClassificationSource.UNKNOWN,
        "No configured classification rule matched.",
        null,
        now);
  }

  public ClassificationResult classifyRecurringFromPatterns(DreamEntry dream) throws SQLException {
    List<String> normalizedTags =
        dream.getSymbolTags().stream()
            .map(DreamTag::getNormalizedName)
            .filter(tag -> !tag.isBlank())
            .toList();
    if (normalizedTags.size() < minSharedTagsForRecurring) {
      return null;
    }

    int matchingDreams =
        dreamRepository.countDreamsSharingAtLeastTags(
            dream.getId(), normalizedTags, minSharedTagsForRecurring);
    if (matchingDreams < minMatchingDreamsForRecurring) {
      return null;
    }

    return new ClassificationResult(
        DreamClassification.RECURRING,
        ClassificationSource.PATTERN_ENGINE,
        "Dream shares multiple normalized tags with previous saved dreams.",
        0.88,
        System.currentTimeMillis());
  }

  public void applyInitialClassification(
      DreamEntry dream, DreamClassification userClassification, String title, String content) {
    if (userClassification != null && userClassification != DreamClassification.UNKNOWN) {
      dream.setUserClassification(userClassification);
      dream.setEffectiveClassification(userClassification);
      dream.setClassificationSource(ClassificationSource.USER);
      dream.setClassificationReason("Type provided by user.");
      dream.setTypeConfidence(1.0);
      dream.setClassificationUpdatedAt(System.currentTimeMillis());
      return;
    }
    long now = System.currentTimeMillis();
    dream.setUserClassification(null);
    dream.setInferredClassification(null);
    dream.setEffectiveClassification(DreamClassification.UNKNOWN);
    dream.setClassificationSource(ClassificationSource.UNKNOWN);
    dream.setClassificationReason("No classification has been inferred yet.");
    dream.setTypeConfidence(null);
    dream.setClassificationUpdatedAt(now);
  }

  public void applyInference(DreamEntry dream, ClassificationResult result) {
    if (result == null) {
      return;
    }

    dream.setInferredClassification(result.classification());
    dream.setClassificationReason(result.reason());
    dream.setTypeConfidence(result.confidence());
    dream.setClassificationUpdatedAt(result.updatedAt());

    if (dream.getUserClassification() == null) {
      dream.setEffectiveClassification(result.classification());
      dream.setClassificationSource(result.source());
    } else {
      dream.setEffectiveClassification(dream.getUserClassification());
      dream.setClassificationSource(ClassificationSource.USER);
      dream.setTypeConfidence(1.0);
    }
  }

  public void resolveEffectiveClassification(DreamEntry dream) {
    if (dream.getUserClassification() != null) {
      dream.setEffectiveClassification(dream.getUserClassification());
      dream.setClassificationSource(ClassificationSource.USER);
      dream.setTypeConfidence(1.0);
    } else if (dream.getInferredClassification() != null) {
      dream.setEffectiveClassification(dream.getInferredClassification());
      ClassificationSource source = dream.getClassificationSource();
      if (source != ClassificationSource.ANALYSIS
          && source != ClassificationSource.PATTERN_ENGINE) {
        source = ClassificationSource.ANALYSIS;
      }
      dream.setClassificationSource(source);
    } else {
      dream.setEffectiveClassification(DreamClassification.UNKNOWN);
      dream.setClassificationSource(ClassificationSource.UNKNOWN);
      dream.setTypeConfidence(null);
    }
    dream.setClassificationUpdatedAt(System.currentTimeMillis());
  }

  public DreamEntry applyUserClassificationOverride(int dreamId, String classification)
      throws SQLException {
    DreamEntry dream = dreamRepository.findById(dreamId);
    if (dream == null) {
      throw new DreamGridException(ApiErrorCode.NOT_FOUND, "Dream not found");
    }

    DreamClassification parsed = parseUserClassification(classification);
    dream.setUserClassification(parsed);
    dream.setEffectiveClassification(parsed);
    dream.setClassificationSource(ClassificationSource.USER);
    dream.setClassificationReason("Type provided by user.");
    dream.setTypeConfidence(1.0);
    dream.setClassificationUpdatedAt(System.currentTimeMillis());
    dreamRepository.updateClassificationFields(dream);
    return dreamRepository.findById(dreamId);
  }

  public DreamEntry clearUserClassificationOverride(int dreamId) throws SQLException {
    DreamEntry dream = dreamRepository.findById(dreamId);
    if (dream == null) {
      throw new DreamGridException(ApiErrorCode.NOT_FOUND, "Dream not found");
    }

    dream.setUserClassification(null);
    resolveEffectiveClassification(dream);
    dreamRepository.updateClassificationFields(dream);
    return dreamRepository.findById(dreamId);
  }

  public void markInferenceStaleAfterContentChange(DreamEntry dream) {
    dream.setInferredClassification(null);
    dream.setClassificationReason("Analysis inference is stale after content update.");
    dream.setTypeConfidence(null);
    dream.setClassificationUpdatedAt(System.currentTimeMillis());

    if (dream.getUserClassification() != null) {
      dream.setEffectiveClassification(dream.getUserClassification());
      dream.setClassificationSource(ClassificationSource.USER);
      dream.setTypeConfidence(1.0);
      return;
    }

    dream.setEffectiveClassification(DreamClassification.UNKNOWN);
    dream.setClassificationSource(ClassificationSource.UNKNOWN);
  }

  private List<TypeRule> parseRules(JsonObject root) {
    List<TypeRule> parsed = new ArrayList<>();
    for (JsonElement element : root.getAsJsonArray("classifications")) {
      JsonObject rule = element.getAsJsonObject();
      parsed.add(
          new TypeRule(
              DreamClassification.valueOf(rule.get("type").getAsString()),
              ClassificationSource.valueOf(rule.get("source").getAsString()),
              toStringList(rule.getAsJsonArray("keywords")),
              rule.get("reason").getAsString(),
              rule.get("confidence").getAsDouble()));
    }
    return parsed;
  }

  private ClassificationResult toResult(TypeRule rule, long updatedAt) {
    return new ClassificationResult(
        rule.classification(), rule.source(), rule.reason(), rule.confidence(), updatedAt);
  }

  private DreamClassification parseUserClassification(String classification) {
    if (classification == null || classification.isBlank()) {
      throw new DreamGridException(ApiErrorCode.VALIDATION_ERROR, "Classification is required");
    }

    try {
      return DreamClassification.valueOf(classification.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new DreamGridException(
          ApiErrorCode.VALIDATION_ERROR, "Invalid classification: " + classification);
    }
  }

  private boolean containsAny(String value, List<String> terms) {
    for (String term : terms) {
      if (value.contains(term)) {
        return true;
      }
    }
    return false;
  }

  private List<String> toStringList(JsonArray values) {
    List<String> parsed = new ArrayList<>();
    for (JsonElement value : values) {
      parsed.add(value.getAsString().toLowerCase(Locale.ROOT));
    }
    return parsed;
  }

  private record TypeRule(
      DreamClassification classification,
      ClassificationSource source,
      List<String> keywords,
      String reason,
      Double confidence) {}
}

package com.dreamgrid.service;

import com.dreamgrid.api.ApiErrorCode;
import com.dreamgrid.model.ClassificationSource;
import com.dreamgrid.model.DreamClassification;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamTag;
import com.dreamgrid.repository.DreamRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DreamClassificationService {
  private final DreamRepository dreamRepository;
  private final Gson gson = new Gson();

  public DreamClassificationService(DreamRepository dreamRepository) {
    this.dreamRepository = dreamRepository;
  }

  public ClassificationResult classifyFromAnalysis(DreamEntry dream, String analysisResult) {
    String evidence = buildEvidence(dream, analysisResult);
    long now = System.currentTimeMillis();

    if (containsAny(evidence, "lucid", "aware i was dreaming", "knew i was dreaming")) {
      return new ClassificationResult(
          DreamClassification.LUCID,
          ClassificationSource.ANALYSIS,
          "Analysis or dream content contains lucid-dream signals.",
          now);
    }

    if (containsAny(evidence, "nightmare", "terror", "terrified", "panic", "chased", "threat")) {
      return new ClassificationResult(
          DreamClassification.NIGHTMARE,
          ClassificationSource.ANALYSIS,
          "Analysis or dream content contains nightmare-related signals.",
          now);
    }

    return new ClassificationResult(
        DreamClassification.NEUTRAL,
        ClassificationSource.ANALYSIS,
        "No strong lucid, nightmare, or recurring pattern was detected.",
        now);
  }

  public ClassificationResult classifyRecurringFromPatterns(DreamEntry dream) throws SQLException {
    List<String> normalizedTags =
        dream.getSymbolTags().stream()
            .map(DreamTag::getNormalizedName)
            .filter(tag -> !tag.isBlank())
            .toList();
    if (normalizedTags.size() < 2) {
      return null;
    }

    int matchingDreams =
        dreamRepository.countDreamsSharingAtLeastTags(dream.getId(), normalizedTags, 2);
    if (matchingDreams == 0) {
      return null;
    }

    return new ClassificationResult(
        DreamClassification.RECURRING,
        ClassificationSource.PATTERN_ENGINE,
        "Dream shares multiple normalized tags with previous saved dreams.",
        System.currentTimeMillis());
  }

  public void applyInitialClassification(DreamEntry dream, DreamClassification userClassification) {
    if (userClassification != null && userClassification != DreamClassification.UNKNOWN) {
      dream.setUserClassification(userClassification);
      dream.setEffectiveClassification(userClassification);
      dream.setClassificationSource(ClassificationSource.USER);
      dream.setClassificationReason("Classification provided by user.");
      dream.setClassificationUpdatedAt(System.currentTimeMillis());
      return;
    }

    dream.setEffectiveClassification(DreamClassification.UNKNOWN);
    dream.setClassificationSource(ClassificationSource.UNKNOWN);
    dream.setClassificationReason(null);
    dream.setClassificationUpdatedAt(System.currentTimeMillis());
  }

  public void applyInference(DreamEntry dream, ClassificationResult result) {
    if (result == null) {
      return;
    }

    dream.setInferredClassification(result.classification());
    dream.setClassificationReason(result.reason());
    dream.setClassificationUpdatedAt(result.updatedAt());

    if (dream.getUserClassification() == null) {
      dream.setEffectiveClassification(result.classification());
      dream.setClassificationSource(result.source());
    } else {
      dream.setEffectiveClassification(dream.getUserClassification());
      dream.setClassificationSource(ClassificationSource.USER);
    }
  }

  public void resolveEffectiveClassification(DreamEntry dream) {
    if (dream.getUserClassification() != null) {
      dream.setEffectiveClassification(dream.getUserClassification());
      dream.setClassificationSource(ClassificationSource.USER);
    } else if (dream.getInferredClassification() != null) {
      dream.setEffectiveClassification(dream.getInferredClassification());
      dream.setClassificationSource(
          sourceForInferredClassification(dream.getInferredClassification()));
    } else {
      dream.setEffectiveClassification(DreamClassification.UNKNOWN);
      dream.setClassificationSource(ClassificationSource.UNKNOWN);
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
    dream.setClassificationReason("Classification provided by user.");
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

  private String buildEvidence(DreamEntry dream, String analysisResult) {
    List<String> parts = new ArrayList<>();
    parts.add(dream.getContent());
    parts.add(analysisResult);

    try {
      JsonObject root = gson.fromJson(analysisResult, JsonObject.class);
      if (root != null) {
        addString(root, "summary", parts);
        addJsonArray(root, "detectedSymbols", parts);
        addJsonArray(root, "detectedThemes", parts);
      }
    } catch (RuntimeException ignored) {
      // Non-JSON analysis is still searched as raw text.
    }

    return String.join(" ", parts).toLowerCase(Locale.ROOT);
  }

  private void addString(JsonObject root, String fieldName, List<String> parts) {
    if (root.has(fieldName) && root.get(fieldName).isJsonPrimitive()) {
      parts.add(root.get(fieldName).getAsString());
    }
  }

  private void addJsonArray(JsonObject root, String fieldName, List<String> parts) {
    if (!root.has(fieldName) || !root.get(fieldName).isJsonArray()) {
      return;
    }
    JsonArray values = root.getAsJsonArray(fieldName);
    for (JsonElement value : values) {
      if (value.isJsonPrimitive()) {
        parts.add(value.getAsString());
      }
    }
  }

  private boolean containsAny(String value, String... terms) {
    for (String term : terms) {
      if (value.contains(term)) {
        return true;
      }
    }
    return false;
  }

  private ClassificationSource sourceForInferredClassification(DreamClassification classification) {
    return classification == DreamClassification.RECURRING
        ? ClassificationSource.PATTERN_ENGINE
        : ClassificationSource.ANALYSIS;
  }
}

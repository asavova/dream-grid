package com.dreamgrid.service;

import com.dreamgrid.api.ApiErrorCode;
import com.dreamgrid.model.ClassificationSource;
import com.dreamgrid.model.DreamClassification;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamTag;
import com.dreamgrid.repository.DreamRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class DreamClassificationService {
  private final DreamRepository dreamRepository;

  public DreamClassificationService(DreamRepository dreamRepository) {
    this.dreamRepository = dreamRepository;
  }

  public ClassificationResult inferFromDreamText(String title, String content) {
    String evidence =
        ((title == null ? "" : title) + " " + (content == null ? "" : content))
            .toLowerCase(Locale.ROOT);
    long now = System.currentTimeMillis();

    if (containsAny(
        evidence,
        "aware i was dreaming",
        "controlled the dream",
        "realized i was dreaming",
        "realised i was dreaming")) {
      return new ClassificationResult(
          DreamClassification.LUCID,
          ClassificationSource.INFERRED,
          "Content indicates lucid dream awareness or control.",
          0.95,
          now);
    }

    if (containsAny(
        evidence, "fear", "threat", "panic", "chased", "trapped", "nightmare", "terror")) {
      return new ClassificationResult(
          DreamClassification.NIGHTMARE,
          ClassificationSource.INFERRED,
          "Content indicates fear, threat, or panic signals.",
          0.9,
          now);
    }

    if (containsAny(evidence, "recurring", "repeated", "again", "same dream")) {
      return new ClassificationResult(
          DreamClassification.RECURRING,
          ClassificationSource.INFERRED,
          "Title or content indicates recurring-dream wording.",
          0.85,
          now);
    }

    return new ClassificationResult(
        DreamClassification.NEUTRAL,
        ClassificationSource.INFERRED,
        "No strong lucid, nightmare, or recurring signals were found.",
        0.55,
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
        ClassificationSource.INFERRED,
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

    ClassificationResult inferred = inferFromDreamText(title, content);
    applyInference(dream, inferred);
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
      dream.setClassificationSource(ClassificationSource.INFERRED);
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

  private DreamClassification parseUserClassification(String classification) {
    if (classification == null || classification.isBlank()) {
      throw new DreamGridException(ApiErrorCode.VALIDATION_ERROR, "Type is required");
    }

    try {
      return DreamClassification.valueOf(classification.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new DreamGridException(
          ApiErrorCode.VALIDATION_ERROR, "Invalid type: " + classification);
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
}

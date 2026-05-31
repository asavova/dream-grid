package com.dreamgrid.dto;

import com.dreamgrid.model.ClassificationSource;
import com.dreamgrid.model.DreamClassification;

public class DreamClassificationResponse {
  private DreamClassification userClassification;
  private DreamClassification inferredClassification;
  private DreamClassification effectiveClassification;
  private ClassificationSource classificationSource;
  private String classificationReason;
  private Long classificationUpdatedAt;

  public DreamClassificationResponse(
      DreamClassification userClassification,
      DreamClassification inferredClassification,
      DreamClassification effectiveClassification,
      ClassificationSource classificationSource,
      String classificationReason,
      Long classificationUpdatedAt) {
    this.userClassification = userClassification;
    this.inferredClassification = inferredClassification;
    this.effectiveClassification = effectiveClassification;
    this.classificationSource = classificationSource;
    this.classificationReason = classificationReason;
    this.classificationUpdatedAt = classificationUpdatedAt;
  }

  public DreamClassification getUserClassification() {
    return userClassification;
  }

  public DreamClassification getInferredClassification() {
    return inferredClassification;
  }

  public DreamClassification getEffectiveClassification() {
    return effectiveClassification;
  }

  public ClassificationSource getClassificationSource() {
    return classificationSource;
  }

  public String getClassificationReason() {
    return classificationReason;
  }

  public Long getClassificationUpdatedAt() {
    return classificationUpdatedAt;
  }
}

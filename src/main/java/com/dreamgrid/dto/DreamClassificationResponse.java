package com.dreamgrid.dto;

import com.dreamgrid.model.ClassificationSource;
import com.dreamgrid.model.DreamClassification;

public class DreamClassificationResponse {
  private DreamClassification userClassification;
  private DreamClassification inferredClassification;
  private DreamClassification effectiveClassification;
  private ClassificationSource classificationSource;
  private String classificationReason;
  private Double typeConfidence;
  private Long classificationUpdatedAt;

  public DreamClassificationResponse(
      DreamClassification userClassification,
      DreamClassification inferredClassification,
      DreamClassification effectiveClassification,
      ClassificationSource classificationSource,
      String classificationReason,
      Double typeConfidence,
      Long classificationUpdatedAt) {
    this.userClassification = userClassification;
    this.inferredClassification = inferredClassification;
    this.effectiveClassification = effectiveClassification;
    this.classificationSource = classificationSource;
    this.classificationReason = classificationReason;
    this.typeConfidence = typeConfidence;
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

  public Double getTypeConfidence() {
    return typeConfidence;
  }

  public Long getClassificationUpdatedAt() {
    return classificationUpdatedAt;
  }

  public DreamClassification getUserType() {
    return userClassification;
  }

  public DreamClassification getInferredType() {
    return inferredClassification;
  }

  public DreamClassification getEffectiveType() {
    return effectiveClassification;
  }

  public ClassificationSource getTypeSource() {
    return classificationSource;
  }
}

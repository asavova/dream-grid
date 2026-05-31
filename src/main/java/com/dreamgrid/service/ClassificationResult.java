package com.dreamgrid.service;

import com.dreamgrid.model.ClassificationSource;
import com.dreamgrid.model.DreamClassification;

public record ClassificationResult(
    DreamClassification classification,
    ClassificationSource source,
    String reason,
    long updatedAt) {}

package com.dreamgrid.service;

import com.dreamgrid.api.ApiErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class DreamValidator {
  public static final int MAX_TITLE_LENGTH = 160;
  public static final int MAX_CONTENT_LENGTH = 12000;
  public static final int MAX_QUESTION_LENGTH = 1000;

  public void validateDream(String title, String content, String dreamDate) {
    validateRequired(title, "Dream title must not be blank");
    validateRequired(content, "Dream content must not be blank");
    validateLength(title, MAX_TITLE_LENGTH, "Dream title is too long");
    validateLength(content, MAX_CONTENT_LENGTH, "Dream content is too long");
    validateDate(dreamDate);
  }

  public void validateQuestion(String question) {
    validateRequired(question, "Question must not be blank");
    validateLength(question, MAX_QUESTION_LENGTH, "Question is too long");
  }

  private void validateRequired(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new DreamGridException(ApiErrorCode.VALIDATION_ERROR, message);
    }
  }

  private void validateLength(String value, int maxLength, String message) {
    if (value != null && value.length() > maxLength) {
      throw new DreamGridException(ApiErrorCode.VALIDATION_ERROR, message);
    }
  }

  private void validateDate(String dreamDate) {
    if (dreamDate == null || dreamDate.isBlank()) {
      return;
    }

    try {
      LocalDate.parse(dreamDate);
    } catch (DateTimeParseException dateOnlyFailure) {
      try {
        LocalDateTime.parse(dreamDate.replace(" ", "T"));
      } catch (DateTimeParseException dateTimeFailure) {
        throw new DreamGridException(
            ApiErrorCode.VALIDATION_ERROR,
            "Dream date must use yyyy-MM-dd or yyyy-MM-dd HH:mm:ss format");
      }
    }
  }
}

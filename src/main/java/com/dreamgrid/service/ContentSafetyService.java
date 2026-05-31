package com.dreamgrid.service;

import com.dreamgrid.api.ApiErrorCode;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ContentSafetyService {
  private static final Map<SafetyCategory, List<String>> BLOCKED_TERMS_BY_CATEGORY =
      createBlockedTerms();

  public void validateDreamContent(String content) {
    validate(content, "Dream content");
  }

  public void validateQuestion(String question) {
    validate(question, "Question");
  }

  private void validate(String value, String fieldName) {
    if (value == null) {
      return;
    }

    findPolicyViolation(value)
        .ifPresent(
            violation -> {
              throw new DreamGridException(
                  ApiErrorCode.CONTENT_REJECTED,
                  fieldName
                      + " is not supported because it matches the "
                      + violation.category().getDisplayName()
                      + " safety policy.");
            });
  }

  Optional<SafetyViolation> findPolicyViolation(String value) {
    String normalized = normalize(value);
    if (normalized.isBlank()) {
      return Optional.empty();
    }

    for (Map.Entry<SafetyCategory, List<String>> entry : BLOCKED_TERMS_BY_CATEGORY.entrySet()) {
      for (String blockedTerm : entry.getValue()) {
        if (normalized.contains(blockedTerm)) {
          return Optional.of(new SafetyViolation(entry.getKey(), blockedTerm));
        }
      }
    }

    return Optional.empty();
  }

  private static String normalize(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
  }

  private static Map<SafetyCategory, List<String>> createBlockedTerms() {
    Map<SafetyCategory, List<String>> terms = new EnumMap<>(SafetyCategory.class);
    terms.put(
        SafetyCategory.SEXUAL_CONTENT,
        List.of("explicit sexual", "graphic rape", "sexual assault instructions"));
    terms.put(
        SafetyCategory.GRAPHIC_VIOLENCE,
        List.of("graphic torture", "graphic mutilation", "violent threat"));
    terms.put(
        SafetyCategory.SELF_HARM,
        List.of("how to self harm", "how to kill myself", "suicide instructions"));
    terms.put(
        SafetyCategory.ILLEGAL_INSTRUCTIONS,
        List.of("make a bomb", "build a bomb", "illegal drug recipe"));
    terms.put(
        SafetyCategory.HATE_OR_HARASSMENT,
        List.of("racial slur", "hate speech", "targeted harassment"));
    return terms;
  }

  enum SafetyCategory {
    SEXUAL_CONTENT("sexual content"),
    GRAPHIC_VIOLENCE("graphic violence"),
    SELF_HARM("self-harm"),
    ILLEGAL_INSTRUCTIONS("illegal instructions"),
    HATE_OR_HARASSMENT("hate or harassment");

    private final String displayName;

    SafetyCategory(String displayName) {
      this.displayName = displayName;
    }

    String getDisplayName() {
      return displayName;
    }
  }

  record SafetyViolation(SafetyCategory category, String matchedTerm) {}
}

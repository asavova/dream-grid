package com.dreamgrid.service;

import com.dreamgrid.api.ApiErrorCode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ContentSafetyService {
  private static final Path DEFAULT_RULE_PATH =
      Path.of("python", "rules", "content_safety_rules.json");

  private final List<SafetyCategory> categories;

  public ContentSafetyService() {
    this(DEFAULT_RULE_PATH);
  }

  ContentSafetyService(Path rulePath) {
    JsonObject root = new RuleFileLoader().load(rulePath, "categories");
    this.categories = parseCategories(root.getAsJsonArray("categories"));
  }

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
              String message = violation.category().message().replace("{field}", fieldName);
              throw new DreamGridException(ApiErrorCode.CONTENT_REJECTED, message);
            });
  }

  Optional<SafetyViolation> findPolicyViolation(String value) {
    String normalized = normalize(value);
    if (normalized.isBlank()) {
      return Optional.empty();
    }

    for (SafetyCategory category : categories) {
      for (String blockedTerm : category.keywords()) {
        if (normalized.contains(blockedTerm)) {
          return Optional.of(new SafetyViolation(category, blockedTerm));
        }
      }
    }

    return Optional.empty();
  }

  private List<SafetyCategory> parseCategories(JsonArray values) {
    List<SafetyCategory> parsed = new ArrayList<>();
    for (JsonElement element : values) {
      JsonObject category = element.getAsJsonObject();
      parsed.add(
          new SafetyCategory(
              category.get("id").getAsString(),
              category.get("message").getAsString(),
              toStringList(category.getAsJsonArray("keywords"))));
    }
    return parsed;
  }

  private List<String> toStringList(JsonArray values) {
    List<String> parsed = new ArrayList<>();
    for (JsonElement value : values) {
      parsed.add(normalize(value.getAsString()));
    }
    return parsed;
  }

  private static String normalize(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
  }

  record SafetyCategory(String id, String message, List<String> keywords) {}

  record SafetyViolation(SafetyCategory category, String matchedTerm) {}
}

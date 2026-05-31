package com.dreamgrid.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum DreamSymbol {
  FIRE,
  WATER,
  CAT,
  EYE,
  DEATH,
  PORTAL,
  SKY,
  UNKNOWN;

  public static String serialize(List<DreamSymbol> tags) {
    return normalizeSymbols(tags).stream().map(Enum::name).collect(Collectors.joining(","));
  }

  DreamSymbol() {}

  public static List<DreamSymbol> deserialize(String str) {
    if (str == null || str.isBlank()) {
      return List.of(UNKNOWN);
    }
    return normalizeTagNames(Arrays.asList(str.split(",")));
  }

  public static Optional<DreamSymbol> fromTag(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }

    String normalized = value.trim().replace("-", "_").replace(" ", "_").toUpperCase(Locale.ROOT);
    try {
      return Optional.of(DreamSymbol.valueOf(normalized));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public static List<DreamSymbol> normalizeTagNames(Collection<String> values) {
    if (values == null || values.isEmpty()) {
      return List.of(UNKNOWN);
    }

    Set<DreamSymbol> normalized = new LinkedHashSet<>();
    for (String value : values) {
      fromTag(value).ifPresent(normalized::add);
    }

    return normalized.isEmpty() ? List.of(UNKNOWN) : List.copyOf(normalized);
  }

  public static List<DreamSymbol> normalizeSymbols(Collection<DreamSymbol> values) {
    if (values == null || values.isEmpty()) {
      return List.of(UNKNOWN);
    }

    Set<DreamSymbol> normalized = new LinkedHashSet<>();
    for (DreamSymbol value : values) {
      if (value != null) {
        normalized.add(value);
      }
    }

    return normalized.isEmpty() ? List.of(UNKNOWN) : List.copyOf(normalized);
  }
}

package com.dreamgrid.model;

import java.util.Arrays;
import java.util.List;
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
    return tags.stream().map(Enum::name).collect(Collectors.joining(","));
  }

  DreamSymbol() {}

  public static List<DreamSymbol> deserialize(String str) {
    if (str == null || str.isBlank()) {
      return List.of(UNKNOWN);
    }
    return Arrays.stream(str.split(",")).map(DreamSymbol::valueOf).collect(Collectors.toList());
  }
}

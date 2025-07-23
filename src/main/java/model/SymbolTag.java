package model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum SymbolTag {
  FIRE,
  WATER,
  CAT,
  EYE,
  DEATH,
  PORTAL,
  SKY,
  UNKNOWN;

  public static String serialize(List<SymbolTag> tags) {
    return tags.stream().map(Enum::name).collect(Collectors.joining(","));
  }

  SymbolTag() {}

  public static List<SymbolTag> deserialize(String str) {
    return Arrays.stream(str.split(",")).map(SymbolTag::valueOf).collect(Collectors.toList());
  }
}

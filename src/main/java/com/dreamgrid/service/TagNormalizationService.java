package com.dreamgrid.service;

import com.dreamgrid.model.DreamTag;
import com.dreamgrid.model.TagSource;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TagNormalizationService {
  public List<DreamTag> normalize(Collection<String> values, TagSource source) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }

    Map<String, DreamTag> tagsByNormalizedName = new LinkedHashMap<>();
    for (String value : values) {
      String normalizedName = normalizeName(value);
      if (normalizedName.isBlank() || tagsByNormalizedName.containsKey(normalizedName)) {
        continue;
      }

      tagsByNormalizedName.put(
          normalizedName, new DreamTag(toDisplayName(normalizedName), normalizedName, source));
    }
    return new ArrayList<>(tagsByNormalizedName.values());
  }

  public String normalizeName(String value) {
    if (value == null) {
      return "";
    }

    String normalized =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[_\\-]+", " ")
            .replaceAll("[^\\p{Alnum}\\s']", " ")
            .replaceAll("\\s+", " ")
            .trim();
    return normalized;
  }

  private String toDisplayName(String normalizedName) {
    return normalizedName;
  }
}

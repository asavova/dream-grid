package com.dreamgrid.service;

import com.dreamgrid.model.DreamSymbol;
import java.util.ArrayList;
import java.util.List;

public class SymbolExtractionService {
  public static List<DreamSymbol> analyze(String content) {
    List<DreamSymbol> tags = new ArrayList<>();
    String lower = content.toLowerCase();

    if (lower.contains("fire") || lower.contains("sun")) tags.add(DreamSymbol.FIRE);
    if (lower.contains("water")) tags.add(DreamSymbol.WATER);
    if (lower.contains("cat")) tags.add(DreamSymbol.CAT);
    if (lower.contains("eye")) tags.add(DreamSymbol.EYE);
    if (lower.contains("death")) tags.add(DreamSymbol.DEATH);
    if (lower.contains("portal")) tags.add(DreamSymbol.PORTAL);
    if (lower.contains("sky") || lower.contains("bird")) tags.add(DreamSymbol.SKY);

    if (tags.isEmpty()) tags.add(DreamSymbol.UNKNOWN);
    return tags;
  }
}

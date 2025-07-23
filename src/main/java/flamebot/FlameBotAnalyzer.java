package flamebot;

import java.util.ArrayList;
import java.util.List;
import model.SymbolTag;

public class FlameBotAnalyzer {
  public static List<SymbolTag> analyze(String content) {
    List<SymbolTag> tags = new ArrayList<>();
    String lower = content.toLowerCase();

    if (lower.contains("fire") || lower.contains("sun")) tags.add(SymbolTag.FIRE);
    if (lower.contains("water")) tags.add(SymbolTag.WATER);
    if (lower.contains("cat")) tags.add(SymbolTag.CAT);
    if (lower.contains("eye")) tags.add(SymbolTag.EYE);
    if (lower.contains("death")) tags.add(SymbolTag.DEATH);
    if (lower.contains("portal")) tags.add(SymbolTag.PORTAL);
    if (lower.contains("sky") || lower.contains("bird")) tags.add(SymbolTag.SKY);

    if (tags.isEmpty()) tags.add(SymbolTag.UNKNOWN);
    return tags;
  }
}

// Файл: DreamDatabase.java
package java_logic;

import java.util.ArrayList;
import java.util.List;

public class DreamCollector {
  private List<IDreamEntry> dreamEntries;

  public DreamCollector() {
    this.dreamEntries = new ArrayList<>();
  }

  // Добавяне на сън
  public void addDream(
      String title, String content, String symbolTag, String moodTag, long timestamp) {
    DreamEntry entry = new DreamEntry(title, content, symbolTag, moodTag, timestamp);
    dreamEntries.add(entry);
  }

  // Връщане на всички сънища
  public List<IDreamEntry> getAllDreams() {
    return dreamEntries;
  }

  // Търсене по символ или настроение
  public List<IDreamEntry> findDreamsByTag(String tag) {
    List<IDreamEntry> result = new ArrayList<>();
    for (IDreamEntry entry : dreamEntries) {
      if (entry.getSymbolTag().equalsIgnoreCase(tag) || entry.getMoodTag().equalsIgnoreCase(tag)) {
        result.add(entry);
      }
    }
    return result;
  }
}

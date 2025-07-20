package java_logic.model;

import java_logic.AbstractDreamEntry;

public class DreamEntry extends AbstractDreamEntry {

  public DreamEntry(
      Integer id, String title, String content, String symbolTag, String moodTag, long timestamp) {
    super(id, title, content, symbolTag, moodTag, timestamp);
  }

  public DreamEntry(
      String title, String content, String symbolTag, String moodTag, long timestamp) {
    super(null, title, content, symbolTag, moodTag, timestamp);
  }

  @Override
  public String getType() {
    return "Raw";
  }
}

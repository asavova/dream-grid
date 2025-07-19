package java_logic;

public class DreamEntry extends AbstractDreamEntry {

  public DreamEntry(
      String title, String content, String symbolTag, String moodTag, long timestamp) {
    super(title, content, symbolTag, moodTag, timestamp);
  }

  @Override
  public String getType() {
    return "Raw";
  }
}

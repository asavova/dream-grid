package java_logic;

public abstract class AbstractDreamEntry implements IDreamEntry {
  protected String title;
  protected String content;
  protected String symbolTag;
  protected String moodTag;
  protected long timestamp;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  private Integer id;

  public AbstractDreamEntry(
      Integer id, String title, String content, String symbolTag, String moodTag, long timestamp) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.symbolTag = symbolTag;
    this.moodTag = moodTag;
    this.timestamp = timestamp;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public String getSymbolTag() {
    return symbolTag;
  }

  public String getMoodTag() {
    return moodTag;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public abstract String getType(); // e.g. "Raw" or "Analyzed"
}

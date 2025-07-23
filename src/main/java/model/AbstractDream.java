package model;

public abstract class AbstractDream implements IDreamEntry {
  protected String title;
  protected String content;
  protected long timestamp;
  protected String dreamDate;
  protected boolean analyzed;
  protected int id;

  public AbstractDream(String title, String content, String dreamDate, long timestamp) {
    this.title = title;
    this.content = content;
    this.dreamDate = dreamDate;
    this.timestamp = timestamp;
    this.analyzed = false;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getDreamDate() {
    return dreamDate;
  }

  public boolean isAnalyzed() {
    return analyzed;
  }

  public void setAnalyzed(boolean analyzed) {
    this.analyzed = analyzed;
  }
}

package model;

import java.util.List;

public class DreamEntry extends AbstractDream {
  private DreamType dreamType;
  private List<SymbolTag> symbolTags;

  public DreamEntry(
      String title,
      String content,
      String dreamDate,
      long timestamp,
      List<SymbolTag> symbolTags,
      DreamType dreamType) {
    super(title, content, dreamDate, timestamp);
    this.symbolTags = symbolTags;
    this.dreamType = dreamType;
    this.analyzed = true;
  }

  public DreamEntry(
      String title, String content, String dreamDate, long timestamp, List<SymbolTag> symbolTags) {
    super(title, content, dreamDate, timestamp);
    this.symbolTags = symbolTags;
    this.dreamType = DreamType.NONE;
    this.analyzed = true;
  }

  public List<SymbolTag> getSymbolTags() {
    return symbolTags;
  }

  public DreamType getDreamType() {
    return dreamType;
  }

  public void setSymbolTags(List<SymbolTag> symbolTags) {
    this.symbolTags = symbolTags;
  }

  public void setDreamType(DreamType dreamType) {
    this.dreamType = dreamType;
  }
}

package java_logic;

public class AnalyzedDreamEntry extends AbstractDreamEntry {
  private final String frequencyLabel;
  private final String matchedTrack;

  public AnalyzedDreamEntry(
      String title,
      String content,
      String symbolTag,
      String moodTag,
      long timestamp,
      String frequencyLabel,
      String matchedTrack) {
    super(title, content, symbolTag, moodTag, timestamp);
    this.frequencyLabel = frequencyLabel;
    this.matchedTrack = matchedTrack;
  }

  @Override
  public String getType() {
    return "Analyzed";
  }

  public String getFrequencyLabel() {
    return frequencyLabel;
  }

  public String getMatchedTrack() {
    return matchedTrack;
  }
}

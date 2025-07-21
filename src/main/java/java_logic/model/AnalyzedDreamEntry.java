package java_logic.model;

import java_logic.AbstractDreamEntry;

public class AnalyzedDreamEntry extends AbstractDreamEntry {

  private String[] symbols;
  private String frequency;
  private String oracleMessage;

  public AnalyzedDreamEntry(
      Integer id,
      String title,
      String content,
      String symbolTag,
      String moodTag,
      long timestamp,
      String[] symbols,
      String frequency,
      String oracleMessage) {
    super(id, title, content, symbolTag, moodTag, timestamp);
    this.symbols = symbols;
    this.frequency = frequency;
    this.oracleMessage = oracleMessage;
  }

  public String[] getSymbols() {
    return symbols;
  }

  public String getFrequency() {
    return frequency;
  }

  public String getOracleMessage() {
    return oracleMessage;
  }

  @Override
  public String getType() {
    return "Analyzed";
  }
}

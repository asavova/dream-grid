package model;

public interface IDreamEntry {
  String getTitle();

  String getContent();

  long getTimestamp();

  String getDreamDate();

  boolean isAnalyzed();
}

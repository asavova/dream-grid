package java_logic.services;

import java.sql.Connection;
import java.util.List;
import java_logic.DreamEntry;
import java_logic.DreamEntryDAO;

public class DreamService {
  private DreamEntryDAO dao;

  public DreamService(Connection conn) {
    this.dao = new DreamEntryDAO(conn);
  }

  public void addDream(
      String title, String content, String symbolTag, String moodTag, long timestamp) {
    final DreamEntry entry = new DreamEntry(title, content, symbolTag, moodTag, timestamp);
    this.addDream(entry);
  }

  public List<DreamEntry> getAllDreams() {
    return dao.getAll();
  }

  public void addDream(DreamEntry entry) {
    dao.insert(entry);
  }

  // Предвидени за бъдеще: филтриране по символи, настроение и т.н.
}

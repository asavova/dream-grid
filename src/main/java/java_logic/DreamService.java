package java_logic;

import java.util.List;
import java_logic.model.DreamEntry;
import java_logic.model.DreamEntryDAO;

public class DreamService {
  private final DreamEntryDAO dao;

  public DreamService(DreamEntryDAO dao) {
    this.dao = dao;
  }

  public void addDream(
      String title, String content, String symbolTag, String moodTag, long timestamp) {
    DreamEntry entry = new DreamEntry(null, title, content, symbolTag, moodTag, timestamp);
    if (title == null || title.isEmpty()) {
      throw new IllegalArgumentException("Title can't be empty");
    }

    try {
      dao.insert(entry);
      System.out.println("✅ Dream added successfully");
    } catch (Exception e) {
      System.err.println("❌ Failed to add dream: " + e.getMessage());
    }
  }

  public List<DreamEntry> getAllDreams() {
    return dao.findAll();
  }
}

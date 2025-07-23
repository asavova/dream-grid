package service;

import database.DreamEntryDAO;
import flamebot.FlameBotAnalyzer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import model.DreamEntry;
import model.SymbolTag;

public class DreamService {
  private DreamEntryDAO dao;

  public DreamService(Connection connection) {
    this.dao = new DreamEntryDAO(connection);
  }

  public DreamService(DreamEntryDAO dreamEntryDAO) {
    this.dao = dreamEntryDAO;
  }

  public void addDream(DreamEntry dream) throws SQLException {
    dao.insert(dream);
  }

  public List<DreamEntry> getAllDreams() throws SQLException {
    return dao.getAll();
  }

  public void processAndStoreDream(String title, String content, String dreamDate) {
    long timestamp = System.currentTimeMillis();
    List<SymbolTag> tags = FlameBotAnalyzer.analyze(content);
    DreamEntry entry = new DreamEntry(title, content, dreamDate, timestamp, tags);

    try {
      dao.insert(entry);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}

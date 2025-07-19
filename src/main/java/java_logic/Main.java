package java_logic;

import java.util.List;
import java_logic.services.DreamService;

public class Main {
  public static void main(String[] args) {
    DreamDatabase.initialize();
    // Можеш да добавиш и първоначално тестване или бот интеракция тук.
    DreamEntry entry =
        new DreamEntry(
            "Flying over the city",
            "I was flying over a city made of glass. The sun never set.",
            "Cat",
            "Euphoric",
            System.currentTimeMillis());
    DreamService service = new DreamService(DreamDatabase.getConnection());
    service.addDream(entry);
    service.addDream(
        "Flying Cat",
        "I flew with a cat over a rainbow",
        "cat",
        "happy",
        System.currentTimeMillis());

    DreamEntryDAO dreamEntryDAO = new DreamEntryDAO(DreamDatabase.getConnection());
    List<DreamEntry> entries = dreamEntryDAO.getAll();

    for (DreamEntry e : entries) {
      System.out.println("Title: " + e.getTitle());
      System.out.println("Symbol: " + e.getSymbolTag());
      System.out.println("Mood: " + e.getMoodTag());
      System.out.println("Time: " + e.getTimestamp());
      System.out.println("Content: " + e.getContent());
      System.out.println("---");
    }
    DreamDatabase.close();
  }
}

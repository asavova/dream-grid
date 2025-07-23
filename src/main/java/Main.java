import database.DreamDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import model.DreamEntry;
import model.SymbolTag;
import service.DreamService;

public class Main {

  public static void main(String[] args) {
    DreamDatabase.initialize();
    try (Connection connection = DreamDatabase.getConnection()) {
      DreamService dreamService = new DreamService(connection);
      Scanner scanner = new Scanner(System.in);

      System.out.println("Welcome to DreamGrid – enter your dream:");

      //            System.out.print("Title: ");
      //            String title = scanner.nextLine();
      //
      //            System.out.print("Content: ");
      //            String content = scanner.nextLine();
      //
      //            System.out.print("Dream date (YYYY-MM-DD): ");
      String content = " I was walking through a forest and saw a cat and a mirror";
      String title = "cat";
      String date = "2020-04-04";

      long timestamp = System.currentTimeMillis();

      // Send to FlameBot for analysis (via Python)
      String flamebotResponse = FlamebotAIClient.analyzeDream(content);
      System.out.println("FlameBot response: " + flamebotResponse);

      // Example: extracting tags from the response
      List<SymbolTag> tags = new ArrayList<>();
      if (flamebotResponse.contains("fire")) tags.add(SymbolTag.FIRE);
      if (flamebotResponse.contains("water")) tags.add(SymbolTag.WATER);
      if (flamebotResponse.contains("cat")) tags.add(SymbolTag.CAT);

      // TODO: JSON parsing

      DreamEntry entry = new DreamEntry(title, content, date, timestamp, tags);
      dreamService.addDream(entry);

      System.out.println("Dream saved with tags: " + tags);

    } catch (SQLException e) {
      e.printStackTrace();
      System.err.println("Database error: " + e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("General error: " + e.getMessage());
    }
  }
}

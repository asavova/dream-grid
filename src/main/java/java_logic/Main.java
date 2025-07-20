package java_logic;

import java.io.*;
import java.sql.SQLException;
import java.util.Scanner;
import java_logic.model.DreamEntryDAO;

public class Main {
  public static void main(String[] args) throws SQLException {
    DreamDatabase.initialize();

    DreamEntryDAO dreamEntryDAO = new DreamEntryDAO(DreamDatabase.getConnection());
    DreamService dreamService = new DreamService(dreamEntryDAO);
    Scanner scanner = new Scanner(System.in);

    System.out.println("🌀 title:");
    String title = scanner.nextLine();

    System.out.println("📜 Enter description of the dream:");
    String description = scanner.nextLine();
    // "A young woman lying on her back in a dark room with just a candle lit in one corner. The
    // room is cozy but suffocating, and there's a strange smell that makes my nostrils flare."

    dreamService.addDream(title, description, "", "", System.currentTimeMillis());

    try {
      // 🔥 Стартира flamebot_ollama_test.py с подаден промпт (description)
      ProcessBuilder pb =
          new ProcessBuilder(
              "/Users/mattdwellers/StudioProjects/dream-grid/flamebot-env/bin/python3",
              "flamebot_ollama_test.py",
              description);
      pb.redirectErrorStream(true);
      Process process = pb.start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      System.out.println("\n🧠 FlameBot says:\n");

      while ((line = reader.readLine()) != null) {
        System.out.println(line);
      }

      int exitCode = process.waitFor();
      System.out.println("\n⚙️ Script ended with status: " + exitCode);

    } catch (IOException | InterruptedException e) {
      System.err.println("Error calling FlameBot: " + e.getMessage());
    }

    scanner.close();
  }
}

import java.io.*;

public class FlameBotBridge {
  private Process pythonProcess;

  public void startPythonServer() throws IOException {
    // Път до Python файла (коректно спрямо структурата ти)
    ProcessBuilder builder = new ProcessBuilder("python3", "flamebot/app.py");
    builder.redirectErrorStream(true);
    pythonProcess = builder.start();

    // Отпечатай изхода от Python сървъра за дебъг
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
    new Thread(() -> reader.lines().forEach(System.out::println)).start();
  }

  public void stopPythonServer() {
    if (pythonProcess != null) pythonProcess.destroy();
  }
}

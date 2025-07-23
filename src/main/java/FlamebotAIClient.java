import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FlamebotAIClient {

  private static final String HUGGINGFACE_TOKEN = System.getenv("HUGGINGFACE_TOKEN");

  private static final String AI_API_ENDPOINT = "http://127.0.0.1:5005/analyze";

  public static String analyzeDream(String dream) throws IOException {
    URL url = new URL(AI_API_ENDPOINT);
    HttpURLConnection con = getHttpURLConnection(dream, url);

    StringBuilder response = new StringBuilder();
    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        response.append(line.trim());
      }
    }

    return response.toString();
  }

  private static HttpURLConnection getHttpURLConnection(String dream, URL url) throws IOException {
    HttpURLConnection con = (HttpURLConnection) url.openConnection();

    con.setRequestMethod("POST");
    con.setDoOutput(true);
    con.setRequestProperty("Authorization", "Bearer " + HUGGINGFACE_TOKEN);

    con.setRequestProperty("Content-Type", "application/json");

    //        String prompt = "[INST] Analyze this dream: \"" + dream + "\". Give me tags and type.
    // [/INST]";

    String jsonInput = "{\"dream\": \"" + dream.replace("\"", "\\\"") + "\"}";

    try (OutputStream os = con.getOutputStream()) {
      byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }
    return con;
  }
}

package com.dreamgrid.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DreamAnalysisClient {

  private static final String HUGGINGFACE_TOKEN = System.getenv("HUGGINGFACE_TOKEN");
  private static final String ANALYZE_ENDPOINT = "http://127.0.0.1:5005/analyze";
  private static final String ASK_ENDPOINT = "http://127.0.0.1:5005/ask";
  private final Gson gson = new Gson();

  public String analyzeDream(String dream) throws IOException {
    return postJson(ANALYZE_ENDPOINT, Map.of("dream", dream));
  }

  public String askQuestion(String dream, String analysis, String question) throws IOException {
    String response =
        postJson(ASK_ENDPOINT, Map.of("dream", dream, "analysis", analysis, "question", question));
    JsonObject json = gson.fromJson(response, JsonObject.class);
    if (json != null && json.has("answer") && !json.get("answer").isJsonNull()) {
      return json.get("answer").getAsString();
    }
    return response;
  }

  private String postJson(String endpoint, Map<String, String> payload) throws IOException {
    URL url = new URL(endpoint);
    HttpURLConnection con = openPostConnection(url);
    String jsonInput = gson.toJson(payload);

    try (OutputStream os = con.getOutputStream()) {
      byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int statusCode = con.getResponseCode();
    InputStream responseStream = statusCode >= 400 ? con.getErrorStream() : con.getInputStream();
    StringBuilder response = new StringBuilder();
    if (responseStream != null) {
      try (BufferedReader br =
          new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
        String line;
        while ((line = br.readLine()) != null) {
          response.append(line.trim());
        }
      }
    }

    if (statusCode >= 400) {
      throw new IOException("Analysis service returned " + statusCode + ": " + response);
    }

    return response.toString();
  }

  private static HttpURLConnection openPostConnection(URL url) throws IOException {
    HttpURLConnection con = (HttpURLConnection) url.openConnection();

    con.setRequestMethod("POST");
    con.setDoOutput(true);
    con.setRequestProperty("Authorization", "Bearer " + HUGGINGFACE_TOKEN);

    con.setRequestProperty("Content-Type", "application/json");
    return con;
  }
}

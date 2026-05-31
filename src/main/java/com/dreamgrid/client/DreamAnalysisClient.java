package com.dreamgrid.client;

import com.dreamgrid.config.AppConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DreamAnalysisClient {

  private final Gson gson = new Gson();
  private final String analysisServiceBaseUrl;
  private final int connectTimeoutMs;
  private final int readTimeoutMs;

  public DreamAnalysisClient() {
    this(AppConfig.load());
  }

  public DreamAnalysisClient(AppConfig config) {
    this(
        config.getAnalysisServiceBaseUrl(),
        config.getConnectTimeoutMs(),
        config.getReadTimeoutMs());
  }

  public DreamAnalysisClient(
      String analysisServiceBaseUrl, int connectTimeoutMs, int readTimeoutMs) {
    this.analysisServiceBaseUrl = trimTrailingSlash(analysisServiceBaseUrl);
    this.connectTimeoutMs = connectTimeoutMs;
    this.readTimeoutMs = readTimeoutMs;
  }

  public String analyzeDream(String dream) throws IOException {
    return postJson("/analyze", Map.of("dream", dream));
  }

  public String askQuestion(String dream, String analysis, String question) throws IOException {
    String response =
        postJson("/ask", Map.of("dream", dream, "analysis", analysis, "question", question));
    JsonObject json = gson.fromJson(response, JsonObject.class);
    if (json != null && json.has("answer") && !json.get("answer").isJsonNull()) {
      return json.get("answer").getAsString();
    }
    return response;
  }

  private String postJson(String path, Map<String, String> payload) throws IOException {
    URL url = new URL(analysisServiceBaseUrl + path);
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

  private HttpURLConnection openPostConnection(URL url) throws IOException {
    HttpURLConnection con = (HttpURLConnection) url.openConnection();

    con.setRequestMethod("POST");
    con.setDoOutput(true);
    con.setConnectTimeout(connectTimeoutMs);
    con.setReadTimeout(readTimeoutMs);
    con.setRequestProperty("Content-Type", "application/json");
    return con;
  }

  private String trimTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      return AppConfig.from(Map.of()).getAnalysisServiceBaseUrl();
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}

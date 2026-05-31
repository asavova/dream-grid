package com.dreamgrid.api;

import static org.junit.Assert.assertEquals;

import com.dreamgrid.client.DreamAnalysisClient;
import com.dreamgrid.model.DreamType;
import com.dreamgrid.repository.DreamRepository;
import com.dreamgrid.service.DreamService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DreamApiHandlerTest {
  private Connection connection;
  private DreamRepository repository;
  private FakeAnalysisClient analysisClient;
  private DreamService dreamService;
  private HttpServer server;
  private String baseUrl;

  @Before
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    createSchema(connection);
    repository = new DreamRepository(connection);
    analysisClient = new FakeAnalysisClient();
    dreamService = new DreamService(repository, analysisClient, "test-analysis-version");

    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", new DreamApiHandler(dreamService));
    server.start();
    baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) {
      server.stop(0);
    }
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  public void missingDreamReturnsStructuredNotFound() throws Exception {
    Response response = post("/dreams/999/analyze", "");
    JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();

    assertEquals(404, response.statusCode());
    assertEquals("NOT_FOUND", body.get("error").getAsString());
  }

  @Test
  public void analysisServiceFailureReturnsStructuredError() throws Exception {
    repository.insert(
        new com.dreamgrid.model.DreamEntry(
            "Dream", "A safe dream.", "2026-05-31", 123L, List.of(), DreamType.ORDINARY));
    analysisClient.nextFailure = new IOException("service unavailable");

    Response response = post("/dreams/1/analyze", "");
    JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();

    assertEquals(502, response.statusCode());
    assertEquals("ANALYSIS_SERVICE_ERROR", body.get("error").getAsString());
  }

  @Test
  public void tagsEndpointReturnsUsageCounts() throws Exception {
    dreamService.saveDream(
        "Forest", "A quiet forest.", "2026-05-31", DreamType.ORDINARY, List.of("Forest", "forest"));

    Response response = get("/tags");
    JsonArray body = JsonParser.parseString(response.body()).getAsJsonArray();

    assertEquals(200, response.statusCode());
    assertEquals("forest", body.get(0).getAsJsonObject().get("normalizedName").getAsString());
    assertEquals(1, body.get(0).getAsJsonObject().get("count").getAsInt());
  }

  private Response get(String path) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
    connection.setRequestMethod("GET");

    int statusCode = connection.getResponseCode();
    InputStream stream =
        statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
    String response = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    return new Response(statusCode, response);
  }

  private Response post(String path, String body) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    if (!body.isBlank()) {
      try (OutputStream os = connection.getOutputStream()) {
        os.write(body.getBytes(StandardCharsets.UTF_8));
      }
    }

    int statusCode = connection.getResponseCode();
    InputStream stream =
        statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
    String response = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    return new Response(statusCode, response);
  }

  private void createSchema(Connection connection) throws Exception {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("PRAGMA foreign_keys = ON");
      stmt.execute(
          """
CREATE TABLE dreams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    dream_date TEXT,
    timestamp INTEGER,
    symbol_tags TEXT,
    dream_type TEXT,
    analyzed INTEGER DEFAULT 0,
    analysis_result TEXT,
    analyzed_at INTEGER,
    analysis_version TEXT,
    analysis_status TEXT NOT NULL DEFAULT 'PENDING'
);
""");
      stmt.execute(
          """
CREATE TABLE dream_tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    normalized_name TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL
);
""");
      stmt.execute(
          """
CREATE TABLE dream_tag_links (
    dream_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    source TEXT NOT NULL,
    confidence_score REAL,
    created_at INTEGER NOT NULL,
    PRIMARY KEY (dream_id, tag_id, source),
    FOREIGN KEY (dream_id) REFERENCES dreams(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES dream_tags(id) ON DELETE CASCADE
);
""");
    }
  }

  private record Response(int statusCode, String body) {}

  private static class FakeAnalysisClient extends DreamAnalysisClient {
    private IOException nextFailure;

    @Override
    public String analyzeDream(String dream) throws IOException {
      if (nextFailure != null) {
        throw nextFailure;
      }
      return "{\"summary\":\"ok\"}";
    }
  }
}

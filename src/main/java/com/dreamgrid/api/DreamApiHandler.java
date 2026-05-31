package com.dreamgrid.api;

import com.dreamgrid.dto.AnalysisResponse;
import com.dreamgrid.dto.DreamRequest;
import com.dreamgrid.dto.DreamResponse;
import com.dreamgrid.dto.ErrorResponse;
import com.dreamgrid.dto.QuestionRequest;
import com.dreamgrid.dto.QuestionResponse;
import com.dreamgrid.dto.TagResponse;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamType;
import com.dreamgrid.service.DreamService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DreamApiHandler implements HttpHandler {
  private final DreamService dreamService;
  private final Gson gson = new Gson();
  private static final Logger logger = Logger.getLogger(DreamApiHandler.class.getName());

  public DreamApiHandler(DreamService dreamService) {
    this.dreamService = dreamService;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    String method = exchange.getRequestMethod();

    try {
      if ("/health".equals(path) && "GET".equals(method)) {
        handleHealth(exchange);
      } else if ("/tags".equals(path) && "GET".equals(method)) {
        handleTags(exchange);
      } else if ("/dreams/search".equals(path) && "GET".equals(method)) {
        handleSearchDreams(exchange);
      } else if (path.matches("/dreams/tags/[^/]+$") && "GET".equals(method)) {
        handleDreamsByTag(exchange);
      } else if ("/dreams".equals(path) && "GET".equals(method)) {
        handleListDreams(exchange);
      } else if ("/dreams".equals(path) && "POST".equals(method)) {
        handleCreateDream(exchange);
      } else if (path.matches("/dreams/\\d+$") && "GET".equals(method)) {
        handleGetDream(exchange);
      } else if (path.matches("/dreams/\\d+/analyze$") && "POST".equals(method)) {
        handleAnalyzeDream(exchange, false);
      } else if (path.matches("/dreams/\\d+/reanalyze$") && "POST".equals(method)) {
        handleAnalyzeDream(exchange, true);
      } else if (path.matches("/dreams/\\d+/questions$") && "POST".equals(method)) {
        handleQuestion(exchange);
      } else {
        sendError(exchange, 404, "Endpoint not found");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error handling request", e);
      sendError(exchange, 500, "Internal server error");
    }
  }

  private void handleHealth(HttpExchange exchange) throws IOException {
    String response = "{\"status\":\"ok\"}";
    sendJsonResponse(exchange, 200, response);
  }

  private void handleListDreams(HttpExchange exchange) throws IOException {
    try {
      Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
      List<DreamEntry> dreams =
          dreamService.filterDreams(
              queryParams.get("type"), queryParams.get("status"), queryParams.get("tag"));
      sendDreamList(exchange, dreams);
    } catch (IllegalArgumentException e) {
      sendError(exchange, 400, e.getMessage());
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error listing dreams", e);
      sendError(exchange, 500, "Database error: " + e.getMessage());
    }
  }

  private void handleSearchDreams(HttpExchange exchange) throws IOException {
    try {
      Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
      List<DreamEntry> dreams = dreamService.searchDreams(queryParams.get("query"));
      sendDreamList(exchange, dreams);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error searching dreams", e);
      sendError(exchange, 500, "Database error: " + e.getMessage());
    }
  }

  private void handleDreamsByTag(HttpExchange exchange) throws IOException {
    try {
      String tag = extractLastPathSegment(exchange.getRequestURI().getPath());
      List<DreamEntry> dreams = dreamService.getDreamsByTag(tag);
      sendDreamList(exchange, dreams);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error listing dreams by tag", e);
      sendError(exchange, 500, "Database error: " + e.getMessage());
    }
  }

  private void handleTags(HttpExchange exchange) throws IOException {
    try {
      List<TagResponse> responses =
          dreamService.getTagUsageCounts().entrySet().stream()
              .map(entry -> new TagResponse(entry.getKey().name().toLowerCase(), entry.getValue()))
              .collect(Collectors.toList());
      sendJsonResponse(exchange, 200, gson.toJson(responses));
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error listing tags", e);
      sendError(exchange, 500, "Database error: " + e.getMessage());
    }
  }

  private void handleCreateDream(HttpExchange exchange) throws IOException {
    try {
      String body = readRequestBody(exchange);
      DreamRequest request = gson.fromJson(body, DreamRequest.class);

      // Validation
      if (request.getTitle() == null || request.getTitle().isBlank()) {
        sendError(exchange, 400, "Title cannot be empty");
        return;
      }

      if (request.getContent() == null || request.getContent().isBlank()) {
        sendError(exchange, 400, "Content cannot be empty");
        return;
      }

      DreamType dreamType = DreamType.NONE;
      if (request.getType() != null && !request.getType().isBlank()) {
        try {
          dreamType = DreamType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
          sendError(exchange, 400, "Invalid dream type: " + request.getType());
          return;
        }
      }

      DreamEntry createdDream =
          dreamService.saveDream(
              request.getTitle(),
              request.getContent(),
              request.getDate(),
              dreamType,
              request.getTags());

      DreamResponse response = toDreamResponse(createdDream);

      String jsonResponse = gson.toJson(response);
      sendJsonResponse(exchange, 201, jsonResponse);
    } catch (JsonSyntaxException e) {
      sendError(exchange, 400, "Invalid JSON request body");
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error creating dream", e);
      sendError(exchange, 500, "Database error: " + e.getMessage());
    }
  }

  private void handleGetDream(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      DreamEntry dream = dreamService.getDreamById(dreamId);

      if (dream == null) {
        sendError(exchange, 404, "Dream not found");
        return;
      }

      DreamResponse response = toDreamResponse(dream);

      String jsonResponse = gson.toJson(response);
      sendJsonResponse(exchange, 200, jsonResponse);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error getting dream", e);
      sendError(exchange, 500, "Database error: " + e.getMessage());
    }
  }

  private void handleAnalyzeDream(HttpExchange exchange, boolean forceReanalysis)
      throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      DreamEntry dream = dreamService.getDreamById(dreamId);

      if (dream == null) {
        sendError(exchange, 404, "Dream not found");
        return;
      }

      String analysis =
          forceReanalysis
              ? dreamService.reanalyzeDream(dreamId)
              : dreamService.analyzeDream(dreamId);
      DreamEntry analyzedDream = dreamService.getDreamById(dreamId);
      AnalysisResponse response =
          new AnalysisResponse(
              dreamId,
              analysis,
              analyzedDream.getAnalyzedAt(),
              analyzedDream.getAnalysisVersion(),
              analyzedDream.getAnalysisStatus());

      String jsonResponse = gson.toJson(response);
      sendJsonResponse(exchange, 200, jsonResponse);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error analyzing dream", e);
      sendError(exchange, 500, "Database error: " + e.getMessage());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "API error analyzing dream", e);
      sendError(exchange, 502, "Analysis API error: " + e.getMessage());
    }
  }

  private void handleQuestion(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      String body = readRequestBody(exchange);
      QuestionRequest request = gson.fromJson(body, QuestionRequest.class);

      if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
        sendError(exchange, 400, "Question cannot be empty");
        return;
      }

      String answer = dreamService.askQuestionAboutDream(dreamId, request.getQuestion());
      QuestionResponse response = new QuestionResponse(dreamId, request.getQuestion(), answer);
      sendJsonResponse(exchange, 200, gson.toJson(response));
    } catch (JsonSyntaxException e) {
      sendError(exchange, 400, "Invalid JSON request body");
    } catch (IllegalArgumentException e) {
      sendError(exchange, 404, e.getMessage());
    } catch (IllegalStateException e) {
      sendError(exchange, 409, e.getMessage());
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error answering dream question", e);
      sendError(exchange, 500, "Database error: " + e.getMessage());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "API error answering dream question", e);
      sendError(exchange, 502, "Analysis API error: " + e.getMessage());
    }
  }

  private int extractDreamId(String path) {
    String[] parts = path.split("/");
    return Integer.parseInt(parts[2]);
  }

  private String extractLastPathSegment(String path) {
    String[] parts = path.split("/");
    return urlDecode(parts[parts.length - 1]);
  }

  private DreamResponse toDreamResponse(DreamEntry dream) {
    return new DreamResponse(
        dream.getId(),
        dream.getTitle(),
        dream.getContent(),
        dream.getDreamDate(),
        dream.getTimestamp(),
        dream.getSymbolTags(),
        dream.getDreamType(),
        dream.isAnalyzed(),
        dream.getAnalysisResult(),
        dream.getAnalyzedAt(),
        dream.getAnalysisVersion(),
        dream.getAnalysisStatus());
  }

  private String readRequestBody(HttpExchange exchange) throws IOException {
    InputStream is = exchange.getRequestBody();
    byte[] bytes = is.readAllBytes();
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private void sendDreamList(HttpExchange exchange, List<DreamEntry> dreams) throws IOException {
    List<DreamResponse> responses =
        dreams.stream().map(dream -> toDreamResponse(dream)).collect(Collectors.toList());
    sendJsonResponse(exchange, 200, gson.toJson(responses));
  }

  private Map<String, String> parseQueryParams(String rawQuery) {
    Map<String, String> params = new HashMap<>();
    if (rawQuery == null || rawQuery.isBlank()) {
      return params;
    }

    for (String pair : rawQuery.split("&")) {
      if (pair.isBlank()) {
        continue;
      }

      String[] parts = pair.split("=", 2);
      String key = urlDecode(parts[0]);
      String value = parts.length > 1 ? urlDecode(parts[1]) : "";
      if (!key.isBlank()) {
        params.put(key, value);
      }
    }
    return params;
  }

  private String urlDecode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse)
      throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(statusCode, jsonResponse.getBytes(StandardCharsets.UTF_8).length);

    try (OutputStream os = exchange.getResponseBody()) {
      os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
    }
  }

  private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
    ErrorResponse error = new ErrorResponse(statusCode, message);
    String jsonResponse = gson.toJson(error);
    sendJsonResponse(exchange, statusCode, jsonResponse);
  }
}

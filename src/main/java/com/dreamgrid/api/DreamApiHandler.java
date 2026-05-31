package com.dreamgrid.api;

import com.dreamgrid.dto.AnalysisResponse;
import com.dreamgrid.dto.AnalysisRunResponse;
import com.dreamgrid.dto.DreamClassificationResponse;
import com.dreamgrid.dto.DreamQuestionResponse;
import com.dreamgrid.dto.DreamRequest;
import com.dreamgrid.dto.DreamResponse;
import com.dreamgrid.dto.ErrorResponse;
import com.dreamgrid.dto.QuestionRequest;
import com.dreamgrid.dto.TagResponse;
import com.dreamgrid.dto.UpdateDreamClassificationRequest;
import com.dreamgrid.dto.UpdateDreamRequest;
import com.dreamgrid.model.AnalysisRun;
import com.dreamgrid.model.DreamClassification;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamQuestion;
import com.dreamgrid.service.DreamGridException;
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
      } else if (path.matches("/dreams/\\d+$") && "PUT".equals(method)) {
        handleUpdateDream(exchange);
      } else if (path.matches("/dreams/\\d+$") && "DELETE".equals(method)) {
        handleDeleteDream(exchange);
      } else if (path.matches("/dreams/\\d+/classification$") && "GET".equals(method)) {
        handleGetClassification(exchange);
      } else if (path.matches("/dreams/\\d+/classification$") && "PUT".equals(method)) {
        handleUpdateClassification(exchange);
      } else if (path.matches("/dreams/\\d+/classification/override$") && "DELETE".equals(method)) {
        handleClearClassificationOverride(exchange);
      } else if (path.matches("/dreams/\\d+/analyses/latest$") && "GET".equals(method)) {
        handleLatestAnalysisRun(exchange);
      } else if (path.matches("/dreams/\\d+/analyses$") && "GET".equals(method)) {
        handleAnalysisHistory(exchange);
      } else if (path.matches("/dreams/\\d+$") && "GET".equals(method)) {
        handleGetDream(exchange);
      } else if (path.matches("/dreams/\\d+/analyze$") && "POST".equals(method)) {
        handleAnalyzeDream(exchange, false);
      } else if (path.matches("/dreams/\\d+/reanalyze$") && "POST".equals(method)) {
        handleAnalyzeDream(exchange, true);
      } else if (path.matches("/dreams/\\d+/questions$") && "POST".equals(method)) {
        handleQuestion(exchange);
      } else if (path.matches("/dreams/\\d+/questions$") && "GET".equals(method)) {
        handleQuestionHistory(exchange);
      } else if (path.matches("/dreams/\\d+/questions/\\d+$") && "GET".equals(method)) {
        handleQuestionById(exchange);
      } else {
        sendError(exchange, 404, ApiErrorCode.NOT_FOUND, "Endpoint not found");
      }
    } catch (DreamGridException e) {
      sendError(exchange, statusFor(e.getErrorCode()), e.getErrorCode(), e.getMessage());
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error handling request", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Internal server error");
    }
  }

  private void handleHealth(HttpExchange exchange) throws IOException {
    String response = "{\"status\":\"ok\"}";
    sendJsonResponse(exchange, 200, response);
  }

  private void handleListDreams(HttpExchange exchange) throws IOException {
    try {
      Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
      String classification =
          queryParams.containsKey("classification")
              ? queryParams.get("classification")
              : queryParams.get("type");
      List<DreamEntry> dreams =
          dreamService.filterDreams(
              classification, queryParams.get("status"), queryParams.get("tag"));
      sendDreamList(exchange, dreams);
    } catch (IllegalArgumentException e) {
      sendError(exchange, 400, ApiErrorCode.VALIDATION_ERROR, e.getMessage());
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error listing dreams", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleSearchDreams(HttpExchange exchange) throws IOException {
    try {
      Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
      List<DreamEntry> dreams = dreamService.searchDreams(queryParams.get("query"));
      sendDreamList(exchange, dreams);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error searching dreams", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleDreamsByTag(HttpExchange exchange) throws IOException {
    try {
      String tag = extractLastPathSegment(exchange.getRequestURI().getPath());
      List<DreamEntry> dreams = dreamService.getDreamsByTag(tag);
      sendDreamList(exchange, dreams);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error listing dreams by tag", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleTags(HttpExchange exchange) throws IOException {
    try {
      List<TagResponse> responses =
          dreamService.getTagUsageCounts().stream()
              .map(
                  usage ->
                      new TagResponse(usage.getName(), usage.getNormalizedName(), usage.getCount()))
              .collect(Collectors.toList());
      sendJsonResponse(exchange, 200, gson.toJson(responses));
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error listing tags", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleCreateDream(HttpExchange exchange) throws IOException {
    try {
      String body = readRequestBody(exchange);
      DreamRequest request = gson.fromJson(body, DreamRequest.class);
      if (request == null) {
        sendError(exchange, 400, ApiErrorCode.VALIDATION_ERROR, "Invalid JSON request body");
        return;
      }

      DreamClassification classification = null;
      String requestedClassification =
          request.getClassification() != null ? request.getClassification() : request.getType();
      if (requestedClassification != null && !requestedClassification.isBlank()) {
        try {
          classification = DreamClassification.valueOf(requestedClassification.toUpperCase());
        } catch (IllegalArgumentException e) {
          sendError(
              exchange,
              400,
              ApiErrorCode.VALIDATION_ERROR,
              "Invalid classification: " + requestedClassification);
          return;
        }
      }

      DreamEntry createdDream =
          dreamService.saveDream(
              request.getTitle(),
              request.getContent(),
              request.getDate(),
              classification,
              request.getTags());

      DreamResponse response = toDreamResponse(createdDream);

      String jsonResponse = gson.toJson(response);
      sendJsonResponse(exchange, 201, jsonResponse);
    } catch (JsonSyntaxException e) {
      sendError(exchange, 400, ApiErrorCode.VALIDATION_ERROR, "Invalid JSON request body");
    } catch (DreamGridException e) {
      sendError(exchange, statusFor(e.getErrorCode()), e.getErrorCode(), e.getMessage());
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error creating dream", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleGetDream(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      DreamEntry dream = dreamService.getDreamById(dreamId);

      if (dream == null) {
        sendError(exchange, 404, ApiErrorCode.NOT_FOUND, "Dream not found");
        return;
      }

      DreamResponse response = toDreamResponse(dream);

      String jsonResponse = gson.toJson(response);
      sendJsonResponse(exchange, 200, jsonResponse);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error getting dream", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleUpdateDream(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      UpdateDreamRequest request =
          gson.fromJson(readRequestBody(exchange), UpdateDreamRequest.class);
      if (request == null) {
        sendError(exchange, 400, ApiErrorCode.VALIDATION_ERROR, "Invalid JSON request body");
        return;
      }

      DreamEntry updated =
          dreamService.updateDream(
              dreamId,
              request.getTitle(),
              request.getContent(),
              request.getDate(),
              request.getType());
      sendJsonResponse(exchange, 200, gson.toJson(toDreamResponse(updated)));
    } catch (JsonSyntaxException e) {
      sendError(exchange, 400, ApiErrorCode.VALIDATION_ERROR, "Invalid JSON request body");
    } catch (DreamGridException e) {
      sendError(exchange, statusFor(e.getErrorCode()), e.getErrorCode(), e.getMessage());
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error updating dream", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleDeleteDream(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      dreamService.deleteDream(dreamId);
      sendJsonResponse(exchange, 204, "");
    } catch (DreamGridException e) {
      sendError(exchange, statusFor(e.getErrorCode()), e.getErrorCode(), e.getMessage());
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error deleting dream", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleGetClassification(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      DreamEntry dream = dreamService.getDreamClassification(dreamId);
      sendJsonResponse(exchange, 200, gson.toJson(toClassificationResponse(dream)));
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error getting dream classification", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleUpdateClassification(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      UpdateDreamClassificationRequest request =
          gson.fromJson(readRequestBody(exchange), UpdateDreamClassificationRequest.class);
      DreamEntry dream =
          dreamService.updateDreamClassification(
              dreamId, request != null ? request.getClassification() : null);
      sendJsonResponse(exchange, 200, gson.toJson(toClassificationResponse(dream)));
    } catch (JsonSyntaxException e) {
      sendError(exchange, 400, ApiErrorCode.VALIDATION_ERROR, "Invalid JSON request body");
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error updating dream classification", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleClearClassificationOverride(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      DreamEntry dream = dreamService.clearDreamClassificationOverride(dreamId);
      sendJsonResponse(exchange, 200, gson.toJson(toClassificationResponse(dream)));
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error clearing dream classification", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleAnalysisHistory(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      List<AnalysisRunResponse> responses =
          dreamService.getAnalysisHistory(dreamId).stream()
              .map(this::toAnalysisRunResponse)
              .collect(Collectors.toList());
      sendJsonResponse(exchange, 200, gson.toJson(responses));
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error getting analysis history", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleLatestAnalysisRun(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      sendJsonResponse(
          exchange,
          200,
          gson.toJson(toAnalysisRunResponse(dreamService.getLatestAnalysisRun(dreamId))));
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error getting latest analysis run", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleAnalyzeDream(HttpExchange exchange, boolean forceReanalysis)
      throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      DreamEntry dream = dreamService.getDreamById(dreamId);

      if (dream == null) {
        sendError(exchange, 404, ApiErrorCode.NOT_FOUND, "Dream not found");
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
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "API error analyzing dream", e);
      sendError(
          exchange,
          502,
          ApiErrorCode.ANALYSIS_SERVICE_ERROR,
          "Analysis API error: " + e.getMessage());
    }
  }

  private void handleQuestion(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      String body = readRequestBody(exchange);
      QuestionRequest request = gson.fromJson(body, QuestionRequest.class);

      String question = request != null ? request.getQuestion() : null;
      String answer = dreamService.askQuestionAboutDream(dreamId, question);
      DreamQuestion latest =
          dreamService.getQuestionHistory(dreamId).stream().findFirst().orElse(null);
      if (latest == null) {
        sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Failed to persist question");
        return;
      }
      sendJsonResponse(exchange, 200, gson.toJson(toQuestionResponse(latest)));
    } catch (JsonSyntaxException e) {
      sendError(exchange, 400, ApiErrorCode.VALIDATION_ERROR, "Invalid JSON request body");
    } catch (DreamGridException e) {
      sendError(exchange, statusFor(e.getErrorCode()), e.getErrorCode(), e.getMessage());
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error answering dream question", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "API error answering dream question", e);
      sendError(
          exchange,
          502,
          ApiErrorCode.ANALYSIS_SERVICE_ERROR,
          "Analysis API error: " + e.getMessage());
    }
  }

  private void handleQuestionHistory(HttpExchange exchange) throws IOException {
    try {
      int dreamId = extractDreamId(exchange.getRequestURI().getPath());
      List<DreamQuestionResponse> responses =
          dreamService.getQuestionHistory(dreamId).stream()
              .map(this::toQuestionResponse)
              .collect(Collectors.toList());
      sendJsonResponse(exchange, 200, gson.toJson(responses));
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error getting question history", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
    }
  }

  private void handleQuestionById(HttpExchange exchange) throws IOException {
    try {
      String[] parts = exchange.getRequestURI().getPath().split("/");
      int dreamId = Integer.parseInt(parts[2]);
      int questionId = Integer.parseInt(parts[4]);
      DreamQuestion question = dreamService.getQuestionById(dreamId, questionId);
      sendJsonResponse(exchange, 200, gson.toJson(toQuestionResponse(question)));
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database error getting question by id", e);
      sendError(exchange, 500, ApiErrorCode.INTERNAL_ERROR, "Database error: " + e.getMessage());
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
        dream.isAnalyzed(),
        dream.getAnalysisResult(),
        dream.getAnalyzedAt(),
        dream.getAnalysisVersion(),
        dream.getAnalysisStatus(),
        dream.getUserClassification(),
        dream.getInferredClassification(),
        dream.getEffectiveClassification(),
        dream.getClassificationSource(),
        dream.getClassificationReason(),
        dream.getTypeConfidence(),
        dream.getClassificationUpdatedAt());
  }

  private DreamClassificationResponse toClassificationResponse(DreamEntry dream) {
    return new DreamClassificationResponse(
        dream.getUserClassification(),
        dream.getInferredClassification(),
        dream.getEffectiveClassification(),
        dream.getClassificationSource(),
        dream.getClassificationReason(),
        dream.getTypeConfidence(),
        dream.getClassificationUpdatedAt());
  }

  private AnalysisRunResponse toAnalysisRunResponse(AnalysisRun run) {
    return new AnalysisRunResponse(
        run.getId(),
        run.getDreamId(),
        run.getRequestedAt(),
        run.getCompletedAt(),
        run.getStatus(),
        run.getAnalysisVersion(),
        run.getAnalysisResult(),
        run.getFailureReason());
  }

  private DreamQuestionResponse toQuestionResponse(DreamQuestion question) {
    return new DreamQuestionResponse(
        question.getId(),
        question.getDreamId(),
        question.getAnalysisRunId(),
        question.getQuestion(),
        question.getAnswer(),
        question.getCreatedAt());
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

  private void sendError(
      HttpExchange exchange, int statusCode, ApiErrorCode errorCode, String message)
      throws IOException {
    ErrorResponse error = new ErrorResponse(errorCode, message);
    String jsonResponse = gson.toJson(error);
    sendJsonResponse(exchange, statusCode, jsonResponse);
  }

  private int statusFor(ApiErrorCode errorCode) {
    return switch (errorCode) {
      case VALIDATION_ERROR -> 400;
      case CONTENT_REJECTED -> 400;
      case NOT_FOUND -> 404;
      case ANALYSIS_SERVICE_ERROR -> 502;
      case INTERNAL_ERROR -> 500;
    };
  }
}

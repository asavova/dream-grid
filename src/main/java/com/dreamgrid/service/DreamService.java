package com.dreamgrid.service;

import com.dreamgrid.client.DreamAnalysisClient;
import com.dreamgrid.model.AnalysisStatus;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamSymbol;
import com.dreamgrid.model.DreamType;
import com.dreamgrid.repository.DreamRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DreamService {
  private static final String DEFAULT_ANALYSIS_VERSION = "v1";

  private final DreamRepository dreamRepository;
  private final DreamAnalysisClient analysisClient;
  private final String analysisVersion;
  private final Gson gson = new Gson();

  public DreamService(Connection connection) {
    this.dreamRepository = new DreamRepository(connection);
    this.analysisClient = new DreamAnalysisClient();
    this.analysisVersion = DEFAULT_ANALYSIS_VERSION;
  }

  public DreamService(DreamRepository dreamRepository, DreamAnalysisClient analysisClient) {
    this(dreamRepository, analysisClient, DEFAULT_ANALYSIS_VERSION);
  }

  public DreamService(
      DreamRepository dreamRepository, DreamAnalysisClient analysisClient, String analysisVersion) {
    this.dreamRepository = dreamRepository;
    this.analysisClient = analysisClient;
    this.analysisVersion =
        analysisVersion != null && !analysisVersion.isBlank()
            ? analysisVersion
            : DEFAULT_ANALYSIS_VERSION;
  }

  public void addDream(DreamEntry dream) throws SQLException {
    dreamRepository.insert(dream);
  }

  public DreamEntry saveDream(String title, String content, String dreamDate, DreamType dreamType)
      throws SQLException {
    long timestamp = System.currentTimeMillis();
    String formattedDate =
        dreamDate != null && !dreamDate.isBlank()
            ? dreamDate
            : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    DreamType type = dreamType != null ? dreamType : DreamType.NONE;
    DreamEntry entry =
        new DreamEntry(
            title, content, formattedDate, timestamp, List.of(DreamSymbol.UNKNOWN), type);
    dreamRepository.insert(entry);
    return entry;
  }

  public String analyzeDream(int dreamId) throws IOException, SQLException {
    return analyzeDream(dreamId, false);
  }

  public String reanalyzeDream(int dreamId) throws IOException, SQLException {
    return analyzeDream(dreamId, true);
  }

  private String analyzeDream(int dreamId, boolean forceReanalysis)
      throws IOException, SQLException {
    DreamEntry dream = dreamRepository.findById(dreamId);

    if (dream == null) {
      throw new IllegalArgumentException("Dream with ID " + dreamId + " not found.");
    }

    if (!forceReanalysis && hasValidCachedAnalysis(dream)) {
      return dream.getAnalysisResult();
    }

    try {
      String analysis = analysisClient.analyzeDream(dream.getContent());
      dream.completeAnalysis(analysis, System.currentTimeMillis(), analysisVersion);
      applyDetectedSymbols(dream, analysis);
      dreamRepository.update(dream);
      return analysis;
    } catch (IOException e) {
      dream.failAnalysis();
      dreamRepository.updateAnalysisFields(dream);
      throw e;
    }
  }

  public List<DreamEntry> getAllDreams() throws SQLException {
    return dreamRepository.getAll();
  }

  public DreamEntry getDreamById(int id) throws SQLException {
    return dreamRepository.findById(id);
  }

  public String askQuestionAboutDream(int dreamId, String question)
      throws IOException, SQLException {
    DreamEntry dream = dreamRepository.findById(dreamId);

    if (dream == null) {
      throw new IllegalArgumentException("Dream with ID " + dreamId + " not found.");
    }

    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("Question cannot be empty.");
    }

    if (!hasCompletedAnalysis(dream)) {
      throw new IllegalStateException("Dream must be analyzed before asking questions.");
    }

    return analysisClient.askQuestion(dream.getContent(), dream.getAnalysisResult(), question);
  }

  private boolean hasValidCachedAnalysis(DreamEntry dream) {
    return hasCompletedAnalysis(dream) && analysisVersion.equals(dream.getAnalysisVersion());
  }

  private boolean hasCompletedAnalysis(DreamEntry dream) {
    return dream.getAnalysisStatus() == AnalysisStatus.COMPLETED
        && dream.getAnalysisResult() != null
        && !dream.getAnalysisResult().isBlank();
  }

  private void applyDetectedSymbols(DreamEntry dream, String analysis) {
    List<DreamSymbol> symbols = parseDetectedSymbols(analysis);
    if (!symbols.isEmpty()) {
      dream.setSymbolTags(symbols);
    }
  }

  private List<DreamSymbol> parseDetectedSymbols(String analysis) {
    try {
      JsonObject root = gson.fromJson(analysis, JsonObject.class);
      if (root == null
          || !root.has("detectedSymbols")
          || !root.get("detectedSymbols").isJsonArray()) {
        return List.of();
      }

      JsonArray detectedSymbols = root.getAsJsonArray("detectedSymbols");
      List<DreamSymbol> symbols = new ArrayList<>();
      for (JsonElement element : detectedSymbols) {
        if (!element.isJsonPrimitive()) {
          continue;
        }

        String value = element.getAsString().trim().toUpperCase(Locale.ROOT);
        try {
          symbols.add(DreamSymbol.valueOf(value));
        } catch (IllegalArgumentException ignored) {
          // The Python service can return free-form symbols. Mirror only known enum values.
        }
      }
      return symbols.isEmpty() ? List.of(DreamSymbol.UNKNOWN) : symbols;
    } catch (RuntimeException e) {
      return List.of();
    }
  }
}

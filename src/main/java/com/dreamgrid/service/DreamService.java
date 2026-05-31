package com.dreamgrid.service;

import com.dreamgrid.api.ApiErrorCode;
import com.dreamgrid.client.DreamAnalysisClient;
import com.dreamgrid.config.AppConfig;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class DreamService {
  private final DreamRepository dreamRepository;
  private final DreamAnalysisClient analysisClient;
  private final String expectedAnalysisVersion;
  private final DreamValidator validator;
  private final ContentSafetyService contentSafetyService;
  private final Gson gson = new Gson();

  public DreamService(Connection connection) {
    this.dreamRepository = new DreamRepository(connection);
    AppConfig config = AppConfig.load();
    this.analysisClient = new DreamAnalysisClient(config);
    this.expectedAnalysisVersion = config.getAnalysisModelVersion();
    this.validator = new DreamValidator();
    this.contentSafetyService = new ContentSafetyService();
  }

  public DreamService(DreamRepository dreamRepository, DreamAnalysisClient analysisClient) {
    this(dreamRepository, analysisClient, AppConfig.load().getAnalysisModelVersion());
  }

  public DreamService(
      DreamRepository dreamRepository, DreamAnalysisClient analysisClient, String analysisVersion) {
    this(
        dreamRepository,
        analysisClient,
        analysisVersion,
        new DreamValidator(),
        new ContentSafetyService());
  }

  public DreamService(
      DreamRepository dreamRepository,
      DreamAnalysisClient analysisClient,
      String analysisVersion,
      DreamValidator validator,
      ContentSafetyService contentSafetyService) {
    this.dreamRepository = dreamRepository;
    this.analysisClient = analysisClient;
    this.expectedAnalysisVersion = analysisVersion == null ? "" : analysisVersion.trim();
    this.validator = validator;
    this.contentSafetyService = contentSafetyService;
  }

  public void addDream(DreamEntry dream) throws SQLException {
    dreamRepository.insert(dream);
  }

  public DreamEntry saveDream(String title, String content, String dreamDate, DreamType dreamType)
      throws SQLException {
    return saveDream(title, content, dreamDate, dreamType, null);
  }

  public DreamEntry saveDream(
      String title,
      String content,
      String dreamDate,
      DreamType dreamType,
      List<String> requestedTags)
      throws SQLException {
    validator.validateDream(title, content, dreamDate);
    contentSafetyService.validateDreamContent(content);
    long timestamp = System.currentTimeMillis();
    String formattedDate =
        dreamDate != null && !dreamDate.isBlank()
            ? dreamDate
            : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    DreamType type = dreamType != null ? dreamType : DreamType.NONE;
    List<DreamSymbol> tags = DreamSymbol.normalizeTagNames(requestedTags);
    DreamEntry entry = new DreamEntry(title, content, formattedDate, timestamp, tags, type);
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
      throw new DreamGridException(
          ApiErrorCode.NOT_FOUND, "Dream with ID " + dreamId + " not found.");
    }

    contentSafetyService.validateDreamContent(dream.getContent());

    if (!forceReanalysis && hasValidCachedAnalysis(dream)) {
      return dream.getAnalysisResult();
    }

    try {
      String analysis = analysisClient.analyzeDream(dream.getContent());
      dream.completeAnalysis(
          analysis, System.currentTimeMillis(), resolveAnalysisVersion(analysis));
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

  public List<DreamEntry> searchDreams(String query) throws SQLException {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    return dreamRepository.findByKeyword(query);
  }

  public List<DreamEntry> getDreamsByTag(String tag) throws SQLException {
    Optional<DreamSymbol> symbol = DreamSymbol.fromTag(tag);
    if (symbol.isEmpty()) {
      return List.of();
    }
    return dreamRepository.findByTag(symbol.get());
  }

  public List<DreamEntry> filterDreams(String type, String status, String tag) throws SQLException {
    DreamType dreamType = parseDreamType(type);
    AnalysisStatus analysisStatus = parseAnalysisStatus(status);
    Optional<DreamSymbol> symbol = DreamSymbol.fromTag(tag);

    if (tag != null && !tag.isBlank() && symbol.isEmpty()) {
      return List.of();
    }

    if (dreamType == null && analysisStatus == null && symbol.isEmpty()) {
      return dreamRepository.getAll();
    }

    return dreamRepository.findByFilters(null, dreamType, analysisStatus, symbol.orElse(null));
  }

  public DreamEntry getDreamById(int id) throws SQLException {
    return dreamRepository.findById(id);
  }

  public Map<DreamSymbol, Integer> getTagUsageCounts() throws SQLException {
    Map<DreamSymbol, Integer> counts = new EnumMap<>(dreamRepository.getTagUsageCounts());
    for (DreamSymbol symbol : DreamSymbol.values()) {
      counts.putIfAbsent(symbol, 0);
    }
    return counts;
  }

  public String askQuestionAboutDream(int dreamId, String question)
      throws IOException, SQLException {
    DreamEntry dream = dreamRepository.findById(dreamId);

    if (dream == null) {
      throw new DreamGridException(
          ApiErrorCode.NOT_FOUND, "Dream with ID " + dreamId + " not found.");
    }

    validator.validateQuestion(question);
    contentSafetyService.validateDreamContent(dream.getContent());
    contentSafetyService.validateQuestion(question);

    if (!hasCompletedAnalysis(dream)) {
      throw new DreamGridException(
          ApiErrorCode.VALIDATION_ERROR, "Dream must be analyzed before asking questions.");
    }

    return analysisClient.askQuestion(dream.getContent(), dream.getAnalysisResult(), question);
  }

  private boolean hasValidCachedAnalysis(DreamEntry dream) {
    if (!hasCompletedAnalysis(dream)) {
      return false;
    }

    if (expectedAnalysisVersion.isBlank()) {
      return true;
    }

    return expectedAnalysisVersion.equals(dream.getAnalysisVersion());
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

  private String resolveAnalysisVersion(String analysis) {
    String responseVersion = extractModelVersion(analysis);
    if (responseVersion != null && !responseVersion.isBlank()) {
      return responseVersion;
    }

    return expectedAnalysisVersion.isBlank() ? null : expectedAnalysisVersion;
  }

  private String extractModelVersion(String analysis) {
    try {
      JsonObject root = gson.fromJson(analysis, JsonObject.class);
      if (root == null
          || !root.has("modelVersion")
          || !root.get("modelVersion").isJsonPrimitive()) {
        return null;
      }

      String modelVersion = root.get("modelVersion").getAsString();
      return modelVersion == null ? null : modelVersion.trim();
    } catch (RuntimeException e) {
      return null;
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
        DreamSymbol.fromTag(value).ifPresent(symbols::add);
      }
      return DreamSymbol.normalizeSymbols(symbols);
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  private DreamType parseDreamType(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return DreamType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new DreamGridException(ApiErrorCode.VALIDATION_ERROR, "Invalid dream type: " + value);
    }
  }

  private AnalysisStatus parseAnalysisStatus(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return AnalysisStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new DreamGridException(
          ApiErrorCode.VALIDATION_ERROR, "Invalid analysis status: " + value);
    }
  }
}

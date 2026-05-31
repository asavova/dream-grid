package com.dreamgrid.service;

import com.dreamgrid.api.ApiErrorCode;
import com.dreamgrid.client.DreamAnalysisClient;
import com.dreamgrid.config.AppConfig;
import com.dreamgrid.dto.TagUsage;
import com.dreamgrid.model.AnalysisStatus;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamTag;
import com.dreamgrid.model.DreamType;
import com.dreamgrid.model.TagSource;
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
  private final DreamRepository dreamRepository;
  private final DreamAnalysisClient analysisClient;
  private final String expectedAnalysisVersion;
  private final DreamValidator validator;
  private final ContentSafetyService contentSafetyService;
  private final TagNormalizationService tagNormalizationService;
  private final Gson gson = new Gson();

  public DreamService(Connection connection) {
    this.dreamRepository = new DreamRepository(connection);
    AppConfig config = AppConfig.load();
    this.analysisClient = new DreamAnalysisClient(config);
    this.expectedAnalysisVersion = config.getAnalysisModelVersion();
    this.validator = new DreamValidator();
    this.contentSafetyService = new ContentSafetyService();
    this.tagNormalizationService = new TagNormalizationService();
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
        new ContentSafetyService(),
        new TagNormalizationService());
  }

  public DreamService(
      DreamRepository dreamRepository,
      DreamAnalysisClient analysisClient,
      String analysisVersion,
      DreamValidator validator,
      ContentSafetyService contentSafetyService,
      TagNormalizationService tagNormalizationService) {
    this.dreamRepository = dreamRepository;
    this.analysisClient = analysisClient;
    this.expectedAnalysisVersion = analysisVersion == null ? "" : analysisVersion.trim();
    this.validator = validator;
    this.contentSafetyService = contentSafetyService;
    this.tagNormalizationService = tagNormalizationService;
  }

  public void addDream(DreamEntry dream) throws SQLException {
    dreamRepository.insert(dream);
    for (DreamTag tag : dream.getSymbolTags()) {
      TagSource source = tag.getSource() != null ? tag.getSource() : TagSource.MANUAL;
      dreamRepository.linkTagToDream(dream.getId(), tag, source, tag.getConfidenceScore());
    }
    dream.setSymbolTags(dreamRepository.listTagsForDream(dream.getId()));
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
    List<DreamTag> tags = tagNormalizationService.normalize(requestedTags, TagSource.MANUAL);
    DreamEntry entry = new DreamEntry(title, content, formattedDate, timestamp, tags, type);
    dreamRepository.insert(entry);
    for (DreamTag tag : tags) {
      dreamRepository.linkTagToDream(entry.getId(), tag, TagSource.MANUAL, null);
    }
    entry.setSymbolTags(dreamRepository.listTagsForDream(entry.getId()));
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
      dreamRepository.update(dream);
      dreamRepository.replaceAnalysisTags(dreamId, parseAnalysisTags(analysis));
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
    String normalizedTag = tagNormalizationService.normalizeName(tag);
    if (normalizedTag.isBlank()) {
      return List.of();
    }
    return dreamRepository.findByTag(normalizedTag);
  }

  public List<DreamEntry> filterDreams(String type, String status, String tag) throws SQLException {
    DreamType dreamType = parseDreamType(type);
    AnalysisStatus analysisStatus = parseAnalysisStatus(status);
    String normalizedTag = tagNormalizationService.normalizeName(tag);

    if (dreamType == null && analysisStatus == null && normalizedTag.isBlank()) {
      return dreamRepository.getAll();
    }

    return dreamRepository.findByFilters(null, dreamType, analysisStatus, normalizedTag);
  }

  public DreamEntry getDreamById(int id) throws SQLException {
    return dreamRepository.findById(id);
  }

  public List<TagUsage> getTagUsageCounts() throws SQLException {
    return dreamRepository.getTagUsageCounts();
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

  private List<DreamTag> parseAnalysisTags(String analysis) {
    try {
      JsonObject root = gson.fromJson(analysis, JsonObject.class);
      if (root == null) {
        return List.of();
      }

      List<String> values = new ArrayList<>();
      addJsonArrayValues(root, "detectedSymbols", values);
      addJsonArrayValues(root, "detectedThemes", values);
      List<DreamTag> tags = tagNormalizationService.normalize(values, TagSource.ANALYSIS);
      Double confidenceScore = extractConfidenceScore(root);
      tags.forEach(tag -> tag.setConfidenceScore(confidenceScore));
      return tags;
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  private void addJsonArrayValues(JsonObject root, String fieldName, List<String> values) {
    if (!root.has(fieldName) || !root.get(fieldName).isJsonArray()) {
      return;
    }

    JsonArray array = root.getAsJsonArray(fieldName);
    for (JsonElement element : array) {
      if (element.isJsonPrimitive()) {
        values.add(element.getAsString());
      }
    }
  }

  private Double extractConfidenceScore(JsonObject root) {
    if (!root.has("confidenceScore") || !root.get("confidenceScore").isJsonPrimitive()) {
      return null;
    }

    try {
      return root.get("confidenceScore").getAsDouble();
    } catch (RuntimeException e) {
      return null;
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

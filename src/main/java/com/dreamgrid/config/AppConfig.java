package com.dreamgrid.config;

import java.nio.file.Path;
import java.util.Map;

public class AppConfig {
  private static final String DEFAULT_SERVER_HOST = "0.0.0.0";
  private static final int DEFAULT_SERVER_PORT = 8080;
  private static final String DEFAULT_ANALYSIS_BASE_URL = "http://127.0.0.1:5005";
  private static final String DEFAULT_ANALYSIS_VERSION = "";
  private static final String DEFAULT_DATABASE_PATH = "data/dreams.db";
  private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
  private static final int DEFAULT_READ_TIMEOUT_MS = 30000;
  private static final String DEFAULT_CONTENT_SAFETY_RULES_PATH =
      "python/rules/content_safety_rules.json";
  private static final String DEFAULT_CLASSIFICATION_RULES_PATH =
      "python/rules/classification_rules.json";
  private static final int DEFAULT_RECURRING_MIN_SHARED_TAGS = 2;
  private static final int DEFAULT_RECURRING_MIN_MATCHING_DREAMS = 1;

  private final String serverHost;
  private final int serverPort;
  private final String analysisServiceBaseUrl;
  private final String analysisModelVersion;
  private final String databasePath;
  private final int connectTimeoutMs;
  private final int readTimeoutMs;
  private final String contentSafetyRulesPath;
  private final String classificationRulesPath;
  private final int recurringMinSharedTags;
  private final int recurringMinMatchingDreams;

  public AppConfig(
      String serverHost,
      int serverPort,
      String analysisServiceBaseUrl,
      String analysisModelVersion,
      String databasePath,
      int connectTimeoutMs,
      int readTimeoutMs) {
    this(
        serverHost,
        serverPort,
        analysisServiceBaseUrl,
        analysisModelVersion,
        databasePath,
        connectTimeoutMs,
        readTimeoutMs,
        DEFAULT_CONTENT_SAFETY_RULES_PATH,
        DEFAULT_CLASSIFICATION_RULES_PATH,
        DEFAULT_RECURRING_MIN_SHARED_TAGS,
        DEFAULT_RECURRING_MIN_MATCHING_DREAMS);
  }

  public AppConfig(
      String serverHost,
      int serverPort,
      String analysisServiceBaseUrl,
      String analysisModelVersion,
      String databasePath,
      int connectTimeoutMs,
      int readTimeoutMs,
      String contentSafetyRulesPath,
      String classificationRulesPath,
      int recurringMinSharedTags,
      int recurringMinMatchingDreams) {
    this.serverHost = serverHost;
    this.serverPort = serverPort;
    this.analysisServiceBaseUrl = trimTrailingSlash(analysisServiceBaseUrl);
    this.analysisModelVersion = analysisModelVersion;
    this.databasePath = databasePath;
    this.connectTimeoutMs = connectTimeoutMs;
    this.readTimeoutMs = readTimeoutMs;
    this.contentSafetyRulesPath = contentSafetyRulesPath;
    this.classificationRulesPath = classificationRulesPath;
    this.recurringMinSharedTags = recurringMinSharedTags;
    this.recurringMinMatchingDreams = recurringMinMatchingDreams;
  }

  public static AppConfig load() {
    return from(System.getenv());
  }

  public static AppConfig from(Map<String, String> env) {
    return new AppConfig(
        getString(env, "DREAMGRID_SERVER_HOST", DEFAULT_SERVER_HOST),
        getInt(env, "DREAMGRID_SERVER_PORT", DEFAULT_SERVER_PORT),
        getString(env, "DREAMGRID_ANALYSIS_BASE_URL", DEFAULT_ANALYSIS_BASE_URL),
        getString(env, "DREAMGRID_ANALYSIS_VERSION", DEFAULT_ANALYSIS_VERSION),
        getString(env, "DREAMGRID_DATABASE_PATH", DEFAULT_DATABASE_PATH),
        getInt(env, "DREAMGRID_CONNECT_TIMEOUT_MS", DEFAULT_CONNECT_TIMEOUT_MS),
        getInt(env, "DREAMGRID_READ_TIMEOUT_MS", DEFAULT_READ_TIMEOUT_MS),
        getString(env, "DREAMGRID_CONTENT_SAFETY_RULES_PATH", DEFAULT_CONTENT_SAFETY_RULES_PATH),
        getString(env, "DREAMGRID_CLASSIFICATION_RULES_PATH", DEFAULT_CLASSIFICATION_RULES_PATH),
        getInt(env, "DREAMGRID_RECURRING_MIN_SHARED_TAGS", DEFAULT_RECURRING_MIN_SHARED_TAGS),
        getInt(
            env, "DREAMGRID_RECURRING_MIN_MATCHING_DREAMS", DEFAULT_RECURRING_MIN_MATCHING_DREAMS));
  }

  public String getServerHost() {
    return serverHost;
  }

  public int getServerPort() {
    return serverPort;
  }

  public String getAnalysisServiceBaseUrl() {
    return analysisServiceBaseUrl;
  }

  public String getAnalysisModelVersion() {
    return analysisModelVersion;
  }

  public boolean hasExpectedAnalysisVersion() {
    return analysisModelVersion != null && !analysisModelVersion.isBlank();
  }

  public String getDatabasePath() {
    return databasePath;
  }

  public String getDatabaseUrl() {
    return "jdbc:sqlite:" + databasePath;
  }

  public int getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  public int getReadTimeoutMs() {
    return readTimeoutMs;
  }

  public Path getContentSafetyRulesPath() {
    return Path.of(contentSafetyRulesPath);
  }

  public Path getClassificationRulesPath() {
    return Path.of(classificationRulesPath);
  }

  public int getRecurringMinSharedTags() {
    return recurringMinSharedTags;
  }

  public int getRecurringMinMatchingDreams() {
    return recurringMinMatchingDreams;
  }

  private static String getString(Map<String, String> env, String key, String defaultValue) {
    String value = env.get(key);
    return value == null || value.isBlank() ? defaultValue : value.trim();
  }

  private static int getInt(Map<String, String> env, String key, int defaultValue) {
    String value = env.get(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }

    try {
      int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : defaultValue;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static String trimTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      return DEFAULT_ANALYSIS_BASE_URL;
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}

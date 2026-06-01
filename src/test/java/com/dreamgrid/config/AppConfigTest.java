package com.dreamgrid.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;
import org.junit.Test;

public class AppConfigTest {
  @Test
  public void defaultsAreLoadedWhenEnvironmentIsEmpty() {
    AppConfig config = AppConfig.from(Map.of());

    assertEquals("0.0.0.0", config.getServerHost());
    assertEquals(8080, config.getServerPort());
    assertEquals("http://127.0.0.1:5005", config.getAnalysisServiceBaseUrl());
    assertEquals("", config.getAnalysisModelVersion());
    assertFalse(config.hasExpectedAnalysisVersion());
    assertEquals("data/dreams.db", config.getDatabasePath());
    assertEquals(3000, config.getConnectTimeoutMs());
    assertEquals(30000, config.getReadTimeoutMs());
    assertEquals(
        "python/rules/content_safety_rules.json",
        config.getContentSafetyRulesPath().toString().replace("\\", "/"));
    assertEquals(
        "python/rules/classification_rules.json",
        config.getClassificationRulesPath().toString().replace("\\", "/"));
    assertEquals(2, config.getRecurringMinSharedTags());
    assertEquals(1, config.getRecurringMinMatchingDreams());
  }

  @Test
  public void environmentOverridesDefaults() {
    AppConfig config =
        AppConfig.from(
            Map.ofEntries(
                Map.entry("DREAMGRID_SERVER_HOST", "127.0.0.1"),
                Map.entry("DREAMGRID_SERVER_PORT", "9090"),
                Map.entry("DREAMGRID_ANALYSIS_BASE_URL", "http://localhost:6000/"),
                Map.entry("DREAMGRID_ANALYSIS_VERSION", "model-v2"),
                Map.entry("DREAMGRID_DATABASE_PATH", "tmp/test.db"),
                Map.entry("DREAMGRID_CONNECT_TIMEOUT_MS", "100"),
                Map.entry("DREAMGRID_READ_TIMEOUT_MS", "200"),
                Map.entry("DREAMGRID_CONTENT_SAFETY_RULES_PATH", "cfg/safety.json"),
                Map.entry("DREAMGRID_CLASSIFICATION_RULES_PATH", "cfg/classification.json"),
                Map.entry("DREAMGRID_RECURRING_MIN_SHARED_TAGS", "3"),
                Map.entry("DREAMGRID_RECURRING_MIN_MATCHING_DREAMS", "2")));

    assertEquals("127.0.0.1", config.getServerHost());
    assertEquals(9090, config.getServerPort());
    assertEquals("http://localhost:6000", config.getAnalysisServiceBaseUrl());
    assertEquals("model-v2", config.getAnalysisModelVersion());
    assertEquals("tmp/test.db", config.getDatabasePath());
    assertEquals(100, config.getConnectTimeoutMs());
    assertEquals(200, config.getReadTimeoutMs());
    assertEquals(
        "cfg/safety.json", config.getContentSafetyRulesPath().toString().replace("\\", "/"));
    assertEquals(
        "cfg/classification.json",
        config.getClassificationRulesPath().toString().replace("\\", "/"));
    assertEquals(3, config.getRecurringMinSharedTags());
    assertEquals(2, config.getRecurringMinMatchingDreams());
  }
}

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
  }

  @Test
  public void environmentOverridesDefaults() {
    AppConfig config =
        AppConfig.from(
            Map.of(
                "DREAMGRID_SERVER_HOST", "127.0.0.1",
                "DREAMGRID_SERVER_PORT", "9090",
                "DREAMGRID_ANALYSIS_BASE_URL", "http://localhost:6000/",
                "DREAMGRID_ANALYSIS_VERSION", "model-v2",
                "DREAMGRID_DATABASE_PATH", "tmp/test.db",
                "DREAMGRID_CONNECT_TIMEOUT_MS", "100",
                "DREAMGRID_READ_TIMEOUT_MS", "200"));

    assertEquals("127.0.0.1", config.getServerHost());
    assertEquals(9090, config.getServerPort());
    assertEquals("http://localhost:6000", config.getAnalysisServiceBaseUrl());
    assertEquals("model-v2", config.getAnalysisModelVersion());
    assertEquals("tmp/test.db", config.getDatabasePath());
    assertEquals(100, config.getConnectTimeoutMs());
    assertEquals(200, config.getReadTimeoutMs());
  }
}

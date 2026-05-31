package com.dreamgrid.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dreamgrid.config.AppConfig;
import com.dreamgrid.model.DreamTag;
import com.dreamgrid.model.TagSource;
import com.dreamgrid.repository.DreamRepository;
import com.dreamgrid.testsupport.TestSchema;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.After;
import org.junit.Test;

public class DreamDatabaseTest {
  private Path tempDirectory;

  @After
  public void tearDown() throws Exception {
    DreamDatabase.close();
    if (tempDirectory != null && Files.exists(tempDirectory)) {
      try (var paths = Files.walk(tempDirectory)) {
        paths
            .sorted((left, right) -> right.compareTo(left))
            .forEach(
                path -> {
                  try {
                    Files.deleteIfExists(path);
                  } catch (Exception ignored) {
                    // Best-effort cleanup for temporary test files.
                  }
                });
      }
    }
  }

  @Test
  public void initializationMigratesLegacySymbolTagsIntoDynamicTagTables() throws Exception {
    tempDirectory = Files.createTempDirectory("dreamgrid-db-test");
    Path databasePath = tempDirectory.resolve("legacy.db");
    createLegacyDatabase(databasePath);

    DreamDatabase.initialize(
        new AppConfig(
            "127.0.0.1", 8080, "http://127.0.0.1:5005", "", databasePath.toString(), 3000, 30000));

    DreamRepository repository = new DreamRepository(DreamDatabase.getConnection());
    List<DreamTag> migratedTags = repository.listTagsForDream(1);

    assertEquals(List.of("clear sky", "fire", "moon"), normalizedNames(migratedTags));
    assertTrue(migratedTags.stream().allMatch(tag -> tag.getSource() == TagSource.MANUAL));
    assertEquals(1, repository.findByTag("clear sky").size());
    assertEquals(1, tagCount(repository, "fire"));
  }

  private void createLegacyDatabase(Path databasePath) throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        Statement stmt = connection.createStatement()) {
      TestSchema.createLegacyDreamsOnlySchema(connection);
      stmt.executeUpdate(
          """
INSERT INTO dreams (title, content, dream_date, timestamp, symbol_tags, dream_type, analyzed)
VALUES ('Legacy Dream', 'A legacy dream.', '2026-05-31', 123, ' FIRE,fire, clear-sky, ,Moon!', 'ORDINARY', 0)
""");
    }
  }

  private List<String> normalizedNames(List<DreamTag> tags) {
    return tags.stream().map(DreamTag::getNormalizedName).toList();
  }

  private int tagCount(DreamRepository repository, String normalizedName) throws Exception {
    return repository.getTagUsageCounts().stream()
        .filter(usage -> normalizedName.equals(usage.getNormalizedName()))
        .map(usage -> usage.getCount())
        .findFirst()
        .orElse(0);
  }
}

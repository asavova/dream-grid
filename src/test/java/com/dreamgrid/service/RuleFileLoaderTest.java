package com.dreamgrid.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dreamgrid.api.ApiErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class RuleFileLoaderTest {

  @Test
  public void loadRejectsMissingRequiredRuleSection() throws Exception {
    Path path = writeRuleFile("{\"categories\":[]}");

    DreamGridException exception =
        assertThrows(
            DreamGridException.class,
            () -> new RuleFileLoader().load(path, "categories", "classifications"));

    assertEquals(ApiErrorCode.INTERNAL_ERROR, exception.getErrorCode());
    assertTrue(exception.getMessage().contains("missing required field: classifications"));
  }

  @Test
  public void loadRejectsRequiredRuleSectionWhenItIsNotArray() throws Exception {
    Path path = writeRuleFile("{\"categories\":{\"id\":\"unsafe\"}}");

    DreamGridException exception =
        assertThrows(DreamGridException.class, () -> new RuleFileLoader().load(path, "categories"));

    assertEquals(ApiErrorCode.INTERNAL_ERROR, exception.getErrorCode());
    assertTrue(exception.getMessage().contains("field must be an array: categories"));
  }

  @Test
  public void loadAcceptsRequiredArraySections() throws Exception {
    Path path = writeRuleFile("{\"categories\":[]}");

    assertTrue(new RuleFileLoader().load(path, "categories").get("categories").isJsonArray());
  }

  private Path writeRuleFile(String json) throws Exception {
    Path path = Files.createTempFile("dreamgrid-rules", ".json");
    path.toFile().deleteOnExit();
    Files.writeString(path, json);
    return path;
  }
}

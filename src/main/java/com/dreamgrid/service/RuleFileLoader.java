package com.dreamgrid.service;

import com.dreamgrid.api.ApiErrorCode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class RuleFileLoader {
  private final Gson gson = new Gson();

  JsonObject load(Path path, String... requiredFields) {
    try {
      JsonObject root = gson.fromJson(Files.readString(path), JsonObject.class);
      if (root == null) {
        throw invalid("Rule file must contain a JSON object: " + path);
      }
      for (String field : requiredFields) {
        if (!root.has(field)) {
          throw invalid("Rule file " + path + " is missing required field: " + field);
        }
      }
      return root;
    } catch (IOException e) {
      throw invalid("Rule file not found or unreadable: " + path);
    } catch (JsonSyntaxException e) {
      throw invalid("Rule file is malformed JSON: " + path);
    }
  }

  private DreamGridException invalid(String message) {
    return new DreamGridException(ApiErrorCode.INTERNAL_ERROR, message);
  }
}

package com.grupozap.search.api.itest.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class JsonFileUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public void readConfigurationFile(String filePath) {
    try {
      OBJECT_MAPPER.readValue(getBoostrapConfig(filePath), new TypeReference<>() {});
    } catch (IOException e) {
      throw new RuntimeException("Cannot read es boostrap file config", e);
    }
  }

  public String getBoostrapConfig(String filePath) throws IOException {
    try (final var stream = getClass().getResourceAsStream(filePath)) {
      return new String(stream.readAllBytes(), UTF_8);
    }
  }
}

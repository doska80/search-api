package com.grupozap.search.api.itest.utils;

import static java.util.stream.Collectors.joining;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
    try (var buffer =
        new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filePath)))) {
      return buffer.lines().collect(joining("\n"));
    }
  }
}

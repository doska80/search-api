package com.grupozap.search.api.utils;

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;

public class MapperUtils {

  private static final Logger LOG = getLogger(MapperUtils.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static <T> T convertValue(Object obj, Class<T> toValueType) {
    return OBJECT_MAPPER.convertValue(obj, toValueType);
  }

  public static <T> T parser(final String content) {
    try {
      return OBJECT_MAPPER.readValue(content, new TypeReference<T>() {});
    } catch (IOException e) {
      LOG.error("Error to parser json [{}] and convert to type reference", content, e);
      throw new IllegalStateException("Error to parser json and convert to type reference", e);
    }
  }

  public static <T> T parser(final String content, Class<T> toValueType) {
    try {
      return OBJECT_MAPPER.readValue(content, toValueType);
    } catch (IOException e) {
      LOG.error("Error to parser json [{}] and convert to object", content, e);
      throw new IllegalStateException("Error to parser json and convert to object", e);
    }
  }
}

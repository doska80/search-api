package com.grupozap.search.api.utils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class FlattenMapUtils {

  private static List<String> invalidFields;
  private static final BiFunction<Entry<String, Object>, String, String> keyValue =
      (entry, key) ->
          Optional.ofNullable(key)
              .filter(s -> !invalidFields.contains(s))
              .map(
                  k -> {
                    if (invalidFields.contains(entry.getKey())) return key;
                    return String.join(".", key, entry.getKey());
                  })
              .orElse(entry.getKey());

  private FlattenMapUtils() {
    super();
  }

  public static Map<String, Object> flat(Map<String, Object> data, List<String> invalidFields) {
    FlattenMapUtils.invalidFields = invalidFields;
    return data.entrySet().stream()
        .flatMap(e -> flatten(e, null))
        .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
  }

  @SuppressWarnings("unchecked")
  private static Stream<Map.Entry<String, Object>> flatten(
      Map.Entry<String, Object> entry, String key) {
    if (entry.getValue() instanceof Map)
      return ((Map<String, Object>) entry.getValue())
          .entrySet().stream().flatMap(e -> flatten(e, keyValue.apply(entry, key)));
    return Stream.of(new AbstractMap.SimpleEntry<>(keyValue.apply(entry, key), entry.getValue()));
  }
}

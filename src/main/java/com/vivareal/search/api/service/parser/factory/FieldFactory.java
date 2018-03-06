package com.vivareal.search.api.service.parser.factory;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.rangeClosed;

import com.vivareal.search.api.model.query.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.LinkedMap;
import org.springframework.stereotype.Component;

@Component
public class FieldFactory {

  private static final String DEFAULT_TYPE = "_obj";

  public Field createField(String fieldName) {
    return createField(fieldName, new HashMap<>());
  }

  public Field createField(String fieldName, Map<String, Object> typePerFieldName) {
    List<String> names = asList(fieldName.split("\\."));
    LinkedMap fieldTypes =
        rangeClosed(1, names.size())
            .boxed()
            .map(i -> names.stream().limit(i).collect(joining(".")))
            .collect(
                LinkedMap::new,
                (map, field) -> map.put(field, typePerFieldName.getOrDefault(field, DEFAULT_TYPE)),
                LinkedMap::putAll);
    return new Field(fieldTypes);
  }

  public Field createField(Boolean not, Field field) {
    return new Field(not, field.getTypesByName());
  }
}

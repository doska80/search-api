package com.vivareal.search.api.service.parser.factory;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.rangeClosed;

import com.google.common.collect.ImmutableMap;
import com.vivareal.search.api.exception.InvalidFieldException;
import com.vivareal.search.api.model.event.ClusterSettingsUpdatedEvent;
import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.service.parser.IndexSettings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.LinkedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component("fieldFactory")
public class FieldFactory implements ApplicationListener<ClusterSettingsUpdatedEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(FieldFactory.class);
  public static final Map<String, String> WHITE_LIST_METAFIELDS =
      ImmutableMap.<String, String>builder().put("_id", "string").put("_score", "float").build();

  private Map<String, Field> validFields = new HashMap<>();

  @Autowired private IndexSettings indexSettings; // Request scoped

  public Field createField(String fieldName) {
    // TODO - Remove this issue when fix the parser :'(
    if ("NOT".equalsIgnoreCase(fieldName)) return null;

    return ofNullable(validFields.get(keyForField(indexSettings.getIndex(), fieldName)))
        .orElseThrow(() -> new InvalidFieldException(fieldName, indexSettings.getIndex()));
  }

  public Field createField(Boolean not, Field field) {
    return new Field(not, field.getTypesByName());
  }

  @Override
  public void onApplicationEvent(ClusterSettingsUpdatedEvent event) {
    this.validFields = preprocessFieldsForIndexes(event.getSettingsByIndex());
    LOG.debug("Refreshing valid fields: " + validFields.toString());
  }

  private Map<String, Field> preprocessFieldsForIndexes(
      Map<String, Map<String, Object>> settingsByIndex) {
    Map<String, Field> fields = new HashMap<>();
    // Add fields from mapping
    settingsByIndex
        .entrySet()
        .stream()
        .map(entry -> processFieldsForIndex(entry.getKey(), entry.getValue()))
        .forEach(fields::putAll);

    // Add whitelist fields
    settingsByIndex.keySet().stream().map(this::getWhiteListFieldsForIndex).forEach(fields::putAll);
    return fields;
  }

  private Map<String, Field> processFieldsForIndex(String indexName, Map<String, Object> settings) {
    return settings
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() instanceof String)
        .map(entry -> createSingleFieldForIndex(entry.getKey(), settings))
        .collect(toMap(field -> keyForField(indexName, field.getName()), identity()));
  }

  private Field createSingleFieldForIndex(String fieldName, Map<String, Object> indexSettings) {
    List<String> names = asList(fieldName.split("\\."));
    LinkedMap fieldTypes =
        rangeClosed(1, names.size())
            .boxed()
            .map(i -> names.stream().limit(i).collect(joining(".")))
            .collect(
                LinkedMap::new,
                (map, field) -> map.put(field, indexSettings.get(field)),
                LinkedMap::putAll);
    return new Field(fieldTypes);
  }

  private Map<String, Field> getWhiteListFieldsForIndex(String indexName) {
    return WHITE_LIST_METAFIELDS
        .entrySet()
        .stream()
        .map(
            entry -> {
              LinkedMap linkedMap = new LinkedMap();
              linkedMap.put(entry.getKey(), entry.getValue());
              return linkedMap;
            })
        .map(Field::new)
        .collect(toMap(field -> keyForField(indexName, field.getName()), identity()));
  }

  public boolean isIndexHasField(String index, String fieldName) {
    return validFields.containsKey(keyForField(index, fieldName));
  }

  private String keyForField(String index, String fieldName) {
    return index + "." + fieldName;
  }
}

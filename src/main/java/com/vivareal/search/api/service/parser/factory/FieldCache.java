package com.vivareal.search.api.service.parser.factory;

import static com.vivareal.search.api.model.query.Facet._COUNT;
import static com.vivareal.search.api.model.query.Facet._KEY;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableMap;
import com.vivareal.search.api.exception.InvalidFieldException;
import com.vivareal.search.api.model.event.ClusterSettingsUpdatedEvent;
import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.service.parser.IndexSettings;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections.map.LinkedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component("fieldCache")
public class FieldCache implements ApplicationListener<ClusterSettingsUpdatedEvent> {

  public static final Map<String, String> WHITE_LIST_METAFIELDS =
      ImmutableMap.<String, String>builder()
          .put("_id", "string")
          .put("_score", "float")
          .put(_KEY, "string")
          .put(_COUNT, "string")
          .build();
  private static final Logger LOG = LoggerFactory.getLogger(FieldCache.class);

  private FieldFactory fieldFactory;
  private Map<String, Field> validFields;
  private IndexSettings indexSettings; // Request scoped

  @Autowired
  public FieldCache(FieldFactory fieldFactory) {
    this.fieldFactory = fieldFactory;
    this.validFields = new HashMap<>();
  }

  @Autowired
  public void setIndexSettings(IndexSettings indexSettings) {
    this.indexSettings = indexSettings;
  }

  public Field getField(String fieldName) {
    // TODO - Remove this issue when fix the parser :'(
    if ("NOT".equalsIgnoreCase(fieldName)) return null;

    return ofNullable(validFields.get(keyForField(indexSettings.getIndex(), fieldName)))
        .orElseThrow(() -> new InvalidFieldException(fieldName, indexSettings.getIndex()));
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
    settingsByIndex.forEach(
        (indexName, settings) -> fields.putAll(processFieldsForIndex(indexName, settings)));
    // Add whitelist fields
    settingsByIndex
        .keySet()
        .forEach(indexName -> fields.putAll(getWhiteListFieldsForIndex(indexName)));
    return fields;
  }

  private Map<String, Field> processFieldsForIndex(String indexName, Map<String, Object> settings) {
    return settings
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() instanceof String)
        .map(entry -> fieldFactory.createField(entry.getKey(), settings))
        .collect(toMap(field -> keyForField(indexName, field.getName()), identity()));
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

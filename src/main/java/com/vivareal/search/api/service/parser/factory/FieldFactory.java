package com.vivareal.search.api.service.parser.factory;

import static com.vivareal.search.api.model.mapping.MappingType.fromType;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.rangeClosed;

import com.vivareal.search.api.model.event.ClusterSettingsUpdatedEvent;
import com.vivareal.search.api.model.mapping.MappingType;
import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.service.parser.IndexSettings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.AbstractHashedMap;
import org.apache.commons.collections.map.LinkedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component("fieldFactory")
public class FieldFactory implements ApplicationListener<ClusterSettingsUpdatedEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(FieldFactory.class);
  private IndexSettings indexSettings; // Request scoped
  private Map<String, MappingType> validFields = new HashMap<>();

  @Autowired
  public void setIndexSettings(IndexSettings indexSettings) {
    this.indexSettings = indexSettings;
  }

  public Field createField(List<String> names) {
    Field field = new Field(getFieldTypes(names));
    indexSettings.validateField(field);
    return field;
  }

  public LinkedMap getFieldTypes(List<String> names) {
    return rangeClosed(1, names.size())
        .boxed()
        .map(i -> names.stream().limit(i).collect(joining(".")))
        .collect(
            LinkedMap::new,
            (map, field) -> map.put(field, indexSettings.getFieldType(field)),
            AbstractHashedMap::putAll);
  }

  public Field createField(Boolean not, Field field) {
    return new Field(not, field.getTypesByName());
  }

  @Override
  public void onApplicationEvent(ClusterSettingsUpdatedEvent event) {
    this.validFields = loadValidFields(event.getSettingsByIndex());
    LOG.debug("Refreshing valid fields: " + validFields.toString());
  }

  private Map<String, MappingType> loadValidFields(
      Map<String, Map<String, Object>> settingsByIndex) {
    Map<String, MappingType> fields = new HashMap<>();
    settingsByIndex
        .entrySet()
        .stream()
        .map(entry -> loadValidFieldsForIndex(entry.getKey(), entry.getValue()))
        .forEach(fields::putAll);
    return fields;
  }

  private Map<String, MappingType> loadValidFieldsForIndex(
      String indexName, Map<String, Object> settings) {
    Map<String, MappingType> indexFields = new HashMap<>();
    settings
        .entrySet()
        .forEach(
            entry ->
                fromType(entry.getValue().toString())
                    .ifPresent(
                        mappingType -> {
                          String fieldKey = keyForField(indexName, entry.getKey());
                          indexFields.put(fieldKey, mappingType);
                        }));
    return indexFields;
  }

  public boolean isIndexHasField(String index, String fieldName) {
    return validFields.containsKey(keyForField(index, fieldName));
  }

  private String keyForField(String index, String fieldName) {
    return index + "." + fieldName;
  }
}

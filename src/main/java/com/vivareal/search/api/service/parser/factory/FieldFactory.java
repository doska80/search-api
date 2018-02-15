package com.vivareal.search.api.service.parser.factory;

import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.rangeClosed;

import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.service.parser.IndexSettings;
import java.util.List;
import org.apache.commons.collections.map.AbstractHashedMap;
import org.apache.commons.collections.map.LinkedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FieldFactory {

  private IndexSettings indexSettings; // Request scoped

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
}

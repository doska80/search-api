package com.vivareal.search.api.model.mapping;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

public enum MappingType {
  FIELD_TYPE_DATE(newHashSet("date")),
  FIELD_TYPE_NESTED(newHashSet("nested")),
  FIELD_TYPE_BOOLEAN(newHashSet("boolean")),
  FIELD_TYPE_GEOPOINT(newHashSet("geo_point")),

  FIELD_TYPE_TEXT(newHashSet("text")),
  FIELD_TYPE_KEYWORD(newHashSet("keyword")),
  FIELD_TYPE_STRING(newHashSet("text", "keyword")),

  FIELD_TYPE_LONG(newHashSet("long")),
  FIELD_TYPE_FLOAT(newHashSet("float")),
  FIELD_TYPE_NUMBER(newHashSet("long", "float"));

  private final Set<String> types;

  MappingType(Set<String> types) {
    this.types = types;
  }

  public boolean typeOf(final String foundType) {
    return types.contains(foundType);
  }

  @Override
  public String toString() {
    return types.toString();
  }
}

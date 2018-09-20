package com.grupozap.search.api.model.mapping;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

public enum MappingType {
  FIELD_TYPE_DATE("date"),
  FIELD_TYPE_NESTED("nested"),
  FIELD_TYPE_BOOLEAN("boolean"),
  FIELD_TYPE_GEOPOINT("geo_point"),

  FIELD_TYPE_TEXT("text"),
  FIELD_TYPE_KEYWORD("keyword"),
  FIELD_TYPE_STRING("text", "keyword"),

  FIELD_TYPE_INTEGER("integer"),
  FIELD_TYPE_LONG("long"),
  FIELD_TYPE_FLOAT("float"),
  FIELD_TYPE_NUMBER("integer", "long", "float"),

  FIELD_TYPE_OBJECT("_obj"),
  FIELD_TYPE_SCRIPT("_script");

  private final Set<String> types;
  private final String defaultType;

  MappingType(String... types) {
    this.types = newHashSet(types);
    this.defaultType = types[0];
  }

  public boolean typeOf(final String foundType) {
    return types.contains(foundType);
  }

  public String getDefaultType() {
    return defaultType;
  }

  @Override
  public String toString() {
    return types.toString();
  }
}

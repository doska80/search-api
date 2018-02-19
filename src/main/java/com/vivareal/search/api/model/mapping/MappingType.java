package com.vivareal.search.api.model.mapping;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.stream;

import java.util.Optional;
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
  FIELD_TYPE_NUMBER("long", "float"),

  FIELD_TYPE_OBJECT("_obj");

  private final Set<String> types;

  MappingType(String... types) {
    this.types = newHashSet(types);
  }

  public static Optional<MappingType> fromType(String rawType) {
    return stream(MappingType.values())
        .filter(mappingType -> mappingType.getTypes().contains(rawType))
        .findFirst();
  }

  public boolean typeOf(final String foundType) {
    return types.contains(foundType);
  }

  private Set<String> getTypes() {
    return types;
  }

  @Override
  public String toString() {
    return types.toString();
  }
}

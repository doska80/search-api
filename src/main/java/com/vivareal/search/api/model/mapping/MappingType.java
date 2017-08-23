package com.vivareal.search.api.model.mapping;

import com.google.common.base.MoreObjects;

import static java.util.Arrays.asList;

public enum MappingType {

    FIELD_TYPE_DATE("date"),
    FIELD_TYPE_NESTED("nested"),
    FIELD_TYPE_BOOLEAN("boolean"),
    FIELD_TYPE_GEOPOINT("geo_point"),

    FIELD_TYPE_TEXT("text"),
    FIELD_TYPE_KEYWORD("keyword"),
    FIELD_TYPE_STRING("text", "keyword"),

    FIELD_TYPE_LONG("long"),
    FIELD_TYPE_FLOAT("float"),
    FIELD_TYPE_NUMBER("long", "float");

    MappingType(String... types) {
        this.types = types;
    }

    private String[] types;

    public boolean typeOf(final String foundType) {
        return asList(types).contains(foundType);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
        .add("types", types)
        .toString();
    }
}

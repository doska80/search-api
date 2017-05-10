package com.vivareal.search.api.parser;

import org.elasticsearch.common.Strings;

public class Field {

    private final String name;

    public Field(final String name) {
        this.name = name.intern();
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        if (Strings.isNullOrEmpty(this.name))
            return "NULL";
        return this.name;
    }

}
package com.vivareal.search.api.parser;

public class Field {

    private final String name;

    public Field(final String name) {
        this.name = name.intern();
    }

    public String getName() {
        return name;
    }

}

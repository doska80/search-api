package com.vivareal.search.api.parser;

import static java.util.stream.Collectors.joining;

import java.util.Collection;

public class Field {

    private Collection<String> names;

    public Field(final Collection<String> names) {
        if(names == null || names.isEmpty())
            throw new IllegalArgumentException("Field list cannot be empty");
        this.names = names;
    }

    public String getName() {
        return this.names.stream().collect(joining("."));
    }

    public Collection<String> getNames() {
        return this.names;
    }

    @Override
    public String toString() {
        return this.names.toString();
    }
}

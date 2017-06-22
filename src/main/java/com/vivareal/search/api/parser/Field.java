package com.vivareal.search.api.parser;

import com.google.common.base.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Field {
    private boolean not;
    private final Collection<String> names;

    public Field(boolean not, final Collection<String> names) {
        this.not = not;
        this.names = (names == null || names.isEmpty()) ? emptyList() : names;
    }

    public String getName() {
        return this.names.stream().collect(joining("."));
    }

    public Collection<String> getNames() {
        return this.names;
    }

    @Override
    public String toString() {
        return String.format("%s%s", isNot() ? "NOT " : "", getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Field field = (Field) o;

        return Objects.equal(this.not, field.not)
                && Objects.equal(this.names, field.names);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.not, this.names);
    }

    public boolean isNot() {
        return not;
    }
}

package com.vivareal.search.api.model.query;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;

public class Value {

    protected List<Object> contents = EMPTY_CONTENTS;

    private static final List<Object> EMPTY_CONTENTS = emptyList();

    public static final Value NULL_VALUE = new Value(null);

    // Empty contructor on purpose in order to allow Fixtures creation by reflection
    private Value() {
    }

    public Value(Object content) {
        this.contents = content instanceof List ? (List<Object>) content : singletonList(content);
    }

    public Value(List<Object> contents) {
        this.contents = contents;
    }

    public List<Object> getContents() {
        return contents;
    }

    public Object getContents(int index) {
        return ofNullable(contents).map(c -> c.get(index)).orElseThrow(() -> new IndexOutOfBoundsException(String.valueOf(index)));
    }

    public <T> T value() {
        return value(0);
    }

    public <T> T value(int index) {
        return (T) getContents(index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Value value = (Value) o;

        return Objects.equal(this.contents, value.contents);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.contents);
    }

    @Override
    public String toString() {
        if (isEmpty(contents) || (contents.size() == 1 && contents.get(0) == null))
            return "NULL";

        if (contents.size() > 1)
            return format("[%s]", Joiner.on(", ").join(contents));

        Object simpleValue = contents.get(0);
        if (simpleValue instanceof String) {
            return format("\"%s\"", simpleValue);
        }

        return simpleValue.toString();
    }
}

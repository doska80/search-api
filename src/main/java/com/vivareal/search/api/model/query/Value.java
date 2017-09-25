package com.vivareal.search.api.model.query;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

public class Value {

    protected List<Object> contents;

    public static final Value NULL_VALUE = new Value(null);

    public Value() {
        this.contents = emptyList();
    }

    public Value(Object content) {
        this(content instanceof List ? (List<Object>) content : singletonList(content));
    }

    public Value(List<Object> contents) {
        this.contents = ofNullable(contents).orElse(emptyList());
    }

    public int size() {
        return contents.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public <T> T first() {
        return value(0);
    }

    public <T> T last() {
        return value(size() - 1);
    }

    public <T> T value() {
        return first();
    }

    public <T> T value(int index) {
        return value(index, 0);
    }

    public <T> T value(int index, int indexList) {
        T value = (T) contents.get(index);

        if (value instanceof Value)
            value = ((Value) value).value(indexList);
        else if (value instanceof List)
            value = (T) ((List) value).get(indexList);

        return value;
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
        if (isEmpty() || (size() == 1 && contents.get(0) == null))
            return "NULL";

        if (size() > 1)
            return format("[%s]", Joiner.on(", ").join(contents));

        Object simpleValue = contents.get(0);
        if (simpleValue instanceof String) {
            return format("\"%s\"", simpleValue);
        }

        return simpleValue.toString();
    }

    public Stream<Object> stream() {
        return contents.stream();
    }

    public List<Object> contents() {
        return Collections.unmodifiableList(contents);
    }
}

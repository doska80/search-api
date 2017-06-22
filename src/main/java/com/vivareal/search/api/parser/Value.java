package com.vivareal.search.api.parser;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

public class Value {
    private List<Object> contents = EMPTY_CONTENTS;

    private static final List<Object> EMPTY_CONTENTS = emptyList();

    public static final Value NULL_VALUE = new Value(null);

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

    @Override
    public String toString() {
        StringBuilder query = new StringBuilder();
        if (contents == null || EMPTY_CONTENTS.equals(contents) || contents.isEmpty())
            query.append("NULL");
        else if (contents.size() == 1) {
            Object simpleValue = contents.get(0);
            if (simpleValue == null) {
                query.append("NULL");
            } else if (simpleValue instanceof String && ((String) simpleValue).isEmpty()) {
                query.append("\"\"");
            } else {
                query.append(simpleValue);
            }
        } else {
            query.append("[\"");
            query.append(Joiner.on("\", \"").join(contents));
            query.append("\"]");
        }
        return query.toString();
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
}
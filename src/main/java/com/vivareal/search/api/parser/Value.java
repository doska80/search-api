package com.vivareal.search.api.parser;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Value {

    private static final List<String> EMPTY_CONTENTS = Collections.emptyList();

    private List<String> contents = EMPTY_CONTENTS;

    public Value(String content) {
        this(new ArrayList<>(Arrays.asList(content)));
    }

    public Value(List<String> contents) {
        this.setContents(contents);
    }

    public String getFirstContent() {
        if (this.contents == null || this.contents.size() == 0 || Strings.isNullOrEmpty(this.contents.get(0)))
            return null;
        return this.contents.get(0); // FAIL doing this twice, BAD!
    }

    public List<String> getContents() {
        return contents;
    }

    public void setContents(List<String> content) {
        this.contents = content;
    }

    public void addContent(String value) {
        if (this.contents == null || EMPTY_CONTENTS.equals(this.contents))
            this.contents = new ArrayList<>(50);
        this.contents.add(value);
    }

    @Override
    public String toString() {
        StringBuilder query = new StringBuilder();
        if (contents == null || EMPTY_CONTENTS.equals(contents) || contents.isEmpty())
            query.append("NULL");
        else if (contents.size() == 1) {
            String simpleValue = contents.get(0);
            if (simpleValue == null) {
                query.append("NULL");
            } else if (Strings.isNullOrEmpty(simpleValue)) {
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

}
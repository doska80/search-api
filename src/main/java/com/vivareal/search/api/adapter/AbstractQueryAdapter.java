package com.vivareal.search.api.adapter;

import com.google.common.collect.ImmutableList;
import com.vivareal.search.api.model.Expression;
import com.vivareal.search.api.model.Field;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class AbstractQueryAdapter {

    protected static final ImmutableList<Field> EMPTY_LIST = ImmutableList.of();
    protected static final Pattern FIELD_VALUES = Pattern.compile("\\s*(\\w+)\\s*(" + Expression.getPattern() + ")\\s*(?:\")?(.*?(?=\"?\\s+\\w+\\s*(" + Expression.getPattern() + ")|(?:\"?)$))");

    public List<Field> parseQuery(final String query) {
        Matcher m = FIELD_VALUES.matcher(query);

        boolean found = m.find();
        if (!found)
            return EMPTY_LIST;

        ImmutableList.Builder<Field> fieldListBuilder = ImmutableList.builder();
        do {
            fieldListBuilder.add(new Field(m.group(1), m.group(2), m.group(3)));
        } while (m.find());

        return fieldListBuilder.build();
    }

}

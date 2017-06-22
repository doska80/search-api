package com.vivareal.search.api.parser;

import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public enum LogicalOperator {
    AND,
    OR;

    private static final Map<String, LogicalOperator> OPERATORS = new HashMap<>(4);

    static {
        OPERATORS.put("&&", AND);
        OPERATORS.put("AND", AND);
        OPERATORS.put("||", OR);
        OPERATORS.put("OR", OR);
    }

    protected static String[] getOperators() {
        return OPERATORS.keySet().toArray(new String[]{});
    }

    public static LogicalOperator get(final String logic) {
        return ofNullable(logic)
                .map(String::toUpperCase)
                .map(OPERATORS::get)
                .orElseThrow(() -> new IllegalArgumentException("Logical Operator \"" + logic + "\" is not recognized!"));
    }
}
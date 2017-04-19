package com.vivareal.search.api.model.query;

import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

public enum Expression {
    DIFFERENT,
    EQUAL,
    GREATER,
    GREATER_EQUAL,
    LESS,
    LESS_EQUAL;

    private static Map<String, Expression> EXPRESSIONS = new HashMap<>(6);

    static {
        EXPRESSIONS.put("NE", DIFFERENT);
        EXPRESSIONS.put("EQ", EQUAL);
        EXPRESSIONS.put(":", EQUAL);
        EXPRESSIONS.put("GT", GREATER);
        EXPRESSIONS.put("GTE", GREATER_EQUAL);
        EXPRESSIONS.put("LT", LESS);
        EXPRESSIONS.put("LTE", LESS_EQUAL);
    }

    public static String getPattern() {
        return EXPRESSIONS.keySet().stream().collect(joining("|"));
    }

    public static Expression get(String expression) {
        return ofNullable(expression)
                .map(String::toUpperCase)
                .map(EXPRESSIONS::get)
                .orElseThrow(() -> new IllegalArgumentException("Expression \"" + expression + "\" not found!"));
    }
}

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
    IN_END,
    IN_START,
    LESS,
    LESS_EQUAL,
    SEPARATOR, // TODO check if really needed!
    SUB_END,  // TODO check if needed after making jparsec really recursive
    SUB_START; // TODO check if needed after making jparsec really recursive

    private static Map<String, Expression> EXPRESSIONS = new HashMap<>(25);

    static {
        EXPRESSIONS.put("NE", DIFFERENT);
        EXPRESSIONS.put("<>", DIFFERENT);
        EXPRESSIONS.put("EQ", EQUAL);
        EXPRESSIONS.put(":", EQUAL);
        EXPRESSIONS.put("=", EQUAL);
        EXPRESSIONS.put("GT", GREATER);
        EXPRESSIONS.put(">", GREATER);
        EXPRESSIONS.put("GTE", GREATER_EQUAL);
        EXPRESSIONS.put(">=", GREATER_EQUAL);
        EXPRESSIONS.put("]", IN_END);
        EXPRESSIONS.put("[", IN_START);
        EXPRESSIONS.put("LT", LESS);
        EXPRESSIONS.put("<", LESS);
        EXPRESSIONS.put("LTE", LESS_EQUAL);
        EXPRESSIONS.put("<=", LESS_EQUAL);
        EXPRESSIONS.put(",", SEPARATOR);
        EXPRESSIONS.put(")", SUB_END); // TODO check if needed after making jparsec really recursive
        EXPRESSIONS.put("(", SUB_START); // TODO check if needed after making jparsec really recursive
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

package com.vivareal.search.api.parser;

import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public enum Comparison {
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

    private static final Map<String, Comparison> EXPRESSIONS = new HashMap<>(25);

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

    public static Comparison get(final String comparison) {
        return ofNullable(comparison)
                .map(String::toUpperCase)
                .map(EXPRESSIONS::get)
                .orElseThrow(() -> new IllegalArgumentException("Expression \"" + comparison + "\" not found!"));
    }

}
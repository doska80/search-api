package com.vivareal.search.api.model.query;

import com.google.common.collect.Lists;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

public enum RelationalOperator {

    DIFFERENT,
    EQUAL,
    GREATER,
    GREATER_EQUAL,
    IN,
    LESS,
    LESS_EQUAL,
    VIEWPORT;

    private static final Map<String, RelationalOperator> OPERATORS = new HashMap<>(16);
    private static final EnumMap<RelationalOperator, List<String>> RELATIONAL_OPERATOR_MAP = new EnumMap<>(RelationalOperator.class);

    static {
        OPERATORS.put("NE", DIFFERENT);
        OPERATORS.put("<>", DIFFERENT);
        OPERATORS.put("EQ", EQUAL);
        OPERATORS.put(":", EQUAL);
        OPERATORS.put("=", EQUAL);
        OPERATORS.put("GT", GREATER);
        OPERATORS.put(">", GREATER);
        OPERATORS.put("GTE", GREATER_EQUAL);
        OPERATORS.put(">=", GREATER_EQUAL);
        OPERATORS.put("IN", IN);
        OPERATORS.put("LT", LESS);
        OPERATORS.put("<", LESS);
        OPERATORS.put("LTE", LESS_EQUAL);
        OPERATORS.put("<=", LESS_EQUAL);
        OPERATORS.put("VIEWPORT", VIEWPORT);
        OPERATORS.put("@", VIEWPORT);
    }

    static {
        OPERATORS.keySet().forEach(
            operator -> {
                RelationalOperator relationalOperator = OPERATORS.get(operator);
                if (RELATIONAL_OPERATOR_MAP.containsKey(relationalOperator)) {
                    RELATIONAL_OPERATOR_MAP.get(relationalOperator).add(operator);
                } else {
                    RELATIONAL_OPERATOR_MAP.put(relationalOperator, Lists.newArrayList(operator));
                }
            }
        );
    }

    public static String[] getOperators() {
        return OPERATORS.keySet().toArray(new String[OPERATORS.size()]);
    }

    public static RelationalOperator get(final String relation) {
        return ofNullable(relation)
        .map(String::toUpperCase)
        .map(OPERATORS::get)
        .orElseThrow(() -> new IllegalArgumentException("Relational Operator \"" + relation + "\" is not recognized!"));
    }

    public static List<String> getOperators(final RelationalOperator relationalOperator) {
        return ofNullable(relationalOperator)
        .map(RELATIONAL_OPERATOR_MAP::get)
        .orElseThrow(() -> new IllegalArgumentException("Relational Operator \"" + relationalOperator + "\" is not recognized!"));
    }
}

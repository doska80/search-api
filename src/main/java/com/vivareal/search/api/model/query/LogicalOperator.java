package com.vivareal.search.api.model.query;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.vivareal.search.api.model.query.LogicalOperator.LogicalOperatorMap.OPERATORS;
import static java.util.Optional.ofNullable;

public enum LogicalOperator implements QueryFragment {

    AND("AND", "&&"),
    OR("OR", "||");

    LogicalOperator(String... alias) {
        if (alias.length < 1)
            throw new IllegalArgumentException("Operator must have at least 1 alias");
        Stream.of(alias).forEach(label -> OPERATORS.put(label, this));
    }

    public static String[] getOperators() {
        return OPERATORS.keySet().toArray(new String[OPERATORS.size()]);
    }

    public static LogicalOperator get(final String logic) {
        return ofNullable(logic)
            .map(OPERATORS::get)
            .orElseThrow(() -> new IllegalArgumentException("Logical Operator \"" + logic + "\" is not recognized!"));
    }

    static class LogicalOperatorMap {
        static final Map<String, LogicalOperator> OPERATORS = new HashMap<>();
    }
}

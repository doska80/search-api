package com.vivareal.search.api.model.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.vivareal.search.api.model.query.RelationalOperator.RelationalOperatorMap.OPERATORS;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public enum RelationalOperator {

    DIFFERENT("NE", "<>"),
    EQUAL("EQ", ":", "="),
    GREATER("GT", ">"),
    GREATER_EQUAL("GTE", ">="),
    IN("IN"),
    LESS("LT", "<"),
    LESS_EQUAL("LTE", "<="),
    VIEWPORT("@");

    private String[] alias;

    RelationalOperator(String... alias) {
        if (alias.length < 1) throw new IllegalArgumentException("Operator must have at least 1 alias");
        this.alias = alias;
        Stream.of(this.alias).forEach(label -> OPERATORS.put(label, this));
    }

    public static String[] getOperators() {
        return OPERATORS.keySet().toArray(new String[OPERATORS.size()]);
    }

    public static RelationalOperator get(final String relation) {
        return ofNullable(relation)
        .map(OPERATORS::get)
        .orElseThrow(() -> new IllegalArgumentException("Relational Operator \"" + relation + "\" is not recognized!"));
    }

    public static List<String> getOperators(final RelationalOperator relationalOperator) {
        return OPERATORS.entrySet()
        .stream()
        .filter(e -> e.getValue().equals(relationalOperator))
        .map(Map.Entry::getKey)
        .collect(toList());
    }

    public String[] getAlias() {
        return alias;
    }

    static class RelationalOperatorMap {
        static Map<String, RelationalOperator> OPERATORS = new HashMap<>();
    }
}

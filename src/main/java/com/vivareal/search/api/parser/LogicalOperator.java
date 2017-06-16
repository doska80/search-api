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

// NOT (a = 2 AND b = 3)
// (a <> 2 OR b <> 3)

// NOT ((filed1 = 2) AND (bla = 3))

// NOT (NOT filed1) AND bla = 3

// NOT negocio:VENTA
// NOT (negocio:VENTA AND negocio:TINCAS)
// NOT negocio:VENTA AND NOT negocio:TINCAS

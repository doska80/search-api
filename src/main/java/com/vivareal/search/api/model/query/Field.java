package com.vivareal.search.api.model.query;

public final class Field {

    public final String name;
    public final Expression expression;
    public final Object value;

    public Field(final String name, final String expression, final String value) {
        this(name, Expression.get(expression), value);
    }

    public Field(final String name, final Expression expression, final String value) {
        this.name = name;
        this.expression = expression;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Expression getExpression() {
        return expression;
    }

    public Object getValue() {
        return value;
    }

}

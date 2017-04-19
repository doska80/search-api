package com.vivareal.search.api.model;

public class Field {

    public String name;
    public Expression expression;
    public Object value;

    public Field(String name, String expression, String value) {
        this(name, Expression.get(expression), value);
    }

    public Field(String name, Expression expression, String value) {
        this.setName(name);
        this.setExpression(expression);
        this.setValue(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}

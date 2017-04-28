package com.vivareal.search.api.parser;

import com.vivareal.search.api.model.query.Expression;

public class Filter {

    private Field field;
    private Expression comparison;
    private Value value;

    public Filter() {
        // do nothing
    }

    public Filter(Field field, Expression expression, Value value) {
        this.field = field;
        this.comparison = expression;
        this.value = value;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Expression getComparison() {
        return comparison;
    }

    public void setComparison(Expression comparison) {
        this.comparison = comparison;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return field.getName() + " " + comparison.toString() + " " + value.getContent();
    }

}
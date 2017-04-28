package com.vivareal.search.api.parser;

public class Filter {

    private Field field;
    private Comparison comparison;
    private Value value;

    public Filter(Object[] parsers) {
        this((Field) parsers[0], (Comparison) parsers[1], (Value) parsers[2]);
    }

    public Filter(Field field, Comparison expression, Value value) {
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

    public Comparison getComparison() {
        return comparison;
    }

    public void setComparison(Comparison comparison) {
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
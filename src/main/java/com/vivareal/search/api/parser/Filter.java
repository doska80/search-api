package com.vivareal.search.api.parser;

public class Filter {

    private Field field;
    private RelationalOperator relationalOperator;
    private Value value;
    private LogicalOperator logicalOperator;

    public Filter(Field field, RelationalOperator expression, Value value) {
        this.field = field;
        this.relationalOperator = expression;
        this.value = value;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public RelationalOperator getRelationalOperator() {
        return relationalOperator;
    }

    public void setRelationalOperator(RelationalOperator relationalOperator) {
        this.relationalOperator = relationalOperator;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Filter setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
        return this;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    @Override
    public String toString() {
        return field.getName() + " " + relationalOperator.toString() + " " + value.getContent();
    }
}

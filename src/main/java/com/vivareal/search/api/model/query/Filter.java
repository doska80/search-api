package com.vivareal.search.api.model.query;

import com.google.common.base.Objects;

import java.util.StringJoiner;

public class Filter {

    private Field field;
    private RelationalOperator relationalOperator;
    private Value value;

    // Empty contructor on purpose in order to allow Fixtures creation by reflection
    private Filter() {
    }

    public Filter(Field field, RelationalOperator relationalOperator, Value value) {
        this.field = field;
        this.relationalOperator = relationalOperator;
        this.value = value;
    }

    public Field getField() {
        return field;
    }

    public RelationalOperator getRelationalOperator() {
        return relationalOperator;
    }

    public Value getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Filter filter = (Filter) o;

        return Objects.equal(this.field, filter.field)
            && Objects.equal(this.relationalOperator, filter.relationalOperator)
            && Objects.equal(this.value, filter.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.field, this.relationalOperator, this.value);
    }

    @Override
    public String toString() {
        if (this.field == null || this.relationalOperator == null || this.value == null)
            return super.toString();

        return new StringJoiner(" ")
            .add(field.getName())
            .add(relationalOperator.name())
            .add(value.toString())
            .toString();
    }
}

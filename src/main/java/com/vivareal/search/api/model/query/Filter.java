package com.vivareal.search.api.model.query;

import com.google.common.base.Objects;

public class Filter {
    private Field field;
    private RelationalOperator relationalOperator;
    private Value value;

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
    public String toString() {
        if (this.field == null || this.relationalOperator == null || this.value == null)
            return super.toString();

        StringBuilder query = new StringBuilder();
        query.append(field.getName());
        query.append(" ");
        query.append(relationalOperator.name());
        query.append(" ");
        query.append(value.toString());
        return query.toString().trim();
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
}

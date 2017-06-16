package com.vivareal.search.api.parser;

public class Filter {

    private boolean not;

    private Field field;
    private RelationalOperator relationalOperator;
    private Value value;

    public Filter(boolean not, Field field, RelationalOperator relationalOperator, Value value) {
        this.not = not;
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
        if (not) {
            query.append("NOT ");
        }
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

        if (field != null ? !field.equals(filter.field) : filter.field != null) return false;
        if (relationalOperator != filter.relationalOperator) return false;
        return value != null ? value.equals(filter.value) : filter.value == null;
    }

    @Override
    public int hashCode() {
        int result = field != null ? field.hashCode() : 0;
        result = 31 * result + (relationalOperator != null ? relationalOperator.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public boolean isNot() {
        return not;
    }
}

// NOT rooms:3 AND suites:2


// NOT (rooms:3 AND suites:2)
// (rooms<>3 OR suites<>2)
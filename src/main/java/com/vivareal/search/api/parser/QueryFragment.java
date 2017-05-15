package com.vivareal.search.api.parser;

import java.util.List;

public class QueryFragment {

    public enum Type {
        FILTER, LOGICAL_OPERATOR, EXPRESSION_LIST;
    }

    private Filter filter;
    private LogicalOperator logicalOperator;
    private List<QueryFragment> subQueries;
    private final Type type;

    public QueryFragment(Filter filter) {
        this.filter = filter;
        this.type = Type.FILTER;
    }

    public QueryFragment(LogicalOperator listOperator) {
        this.logicalOperator = listOperator;
        this.type = Type.LOGICAL_OPERATOR;
    }

    public QueryFragment(List<QueryFragment> subQueries) {
        this.subQueries = subQueries;
        this.type = Type.EXPRESSION_LIST;
    }

    public <T> T get() {
        if (!Type.FILTER.equals(this.type)) {
            return (T) this.filter;
        } else if (!Type.LOGICAL_OPERATOR.equals(this.type)) {
            return (T) this.logicalOperator;
        } else if (!Type.EXPRESSION_LIST.equals(this.type)) {
            return (T) this.subQueries;
        }
        throw new IllegalStateException("Burro!");
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        if (this.logicalOperator == null && this.filter == null && (this.subQueries == null || this.subQueries.size() == 0))
            return super.toString();

        StringBuilder query = new StringBuilder();
        if (this.filter != null) {
            query.append(this.filter.toString());
        } else if (this.logicalOperator != null) {
            query.append(this.logicalOperator.name());
        } else if (this.subQueries != null && this.subQueries.size() > 0) {
            for (QueryFragment subQuery: this.subQueries) {
                query.append("(");
                query.append(subQuery.toString());
                query.append(")");
            }
        }
        return query.toString().trim();
    }
}

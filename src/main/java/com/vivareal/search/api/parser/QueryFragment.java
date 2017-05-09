package com.vivareal.search.api.parser;

import java.util.List;

public class QueryFragment {

    enum Type {
        FILTER, LOGICAL_OPERATOR, EXPRESSION_LIST;
    }

    private Filter filter;
    private LogicalOperator logicalOperator;
    private List<QueryFragment> queryFragments;
    private final Type type;

    public QueryFragment(Filter filter) {
        this.filter = filter;
        this.type = Type.FILTER;
    }

    public QueryFragment(LogicalOperator listOperator) {
        this.logicalOperator = listOperator;
        this.type = Type.LOGICAL_OPERATOR;
    }

    public QueryFragment(List<QueryFragment> queryFragments) {
        this.queryFragments = queryFragments;
        this.type = Type.EXPRESSION_LIST;
    }

    public <T> T get() {
        if (!Type.FILTER.equals(this.type)) {
            return (T) this.filter;
        } else if (!Type.LOGICAL_OPERATOR.equals(this.type)) {
            return (T) this.logicalOperator;
        } else if (!Type.EXPRESSION_LIST.equals(this.type)) {
            return (T) this.queryFragments;
        }
        throw new IllegalStateException("Burro!");
    }

}

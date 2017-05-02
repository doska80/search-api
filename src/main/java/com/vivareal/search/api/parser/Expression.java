package com.vivareal.search.api.parser;

import java.util.List;

public class Expression {

    enum ExpressionType {
        FILTER, LOGICAL_OPERATOR, EXPRESSION_LIST;
    }

    private Filter filter;
    private LogicalOperator logicalOperator;
    private List<Expression> expressions;
    private final ExpressionType type;

    public Expression(Filter filter) {
        this.filter = filter;
        this.type = ExpressionType.FILTER;
    }

    public Expression(LogicalOperator listOperator) {
        this.logicalOperator = listOperator;
        this.type = ExpressionType.LOGICAL_OPERATOR;
    }

    public Expression(List<Expression> expressions) {
        this.expressions = expressions;
        this.type = ExpressionType.EXPRESSION_LIST;
    }

    public <T> T get() {
        if (!ExpressionType.FILTER.equals(this.type)) {
            return (T) this.filter;
        } else if (!ExpressionType.LOGICAL_OPERATOR.equals(this.type)) {
            return (T) this.logicalOperator;
        } else if (!ExpressionType.EXPRESSION_LIST.equals(this.type)) {
            return (T) this.expressions;
        }
        throw new IllegalStateException("Burro!");
    }

}

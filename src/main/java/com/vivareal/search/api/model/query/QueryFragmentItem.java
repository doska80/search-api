package com.vivareal.search.api.model.query;

import com.google.common.base.Objects;

import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class QueryFragmentItem implements QueryFragment {
    private final Filter filter;
    private final LogicalOperator logicalOperator;

    public QueryFragmentItem(Optional<LogicalOperator> logicalOperator, Filter filter) {
        this.filter = filter;
        this.logicalOperator = logicalOperator.orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryFragmentItem item = (QueryFragmentItem) o;

        return Objects.equal(this.filter, item.filter)
                && Objects.equal(this.logicalOperator, item.logicalOperator);
    }

    public Filter getFilter() {
        return filter;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.filter, this.logicalOperator);
    }

    @Override
    public String toString() {
        return format("%s%s", ofNullable(logicalOperator).map(l -> format("%s ", l.toString())).orElse(""), filter.toString());
    }
}

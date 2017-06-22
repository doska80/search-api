package com.vivareal.search.api.parser;

import com.google.common.base.Objects;

import java.util.List;
import java.util.Optional;

public class QueryFragment {
    private Filter filter;
    private LogicalOperator logicalOperator;
    private List<QueryFragment> subQueries;

    public QueryFragment(Filter filter, Optional<LogicalOperator> logicalOperator) {
        this.filter = filter;
        this.logicalOperator = logicalOperator.orElse(null);
    }

    public QueryFragment(List<QueryFragment> queryFragments) {
        if (this.getSubQueries() == null && queryFragments.size() == 1) {
            QueryFragment fragment = queryFragments.get(0);
            this.filter = fragment.getFilter();
            this.logicalOperator = fragment.getLogicalOperator();
        } else {
            this.subQueries = queryFragments;
        }
    }

    public List<QueryFragment> getSubQueries() {
        return subQueries;
    }

    public Filter getFilter() {
        return filter;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    @Override
    public String toString() {
        StringBuilder query = new StringBuilder();
        if (this.getSubQueries() == null) {
            if (this.filter != null)
                query.append(this.filter.toString());
            if (this.logicalOperator != null) {
                query.append(" ");
                query.append(this.logicalOperator.name());
                query.append(" ");
            }
        } else {
            //query.append("(");
            for (QueryFragment fragment : this.subQueries) {
                if (fragment.getSubQueries() == null) {
                    query.append(fragment.toString());
                } else {
                    query.append("(");
                    query.append(fragment.toString());
                    query.append(")");
                }
            }
           // query.append(")");
        }
        return query.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryFragment fragment = (QueryFragment) o;

        return Objects.equal(this.filter, fragment.filter)
                && Objects.equal(this.logicalOperator, fragment.logicalOperator)
                && Objects.equal(this.subQueries, fragment.subQueries);
    }

    @Override
    public int hashCode() {
       return Objects.hashCode(this.filter, this.logicalOperator, this.subQueries);
    }
}

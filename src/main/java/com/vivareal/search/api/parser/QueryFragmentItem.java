package com.vivareal.search.api.parser;

import com.google.common.base.Objects;

import java.util.AbstractList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

class QueryFragmentOperator implements QueryFragment {
    private LogicalOperator operator;

    public QueryFragmentOperator(LogicalOperator operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return operator.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryFragmentOperator that = (QueryFragmentOperator) o;

        return operator == that.operator;
    }

    @Override
    public int hashCode() {
        return operator != null ? operator.hashCode() : 0;
    }
}

class QueryFragmentList extends AbstractList<QueryFragment> implements QueryFragment {
    private final List<QueryFragment> fragments;

    public QueryFragmentList(List<QueryFragment> fragments) {
        this.fragments = fragments;
    }

    @Override
    public QueryFragment get(int index) {
        return fragments.get(index);
    }

    @Override
    public int size() {
        return fragments.size();
    }

    @Override
    public String toString() {
        return String.format("(%s)", fragments.stream().map(QueryFragment::toString).collect(Collectors.joining(" ")));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        QueryFragmentList that = (QueryFragmentList) o;

        return Objects.equal(this.fragments, that.fragments);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.fragments);
    }
}

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

    @Override
    public int hashCode() {
        return Objects.hashCode(this.filter, this.logicalOperator);
    }

    @Override
    public String toString() {
        return format("%s%s", ofNullable(logicalOperator).map(l -> format("%s ", l.toString())).orElse(""), filter.toString());
    }
}

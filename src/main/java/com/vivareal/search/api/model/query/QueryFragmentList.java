package com.vivareal.search.api.model.query;

import com.google.common.base.Objects;

import java.util.AbstractList;
import java.util.List;
import java.util.stream.Collectors;

public class QueryFragmentList extends AbstractList<QueryFragment> implements QueryFragment {
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

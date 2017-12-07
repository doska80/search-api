package com.vivareal.search.api.model.query;

import com.google.common.base.Objects;

import java.util.AbstractList;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class QueryFragmentList extends AbstractList<QueryFragment> implements QueryFragment {

    private final List<QueryFragment> fragments;

    public QueryFragmentList(List<QueryFragment> fragments) {
        if (fragments.size() > MAX_FRAGMENTS) throw new IllegalArgumentException(format("Exceeded the number of fragments: %d (max: %d)", fragments.size(), MAX_FRAGMENTS));
        this.fragments = validateSingleRecursiveQueryFragmentList(fragments);
    }

    private List<QueryFragment> validateSingleRecursiveQueryFragmentList(List<QueryFragment> queryFragments) {
        if (isNotEmpty(queryFragments)) {
            QueryFragment fragment = queryFragments.get(0);
            if (hasOnlyAnInternalQueryFragmentList(queryFragments)) // e.g. ((((queryFragmentList)))), will be extracted to a single (queryFragmentList)
                return (List<QueryFragment>) fragment;

            if (fragment instanceof QueryFragmentItem && ((QueryFragmentItem) fragment).getLogicalOperator() != null)
                throw new IllegalArgumentException("The first item cannot have a logical operator prefix");
        }

        // If the QueryFragmentList is empty or if there are multiple nested QueryFragmentList
        return queryFragments;
    }

    private boolean hasOnlyAnInternalQueryFragmentList(List<QueryFragment> queryFragments) {
        return isNotEmpty(queryFragments)
            && queryFragments.size() == 1
            && queryFragments.get(0) instanceof QueryFragmentList;
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
        return format("(%s)", fragments.stream().map(QueryFragment::toString).collect(joining(" ")));
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

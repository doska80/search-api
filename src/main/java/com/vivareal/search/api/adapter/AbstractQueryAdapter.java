package com.vivareal.search.api.adapter;

import com.google.common.collect.ImmutableList;
import com.vivareal.search.api.model.query.Sort;
import com.vivareal.search.api.parser.QueryFragment;
import com.vivareal.search.api.parser.QueryParser;
import org.elasticsearch.common.Strings;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractQueryAdapter<Q,F,S> implements QueryAdapter<Q,F,S> {

    protected static final ImmutableList<QueryFragment> EMPTY_QUERY_FRAGMENT_LIST = ImmutableList.of();
    protected static final ImmutableList<Sort> EMPTY_SORT_LIST = ImmutableList.of();
//    protected static final Pattern FIELD_VALUES = Pattern.compile("\\s*(\\w+)\\s*(" + Expression.getPattern() + ")\\s*(?:\")?(.*?(?=\"?\\s+\\w+\\s*(" + Expression.getPattern() + ")|(?:\"?)$))");
    protected static final Pattern SORT_VALUES = Pattern.compile("\\s*(\\w+)(\\s+(ASC|DESC))?\\s*(,)?");

    protected abstract F getFilter(List<String> filter);
    protected abstract S getSort(List<String> sort);

    public static final List<QueryFragment> parseFilter(final String filter) {
        if (Strings.isNullOrEmpty(filter))
            return EMPTY_QUERY_FRAGMENT_LIST;
        List<QueryFragment> fragments = QueryParser.get().parse(filter);
        // TODO log
        return fragments;
    }

    public static final List<Sort> parseSort(final String sort) {
        Matcher sortMatcher = SORT_VALUES.matcher(sort);

        boolean found = sortMatcher.find();
        if (!found)
            return EMPTY_SORT_LIST;

        ImmutableList.Builder<Sort> sortListBuilder = ImmutableList.builder();
        do {
            String sortDirection = sortMatcher.group(3);
            if (Strings.isNullOrEmpty(sortDirection))
                sortListBuilder.add(new Sort(sortMatcher.group(1)));
            else
                sortListBuilder.add(new Sort(sortMatcher.group(1), sortMatcher.group(3)));
        } while (sortMatcher.find());

        return sortListBuilder.build();
    }

}

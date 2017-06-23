package com.vivareal.search.api.adapter;

import com.google.common.collect.ImmutableList;
import com.vivareal.search.api.model.parser.SortParser;
import com.vivareal.search.api.model.query.Sort;
import com.vivareal.search.api.model.query.QueryFragment;
import com.vivareal.search.api.model.parser.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public abstract class AbstractQueryAdapter<Q, F, S> implements QueryAdapter<Q, F, S> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractQueryAdapter.class);

    private static final ImmutableList<QueryFragment> EMPTY_QUERY_FRAGMENT_LIST = ImmutableList.of();

    protected abstract F getFilter(List<String> filter);

    protected abstract S getSort(List<String> sort);

    public static List<QueryFragment> parseFilter(final String filter) {
        if (isEmpty(filter))
            return EMPTY_QUERY_FRAGMENT_LIST;
        QueryFragment fragments = QueryParser.get().parse(filter);
        LOG.debug("Query parse: {}", fragments);
//        return fragments.getSubQueries();
        return null;
    }

    public static Sort parseSort(final String sort) {
        return SortParser.get().parse(sort);
    }
}

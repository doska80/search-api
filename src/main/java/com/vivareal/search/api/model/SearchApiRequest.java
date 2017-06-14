package com.vivareal.search.api.model;

import com.vivareal.search.api.model.query.Sort;
import com.vivareal.search.api.parser.LogicalOperator;
import com.vivareal.search.api.parser.QueryFragment;
import com.vivareal.search.api.parser.QueryParser;
import org.jparsec.Parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.vivareal.search.api.adapter.AbstractQueryAdapter.parseSort;

public final class SearchApiRequest {

    private static final Parser<List<QueryFragment>> QUERY_PARSER = QueryParser.get();

    private List<String> includeFields = Collections.emptyList();
    private List<String> excludeFields = Collections.emptyList();
    private List<QueryFragment> filter = Collections.emptyList();
    private List<Sort> sort = Collections.emptyList();

    private String q;
    private Integer from, size;

    public List<QueryFragment> getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        List<QueryFragment> parsed = QUERY_PARSER.parse(filter);
        if (!parsed.isEmpty()) {
            this.filter = new ArrayList<>();
            this.filter.addAll(parsed);
        }
    }

    public void XsetFilter(List<String> filters) { // FIXME does not work. Spring split in every "," it finds, breaking our IN []
        if (filters == null || filters.isEmpty()) return;
        boolean hasNext;
        this.filter = new ArrayList<>();
        Iterator<String> iterator = filters.iterator();
        do {
            this.filter.addAll(QUERY_PARSER.parse(iterator.next()));
            hasNext = iterator.hasNext();
            if (hasNext)
                this.filter.add(new QueryFragment(LogicalOperator.AND));
        } while (hasNext);
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        if (q != null) this.q = q.trim();
    }

    public List<Sort> getSort() {
        return sort;
    }

    public void setSort(List<String> sorts) {
        this.sort = new ArrayList<>();
        sorts.forEach(sort -> this.sort.addAll(parseSort(sort)));
    }

    public List<String> getIncludeFields() {
        return includeFields;
    }

    public void setIncludeFields(List<String> includeFields) {
        this.includeFields = includeFields;
    }

    public List<String> getExcludeFields() {
        return excludeFields;
    }

    public void setExcludeFields(List<String> excludeFields) {
        this.excludeFields = excludeFields;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public void setPaginationValues(int defaultSize, int maxSize) {
        this.from = from != null && from >= 0 ? from : 0;
        this.size = size != null && size >= 0 && size <= maxSize ? size : defaultSize;
    }
}

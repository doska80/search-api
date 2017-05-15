package com.vivareal.search.api.model;

import com.google.common.base.Joiner;
import com.vivareal.search.api.model.query.Sort;
import com.vivareal.search.api.parser.LogicalOperator;
import com.vivareal.search.api.parser.QueryFragment;
import com.vivareal.search.api.parser.QueryParser;
import org.jparsec.Parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.vivareal.search.api.adapter.AbstractQueryAdapter.parseSort;

public final class SearchApiRequest {

    private List<String> field = Collections.emptyList();
    private List<QueryFragment> filter = Collections.emptyList();
    private List<Sort> sort = Collections.emptyList();

    private String q;
    private String from;
    private String size;

    public List<String> getField() {
        return field;
    }

    public void setField(List<String> fields) {
        this.field = fields;
    }

    public List<QueryFragment> getFilter() {
        return filter;
    }

    public void setFilter(List<String> filters) {
//        Parser<List<QueryFragment>> queryParser = QueryParser.get();
//        Iterator<String> iterator = filters.iterator();
//        this.filter = new ArrayList();
//        boolean hasNext = false;
//        while (hasNext = iterator.hasNext()) {
//            this.filter.addAll(queryParser.parse(iterator.next()));
//            if (hasNext)
//                this.filter.add(new QueryFragment(LogicalOperator.AND));
//        }
//        filters.forEach(filter -> {
//            this.filter.addAll(QueryParser.get().parse(filter));
//            if (nao sou o ultimo)
//                this.filter.add()
//        });
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
        this.sort = new ArrayList();
        sorts.forEach(sort -> {
            this.sort.addAll(parseSort(sort));
        });
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }
}

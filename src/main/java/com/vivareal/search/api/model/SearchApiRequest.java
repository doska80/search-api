package com.vivareal.search.api.model;

import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.model.query.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.vivareal.search.api.adapter.AbstractQueryAdapter.parseFilter;
import static com.vivareal.search.api.adapter.AbstractQueryAdapter.parseSort;

public final class SearchApiRequest {

    private List<String> field = Collections.emptyList();
    private List<Field> filter = Collections.emptyList();
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

    public List<Field> getFilter() {
        return filter;
    }

    public void setFilter(List<String> filters) {
        this.filter = new ArrayList();
        filters.forEach(filter -> {
            this.filter.addAll(parseFilter(filter));
        });
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

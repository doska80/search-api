package com.vivareal.search.api.model;

import java.util.Collections;
import java.util.List;

public final class SearchApiRequest {

    private List<String> field = Collections.emptyList();
    private List<String> filter = Collections.emptyList();
    private String q;
    private List<String> sort = Collections.emptyList();

    public List<String> getField() {
        return field;
    }

    public void setField(List<String> fields) {
        this.field = fields;
    }

    public List<String> getFilter() {
        return filter;
    }

    public void setFilter(List<String> filters) {
        this.filter = filters;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        if (q != null) this.q = q.trim();
    }

    public List<String> getSort() {
        return sort;
    }

    public void setSort(List<String> sort) {
        this.sort = sort;
    }

}

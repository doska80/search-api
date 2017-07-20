package com.vivareal.search.api.model.http;

import com.google.common.base.MoreObjects;
import com.vivareal.search.api.model.search.*;

import java.util.Set;

public class SearchApiRequest extends BaseApiRequest implements Filterable, Queryable, Pageable, Sortable, Facetable {
    private String q;

    private String mm;
    private String filter;

    private Set<String> fields;
    private Set<String> sort;

    private Set<String> facets;

    private Integer facetSize;

    private Integer from;

    private Integer size;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getMm() {
        return mm;
    }

    public void setMm(String mm) {
        this.mm = mm;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Set<String> getFields() {
        return fields;
    }

    public void setFields(Set<String> fields) {
        this.fields = fields;
    }

    public Set<String> getSort() {
        return sort;
    }

    public void setSort(Set<String> sort) {
        this.sort = sort;
    }

    public Set<String> getFacets() {
        return facets;
    }

    public void setFacets(Set<String> facets) {
        this.facets = facets;
    }

    public Integer getFacetSize() {
        return facetSize;
    }

    public void setFacetSize(Integer facetSize) {
        this.facetSize = facetSize;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
        .add("index", getIndex())
        .add("mm", getMm())
        .add("fields", getFields())
        .add("includeFields", getIncludeFields())
        .add("excludeFields", getExcludeFields())
        .add("filter", getFilter())
        .add("sort", getSort())
        .add("facets", getFacets())
        .add("facetSize", getFacetSize())
        .add("q", getQ())
        .add("from", getFrom())
        .add("size", getSize())
        .omitNullValues()
        .toString();
    }
}

package com.vivareal.search.api.model.http;

import com.google.common.base.MoreObjects;
import com.vivareal.search.api.model.search.*;
import io.swagger.annotations.ApiModelProperty;

import java.util.Set;

public class SearchApiRequest extends BaseApiRequest implements Filterable, Queryable, Pageable, Sortable, Facetable {
    @ApiModelProperty(value = "Query string")
    private String q;

    @ApiModelProperty(value = "Minimum should match (-100..+100)", example = "10, 75%")
    private String mm;

    @ApiModelProperty(value = "Query DSL", example = "field1:3 AND field2:2 AND(field3=1 OR (field4 IN [1,\"abc\"] AND field5 <> 3))")
    private String filter;

    @ApiModelProperty(value = "Field list that will be filtered for query string", example = "field1, field2, field3")
    private Set<String> fields;

    @ApiModelProperty(value = "Sorting in the format: field (ASC|DESC), default sort order is ascending, multiple sort are supported", example = "field1 ASC, field2 DESC")
    private Set<String> sort;

    @ApiModelProperty(value = "Facet field list", example = "field, field2, field3")
    private Set<String> facets;

    @ApiModelProperty("Sets the size of facets")
    private Integer facetSize;

    @ApiModelProperty("From index to start the search from")
    private Integer from;

    @ApiModelProperty("The number of search hits to return")
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

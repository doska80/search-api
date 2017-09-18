package com.vivareal.search.api.model.http;

import com.google.common.base.MoreObjects;
import com.vivareal.search.api.model.search.*;
import io.swagger.annotations.ApiModelProperty;

import java.util.Set;

public class SearchApiRequest extends FilterableApiRequest implements Pageable, Facetable {

    @ApiModelProperty(value = "Facet field list", example = "field, field2, field3")
    private Set<String> facets;

    @ApiModelProperty("Sets the size of facets")
    private int facetSize = Integer.MAX_VALUE;

    @ApiModelProperty("From index to start the search from")
    private int from = 0;

    @ApiModelProperty("The number of search hits to return")
    private int size = Integer.MAX_VALUE;

    public Set<String> getFacets() {
        return facets;
    }

    public void setFacets(Set<String> facets) {
        this.facets = facets;
    }

    public int getFacetSize() {
        return facetSize;
    }

    public void setFacetSize(int facetSize) {
        this.facetSize = facetSize;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
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

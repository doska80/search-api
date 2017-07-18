package com.vivareal.search.api.fixtures.model;

import com.vivareal.search.api.model.SearchApiRequest;

import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.ObjectUtils.allNotNull;

/**
 * Created by leandropereirapinto on 7/6/17.
 */
public class SearchApiRequestBuilder {

    public static final String INDEX_NAME = "my_index";

    private String index;
    private String mm;
    private Set<String> fields;
    private Set<String> includeFields;
    private Set<String> excludeFields;
    private String filter;
    private String sort;
    private List<String> facets;
    private int facetSize;
    private String q;
    private int from;
    private int size;

    public SearchApiRequest basicRequest() {
        return index(INDEX_NAME).from(0).size(20).builder();
    }

    private SearchApiRequest builder() {
        SearchApiRequest searchApiRequest = new SearchApiRequest();

        if (allNotNull(index))
            searchApiRequest.setIndex(index);

        if (allNotNull(mm))
            searchApiRequest.setMm(mm);

        if (allNotNull(fields))
            searchApiRequest.setFields(fields);

        if (allNotNull(includeFields))
            searchApiRequest.setIncludeFields(includeFields);

        if (allNotNull(excludeFields))
            searchApiRequest.setExcludeFields(excludeFields);

        if (allNotNull(filter))
            searchApiRequest.setFilter(filter);

        if (allNotNull(sort))
            searchApiRequest.setSort(sort);

        if (allNotNull(facets))
            searchApiRequest.setFacets(facets);

        if (allNotNull(facetSize))
            searchApiRequest.setFacetSize(facetSize);

        if (allNotNull(q))
            searchApiRequest.setQ(q);

        if (allNotNull(from))
            searchApiRequest.setFrom(from);

        if (allNotNull(size))
            searchApiRequest.setSize(size);

        return searchApiRequest;
    }

    public SearchApiRequestBuilder index(final String index) {
        this.index = index;
        return this;
    }

    public SearchApiRequestBuilder mm(String mm) {
        this.mm = mm;
        return this;
    }

    public SearchApiRequestBuilder fields(Set<String> fields) {
        this.fields = fields;
        return this;
    }

    public SearchApiRequestBuilder includeFields(Set<String> includeFields) {
        this.includeFields = includeFields;
        return this;
    }

    public SearchApiRequestBuilder excludeFields(Set<String> excludeFields) {
        this.excludeFields = excludeFields;
        return this;
    }

    public SearchApiRequestBuilder filter(String filter) {
        this.filter = filter;
        return this;
    }

    public SearchApiRequestBuilder sort(String sort) {
        this.sort = sort;
        return this;
    }

    public SearchApiRequestBuilder facets(List<String> facets) {
        this.facets = facets;
        return this;
    }

    public SearchApiRequestBuilder facetSize(int facetSize) {
        this.facetSize = facetSize;
        return this;
    }

    public SearchApiRequestBuilder q(String q) {
        this.q = q;
        return this;
    }

    public SearchApiRequestBuilder from(int from) {
        this.from = from;
        return this;
    }

    public SearchApiRequestBuilder size(int size) {
        this.size = size;
        return this;
    }
}

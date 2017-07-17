package com.vivareal.search.api.model;

import com.google.common.base.MoreObjects;
import com.vivareal.search.api.model.parser.FacetParser;
import com.vivareal.search.api.model.parser.QueryParser;
import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.model.query.QueryFragment;
import com.vivareal.search.api.model.query.Sort;
import org.jparsec.Parser;

import java.util.List;

import static com.vivareal.search.api.configuration.SearchApiEnv.RemoteProperties.ES_DEFAULT_SORT;
import static com.vivareal.search.api.model.parser.SortParser.get;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apache.logging.log4j.util.Strings.isEmpty;

public final class SearchApiRequest {

    private static final Parser<QueryFragment> QUERY_PARSER = QueryParser.get();

    private String index;
    private String mm;
    private List<String> fields;
    private List<String> includeFields;
    private List<String> excludeFields;
    private QueryFragment filter;
    private Sort sort;
    private List<Field> facets;
    private Integer facetSize;

    private String q;
    private Integer from;
    private Integer size;

    public QueryFragment getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = QUERY_PARSER.parse(filter);
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getMm() {
        return mm;
    }

    public void setMm(String mm) {
        this.mm = mm;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        if (q != null) this.q = q.trim();
    }

    public Sort getSort() {
        if (sort != null)
            return sort;

        if (!isEmpty(ES_DEFAULT_SORT.getValue(index)))
            setSort(ES_DEFAULT_SORT.getValue(index));

        return sort;
    }

    public void setSort(String... sort) {
        this.sort = get().parse(stream(sort).collect(joining(",")));
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
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

    public List<Field> getFacets() {
        return facets;
    }

    public void setFacets(List<String> facets) {
        this.facets = FacetParser.get().parse(facets.stream().collect(joining(",")));
    }

    public Integer getFacetSize() {
        return facetSize;
    }

    public void setFacetSize(Integer facetSize) {
        this.facetSize = facetSize;
    }

    public void setPaginationValues(int defaultSize, int maxSize) {
        this.from = from != null && from >= 0 ? from : 0;
        this.size = size != null && size >= 0 && size <= maxSize ? size : defaultSize;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
        .add("index", index)
        .add("mm", mm)
        .add("fields", fields)
        .add("includeFields", includeFields)
        .add("excludeFields", excludeFields)
        .add("filter", filter)
        .add("sort", sort)
        .add("facets", facets)
        .add("facetSize", facetSize)
        .add("q", q)
        .add("from", from)
        .add("size", size)
        .omitNullValues()
        .toString();
    }
}

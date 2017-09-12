package com.vivareal.search.api.model.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.terms.InternalMappedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SearchApiResponse {

    private long time;
    private long totalCount;
    private Map<String, Object> result;

    public SearchApiResponse time(final long time) {
        this.time = time;
        return this;
    }

    public SearchApiResponse totalCount(final long totalCount) {
        this.totalCount = totalCount;
        return this;
    }

    public SearchApiResponse result(final String elementName, final Object object) {
        if (this.result == null)
            this.result = new LinkedHashMap<>();

        this.result.put(elementName, object);
        return this;
    }

    private void putIntoFacetMap(Aggregation agg, Map<String, Map<String, Long>> facets) {
        facets.put(agg.getName(), addBuckets(((InternalMappedTerms) agg).getBuckets()));
    }

    public SearchApiResponse facets(final Aggregations aggregations) {
        if (aggregations != null) {
            Map<String, Map<String, Long>> facets = new LinkedHashMap<>();
            aggregations.forEach(agg -> {
                if (agg instanceof InternalMappedTerms) {
                    putIntoFacetMap(agg, facets);
                } else if (agg instanceof InternalNested) {
                    ((InternalNested) agg).getAggregations().forEach(aggregation -> putIntoFacetMap(aggregation, facets));
                }
            });
            result("facets", facets);
        }
        return this;
    }

    private static Map<String, Long> addBuckets(List<Terms.Bucket> buckets) {
        Map<String, Long> result = new LinkedHashMap<>();
        buckets.forEach(bucket -> result.put(bucket.getKeyAsString(), bucket.getDocCount()));
        return result;
    }

    public long getTime() {
        return time;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public Map<String, Object> getResult() {
        return result;
    }
}

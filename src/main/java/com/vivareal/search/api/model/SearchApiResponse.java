package com.vivareal.search.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.DoubleTerms;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SearchApiResponse {

    private long time;
    private long totalCount;
    private Map<String, List<Object>> result;
    private Map<String, Map<String, Long>> facets;

    private SearchApiResponse() {
        super();
    }

    public static SearchApiResponse builder() {
        return new SearchApiResponse();
    }

    public SearchApiResponse time(final long time) {
        this.time = time;
        return this;
    }

    public SearchApiResponse totalCount(final long totalCount) {
        this.totalCount = totalCount;
        return this;
    }

    public SearchApiResponse result(final String resultName, Object result) {
        return result(resultName, result != null ? newArrayList(result) : null);
    }

    public SearchApiResponse result(final String resultName, List<Object> result) {
        if (this.result == null)
            this.result = new LinkedHashMap<>();
        this.result.put(resultName, result != null ? result : new ArrayList<>());
        return this;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public SearchApiResponse facets(final Optional<Aggregations> aggregationsOptional) {
        aggregationsOptional.ifPresent(aggregations -> {
            if (this.facets == null)
                this.facets = new LinkedHashMap<>();

            aggregations.asList().forEach(aggregation -> {

                if (aggregation instanceof LongTerms) {
                    LongTerms longTerms = (LongTerms) aggregation;
                    this.facets.put(longTerms.getName(), addBuckets(longTerms.getBuckets()));

                } else if (aggregation instanceof DoubleTerms) {
                    DoubleTerms doubleTerms = (DoubleTerms) aggregation;
                    this.facets.put(doubleTerms.getName(), addBuckets(doubleTerms.getBuckets()));

                } else if (aggregation instanceof StringTerms) {
                    StringTerms stringTerms = (StringTerms) aggregation;
                    this.facets.put(stringTerms.getName(), addBuckets(stringTerms.getBuckets()));
                }
            });
        });

        return this;
    }

    private static Map<String, Long> addBuckets(List<?> objBuckets) {
        Map<String, Long> buckets = new LinkedHashMap<>();
        objBuckets.forEach(obj -> {
            if (obj instanceof Terms.Bucket) {
                Terms.Bucket bucket = (Terms.Bucket) obj;
                String key = bucket.getKeyAsString();
                long count = bucket.getDocCount();
                buckets.put(key, count);

            }
        });
        return buckets;
    }

    public Map<String, Map<String, Long>> getFacets() {
        return facets;
    }

    public long getTime() {
        return time;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public Map<String, List<Object>> getResult() {
        return result;
    }
}

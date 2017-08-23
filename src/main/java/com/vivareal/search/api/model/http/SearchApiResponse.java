package com.vivareal.search.api.model.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.terms.InternalMappedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public SearchApiResponse facets(final Optional<Aggregations> aggregationsOptional) {
        aggregationsOptional.ifPresent(
            aggregations -> {
                Map<String, Object> facets = new LinkedHashMap<>();

                aggregations.asList().forEach(agg -> {
                    if (agg instanceof InternalMappedTerms) {
                        facets.put((agg).getName(), addBuckets(((InternalMappedTerms) agg).getBuckets()));
                    } else if (agg instanceof InternalNested) {
                        ((InternalNested) agg).getAggregations().asList().forEach(aggregation -> facets.put((aggregation).getName(), addBuckets(((InternalMappedTerms) aggregation).getBuckets())));
                    }
                });
                result("facets", facets);
            }
        );
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

package com.vivareal.search.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.vivareal.search.api.model.serializer.SearchResponseEnvelope;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.terms.InternalMappedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static java.util.Optional.ofNullable;

public class ESResponseSerializer extends StdSerializer<SearchResponseEnvelope<SearchResponse>> {

    private static Logger LOG = LoggerFactory.getLogger(ESResponseSerializer.class);

    public ESResponseSerializer() {
        this(null);
    }

    private ESResponseSerializer(Class<SearchResponseEnvelope<SearchResponse>> t) {
        super(t);
    }

    @Override
    public void serialize(SearchResponseEnvelope<SearchResponse> value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        SearchResponse searchResponse = value.getSearchResponse();

        jgen.writeStartObject();
        jgen.writeNumberField("time", searchResponse.getTookInMillis());
        jgen.writeNumberField("totalCount", ofNullable(searchResponse.getHits()).map(SearchHits::getTotalHits).orElse(0L));

        jgen.writeObjectFieldStart("result");

        jgen.writeArrayFieldStart(value.getIndexName());
        writeResultSet(searchResponse, jgen);
        jgen.writeEndArray();

        writeFacets(searchResponse, jgen);

        jgen.writeEndObject();
        jgen.writeEndObject();
    }

    private void writeResultSet(SearchResponse searchResponse, JsonGenerator jgen) {
        ofNullable(searchResponse.getHits()).filter(searchHits -> !ArrayUtils.isEmpty(searchHits.getHits())).ifPresent(searchHits -> {
            SearchHit[] hits = searchHits.getHits();
            int len = hits.length - 1;
            try {
                for (int i = 0; i < len; i++) {
                    jgen.writeRaw(hits[i].getSourceRef().utf8ToString());
                    jgen.writeRaw(",");
                }
                jgen.writeRaw(hits[len].getSourceRef().utf8ToString());
            } catch (IOException e) {
                LOG.error("Error to write results on response", e);
            }
        });
    }

    private void writeFacets(SearchResponse value, JsonGenerator jgen) {
        if (value.getAggregations() != null) {
            try {
                jgen.writeObjectFieldStart("facets");
                for (Aggregation agg : value.getAggregations()) {
                    if (agg instanceof InternalMappedTerms) {
                        writeFacet(agg, jgen);
                    } else if (agg instanceof InternalNested) {
                        for (Aggregation aggregation : ((InternalNested) agg).getAggregations()) {
                            writeFacet(aggregation, jgen);
                        }
                    }
                }
                jgen.writeEndObject();
            } catch (IOException e) {
                LOG.error("Error to write facets on response", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeFacet(Aggregation agg, JsonGenerator jgen) throws IOException {
        jgen.writeObjectFieldStart(agg.getName());
        writeFacetBuckets(((InternalMappedTerms) agg).getBuckets(), jgen);
        jgen.writeEndObject();
    }

    private void writeFacetBuckets(List<Terms.Bucket> buckets, JsonGenerator jgen) throws IOException {
        for (Terms.Bucket bucket : buckets) {
            jgen.writeNumberField(bucket.getKeyAsString(), bucket.getDocCount());
        }
    }
}

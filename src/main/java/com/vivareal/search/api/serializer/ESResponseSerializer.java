package com.vivareal.search.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.vivareal.search.api.model.serializer.SearchResponseEnvelope;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.terms.InternalMappedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

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
        jgen.writeNumberField("totalCount", searchResponse.getHits() != null ? searchResponse.getHits().getTotalHits() : 0);

        jgen.writeObjectFieldStart("result");
        writeResultSet(searchResponse, jgen, value.getIndexName());
        writeFacets(searchResponse, jgen);
        jgen.writeEndObject();

        jgen.writeEndObject();
    }

    private void writeResultSet(SearchResponse value, JsonGenerator jgen, String indexName) {
        try {
            jgen.writeArrayFieldStart(indexName);
            if (value.getHits() != null && !ArrayUtils.isEmpty(value.getHits().getHits())) {
                SearchHit[] hits = value.getHits().getHits();
                int size = hits.length;
                for (int i = 0; i < size; i++) {
                    jgen.writeRaw(hits[i].getSourceAsString());
                    if ((i+1) < size)
                        jgen.writeRaw(",");
                }
            }
            jgen.writeEndArray();
        } catch (IOException e) {
            LOG.error("Error to write results on response", e);
        }
    }

    private void writeFacets(SearchResponse value, JsonGenerator jgen) {
        if (value.getAggregations() != null) {
            try {
                jgen.writeObjectFieldStart("facets");
                value.getAggregations().forEach(agg -> {
                    if (agg instanceof InternalMappedTerms) {
                        writeFacet(agg, jgen);
                    } else if (agg instanceof InternalNested) {
                        ((InternalNested) agg).getAggregations().forEach(aggregation -> writeFacet(aggregation, jgen));
                    }
                });
                jgen.writeEndObject();
            } catch (IOException e) {
                LOG.error("Error to write facets on response", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeFacet(Aggregation agg, JsonGenerator jgen) {
        try {
            jgen.writeObjectFieldStart(agg.getName());
            writeFacetBuckets(((InternalMappedTerms) agg).getBuckets(), jgen);
            jgen.writeEndObject();
        } catch (IOException e) {
            LOG.error("Error to write facet on response", e);
        }
    }

    private void writeFacetBuckets(List<Terms.Bucket> buckets, JsonGenerator jgen) {
        buckets.forEach(bucket -> {
            try {
                jgen.writeNumberField(bucket.getKeyAsString(), bucket.getDocCount());
            } catch (IOException e) {
                LOG.error("Error to write bucket facet on response", e);
            }
        });
    }
}

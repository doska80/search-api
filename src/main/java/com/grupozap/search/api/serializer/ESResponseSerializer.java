package com.grupozap.search.api.serializer;

import static java.lang.Float.isNaN;
import static org.elasticsearch.common.bytes.BytesReference.toBytes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.grupozap.search.api.model.serializer.SearchResponseEnvelope;
import java.io.IOException;
import java.util.List;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;

public class ESResponseSerializer extends StdSerializer<SearchResponseEnvelope<SearchResponse>> {

  public ESResponseSerializer() {
    this((Class<SearchResponseEnvelope<SearchResponse>>) null);
  }

  public ESResponseSerializer(JavaType type) {
    super(type);
  }

  private ESResponseSerializer(Class<SearchResponseEnvelope<SearchResponse>> t) {
    super(t);
  }

  @Override
  public void serialize(
      SearchResponseEnvelope<SearchResponse> value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {
    var searchResponse = value.getSearchResponse();

    jgen.writeStartObject();

    jgen.writeNumberField("time", searchResponse.getTook().getMillis());
    if (!isNaN(searchResponse.getHits().getMaxScore()))
      jgen.writeNumberField("maxScore", searchResponse.getHits().getMaxScore());
    jgen.writeNumberField("totalCount", searchResponse.getHits().getTotalHits().value);

    var hits = searchResponse.getHits().getHits();
    jgen.writeObjectFieldStart("result");
    jgen.writeArrayFieldStart(value.getIndexName());
    writeResultSet(hits, jgen);
    jgen.writeEndArray();
    writeFacets(searchResponse, jgen);
    jgen.writeEndObject();

    jgen.writeEndObject();
  }

  private String hitAsString(SearchHit h) {
    var bytes = toBytes(h.getSourceRef());
    return new String(bytes, 0, bytes.length);
  }

  private void writeResultSet(final SearchHit[] hits, JsonGenerator jgen) throws IOException {
    if (hits.length > 0) {
      var len = hits.length - 1;
      for (var i = 0; i < len; i++) {
        jgen.writeRaw(hitAsString(hits[i]));
        jgen.writeRaw(",");
      }
      jgen.writeRaw(hitAsString(hits[len]));
    }
  }

  private void writeFacets(final SearchResponse searchResponse, JsonGenerator jgen)
      throws IOException {
    if (searchResponse.getAggregations() != null) {
      jgen.writeObjectFieldStart("facets");
      for (var agg : searchResponse.getAggregations()) {
        if (agg instanceof Terms) {
          writeFacet(agg, jgen);
        } else if (agg instanceof Nested) {
          for (var nestedAgg : ((Nested) agg).getAggregations()) {
            writeFacet(nestedAgg, jgen);
          }
        }
      }
      jgen.writeEndObject();
    }
  }

  @SuppressWarnings("unchecked")
  private void writeFacet(final Aggregation agg, JsonGenerator jgen) throws IOException {
    jgen.writeObjectFieldStart(agg.getName());
    writeFacetBuckets(((Terms) agg).getBuckets(), jgen);
    jgen.writeEndObject();
  }

  private void writeFacetBuckets(final List<? extends Bucket> buckets, JsonGenerator jgen)
      throws IOException {
    for (Bucket bucket : buckets) {
      jgen.writeNumberField(bucket.getKeyAsString(), bucket.getDocCount());
    }
  }
}

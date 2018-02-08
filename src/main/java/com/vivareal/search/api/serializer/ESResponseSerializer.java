package com.vivareal.search.api.serializer;

import static com.vivareal.search.api.adapter.SearchAfterQueryAdapter.SORT_SEPARATOR;
import static java.lang.Float.isNaN;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.of;
import static org.elasticsearch.common.bytes.BytesReference.toBytes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.vivareal.search.api.model.serializer.SearchResponseEnvelope;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.terms.InternalMappedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

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
    SearchResponse searchResponse = value.getSearchResponse();

    jgen.writeStartObject();

    jgen.writeNumberField("time", searchResponse.getTook().getMillis());
    if (!isNaN(searchResponse.getHits().getMaxScore()))
      jgen.writeNumberField("maxScore", searchResponse.getHits().getMaxScore());
    jgen.writeNumberField("totalCount", searchResponse.getHits().getTotalHits());

    SearchHit[] hits = searchResponse.getHits().getHits();
    writeCursorId(hits, jgen);

    jgen.writeObjectFieldStart("result");
    jgen.writeArrayFieldStart(value.getIndexName());
    writeResultSet(hits, jgen);
    jgen.writeEndArray();
    writeFacets(searchResponse, jgen);
    jgen.writeEndObject();

    jgen.writeEndObject();
  }

  private String hitAsString(SearchHit h) {
    byte[] bytes = toBytes(h.getSourceRef());
    return new String(bytes, 0, bytes.length);
  }

  private void writeResultSet(final SearchHit[] hits, JsonGenerator jgen) throws IOException {
    if (hits.length > 0) {
      int len = hits.length - 1;
      for (int i = 0; i < len; i++) {
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
      for (Aggregation agg : searchResponse.getAggregations()) {
        if (agg instanceof InternalMappedTerms) {
          writeFacet(agg, jgen);
        } else if (agg instanceof InternalNested) {
          for (Aggregation aggregation : ((InternalNested) agg).getAggregations()) {
            writeFacet(aggregation, jgen);
          }
        }
      }
      jgen.writeEndObject();
    }
  }

  @SuppressWarnings("unchecked")
  private void writeFacet(final Aggregation agg, JsonGenerator jgen) throws IOException {
    jgen.writeObjectFieldStart(agg.getName());
    writeFacetBuckets(((InternalMappedTerms) agg).getBuckets(), jgen);
    jgen.writeEndObject();
  }

  private void writeFacetBuckets(final List<Terms.Bucket> buckets, JsonGenerator jgen)
      throws IOException {
    for (Terms.Bucket bucket : buckets) {
      jgen.writeNumberField(bucket.getKeyAsString(), bucket.getDocCount());
    }
  }

  private void writeCursorId(final SearchHit[] hits, JsonGenerator jgen) throws IOException {
    if (hits.length > 0) {
      Object[] sortValues = hits[hits.length - 1].getSortValues();
      if (!ArrayUtils.isEmpty(sortValues))
        jgen.writeStringField(
            "cursorId",
            of(sortValues)
                .map(value -> String.valueOf(value).replaceAll("_", "%5f"))
                .collect(joining(SORT_SEPARATOR)));
    }
  }
}

package com.grupozap.search.api.serializer;

import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static org.assertj.core.util.Lists.newArrayList;
import static org.elasticsearch.search.SearchHit.createFromMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.grupozap.search.api.model.serializer.SearchResponseEnvelope;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.SearchSortValues;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.terms.InternalMappedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.BeforeClass;
import org.junit.Test;

public class ESResponseSerializerTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @BeforeClass
  public static void setup() {
    CollectionLikeType type =
        mapper
            .getTypeFactory()
            .constructCollectionLikeType(SearchResponseEnvelope.class, SearchResponse.class);
    mapper.registerModule(new SimpleModule().addSerializer(new ESResponseSerializer(type)));
  }

  @Test
  public void shouldValidateResponseFromEsResponseSerializerByResultsAndFacets()
      throws IOException {
    SearchResponse searchResponse = mock(SearchResponse.class);
    TimeValue took = mock(TimeValue.class);
    when(searchResponse.getTook()).thenReturn(took);
    when(took.getMillis()).thenReturn(123L);

    SearchHit[] hits = new SearchHit[2];
    SearchHits searchHits = new SearchHits(hits, hits.length, 0);
    when(searchResponse.getHits()).thenReturn(searchHits);

    Map<String, Object> values1 = new LinkedHashMap<>();
    BytesReference bytesReference1 = new BytesArray("{\"id\":\"1\",\"field\":\"string\"}");
    values1.put("_source", bytesReference1);
    hits[0] = createFromMap(values1);

    Map<String, Object> values2 = new LinkedHashMap<>();
    BytesReference bytesReference2 = new BytesArray("{\"id\":\"2\",\"facet.field\":\"string\"}");
    values2.put("_source", bytesReference2);
    hits[1] = createFromMap(values2);

    // create aggregations
    Aggregations aggregations = mock(Aggregations.class);
    when(searchResponse.getAggregations()).thenReturn(aggregations);

    // create aggregation for normal fields
    InternalMappedTerms normalAggregation = mock(InternalMappedTerms.class);
    when(normalAggregation.getName()).thenReturn("field");

    List<Terms.Bucket> bucketTerms = newArrayList();
    Terms.Bucket bucket = mock(Terms.Bucket.class);
    when(bucket.getKeyAsString()).thenReturn("string");
    when(bucket.getDocCount()).thenReturn(1L);
    bucketTerms.add(bucket);
    when(normalAggregation.getBuckets()).thenReturn(bucketTerms);

    // create aggregation for nested fields
    InternalNested internalNested = mock(InternalNested.class);
    List<InternalAggregation> nestedAggregations = newArrayList();

    InternalMappedTerms internalAggregation = mock(InternalMappedTerms.class);
    when(internalAggregation.getName()).thenReturn("facet.field");
    nestedAggregations.add(internalAggregation);

    List<Terms.Bucket> nestedBucketTerms = newArrayList();
    Terms.Bucket nestedBucket = mock(Terms.Bucket.class);
    when(nestedBucket.getKeyAsString()).thenReturn("string");
    when(nestedBucket.getDocCount()).thenReturn(1L);
    nestedBucketTerms.add(nestedBucket);
    when(internalAggregation.getBuckets()).thenReturn(nestedBucketTerms);

    InternalAggregations internalAggregations = new InternalAggregations(nestedAggregations);
    when(internalNested.getAggregations()).thenReturn(internalAggregations);

    setField(internalAggregations, "aggregations", nestedAggregations);

    // add aggregations
    List<Aggregation> aggregationList = newArrayList();
    aggregationList.add(normalAggregation);
    aggregationList.add(internalNested);

    setField(aggregations, "aggregations", aggregationList);

    String expected =
        "{\"time\":123,\"maxScore\":0.0,\"totalCount\":2,\"result\":{\""
            + INDEX_NAME
            + "\":[{\"id\":\"1\",\"field\":\"string\"},{\"id\":\"2\",\"facet.field\":\"string\"}],\"facets\":{\"field\":{\"string\":1},\"facet.field\":{\"string\":1}}}}";

    assertEquals(
        expected,
        mapper.writeValueAsString(new SearchResponseEnvelope<>(INDEX_NAME, searchResponse)));
  }

  @Test
  public void shouldValidateResponseFromEsResponseSerializerByResults() throws IOException {
    SearchResponse searchResponse = mock(SearchResponse.class);
    TimeValue took = mock(TimeValue.class);
    when(searchResponse.getTook()).thenReturn(took);
    when(took.getMillis()).thenReturn(2L);

    SearchHit[] hits = new SearchHit[1];
    SearchHits searchHits = new SearchHits(hits, hits.length, 0);

    when(searchResponse.getHits()).thenReturn(searchHits);

    Map<String, Object> values1 = new LinkedHashMap<>();
    BytesReference bytesReference1 =
        new BytesArray(
            "{\"string\":\"string\",\"float\":1.5,\"int\":1,\"negative_number\":-4.4,\"boolean\":true,\"array\":[\"a\",\"b\",\"c\"],\"object\":{\"child\":{\"string\":\"string\",\"float\":1.5,\"int\":1,\"negative_number\":-4.4,\"boolean\":true,\"array\":[\"a\",\"b\",\"c\"],}}}");
    values1.put("_source", bytesReference1);
    hits[0] = createFromMap(values1);

    String expected =
        "{\"time\":2,\"maxScore\":0.0,\"totalCount\":1,\"result\":{\""
            + INDEX_NAME
            + "\":[{\"string\":\"string\",\"float\":1.5,\"int\":1,\"negative_number\":-4.4,\"boolean\":true,\"array\":[\"a\",\"b\",\"c\"],\"object\":{\"child\":{\"string\":\"string\",\"float\":1.5,\"int\":1,\"negative_number\":-4.4,\"boolean\":true,\"array\":[\"a\",\"b\",\"c\"],}}}]}}";

    assertEquals(
        expected,
        mapper.writeValueAsString(new SearchResponseEnvelope<>(INDEX_NAME, searchResponse)));
  }

  @Test
  public void shouldReturnMaxScore() throws IOException {
    SearchResponse searchResponse = mock(SearchResponse.class);
    TimeValue took = mock(TimeValue.class);
    when(searchResponse.getTook()).thenReturn(took);
    when(took.getMillis()).thenReturn(1L);

    SearchHit[] hits = new SearchHit[0];
    SearchHits searchHits = new SearchHits(hits, 1, Float.NaN);

    when(searchResponse.getHits()).thenReturn(searchHits);

    String expected = "{\"time\":1,\"totalCount\":1,\"result\":{\"" + INDEX_NAME + "\":[]}}";
    assertEquals(
        expected,
        mapper.writeValueAsString(new SearchResponseEnvelope<>(INDEX_NAME, searchResponse)));
  }

  @Test
  public void shouldNotReturnMaxScore() throws IOException {
    SearchResponse searchResponse = mock(SearchResponse.class);
    TimeValue took = mock(TimeValue.class);
    when(searchResponse.getTook()).thenReturn(took);
    when(took.getMillis()).thenReturn(1L);

    SearchHit[] hits = new SearchHit[0];
    SearchHits searchHits = new SearchHits(hits, 1, 1f);

    when(searchResponse.getHits()).thenReturn(searchHits);

    String expected =
        "{\"time\":1,\"maxScore\":1.0,\"totalCount\":1,\"result\":{\"" + INDEX_NAME + "\":[]}}";
    assertEquals(
        expected,
        mapper.writeValueAsString(new SearchResponseEnvelope<>(INDEX_NAME, searchResponse)));
  }

  @Test
  public void shouldValidateResponseFromEsResponseSerializerByFacetsWhenPropertySizeIsZero()
      throws IOException {
    SearchResponse searchResponse = mock(SearchResponse.class);
    TimeValue took = mock(TimeValue.class);
    when(searchResponse.getTook()).thenReturn(took);
    when(took.getMillis()).thenReturn(123L);

    SearchHit[] hits = new SearchHit[0];
    SearchHits searchHits = new SearchHits(hits, 56789L, 0);

    when(searchResponse.getHits()).thenReturn(searchHits);

    // create aggregations
    Aggregations aggregations = mock(Aggregations.class);
    when(searchResponse.getAggregations()).thenReturn(aggregations);

    // create aggregation for normal fields
    InternalMappedTerms normalAggregation = mock(InternalMappedTerms.class);
    when(normalAggregation.getName()).thenReturn("field");

    List<Terms.Bucket> bucketTerms = newArrayList();
    Terms.Bucket bucket = mock(Terms.Bucket.class);
    when(bucket.getKeyAsString()).thenReturn("value");
    when(bucket.getDocCount()).thenReturn(10L);
    bucketTerms.add(bucket);
    when(normalAggregation.getBuckets()).thenReturn(bucketTerms);

    // create aggregation for nested fields
    InternalNested internalNested = mock(InternalNested.class);
    List<InternalAggregation> nestedAggregations = newArrayList();

    InternalMappedTerms internalAggregation = mock(InternalMappedTerms.class);
    when(internalAggregation.getName()).thenReturn("facet.field");
    nestedAggregations.add(internalAggregation);

    List<Terms.Bucket> nestedBucketTerms = newArrayList();
    Terms.Bucket nestedBucket = mock(Terms.Bucket.class);
    when(nestedBucket.getKeyAsString()).thenReturn("value");
    when(nestedBucket.getDocCount()).thenReturn(10L);
    nestedBucketTerms.add(nestedBucket);
    when(internalAggregation.getBuckets()).thenReturn(nestedBucketTerms);

    InternalAggregations internalAggregations = new InternalAggregations(nestedAggregations);
    when(internalNested.getAggregations()).thenReturn(internalAggregations);

    setField(internalAggregations, "aggregations", nestedAggregations);

    // add aggregations
    List<Aggregation> aggregationList = newArrayList();
    aggregationList.add(normalAggregation);
    aggregationList.add(internalNested);

    setField(aggregations, "aggregations", aggregationList);

    String expected =
        "{\"time\":123,\"maxScore\":0.0,\"totalCount\":56789,\"result\":{\""
            + INDEX_NAME
            + "\":[],\"facets\":{\"field\":{\"value\":10},\"facet.field\":{\"value\":10}}}}";

    assertEquals(
        expected,
        mapper.writeValueAsString(new SearchResponseEnvelope<>(INDEX_NAME, searchResponse)));
  }

  @Test
  public void shouldValidateResponseFromEsResponseSerializerWhenSearchReturnZeroResults()
      throws IOException {
    SearchResponse searchResponse = mock(SearchResponse.class);
    TimeValue took = mock(TimeValue.class);
    when(searchResponse.getTook()).thenReturn(took);
    when(took.getMillis()).thenReturn(123L);

    SearchHit[] hits = new SearchHit[0];
    SearchHits searchHits = new SearchHits(hits, 0, 0);
    when(searchResponse.getHits()).thenReturn(searchHits);

    // create aggregations
    Aggregations aggregations = mock(Aggregations.class);
    when(searchResponse.getAggregations()).thenReturn(aggregations);

    // create aggregation for normal fields with zero buckets
    InternalMappedTerms normalAggregation = mock(InternalMappedTerms.class);
    when(normalAggregation.getName()).thenReturn("field");
    when(normalAggregation.getBuckets()).thenReturn(newArrayList());

    // create aggregation for nested fields  with zero buckets
    InternalNested internalNested = mock(InternalNested.class);
    List<InternalAggregation> nestedAggregations = newArrayList();

    InternalMappedTerms internalAggregation = mock(InternalMappedTerms.class);
    when(internalAggregation.getName()).thenReturn("facet.field");
    nestedAggregations.add(internalAggregation);
    when(internalAggregation.getBuckets()).thenReturn(newArrayList());

    InternalAggregations internalAggregations = new InternalAggregations(nestedAggregations);
    when(internalNested.getAggregations()).thenReturn(internalAggregations);

    setField(internalAggregations, "aggregations", nestedAggregations);

    // add aggregations
    List<Aggregation> aggregationList = newArrayList();
    aggregationList.add(normalAggregation);
    aggregationList.add(internalNested);

    setField(aggregations, "aggregations", aggregationList);

    String expected =
        "{\"time\":123,\"maxScore\":0.0,\"totalCount\":0,\"result\":{\""
            + INDEX_NAME
            + "\":[],\"facets\":{\"field\":{},\"facet.field\":{}}}}";

    assertEquals(
        expected,
        mapper.writeValueAsString(new SearchResponseEnvelope<>(INDEX_NAME, searchResponse)));
  }

  @Test
  public void shouldReturnCursorIdOnResponse() throws IOException {
    SearchResponse searchResponse = mock(SearchResponse.class);
    TimeValue took = mock(TimeValue.class);
    when(searchResponse.getTook()).thenReturn(took);
    when(took.getMillis()).thenReturn(2L);

    SearchHit[] hits = new SearchHit[1];
    SearchHits searchHits = new SearchHits(hits, hits.length, 0);

    when(searchResponse.getHits()).thenReturn(searchHits);

    Map<String, Object> values = new LinkedHashMap<>();
    BytesReference source = new BytesArray("{\"id\":1}");
    values.put("_source", source);

    String _id = INDEX_NAME + "#1028071465";

    values.put(
        "sort",
        new SearchSortValues(
            new Object[] {0.23456, "A_B", _id},
            new DocValueFormat[] {DocValueFormat.RAW, DocValueFormat.RAW}));
    hits[0] = createFromMap(values);

    assertThat(
        mapper.writeValueAsString(new SearchResponseEnvelope<>(INDEX_NAME, searchResponse)),
        containsString("\"cursorId\":\"0.23456_A%5fB_" + _id + "\""));
  }
}

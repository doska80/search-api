package com.grupozap.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.DEFAULT_INDEX;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_FACET_SIZE;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.facetParserFixture;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.create;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.grupozap.search.api.model.search.Facetable;
import com.grupozap.search.api.service.parser.IndexSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.support.ValuesSourceAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;

public class FacetQueryAdapterTest extends SearchTransportClientMock {

  private static final int DEFAULT_SHARD_SIZE = 8;

  private final FacetQueryAdapter facetQueryAdapter;
  private final IndexSettings indexSettings;

  public FacetQueryAdapterTest() {
    indexSettings = mock(IndexSettings.class);
    when(indexSettings.getShards()).thenReturn(DEFAULT_SHARD_SIZE);

    facetQueryAdapter = new FacetQueryAdapter(facetParserFixture());
    facetQueryAdapter.setIndexSettings(indexSettings);

    ES_FACET_SIZE.setValue(DEFAULT_INDEX, 10);
    ES_FACET_SIZE.setValue(INDEX_NAME, 20);
  }

  private List<AggregationBuilder> simulateAggregationsApply(Set<String> facets) {
    Facetable request = create().index(INDEX_NAME).facets(facets).build();
    var searchSourceBuilder = new SearchSourceBuilder();
    facetQueryAdapter.apply(searchSourceBuilder, request);
    return new ArrayList<>(searchSourceBuilder.aggregations().getAggregatorFactories());
  }

  private long countNestedAggregations(List<AggregationBuilder> aggregations) {
    return aggregations.stream()
        .filter(aggregationBuilder -> aggregationBuilder instanceof NestedAggregationBuilder)
        .count();
  }

  private Set<String> getFieldFirstNames(Set<String> facets) {
    return facets.stream().map(s -> s.split("\\.|\\s")[0]).collect(toSet());
  }

  @Test
  public void shouldApplyFacetsOnlyForNonNestedFields() {
    Set<String> facets =
        newLinkedHashSet(
            newArrayList(
                "field1",
                "field2 sortFacet: _key",
                "field3 sortFacet: _count DESC",
                "field4 sortFacet: _key DESC"));

    Facetable request = create().index(INDEX_NAME).facets(facets).build();

    var searchSourceBuilder = new SearchSourceBuilder();
    facetQueryAdapter.apply(searchSourceBuilder, request);

    var aggregations = simulateAggregationsApply(facets);
    var nestedAggregations = countNestedAggregations(aggregations);
    var facetAsJson = searchSourceBuilder.toString();

    assertNotNull(aggregations);
    assertEquals(facets.size(), aggregations.size());
    assertEquals(0, nestedAggregations);
    assertEquals(aggregations.size(), aggregations.size() - nestedAggregations);
    assertEquals(facets.size(), countMatches(facetAsJson, "\"shard_size\":8"));
    assertEquals(
        facets.size(), countMatches(facetAsJson, "\"size\":" + ES_FACET_SIZE.getValue(INDEX_NAME)));
    assertTrue(
        getFieldFirstNames(facets)
            .containsAll(aggregations.stream().map(AggregationBuilder::getName).collect(toSet())));

    assertEquals("field1", ((TermsAggregationBuilder) aggregations.get(0)).field());
    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]", getSortAsString(aggregations.get(0)));

    assertEquals("field2", ((TermsAggregationBuilder) aggregations.get(1)).field());
    assertEquals("{\"_key\":\"asc\"}", getSortAsString(aggregations.get(1)));

    assertEquals("field3", ((TermsAggregationBuilder) aggregations.get(2)).field());
    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]", getSortAsString(aggregations.get(2)));

    assertEquals("field4", ((TermsAggregationBuilder) aggregations.get(3)).field());
    assertEquals("{\"_key\":\"desc\"}", getSortAsString(aggregations.get(3)));
  }

  @Test
  public void shouldApplyFacetsOnlyForNestedFields() {
    Set<String> facets =
        newLinkedHashSet(
            newArrayList(
                "nested1.field1",
                "nested2.field2 sortFacet: _key",
                "nested3.field3 sortFacet: _key DESC",
                "nested4.field4 sortFacet: _count",
                "nested5.field6 sortFacet: _count DESC"));

    Facetable request = create().index(INDEX_NAME).facets(facets).build();

    var searchSourceBuilder = new SearchSourceBuilder();
    facetQueryAdapter.apply(searchSourceBuilder, request);

    var aggregations = simulateAggregationsApply(facets);
    var nestedAggregations = countNestedAggregations(aggregations);
    var facetAsJson = searchSourceBuilder.toString();

    assertNotNull(aggregations);
    assertEquals(facets.size(), aggregations.size());
    assertEquals(5, nestedAggregations);
    assertEquals(0, aggregations.size() - nestedAggregations);

    assertEquals(facets.size(), countMatches(facetAsJson, "\"shard_size\":8"));

    assertEquals(
        facets.size(), countMatches(facetAsJson, "\"size\":" + ES_FACET_SIZE.getValue(INDEX_NAME)));

    var fieldsFirstNames = getFieldFirstNames(facets);

    assertTrue(
        fieldsFirstNames.containsAll(
            aggregations.stream().map(AggregationBuilder::getName).collect(toSet())));

    assertEquals("nested1", ((NestedAggregationBuilder) aggregations.get(0)).path());
    assertEquals(
        "nested1.field1",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(0).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(new ArrayList<>(aggregations.get(0).getSubAggregations()).get(0)));

    assertEquals("nested2", ((NestedAggregationBuilder) aggregations.get(1)).path());
    assertEquals(
        "nested2.field2",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(1).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "{\"_key\":\"asc\"}",
        getSortAsString(new ArrayList<>(aggregations.get(1).getSubAggregations()).get(0)));

    assertEquals("nested3", ((NestedAggregationBuilder) aggregations.get(2)).path());
    assertEquals(
        "nested3.field3",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(2).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "{\"_key\":\"desc\"}",
        getSortAsString(new ArrayList<>(aggregations.get(2).getSubAggregations()).get(0)));

    assertEquals("nested4", ((NestedAggregationBuilder) aggregations.get(3)).path());
    assertEquals(
        "nested4.field4",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(3).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "[{\"_count\":\"asc\"},{\"_key\":\"asc\"}]",
        getSortAsString(new ArrayList<>(aggregations.get(3).getSubAggregations()).get(0)));

    assertEquals("nested5", ((NestedAggregationBuilder) aggregations.get(4)).path());
    assertEquals(
        "nested5.field6",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(4).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(new ArrayList<>(aggregations.get(4).getSubAggregations()).get(0)));
  }

  @Test
  public void shouldApplyFacetsForNestedAndNotNestedFieldsUsingFacetSize() {
    Set<String> facets =
        newLinkedHashSet(
            newArrayList(
                "field1",
                "nested1.field2",
                "nested2.field3",
                "field4",
                "nested3.field5",
                "field6 sortFacet: _key",
                "field7 sortFacet: _count DESC",
                "field8 sortFacet: _key DESC",
                "nested4.field9 sortFacet: _key",
                "nested5.field10 sortFacet: _key DESC",
                "nested6.field11 sortFacet: _count",
                "nested7.field12 sortFacet: _count DESC"));

    var facetSize = 50;
    Facetable request = create().index(INDEX_NAME).facets(facets).facetSize(facetSize).build();

    var searchSourceBuilder = new SearchSourceBuilder();
    facetQueryAdapter.apply(searchSourceBuilder, request);

    var aggregations = simulateAggregationsApply(facets);
    var nestedAggregations = countNestedAggregations(aggregations);
    var facetAsJson = searchSourceBuilder.toString();

    assertNotNull(aggregations);
    assertEquals(12, aggregations.size());
    assertEquals(7, nestedAggregations);
    assertEquals(5, aggregations.size() - nestedAggregations);

    assertEquals(facets.size(), countMatches(facetAsJson, "\"shard_size\":" + DEFAULT_SHARD_SIZE));
    assertEquals(facets.size(), countMatches(facetAsJson, "\"size\":" + facetSize));
    var fieldsFirstNames = getFieldFirstNames(facets);
    assertTrue(
        fieldsFirstNames.containsAll(
            aggregations.stream().map(AggregationBuilder::getName).collect(toSet())));

    assertEquals("field1", ((TermsAggregationBuilder) aggregations.get(0)).field());
    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]", getSortAsString(aggregations.get(0)));

    assertEquals("nested1", ((NestedAggregationBuilder) aggregations.get(1)).path());
    assertEquals(
        "nested1.field2",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(1).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(new ArrayList<>(aggregations.get(1).getSubAggregations()).get(0)));

    assertEquals("nested2", ((NestedAggregationBuilder) aggregations.get(2)).path());
    assertEquals(
        "nested2.field3",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(2).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(new ArrayList<>(aggregations.get(2).getSubAggregations()).get(0)));

    assertEquals("field4", ((TermsAggregationBuilder) aggregations.get(3)).field());
    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]", getSortAsString(aggregations.get(3)));

    assertEquals("nested3", ((NestedAggregationBuilder) aggregations.get(4)).path());
    assertEquals(
        "nested3.field5",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(4).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(new ArrayList<>(aggregations.get(4).getSubAggregations()).get(0)));

    assertEquals("field6", ((TermsAggregationBuilder) aggregations.get(5)).field());
    assertEquals("{\"_key\":\"asc\"}", getSortAsString(aggregations.get(5)));

    assertEquals("field7", ((TermsAggregationBuilder) aggregations.get(6)).field());
    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]", getSortAsString(aggregations.get(6)));

    assertEquals("field8", ((TermsAggregationBuilder) aggregations.get(7)).field());
    assertEquals("{\"_key\":\"desc\"}", getSortAsString(aggregations.get(7)));

    assertEquals("nested4", ((NestedAggregationBuilder) aggregations.get(8)).path());
    assertEquals(
        "nested4.field9",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(8).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "{\"_key\":\"asc\"}",
        getSortAsString(new ArrayList<>(aggregations.get(8).getSubAggregations()).get(0)));

    assertEquals("nested5", ((NestedAggregationBuilder) aggregations.get(9)).path());
    assertEquals(
        "nested5.field10",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(9).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "{\"_key\":\"desc\"}",
        getSortAsString(new ArrayList<>(aggregations.get(9).getSubAggregations()).get(0)));

    assertEquals("nested6", ((NestedAggregationBuilder) aggregations.get(10)).path());
    assertEquals(
        "nested6.field11",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(10).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "[{\"_count\":\"asc\"},{\"_key\":\"asc\"}]",
        getSortAsString(new ArrayList<>(aggregations.get(10).getSubAggregations()).get(0)));

    assertEquals("nested7", ((NestedAggregationBuilder) aggregations.get(11)).path());
    assertEquals(
        "nested7.field12",
        ((ValuesSourceAggregationBuilder)
                new ArrayList<>(aggregations.get(11).getSubAggregations()).get(0))
            .field());
    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(new ArrayList<>(aggregations.get(11).getSubAggregations()).get(0)));
  }

  private String getSortAsString(AggregationBuilder aggregationBuilder) {
    return ((TermsAggregationBuilder) aggregationBuilder).order().toString();
  }
}

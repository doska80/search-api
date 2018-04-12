package com.vivareal.search.api.adapter;

import static com.google.common.collect.Sets.newHashSet;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.DEFAULT_INDEX;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_FACET_SIZE;
import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.facetParserFixture;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.create;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.elasticsearch.common.settings.Settings.EMPTY;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.search.Facetable;
import com.vivareal.search.api.service.parser.IndexSettings;
import java.util.List;
import java.util.Set;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.transport.MockTransportClient;
import org.junit.Test;

public class FacetQueryAdapterTest extends SearchTransportClientMock {

  private static int DEFAULT_SHARD_SIZE = 8;

  private FacetQueryAdapter facetQueryAdapter;
  private IndexSettings indexSettings;

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

    ESClient esClient = new ESClient(new MockTransportClient(EMPTY));
    SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch((BaseApiRequest) request);
    facetQueryAdapter.apply(searchRequestBuilder, request);

    return searchRequestBuilder.request().source().aggregations().getAggregatorFactories();
  }

  private long countNestedAggregations(List<AggregationBuilder> aggregations) {
    return aggregations
        .stream()
        .filter(aggregationBuilder -> aggregationBuilder instanceof NestedAggregationBuilder)
        .count();
  }

  private Set<String> getFieldFirstNames(Set<String> facets) {
    return facets.stream().map(s -> s.split("\\.|\\s")[0]).collect(toSet());
  }

  @Test
  public void shouldApplyFacetsOnlyForNonNestedFields() {
    Set<String> facets =
        newHashSet(
            "field1",
            "field2 sortFacet: _key",
            "field3 sortFacet: _count DESC",
            "field4 sortFacet: _key DESC");

    Facetable request = create().index(INDEX_NAME).facets(facets).build();

    ESClient esClient = new ESClient(new MockTransportClient(EMPTY));
    SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch((BaseApiRequest) request);
    facetQueryAdapter.apply(searchRequestBuilder, request);

    List<AggregationBuilder> aggregations = simulateAggregationsApply(facets);
    long nestedAggregations = countNestedAggregations(aggregations);
    String facetAsJson = searchRequestBuilder.toString();

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

    assertEquals(
        "{\"_key\":\"asc\"}", getSortAsString(aggregations.get(0))); // field2 sortFacet: _key

    assertEquals(
        "{\"_key\":\"desc\"}", getSortAsString(aggregations.get(1))); // field4 sortFacet: _key DESC

    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(aggregations.get(2))); // field1

    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(aggregations.get(3))); // field3 sortFacet: _count DESC
  }

  @Test
  public void shouldApplyFacetsOnlyForNestedFields() {
    Set<String> facets =
        newHashSet(
            "nested1.field1",
            "nested2.field2 sortFacet: _key",
            "nested3.field3 sortFacet: _key DESC",
            "nested4.field4 sortFacet: _count",
            "nested5.field6 sortFacet: _count DESC");

    Facetable request = create().index(INDEX_NAME).facets(facets).build();

    ESClient esClient = new ESClient(new MockTransportClient(EMPTY));
    SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch((BaseApiRequest) request);
    facetQueryAdapter.apply(searchRequestBuilder, request);

    List<AggregationBuilder> aggregations = simulateAggregationsApply(facets);
    long nestedAggregations = countNestedAggregations(aggregations);
    String facetAsJson = searchRequestBuilder.toString();

    assertNotNull(aggregations);
    assertEquals(facets.size(), aggregations.size());
    assertEquals(5, nestedAggregations);
    assertEquals(0, aggregations.size() - nestedAggregations);

    assertEquals(facets.size(), countMatches(facetAsJson, "\"shard_size\":8"));

    assertEquals(
        facets.size(), countMatches(facetAsJson, "\"size\":" + ES_FACET_SIZE.getValue(INDEX_NAME)));

    Set<String> fieldsFirstNames = getFieldFirstNames(facets);

    assertTrue(
        fieldsFirstNames.containsAll(
            aggregations.stream().map(AggregationBuilder::getName).collect(toSet())));

    assertEquals(
        "{\"_key\":\"asc\"}",
        getSortAsString(
            aggregations.get(0).getSubAggregations().get(0))); // nested2.field2 sortFacet: _key

    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(
            aggregations
                .get(1)
                .getSubAggregations()
                .get(0))); // nested5.field6 sortFacet: _count DESC

    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(aggregations.get(2).getSubAggregations().get(0))); // nested1.field1

    assertEquals(
        "[{\"_count\":\"asc\"},{\"_key\":\"asc\"}]",
        getSortAsString(
            aggregations.get(3).getSubAggregations().get(0))); // nested4.field4 sortFacet: _count

    assertEquals(
        "{\"_key\":\"desc\"}",
        getSortAsString(
            aggregations
                .get(4)
                .getSubAggregations()
                .get(0))); // nested3.field3 sortFacet: _key DESC
  }

  @Test
  public void shouldApplyFacetsForNestedAndNotNestedFieldsUsingFacetSize() {
    Set<String> facets =
        newHashSet(
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
            "nested7.field12 sortFacet: _count DESC");

    int facetSize = 50;
    Facetable request = create().index(INDEX_NAME).facets(facets).facetSize(facetSize).build();

    ESClient esClient = new ESClient(new MockTransportClient(EMPTY));
    SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch((BaseApiRequest) request);
    facetQueryAdapter.apply(searchRequestBuilder, request);

    List<AggregationBuilder> aggregations = simulateAggregationsApply(facets);
    long nestedAggregations = countNestedAggregations(aggregations);
    String facetAsJson = searchRequestBuilder.toString();

    assertNotNull(aggregations);
    assertEquals(12, aggregations.size());
    assertEquals(7, nestedAggregations);
    assertEquals(5, aggregations.size() - nestedAggregations);

    assertEquals(facets.size(), countMatches(facetAsJson, "\"shard_size\":" + DEFAULT_SHARD_SIZE));
    assertEquals(facets.size(), countMatches(facetAsJson, "\"size\":" + facetSize));
    Set<String> fieldsFirstNames = getFieldFirstNames(facets);
    assertTrue(
        fieldsFirstNames.containsAll(
            aggregations.stream().map(AggregationBuilder::getName).collect(toSet())));

    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(aggregations.get(0))); // field1

    assertEquals(
        "{\"_key\":\"desc\"}",
        getSortAsString(
            aggregations
                .get(1)
                .getSubAggregations()
                .get(0))); // nested5.field10 sortFacet: _key DESC

    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(aggregations.get(2).getSubAggregations().get(0))); // nested1.field2

    assertEquals(
        "[{\"_count\":\"asc\"},{\"_key\":\"asc\"}]",
        getSortAsString(
            aggregations.get(3).getSubAggregations().get(0))); // nested6.field11 sortFacet: _count

    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(aggregations.get(4).getSubAggregations().get(0))); // nested2.field3

    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(aggregations.get(5).getSubAggregations().get(0))); // nested3.field5

    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(aggregations.get(6))); // field7 sortFacet: _count DESC

    assertEquals(
        "{\"_key\":\"desc\"}", getSortAsString(aggregations.get(7))); // field8 sortFacet: _key DESC

    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(
            aggregations
                .get(8)
                .getSubAggregations()
                .get(0))); // nested7.field12 sortFacet: _count DESC

    assertEquals(
        "{\"_key\":\"asc\"}",
        getSortAsString(
            aggregations.get(9).getSubAggregations().get(0))); // nested4.field9 sortFacet: _key

    assertEquals(
        "{\"_key\":\"asc\"}", getSortAsString(aggregations.get(10))); // field6 sortFacet: _key

    assertEquals(
        "[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]",
        getSortAsString(aggregations.get(11))); // field4
  }

  private String getSortAsString(AggregationBuilder aggregationBuilder) {
    return ((TermsAggregationBuilder) aggregationBuilder).order().toString();
  }
}

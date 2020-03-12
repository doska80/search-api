package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.DEFAULT_INDEX;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_RFQ;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.RankFeatureQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.junit.Before;
import org.junit.Test;

public class RankFeatureQueryAdapterTest extends SearchTransportClientMock {

  private RankFeatureQueryAdapter queryAdapter = new RankFeatureQueryAdapter();

  @Before
  public void setup() {
    ES_RFQ.setValue(DEFAULT_INDEX, null);
  }

  @Test
  public void
      shouldApplyTheRankFeatureQueryIfIsConfiguredOnSearchApiPropertiesAndTheSortStartsWithPriority() {

    ES_RFQ.setValue(DEFAULT_INDEX, "field1:4");

    var queryBuilder = new BoolQueryBuilder();
    queryBuilder.filter().add(termQuery("id", 1));
    queryAdapter.apply(queryBuilder, filterableRequest.sort("priority_x").build());

    assertEquals(1, queryBuilder.should().size());
    assertEquals(RankFeatureQueryBuilder.class, queryBuilder.should().get(0).getClass());

    assertEquals(1, queryBuilder.filter().size());
    assertEquals(TermQueryBuilder.class, queryBuilder.filter().get(0).getClass());

    assertEquals(
        "{\"rank_feature\":{\"field\":\"field1\",\"log\":{\"scaling_factor\":4.0},\"boost\":1.0}}",
        queryBuilder.should().get(0).toString().replaceAll("[\n|\\s]", ""));
  }

  @Test
  public void shouldApplyTheRankFeatureQueryIfIsConfiguredOnSearchApiProperties() {

    ES_RFQ.setValue(DEFAULT_INDEX, "field1:4");

    var queryBuilder = new BoolQueryBuilder();
    queryBuilder.filter().add(termQuery("id", 1));
    queryAdapter.apply(queryBuilder, filterableRequest.disableRfq(false).build());

    assertEquals(1, queryBuilder.should().size());
    assertEquals(RankFeatureQueryBuilder.class, queryBuilder.should().get(0).getClass());

    assertEquals(1, queryBuilder.filter().size());
    assertEquals(TermQueryBuilder.class, queryBuilder.filter().get(0).getClass());

    assertEquals(
        "{\"rank_feature\":{\"field\":\"field1\",\"log\":{\"scaling_factor\":4.0},\"boost\":1.0}}",
        queryBuilder.should().get(0).toString().replaceAll("[\n|\\s]", ""));
  }

  @Test
  public void
      shouldApplyTheRFQIfIsConfiguredOnSearchApiPropertiesAndIncludeTheMatchAllQueryWithoutAnyFilter() {

    ES_RFQ.setValue(DEFAULT_INDEX, "field1:4");

    var queryBuilder = new BoolQueryBuilder();
    queryAdapter.apply(queryBuilder, filterableRequest.disableRfq(false).build());

    assertEquals(1, queryBuilder.should().size());
    assertEquals(RankFeatureQueryBuilder.class, queryBuilder.should().get(0).getClass());

    assertEquals(1, queryBuilder.filter().size());
    assertEquals(MatchAllQueryBuilder.class, queryBuilder.filter().get(0).getClass());

    assertEquals(
        "{\"rank_feature\":{\"field\":\"field1\",\"log\":{\"scaling_factor\":4.0},\"boost\":1.0}}",
        queryBuilder.should().get(0).toString().replaceAll("[\n|\\s]", ""));
  }

  @Test
  public void shouldNotApplyTheRFQWhenDisabledOnRequest() {

    ES_RFQ.setValue(DEFAULT_INDEX, "field1:4");

    var queryBuilder = new BoolQueryBuilder();
    queryAdapter.apply(queryBuilder, filterableRequest.disableRfq(true).build());

    assertTrue(queryBuilder.should().isEmpty());
    assertTrue(queryBuilder.filter().isEmpty());
  }

  @Test
  public void shouldNotApplyTheRFQWhenNotExplicitEnabledOnRequest() {

    ES_RFQ.setValue(DEFAULT_INDEX, "field1:4");

    var queryBuilder = new BoolQueryBuilder();
    queryAdapter.apply(queryBuilder, filterableRequest.build());

    assertTrue(queryBuilder.should().isEmpty());
    assertTrue(queryBuilder.filter().isEmpty());
  }

  @Test
  public void shouldNotApplyTheRankFeatureQueryWhenEmptyPropertyOnSearchApiProperties() {

    ES_RFQ.setValue(DEFAULT_INDEX, "");

    var queryBuilder = new BoolQueryBuilder();
    queryAdapter.apply(queryBuilder, filterableRequest.build());

    assertTrue(queryBuilder.should().isEmpty());
    assertTrue(queryBuilder.filter().isEmpty());
  }

  @Test
  public void shouldNotApplyTheRankFeatureQueryWhenNullPropertyOnSearchApiProperties() {
    var queryBuilder = new BoolQueryBuilder();
    queryAdapter.apply(queryBuilder, filterableRequest.build());

    assertTrue(queryBuilder.should().isEmpty());
    assertTrue(queryBuilder.filter().isEmpty());
  }
}

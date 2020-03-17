package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.grupozap.search.api.model.listener.rfq.Rfq.build;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.grupozap.search.api.listener.ESSortListener;
import com.grupozap.search.api.model.listener.rfq.Rfq;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.RankFeatureQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.junit.Test;

public class RankFeatureQueryAdapterTest extends SearchTransportClientMock {

  private ESSortListener esSortListener = mock(ESSortListener.class);
  private RankFeatureQueryAdapter queryAdapter = new RankFeatureQueryAdapter(esSortListener);

  @Test
  public void shouldApplyTheRankFeatureQueryIfConfiguredOnSearchApiProperties() {

    var request = filterableRequest.index(INDEX_NAME).build();

    when(this.esSortListener.getRfq(request)).thenReturn(buildRfq());

    var queryBuilder = new BoolQueryBuilder();
    queryBuilder.filter().add(termQuery("id", 1));
    queryAdapter.apply(queryBuilder, request);

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
      shouldApplyTheRFQIfConfiguredOnSearchApiPropertiesAndIncludeTheMatchAllQueryWhenFilterIsEmpty() {

    var request = filterableRequest.index(INDEX_NAME).disableRfq(false).build();

    when(this.esSortListener.getRfq(request)).thenReturn(buildRfq());

    var queryBuilder = new BoolQueryBuilder();
    queryAdapter.apply(queryBuilder, request);

    assertEquals(1, queryBuilder.should().size());
    assertEquals(RankFeatureQueryBuilder.class, queryBuilder.should().get(0).getClass());

    assertEquals(1, queryBuilder.filter().size());
    assertEquals(MatchAllQueryBuilder.class, queryBuilder.filter().get(0).getClass());

    assertEquals(
        "{\"rank_feature\":{\"field\":\"field1\",\"log\":{\"scaling_factor\":4.0},\"boost\":1.0}}",
        queryBuilder.should().get(0).toString().replaceAll("[\n|\\s]", ""));
  }

  @Test
  public void shouldNotApplyTheRFQWhenNotConfiguredOnSearchApiProperties() {

    var request = filterableRequest.index(INDEX_NAME).disableRfq(false).build();

    when(this.esSortListener.getRfq(request)).thenReturn(empty());

    var queryBuilder = new BoolQueryBuilder();
    queryAdapter.apply(queryBuilder, request);

    assertTrue(queryBuilder.should().isEmpty());
    assertTrue(queryBuilder.filter().isEmpty());
  }

  @SuppressWarnings("ConstantConditions")
  private Optional<Rfq> buildRfq() {
    var map = new LinkedHashMap<String, Object>();
    map.put("field", "field1");
    map.put("function", "log");
    map.put("boost", 0.0);
    map.put("scaling_factor", (float) 4.0);
    map.put("pivot", 0.0);
    map.put("exponent", 0.0);

    var entryValue = new LinkedHashMap<String, Map<String, Object>>();
    entryValue.put("rfq", map);

    return of(build(entryValue));
  }
}

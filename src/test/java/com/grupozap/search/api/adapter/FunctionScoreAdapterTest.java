package com.grupozap.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.grupozap.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.*;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.*;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier.NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import com.grupozap.search.api.model.mapping.MappingType;
import com.grupozap.search.api.service.parser.factory.DefaultFilterFactory;
import java.util.LinkedList;
import java.util.stream.Stream;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class FunctionScoreAdapterTest extends SearchTransportClientMock {

  private static final String DEFAULT_SCORE_FACTOR_FIELD = "factorField";

  private QueryAdapter<GetRequest, SearchRequest> queryAdapter;

  @Mock private ElasticsearchSettingsAdapter settingsAdapter;

  @Mock private SearchAfterQueryAdapter searchAfterQueryAdapter;

  @Mock private SortQueryAdapter sortQueryAdapter;

  @Before
  public void setup() {
    initMocks(this);

    QS_MM.setValue(INDEX_NAME, "75%");
    QS_DEFAULT_FIELDS.setValue(INDEX_NAME, newArrayList("field", "field1"));
    SOURCE_INCLUDES.setValue(INDEX_NAME, new LinkedList<>());
    SOURCE_EXCLUDES.setValue(INDEX_NAME, new LinkedList<>());
    ES_QUERY_TIMEOUT_VALUE.setValue(INDEX_NAME, 100);
    ES_QUERY_TIMEOUT_UNIT.setValue(INDEX_NAME, "MILLISECONDS");
    ES_DEFAULT_SIZE.setValue(INDEX_NAME, 20);
    ES_MAX_SIZE.setValue(INDEX_NAME, 200);
    ES_FACET_SIZE.setValue(INDEX_NAME, 20);
    ES_MAPPING_META_FIELDS_ID.setValue(INDEX_NAME, "id");
    SCORE_FACTOR_FIELD.setValue(INDEX_NAME, DEFAULT_SCORE_FACTOR_FIELD);

    PageQueryAdapter pageQueryAdapter = new PageQueryAdapter();
    QueryStringAdapter queryStringAdapter = new QueryStringAdapter(fieldCacheFixture());
    FunctionScoreAdapter functionScoreAdapter = new FunctionScoreAdapter(fieldParserFixture());
    FacetQueryAdapter facetQueryAdapter = new FacetQueryAdapter(facetParserFixture());
    FilterQueryAdapter filterQueryAdapter = new FilterQueryAdapter(queryParserFixture());
    DefaultFilterFactory defaultFilterFactory =
        new DefaultFilterFactory(queryParserWithOutValidationFixture(), filterQueryAdapter);

    this.queryAdapter =
        new ElasticsearchQueryAdapter(
            mock(SourceFieldAdapter.class),
            pageQueryAdapter,
            searchAfterQueryAdapter,
            sortQueryAdapter,
            queryStringAdapter,
            functionScoreAdapter,
            queryParserFixture(),
            filterQueryAdapter,
            defaultFilterFactory,
            facetQueryAdapter);

    doNothing().when(settingsAdapter).checkIndex(any());
    doNothing().when(searchAfterQueryAdapter).apply(any(), any());
    doNothing().when(sortQueryAdapter).apply(any(), any());

    when(settingsAdapter.settingsByKey(INDEX_NAME, SHARDS)).thenReturn("8");
    when(settingsAdapter.isTypeOf(anyString(), anyString(), any(MappingType.class)))
        .thenReturn(false);
  }

  @After
  public void closeClient() {
    SCORE_FACTOR_FIELD.setValue(INDEX_NAME, null);
  }

  @Test
  public void shouldReturnSearchRequestByQueryStringWithDefaultFactorField() {
    SearchRequest searchRequest = queryAdapter.query(fullRequest.build());

    assertEquals(FunctionScoreQueryBuilder.class, searchRequest.source().query().getClass());

    FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders =
        ((FunctionScoreQueryBuilder) searchRequest.source().query()).filterFunctionBuilders();

    assertTrue(filterFunctionBuilders.length == 1);

    assertEquals(
        DEFAULT_SCORE_FACTOR_FIELD,
        ((FieldValueFactorFunctionBuilder) filterFunctionBuilders[0].getScoreFunction())
            .fieldName());
    assertEquals(
        NONE,
        ((FieldValueFactorFunctionBuilder) filterFunctionBuilders[0].getScoreFunction())
            .modifier());
  }

  @Test
  public void shouldReturnSearchRequestByQueryStringWithFactorField() {
    String field = "myFactorField";
    SearchRequest searchRequest = queryAdapter.query(fullRequest.factorField(field).build());

    assertEquals(FunctionScoreQueryBuilder.class, searchRequest.source().query().getClass());

    FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders =
        ((FunctionScoreQueryBuilder) searchRequest.source().query()).filterFunctionBuilders();

    assertTrue(filterFunctionBuilders.length == 1);

    assertEquals(
        field,
        ((FieldValueFactorFunctionBuilder) filterFunctionBuilders[0].getScoreFunction())
            .fieldName());
  }

  @Test
  public void shouldReturnSearchRequestByQueryStringWithFactorModifier() {
    Stream.of(FieldValueFactorFunction.Modifier.values())
        .forEach(
            modifier -> {
              SearchRequest searchRequest =
                  queryAdapter.query(
                      fullRequest
                          .factorField("myFactorField")
                          .factorModifier(modifier.toString())
                          .build());
              FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders =
                  ((FunctionScoreQueryBuilder) searchRequest.source().query())
                      .filterFunctionBuilders();

              assertEquals(
                  modifier,
                  ((FieldValueFactorFunctionBuilder) filterFunctionBuilders[0].getScoreFunction())
                      .modifier());
            });
  }
}

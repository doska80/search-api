package com.grupozap.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.grupozap.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SIZE;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_FACET_SIZE;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_MAPPING_META_FIELDS_ID;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_MAX_SIZE;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_QUERY_TIMEOUT_UNIT;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_QUERY_TIMEOUT_VALUE;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.QS_DEFAULT_FIELDS;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.QS_MM;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.SCORE_FACTOR_FIELD;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.SOURCE_EXCLUDES;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.SOURCE_INCLUDES;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.facetParserFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldCacheFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserWithOutValidationFixture;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier.NONE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

  @Mock private SortQueryAdapter sortQueryAdapter;

  @Mock private RankFeatureQueryAdapter rankFeatureQueryAdapter;

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

    var pageQueryAdapter = new PageQueryAdapter();
    var queryStringAdapter = new QueryStringAdapter(fieldCacheFixture());
    var functionScoreAdapter = new FunctionScoreAdapter(fieldParserFixture());
    var facetQueryAdapter = new FacetQueryAdapter(facetParserFixture());
    var filterQueryAdapter = new FilterQueryAdapter(queryParserFixture());
    var defaultFilterFactory =
        new DefaultFilterFactory(queryParserWithOutValidationFixture(), filterQueryAdapter);

    this.queryAdapter =
        new ElasticsearchQueryAdapter(
            mock(SourceFieldAdapter.class),
            pageQueryAdapter,
            sortQueryAdapter,
            queryStringAdapter,
            functionScoreAdapter,
            queryParserFixture(),
            filterQueryAdapter,
            defaultFilterFactory,
            facetQueryAdapter,
            rankFeatureQueryAdapter,
            false);

    doNothing().when(settingsAdapter).checkIndex(any());
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
    var searchRequest = queryAdapter.query(fullRequest.build());

    assertEquals(FunctionScoreQueryBuilder.class, searchRequest.source().query().getClass());

    var filterFunctionBuilders =
        ((FunctionScoreQueryBuilder) searchRequest.source().query()).filterFunctionBuilders();

    assertEquals(1, filterFunctionBuilders.length);

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
    var field = "myFactorField";
    var searchRequest = queryAdapter.query(fullRequest.factorField(field).build());

    assertEquals(FunctionScoreQueryBuilder.class, searchRequest.source().query().getClass());

    var filterFunctionBuilders =
        ((FunctionScoreQueryBuilder) searchRequest.source().query()).filterFunctionBuilders();

    assertEquals(1, filterFunctionBuilders.length);

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
              var searchRequest =
                  queryAdapter.query(
                      fullRequest
                          .factorField("myFactorField")
                          .factorModifier(modifier.toString())
                          .build());
              var filterFunctionBuilders =
                  ((FunctionScoreQueryBuilder) searchRequest.source().query())
                      .filterFunctionBuilders();

              assertEquals(
                  modifier,
                  ((FieldValueFactorFunctionBuilder) filterFunctionBuilders[0].getScoreFunction())
                      .modifier());
            });
  }
}

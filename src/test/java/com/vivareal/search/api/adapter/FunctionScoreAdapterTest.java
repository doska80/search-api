package com.vivareal.search.api.adapter;

import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier.NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.vivareal.search.api.model.mapping.MappingType;
import com.vivareal.search.api.model.parser.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class FunctionScoreAdapterTest extends SearchTransportClientMock {

  private QueryAdapter<GetRequestBuilder, SearchRequestBuilder> queryAdapter;

  @Mock private ElasticsearchSettingsAdapter settingsAdapter;

  @Mock private SearchAfterQueryAdapter searchAfterQueryAdapter;

  @Mock private SortQueryAdapter sortQueryAdapter;

  @Before
  public void setup() {
    initMocks(this);

    QS_MM.setValue(INDEX_NAME, "75%");
    QS_DEFAULT_FIELDS.setValue(INDEX_NAME, "field,field1");
    SOURCE_INCLUDES.setValue(INDEX_NAME, "");
    SOURCE_EXCLUDES.setValue(INDEX_NAME, "");
    ES_QUERY_TIMEOUT_VALUE.setValue(INDEX_NAME, "100");
    ES_QUERY_TIMEOUT_UNIT.setValue(INDEX_NAME, "MILLISECONDS");
    ES_DEFAULT_SIZE.setValue(INDEX_NAME, "20");
    ES_MAX_SIZE.setValue(INDEX_NAME, "200");
    ES_FACET_SIZE.setValue(INDEX_NAME, "20");
    ES_MAPPING_META_FIELDS_ID.setValue(INDEX_NAME, "id");

    ESClient esClient = new ESClient(transportClient);
    SourceFieldAdapter sourceFieldAdapter = new SourceFieldAdapter(settingsAdapter);

    when(settingsAdapter.getFetchSourceIncludeFields(any())).thenCallRealMethod();
    when(settingsAdapter.getFetchSourceExcludeFields(any(), any())).thenCallRealMethod();

    OperatorParser operatorParser = new OperatorParser();
    NotParser notParser = new NotParser();
    FieldParser fieldParser = new FieldParser(notParser);
    FilterParser filterParser = new FilterParser(fieldParser, operatorParser, new ValueParser());
    QueryParser queryParser = new QueryParser(operatorParser, filterParser, notParser);
    FacetParser facetParser = new FacetParser(fieldParser);
    PageQueryAdapter pageQueryAdapter = new PageQueryAdapter();
    QueryStringAdapter queryStringAdapter = new QueryStringAdapter(settingsAdapter);
    FunctionScoreAdapter functionScoreAdapter = new FunctionScoreAdapter(settingsAdapter);
    FacetQueryAdapter facetQueryAdapter = new FacetQueryAdapter(settingsAdapter, facetParser);
    FilterQueryAdapter filterQueryAdapter = new FilterQueryAdapter(settingsAdapter, queryParser);
    this.queryAdapter =
        new ElasticsearchQueryAdapter(
            esClient,
            settingsAdapter,
            sourceFieldAdapter,
            pageQueryAdapter,
            searchAfterQueryAdapter,
            sortQueryAdapter,
            queryStringAdapter,
            functionScoreAdapter,
            filterQueryAdapter,
            facetQueryAdapter);

    Map<String, String[]> defaultSourceFields = new HashMap<>();
    defaultSourceFields.put(INDEX_NAME, new String[0]);

    setField(settingsAdapter, "defaultSourceIncludes", defaultSourceFields);
    setField(settingsAdapter, "defaultSourceExcludes", defaultSourceFields);

    doNothing().when(settingsAdapter).checkIndex(any());
    doNothing().when(searchAfterQueryAdapter).apply(any(), any());
    doNothing().when(sortQueryAdapter).apply(any(), any());

    when(settingsAdapter.settingsByKey(INDEX_NAME, SHARDS)).thenReturn("8");
    when(settingsAdapter.isTypeOf(anyString(), anyString(), any(MappingType.class)))
        .thenReturn(false);
  }

  @After
  public void closeClient() {
    this.transportClient.close();
  }

  @Test
  public void shouldReturnSearchRequestBuilderByQueryStringWithFactorField() {
    String field = "myFactorField";
    SearchRequestBuilder searchRequestBuilder =
        queryAdapter.query(fullRequest.factorField(field).build());

    assertEquals(
        FunctionScoreQueryBuilder.class,
        searchRequestBuilder.request().source().query().getClass());

    FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders =
        ((FunctionScoreQueryBuilder) searchRequestBuilder.request().source().query())
            .filterFunctionBuilders();

    assertTrue(filterFunctionBuilders.length == 1);

    assertEquals(
        field,
        ((FieldValueFactorFunctionBuilder) filterFunctionBuilders[0].getScoreFunction())
            .fieldName());
    assertEquals(
        NONE,
        ((FieldValueFactorFunctionBuilder) filterFunctionBuilders[0].getScoreFunction())
            .modifier());
  }

  @Test
  public void shouldReturnSearchRequestBuilderByQueryStringWithFactorModifier() {
    Stream.of(FieldValueFactorFunction.Modifier.values())
        .forEach(
            modifier -> {
              SearchRequestBuilder searchRequestBuilder =
                  queryAdapter.query(
                      fullRequest
                          .factorField("myFactorField")
                          .factorModifier(modifier.toString())
                          .build());
              FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders =
                  ((FunctionScoreQueryBuilder) searchRequestBuilder.request().source().query())
                      .filterFunctionBuilders();

              assertEquals(
                  modifier,
                  ((FieldValueFactorFunctionBuilder) filterFunctionBuilders[0].getScoreFunction())
                      .modifier());
            });
  }
}

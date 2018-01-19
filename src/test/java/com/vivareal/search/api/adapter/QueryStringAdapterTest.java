package com.vivareal.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_STRING;
import static org.elasticsearch.index.query.Operator.OR;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.google.common.collect.Sets;
import com.vivareal.search.api.model.mapping.MappingType;
import com.vivareal.search.api.model.parser.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.util.Lists;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class QueryStringAdapterTest extends SearchTransportClientMock {

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
  public void shouldReturnSimpleSearchRequestBuilderByQueryString() {
    String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              SearchRequestBuilder searchRequestBuilder = queryAdapter.query(request.q(q).build());
              MultiMatchQueryBuilder multiMatchQueryBuilder =
                  (MultiMatchQueryBuilder)
                      ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                          .must()
                          .get(0);

              assertNotNull(multiMatchQueryBuilder);
              assertEquals(q, multiMatchQueryBuilder.value());
              assertEquals(OR, multiMatchQueryBuilder.operator());
            });
  }

  @Test
  public void shouldReturnSimpleSearchRequestBuilderByQueryStringWithSpecifiedFieldToSearch() {
    String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";

    String fieldName1 = "field1";
    float boostValue1 = 1.0f; // default boost value

    String fieldName2 = "field2";
    float boostValue2 = 2.0f;

    String fieldName3 = "field3";
    float boostValue3 = 5.0f;

    when(settingsAdapter.isTypeOf(INDEX_NAME, fieldName1, FIELD_TYPE_STRING)).thenReturn(true);
    when(settingsAdapter.isTypeOf(INDEX_NAME, fieldName2, FIELD_TYPE_STRING)).thenReturn(false);
    when(settingsAdapter.isTypeOf(INDEX_NAME, fieldName3, FIELD_TYPE_STRING)).thenReturn(false);

    Set<String> fields =
        Sets.newLinkedHashSet(
            newArrayList(
                String.format("%s", fieldName1),
                String.format("%s:%s", fieldName2, boostValue2),
                String.format("%s:%s", fieldName3, boostValue3)));

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              SearchRequestBuilder searchRequestBuilder =
                  queryAdapter.query(request.q(q).fields(fields).build());
              MultiMatchQueryBuilder multiMatchQueryBuilder =
                  (MultiMatchQueryBuilder)
                      ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                          .must()
                          .get(0);

              assertNotNull(multiMatchQueryBuilder);
              assertEquals(q, multiMatchQueryBuilder.value());

              Map<String, Float> fieldsAndWeights = new HashMap<>(3);
              fieldsAndWeights.put(fieldName1, boostValue1);
              fieldsAndWeights.put(fieldName2, boostValue2);
              fieldsAndWeights.put(fieldName3, boostValue3);

              assertTrue(fieldsAndWeights.equals(multiMatchQueryBuilder.fields()));
            });
  }

  @Test
  public void shouldReturnSearchRequestBuilderByQueryStringWithValidMinimalShouldMatch() {
    String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
    List<String> validMMs = Lists.newArrayList("-100%", "100%", "75%", "-2");

    validMMs.forEach(
        mm ->
            newArrayList(filterableRequest, fullRequest)
                .parallelStream()
                .forEach(
                    request -> {
                      SearchRequestBuilder searchRequestBuilder =
                          queryAdapter.query(request.q(q).mm(mm).build());
                      MultiMatchQueryBuilder multiMatchQueryBuilder =
                          (MultiMatchQueryBuilder)
                              ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                                  .must()
                                  .get(0);

                      assertNotNull(multiMatchQueryBuilder);
                      assertEquals(q, multiMatchQueryBuilder.value());
                      assertEquals(mm, multiMatchQueryBuilder.minimumShouldMatch());
                      assertEquals(OR, multiMatchQueryBuilder.operator());
                    }));
  }
}

package com.grupozap.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.grupozap.search.api.model.query.OrderOperator.DESC;
import static com.grupozap.search.api.utils.EsSortUtils.SortType.*;
import static org.elasticsearch.common.lucene.search.function.FunctionScoreQuery.ScoreMode.SUM;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.script.ScriptType.STORED;
import static org.elasticsearch.search.rescore.QueryRescoreMode.fromString;
import static org.elasticsearch.search.sort.ScriptSortBuilder.ScriptSortType.NUMBER;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.grupozap.search.api.exception.RescoreConjunctionSortException;
import com.grupozap.search.api.listener.ESSortListener;
import com.grupozap.search.api.model.listener.SearchSort;
import com.grupozap.search.api.model.listener.rescore.LtrRescore;
import com.grupozap.search.api.model.parser.OperatorParser;
import com.grupozap.search.api.model.parser.SortParser;
import com.grupozap.search.api.model.parser.ValueParser;
import com.grupozap.search.api.model.query.Field;
import com.grupozap.search.api.model.query.Sort;
import com.grupozap.search.api.query.LtrQueryBuilder;
import com.grupozap.search.api.utils.EsSortUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.map.LinkedMap;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Before;
import org.junit.Test;

public class SortQueryAdapterTest extends SearchTransportClientMock {

  private final ElasticsearchSettingsAdapter elasticsearchSettingsAdapter;
  private final ESSortListener ESSortListener;
  private final SortParser sortParser;
  private SortQueryAdapter sortQueryAdapter;
  private EsSortUtils esSortUtils;

  public SortQueryAdapterTest() {
    sortParser =
        new SortParser(
            fieldParserFixture(), new OperatorParser(), new ValueParser(), queryParserFixture());

    elasticsearchSettingsAdapter = mock(ElasticsearchSettingsAdapter.class);
    ESSortListener = mock(ESSortListener.class);
    esSortUtils = new EsSortUtils();
  }

  @Before
  public void setup() {
    this.sortQueryAdapter =
        new SortQueryAdapter(sortParser, mock(FilterQueryAdapter.class), ESSortListener);

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(new SearchSort.SearchSortBuilder().disabled(false).build());
  }

  @Test
  public void shouldApplySortByRequest() {
    var fieldName1 = "id";
    var sortOrder1 = ASC;

    var fieldName2 = "nested.field";
    var sortOrder2 = SortOrder.DESC;

    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();
    request.setSort(
        fieldName1 + " " + sortOrder1.name() + ", " + fieldName2 + " " + sortOrder2.name());

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sorts = (List) requestBuilder.sorts();

    assertEquals(fieldName1, sorts.get(0).getFieldName());
    assertEquals(sortOrder1, sorts.get(0).order());
    assertNull(sorts.get(0).getNestedSort());

    assertEquals(fieldName2, sorts.get(1).getFieldName());
    assertEquals(sortOrder2, sorts.get(1).order());
    assertEquals("nested", sorts.get(1).getNestedSort().getPath());
    assertNull(sorts.get(1).getNestedSort().getFilter());
  }

  @Test
  public void shouldApplySortByScore() {
    var fieldName = "_score";

    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();
    request.setSort(fieldName);

    sortQueryAdapter.apply(requestBuilder, request);
    List<SortBuilder> sorts = (List) requestBuilder.sorts();

    assertEquals(SortOrder.DESC, sorts.get(0).order());
    assertEquals(ScoreSortBuilder.class, sorts.get(0).getClass());
  }

  @Test
  public void shouldApplySortFilterWhenExplicit() {
    var fieldName1 = "id";
    var sortOrder1 = ASC;

    var fieldName2 = "nested.field";
    var sortOrder2 = SortOrder.DESC;
    var sortFilter2 = "sortFilter: fieldName EQ \"value\"";

    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();
    request.setSort(
        fieldName1
            + " "
            + sortOrder1.name()
            + ", "
            + fieldName2
            + " "
            + sortOrder2.name()
            + " "
            + sortFilter2);

    var boolQueryBuilder = boolQuery();

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sorts = (List) requestBuilder.sorts();

    assertEquals(fieldName1, sorts.get(0).getFieldName());
    assertEquals(sortOrder1, sorts.get(0).order());
    assertNull(sorts.get(0).getNestedSort());

    assertEquals(fieldName2, sorts.get(1).getFieldName());
    assertEquals(sortOrder2, sorts.get(1).order());
    assertEquals("nested", sorts.get(1).getNestedSort().getPath());
    assertEquals(boolQueryBuilder, sorts.get(1).getNestedSort().getFilter());
  }

  @Test
  public void mustNotApplySortWhenClientDisablesSortOnRequest() {
    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();
    request.setDisableSort(true);

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(new SearchSort.SearchSortBuilder().disabled(true).build());

    sortQueryAdapter.apply(requestBuilder, request);
    assertNull(requestBuilder.sorts());
  }

  @Test
  public void mustNotApplySortWhenSortDisabledOnProperty() {
    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(new SearchSort.SearchSortBuilder().disabled(true).build());

    sortQueryAdapter.apply(requestBuilder, request);
    assertNull(requestBuilder.sorts());
  }

  @Test
  public void validateDistanceSortBuilder() {
    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();
    request.setSort("field.geo NEAR [10.0, -20.0]");

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sortFields = (List) requestBuilder.sorts();

    assertEquals(1, sortFields.size());

    assertTrue((SortBuilder) sortFields.get(0) instanceof GeoDistanceSortBuilder);
    assertEquals("ASC", ((SortBuilder) sortFields.get(0)).order().name());
    assertEquals(
        "field.geo", ((GeoDistanceSortBuilder) (SortBuilder) sortFields.get(0)).fieldName());
  }

  @Test
  public void shouldApplyDefaultSortWhenClientNotSendItOnSortParameter() {
    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(
            new SearchSort.SearchSortBuilder().defaultSort("id ASC").disabled(false).build());

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sortFields = (List) requestBuilder.sorts();

    assertEquals(1, sortFields.size());

    assertEquals("id", sortFields.get(0).getFieldName());
    assertEquals(ASC, sortFields.get(0).order());
  }

  @Test
  public void shouldNotApplySortWhenDisabledSortIsActivatedOnProperties() {
    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(new SearchSort.SearchSortBuilder().disabled(true).build());

    sortQueryAdapter.apply(requestBuilder, request);
    assertNull(requestBuilder.sorts());
  }

  @Test
  public void shouldNotApplySortWhenDisabledSortIsActivatedOnRequest() {
    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();
    request.setDisableSort(true);

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(new SearchSort.SearchSortBuilder().disabled(true).build());

    sortQueryAdapter.apply(requestBuilder, request);
    assertNull(requestBuilder.sorts());
  }

  @Test
  public void shouldApplyScriptSort() {
    var scriptSortId = "script_sort";
    var scriptSort = new LinkedHashMap<String, Object>();
    scriptSort.put("id", scriptSortId);
    scriptSort.put("scriptType", "stored");
    scriptSort.put("scriptSortType", "number");
    var paramsSort = new LinkedHashMap<>();
    paramsSort.put("score_factor", "2");
    scriptSort.put("params", paramsSort);

    var scriptMap = esSortUtils.buildSortTypeWithCustomProperties(scriptSort, SCRIPT_SORT_TYPE);
    var esSortMap = esSortUtils.buildEsSort(scriptSortId, Map.of("scripts", List.of(scriptMap)));

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(
            new SearchSort.SearchSortBuilder()
                .disabled(false)
                .defaultSort(scriptSortId)
                .sorts(esSortMap)
                .build());

    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();
    request.setSort(scriptSortId + " DESC");

    var sortParser = mock(SortParser.class);
    when(sortParser.parse(request.getSort())).thenReturn(buildSort(scriptSortId, "_rescore"));

    sortQueryAdapter =
        new SortQueryAdapter(sortParser, mock(FilterQueryAdapter.class), ESSortListener);

    sortQueryAdapter.apply(requestBuilder, request);
    var scriptSortBuilder = (ScriptSortBuilder) requestBuilder.sorts().get(0);

    assertEquals(SortOrder.DESC, scriptSortBuilder.order());
    assertEquals(NUMBER, scriptSortBuilder.type());
    assertNull(scriptSortBuilder.script().getLang());
    assertEquals(scriptSortId, scriptSortBuilder.script().getIdOrCode());
    assertEquals(STORED, scriptSortBuilder.script().getType());
    assertTrue(scriptSortBuilder.script().getParams().containsKey("score_factor"));
    assertEquals("2", scriptSortBuilder.script().getParams().get("score_factor"));
  }

  @Test
  public void shouldApplyRescoreOrderWithDefaultPropertyValues() {
    var rescorePropertiesKey = "rescore_defaut";
    var esSortMap =
        esSortUtils.buildEsSort(
            rescorePropertiesKey, Map.of("rescores", List.of(LTR_RESCORE_TYPE.getSortType())));

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(
            new SearchSort.SearchSortBuilder()
                .disabled(false)
                .defaultSort(rescorePropertiesKey)
                .sorts(esSortMap)
                .build());

    var rescores = buildSearchSourceBuilder(rescorePropertiesKey, "_rescore").rescores();
    assertEquals(1, rescores.size());

    var queryRescorerBuilder = (QueryRescorerBuilder) rescores.get(0);
    assertEquals(new LtrRescore().getWindowSize(), rescores.get(0).windowSize().intValue());
    assertEquals(new LtrRescore().getQueryWeight(), queryRescorerBuilder.getQueryWeight(), 0.0);
    assertEquals(fromString(new LtrRescore().getScoreMode()), queryRescorerBuilder.getScoreMode());
    assertEquals(
        new LtrRescore().getRescoreQueryWeight(),
        queryRescorerBuilder.getRescoreQueryWeight(),
        0.0);

    var rescoreQuery = (LtrQueryBuilder) ((QueryRescorerBuilder) rescores.get(0)).getRescoreQuery();
    assertEquals("model_v2", rescoreQuery.getModel());
    assertEquals(List.of(), rescoreQuery.getActiveFeatures());
    assertEquals(new HashMap<String, Object>(), rescoreQuery.getParams());
  }

  @Test
  public void shouldApplyRescoreOrderWithOverridingTheDefaultPropertyValues() {
    var model = "model-v2";
    var windowSize = 5;
    var queryWeight = 1.3f;
    var rescoreQueryWeight = 2.5f;
    var scoreMode = "total";
    var activeFeatures = newArrayList("feature_1", "feature_2");
    var rescoreType = "ltr_rescore";
    var params = new HashMap<String, Object>();
    params.put("query_string", "rambo");

    var rescore = new LinkedHashMap<String, Object>();
    rescore.put("model", model);
    rescore.put("window_size", windowSize);
    rescore.put("query_weight", queryWeight);
    rescore.put("rescore_query_weight", rescoreQueryWeight);
    rescore.put("score_mode", scoreMode);
    rescore.put("active_features", activeFeatures);
    rescore.put("params", params);
    rescore.put("rescore_type", rescoreType);

    var rescorePropertiesKey = "rescore_defaut";

    var rescoreMap = esSortUtils.buildSortTypeWithCustomProperties(rescore, LTR_RESCORE_TYPE);

    var esSortMap =
        esSortUtils.buildEsSort(rescorePropertiesKey, Map.of("rescores", List.of(rescoreMap)));

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(
            new SearchSort.SearchSortBuilder()
                .disabled(false)
                .defaultSort(rescorePropertiesKey)
                .sorts(esSortMap)
                .build());

    var rescores = buildSearchSourceBuilder(rescorePropertiesKey, "_rescore").rescores();
    assertEquals(1, rescores.size());

    var queryRescorerBuilder = (QueryRescorerBuilder) rescores.get(0);
    assertEquals(windowSize, rescores.get(0).windowSize().intValue());
    assertEquals(queryWeight, queryRescorerBuilder.getQueryWeight(), 0.0);
    assertEquals(rescoreQueryWeight, queryRescorerBuilder.getRescoreQueryWeight(), 0.0);
    assertEquals(fromString(scoreMode), queryRescorerBuilder.getScoreMode());

    var rescoreQuery = (LtrQueryBuilder) ((QueryRescorerBuilder) rescores.get(0)).getRescoreQuery();
    assertEquals(model, rescoreQuery.getModel());
    assertEquals(activeFeatures, rescoreQuery.getActiveFeatures());
    assertEquals(params, rescoreQuery.getParams());
  }

  @Test
  public void shouldApplyRandomRescoreOrderWithOverridingTheDefaultPropertyValues() {
    var windowSize = 5;
    var queryWeight = 1.3f;
    var rescoreQueryWeight = 2.5f;
    var scoreMode = "total";
    var seed = 100;
    var field = "_id";
    var rescoreType = "random_rescore";

    var rescore = new LinkedHashMap<String, Object>();
    rescore.put("window_size", windowSize);
    rescore.put("query_weight", queryWeight);
    rescore.put("rescore_query_weight", rescoreQueryWeight);
    rescore.put("score_mode", scoreMode);
    rescore.put("seed", seed);
    rescore.put("field", field);
    rescore.put("rescore_type", rescoreType);

    var rescorePropertiesKey = "rescore_seed";
    var rescoreMap = esSortUtils.buildSortTypeWithCustomProperties(rescore, RANDOM_RESCORE_TYPE);
    var esSortMap =
        esSortUtils.buildEsSort(rescorePropertiesKey, Map.of("rescores", List.of(rescoreMap)));

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(
            new SearchSort.SearchSortBuilder()
                .disabled(false)
                .defaultSort(rescorePropertiesKey)
                .sorts(esSortMap)
                .build());

    var rescores = buildSearchSourceBuilder(rescorePropertiesKey, "_rescore").rescores();
    assertEquals(1, rescores.size());

    var queryRescorerBuilder = (QueryRescorerBuilder) rescores.get(0);
    assertEquals(windowSize, queryRescorerBuilder.windowSize().intValue());
    assertEquals(queryWeight, queryRescorerBuilder.getQueryWeight(), 0.0);
    assertEquals(rescoreQueryWeight, queryRescorerBuilder.getRescoreQueryWeight(), 0.0);
    assertEquals(fromString(scoreMode), queryRescorerBuilder.getScoreMode());

    var rescoreQuery = (FunctionScoreQueryBuilder) queryRescorerBuilder.getRescoreQuery();
    var functionScore =
        (RandomScoreFunctionBuilder) rescoreQuery.filterFunctionBuilders()[0].getScoreFunction();
    assertEquals(field, functionScore.getField());
    assertEquals(seed, (int) functionScore.getSeed());
  }

  @Test(expected = RescoreConjunctionSortException.class)
  public void shouldThrowExceptionWhenSortOptionInConjunctionWithRescore() {
    var esSortMap = new HashMap<String, Object>();
    var sortList = new ArrayList<>();
    var sortsMap = new LinkedHashMap<String, LinkedHashMap<String, Object>>();
    var rescoresMap = new LinkedHashMap<String, Object>();
    var rescoresList = new ArrayList<>();

    var model = "model-v2";
    var rescoreType = "ltr_rescore";

    var rescore = new LinkedHashMap<>();
    rescore.put("model", model);
    rescore.put("rescore_type", rescoreType);

    var rescorePropertiesKey = "rescore_default";
    rescoresList.add(rescore);
    rescoresMap.put("rescores", rescoresList);
    sortsMap.put(rescorePropertiesKey, rescoresMap);
    sortList.add(sortsMap);
    esSortMap.put("sorts", sortList);

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(
            new SearchSort.SearchSortBuilder()
                .disabled(false)
                .defaultSort(rescorePropertiesKey)
                .sorts(esSortMap)
                .build());

    var request = fullRequest.build();
    request.setSort(rescorePropertiesKey + ",_id DESC");

    var sortParser = mock(SortParser.class);
    when(sortParser.parse(request.getSort()))
        .thenReturn(
            buildSort(buildSort(rescorePropertiesKey, "_rescore"), buildSort("_id", "_obj")));

    sortQueryAdapter =
        new SortQueryAdapter(sortParser, mock(FilterQueryAdapter.class), ESSortListener);

    var searchSourceBuilder = new SearchSourceBuilder();
    sortQueryAdapter.apply(searchSourceBuilder, request);
  }

  @Test
  public void shouldApplyRescoreModelWithFunctionRescore() {
    var rescoreFunction = new LinkedHashMap<String, Object>();
    var weight = 10;
    var script = new LinkedHashMap<>();
    var params = new LinkedHashMap<>();

    var defaultValue = 0.001;
    params.put("default_value", defaultValue);
    script.put("source", "");
    script.put("params", params);

    rescoreFunction.put("rescore_type", "function_score");
    rescoreFunction.put("weight", weight);
    rescoreFunction.put("script", script);

    var rescorePropertiesKey = "rescore_defaut";

    var functionRescoreMap =
        esSortUtils.buildSortTypeWithCustomProperties(rescoreFunction, FUNCTION_RESCORE_TYPE);

    var esSortMap =
        esSortUtils.buildEsSort(
            rescorePropertiesKey,
            Map.of("rescores", List.of(functionRescoreMap, LTR_RESCORE_TYPE.getSortType())));

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(
            new SearchSort.SearchSortBuilder()
                .disabled(false)
                .defaultSort(rescorePropertiesKey)
                .sorts(esSortMap)
                .build());

    var request = fullRequest.build();
    request.setSort(rescorePropertiesKey);

    var sortParser = mock(SortParser.class);
    when(sortParser.parse(request.getSort()))
        .thenReturn(buildSort(rescorePropertiesKey, "_rescore"));

    sortQueryAdapter =
        new SortQueryAdapter(sortParser, mock(FilterQueryAdapter.class), ESSortListener);

    var searchSourceBuilder = new SearchSourceBuilder();
    sortQueryAdapter.apply(searchSourceBuilder, request);

    var rescores = searchSourceBuilder.rescores();
    assertEquals(2, rescores.size());
    assertEquals(
        FunctionScoreQueryBuilder.class,
        ((QueryRescorerBuilder) rescores.get(0)).getRescoreQuery().getClass());
    assertEquals(
        LtrQueryBuilder.class,
        ((QueryRescorerBuilder) rescores.get(1)).getRescoreQuery().getClass());
    assertEquals(
        SUM,
        ((FunctionScoreQueryBuilder) ((QueryRescorerBuilder) rescores.get(0)).getRescoreQuery())
            .scoreMode());
  }

  @Test
  public void shouldApplyRescoreModelWithScriptScore() {
    var rescorePropertiesKey = "rescore_defaut";

    var esSortMap =
        esSortUtils.buildEsSort(
            rescorePropertiesKey,
            Map.of(
                "rescores", List.of(LTR_RESCORE_TYPE.getSortType()),
                "scripts", List.of(SCRIPT_SORT_TYPE.getSortType())));

    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(
            new SearchSort.SearchSortBuilder()
                .disabled(false)
                .defaultSort(rescorePropertiesKey)
                .sorts(esSortMap)
                .build());

    var searchBuilder = buildSearchSourceBuilder(rescorePropertiesKey, "_rescore");
    var rescores = searchBuilder.rescores();

    assertEquals(1, rescores.size());
    assertEquals(1, searchBuilder.sorts().size());
    assertEquals(
        LtrQueryBuilder.class,
        ((QueryRescorerBuilder) rescores.get(0)).getRescoreQuery().getClass());
    assertEquals(ScriptSortBuilder.class, searchBuilder.sorts().get(0).getClass());
  }

  private SearchSourceBuilder buildSearchSourceBuilder(String sortParameter, String sortType) {
    var request = fullRequest.build();
    request.setSort(sortParameter);

    var sortParser = mock(SortParser.class);
    when(sortParser.parse(request.getSort())).thenReturn(buildSort(sortParameter, sortType));

    sortQueryAdapter =
        new SortQueryAdapter(sortParser, mock(FilterQueryAdapter.class), ESSortListener);

    var searchSourceBuilder = new SearchSourceBuilder();
    sortQueryAdapter.apply(searchSourceBuilder, request);

    return searchSourceBuilder;
  }

  private Sort buildSort(final String fields, final String type) {
    var typesByName = new LinkedMap<String, String>();
    typesByName.put(fields, type);
    return new Sort(new Field(typesByName), DESC);
  }

  private Sort buildSort(Sort... sorts) {
    return new Sort(List.of(sorts));
  }
}

package com.grupozap.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SORT;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_SORT_DISABLE;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_SORT_RESCORE;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.grupozap.search.api.model.mapping.MappingType.FIELD_TYPE_SCRIPT;
import static com.grupozap.search.api.model.query.OrderOperator.DESC;
import static com.grupozap.search.api.utils.MapperUtils.convertValue;
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

import com.google.common.collect.Sets;
import com.grupozap.search.api.exception.RescoreConjunctionSortException;
import com.grupozap.search.api.listener.ScriptRemotePropertiesListener;
import com.grupozap.search.api.listener.ScriptRemotePropertiesListener.ScriptField;
import com.grupozap.search.api.listener.SortRescoreListener;
import com.grupozap.search.api.listener.SortRescoreListener.SortRescore;
import com.grupozap.search.api.model.parser.OperatorParser;
import com.grupozap.search.api.model.parser.SortParser;
import com.grupozap.search.api.model.parser.ValueParser;
import com.grupozap.search.api.model.query.Field;
import com.grupozap.search.api.model.query.Sort;
import com.grupozap.search.api.query.LtrQueryBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private final ScriptRemotePropertiesListener scriptRemotePropertiesListener;
  private final ElasticsearchSettingsAdapter elasticsearchSettingsAdapter;
  private final SortRescoreListener sortRescoreListener;
  private final SortParser sortParser;
  private SortQueryAdapter sortQueryAdapter;

  public SortQueryAdapterTest() {
    sortParser =
        new SortParser(
            fieldParserFixture(), new OperatorParser(), new ValueParser(), queryParserFixture());

    scriptRemotePropertiesListener = mock(ScriptRemotePropertiesListener.class);
    elasticsearchSettingsAdapter = mock(ElasticsearchSettingsAdapter.class);
    sortRescoreListener = mock(SortRescoreListener.class);
  }

  @Before
  public void setup() {
    ES_DEFAULT_SORT.setValue(INDEX_NAME, "id ASC");
    ES_SORT_DISABLE.setValue(INDEX_NAME, false);

    this.sortQueryAdapter =
        new SortQueryAdapter(
            sortParser,
            mock(FilterQueryAdapter.class),
            scriptRemotePropertiesListener,
            elasticsearchSettingsAdapter,
            sortRescoreListener);
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

    sortQueryAdapter.apply(requestBuilder, request);
    assertNull(requestBuilder.sorts());
  }

  @Test
  public void mustNotApplySortWhenSortDisabledOnProperty() {
    ES_SORT_DISABLE.setValue(INDEX_NAME, true);
    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();

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

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sortFields = (List) requestBuilder.sorts();

    assertEquals(1, sortFields.size());

    assertEquals("id", sortFields.get(0).getFieldName());
    assertEquals(ASC, sortFields.get(0).order());
  }

  @Test
  public void shouldNotApplySortWhenDisabledSortIsActivatedOnProperties() {
    ES_SORT_DISABLE.setValue(INDEX_NAME, true);
    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();

    sortQueryAdapter.apply(requestBuilder, request);
    assertNull(requestBuilder.sorts());
  }

  @Test
  public void shouldNotApplySortWhenDisabledSortIsActivatedOnRequest() {
    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();
    request.setDisableSort(true);

    sortQueryAdapter.apply(requestBuilder, request);
    assertNull(requestBuilder.sorts());
  }

  @Test
  public void shouldApplyScriptSort() {
    var scriptSortId = "my_script_sort";

    when(elasticsearchSettingsAdapter.getFieldType(INDEX_NAME, scriptSortId))
        .thenReturn(FIELD_TYPE_SCRIPT.getDefaultType());

    var scriptSort = new HashMap<String, Object>();
    scriptSort.put("id", scriptSortId);
    scriptSort.put("scriptType", "stored");
    scriptSort.put("scriptSortType", "number");

    var paramsSort = new HashMap<String, Object>();
    paramsSort.put("score_factor", "2");

    scriptSort.put("params", paramsSort);

    var scriptField = convertValue(scriptSort, ScriptField.class);

    var scripts = new HashMap<String, Set<ScriptField>>();
    scripts.put(INDEX_NAME, Sets.newHashSet(scriptField));

    when(this.scriptRemotePropertiesListener.getScripts()).thenReturn(scripts);

    var requestBuilder = new SearchSourceBuilder();
    var request = fullRequest.build();
    request.setSort(scriptSortId + " DESC");

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
    var properties = new ArrayList();
    var model = "model-v2";
    var rescoreType = "ltr_rescore";

    var rescoreDefaultProperties = new LinkedHashMap();
    rescoreDefaultProperties.put("model", model);
    rescoreDefaultProperties.put("rescore_type", rescoreType);

    var rescorePropertiesKey = "rescore_default";
    var rescoreProperties = new LinkedHashMap<String, ArrayList>();
    properties.add(rescoreDefaultProperties);

    rescoreProperties.put(rescorePropertiesKey, properties);
    ES_SORT_RESCORE.setValue(INDEX_NAME, rescoreProperties);

    when(this.sortRescoreListener.getRescorerOrders(INDEX_NAME))
        .thenReturn(build(rescoreProperties));

    var request = fullRequest.build();
    request.setSort(rescorePropertiesKey);

    var sortParser = mock(SortParser.class);
    when(sortParser.parse(request.getSort()))
        .thenReturn(buildSort(rescorePropertiesKey, "_rescore"));

    sortQueryAdapter =
        new SortQueryAdapter(
            sortParser,
            mock(FilterQueryAdapter.class),
            scriptRemotePropertiesListener,
            elasticsearchSettingsAdapter,
            sortRescoreListener);

    var searchSourceBuilder = new SearchSourceBuilder();
    sortQueryAdapter.apply(searchSourceBuilder, request);

    var rescores = searchSourceBuilder.rescores();
    assertEquals(1, rescores.size());

    var queryRescorerBuilder = (QueryRescorerBuilder) rescores.get(0);
    assertEquals(
        new SortRescoreListener.LtrRescore().getWindowSize(),
        rescores.get(0).windowSize().intValue());
    assertEquals(
        new SortRescoreListener.LtrRescore().getQueryWeight(),
        queryRescorerBuilder.getQueryWeight(),
        0.0);
    assertEquals(
        new SortRescoreListener.LtrRescore().getRescoreQueryWeight(),
        queryRescorerBuilder.getRescoreQueryWeight(),
        0.0);
    assertEquals(
        fromString(new SortRescoreListener.LtrRescore().getScoreMode()),
        queryRescorerBuilder.getScoreMode());

    var rescoreQuery = (LtrQueryBuilder) ((QueryRescorerBuilder) rescores.get(0)).getRescoreQuery();
    assertEquals(model, rescoreQuery.getModel());
    assertEquals(newArrayList(), rescoreQuery.getActiveFeatures());
    assertEquals(new HashMap<String, Object>(), rescoreQuery.getParams());
  }

  @Test
  public void shouldApplyRescoreOrderWithOverridingTheDefaultPropertyValues() {
    var properties = new ArrayList();
    var model = "model-v2";
    var windowSize = 5;
    var queryWeight = 1.3f;
    var rescoreQueryWeight = 2.5f;
    var scoreMode = "total";
    var activeFeatures = newArrayList("feature_1", "feature_2");
    var rescoreType = "ltr_rescore";

    var params = new HashMap<String, Object>();
    params.put("query_string", "rambo");

    var ltrRescoreProperties = new LinkedHashMap<>();
    ltrRescoreProperties.put("model", model);
    ltrRescoreProperties.put("window_size", windowSize);
    ltrRescoreProperties.put("query_weight", queryWeight);
    ltrRescoreProperties.put("rescore_query_weight", rescoreQueryWeight);
    ltrRescoreProperties.put("score_mode", scoreMode);
    ltrRescoreProperties.put("active_features", activeFeatures);
    ltrRescoreProperties.put("params", params);
    ltrRescoreProperties.put("rescore_type", rescoreType);

    properties.add(ltrRescoreProperties);

    var rescorePropertiesKey = "rescore_default";
    var rescoreProperties = new LinkedHashMap<String, ArrayList>();
    rescoreProperties.put(rescorePropertiesKey, properties);
    ES_SORT_RESCORE.setValue(INDEX_NAME, rescoreProperties);

    when(this.sortRescoreListener.getRescorerOrders(INDEX_NAME))
        .thenReturn(build(rescoreProperties));

    var request = fullRequest.build();
    request.setSort(rescorePropertiesKey);

    var sortParser = mock(SortParser.class);
    when(sortParser.parse(request.getSort()))
        .thenReturn(buildSort(rescorePropertiesKey, "_rescore"));

    sortQueryAdapter =
        new SortQueryAdapter(
            sortParser,
            mock(FilterQueryAdapter.class),
            scriptRemotePropertiesListener,
            elasticsearchSettingsAdapter,
            sortRescoreListener);

    var searchSourceBuilder = new SearchSourceBuilder();
    sortQueryAdapter.apply(searchSourceBuilder, request);

    var rescores = searchSourceBuilder.rescores();
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

    var properties = new ArrayList();

    var seedRescoreProperties = new LinkedHashMap<>();
    seedRescoreProperties.put("window_size", windowSize);
    seedRescoreProperties.put("query_weight", queryWeight);
    seedRescoreProperties.put("rescore_query_weight", rescoreQueryWeight);
    seedRescoreProperties.put("score_mode", scoreMode);
    seedRescoreProperties.put("seed", seed);
    seedRescoreProperties.put("field", field);
    seedRescoreProperties.put("rescore_type", rescoreType);

    var rescorePropertiesKey = "rescore_seed";
    var rescoreProperties = new LinkedHashMap<String, ArrayList>();
    properties.add(seedRescoreProperties);

    rescoreProperties.put(rescorePropertiesKey, properties);
    ES_SORT_RESCORE.setValue(INDEX_NAME, rescoreProperties);

    when(this.sortRescoreListener.getRescorerOrders(INDEX_NAME))
        .thenReturn(build(rescoreProperties));

    var request = fullRequest.build();
    request.setSort(rescorePropertiesKey);

    var sortParser = mock(SortParser.class);
    when(sortParser.parse(request.getSort()))
        .thenReturn(buildSort(rescorePropertiesKey, "_rescore"));

    sortQueryAdapter =
        new SortQueryAdapter(
            sortParser,
            mock(FilterQueryAdapter.class),
            scriptRemotePropertiesListener,
            elasticsearchSettingsAdapter,
            sortRescoreListener);

    var searchSourceBuilder = new SearchSourceBuilder();
    sortQueryAdapter.apply(searchSourceBuilder, request);

    var rescores = searchSourceBuilder.rescores();
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
    var model = "model-v2";
    var rescoreType = "ltr_rescore";

    var rescoreDefaultProperties = new LinkedHashMap<>();
    rescoreDefaultProperties.put("model", model);
    rescoreDefaultProperties.put("rescore_type", rescoreType);

    var rescorePropertiesKey = "rescore_default";
    var rescoreProperties = new LinkedHashMap<String, ArrayList>();

    var properties = new ArrayList();

    properties.add(rescoreDefaultProperties);
    rescoreProperties.put(rescorePropertiesKey, properties);
    ES_SORT_RESCORE.setValue(INDEX_NAME, rescoreProperties);

    when(this.sortRescoreListener.getRescorerOrders(INDEX_NAME))
        .thenReturn(build(rescoreProperties));

    var request = fullRequest.build();
    request.setSort(rescorePropertiesKey + ",_id DESC");

    var sortParser = mock(SortParser.class);
    when(sortParser.parse(request.getSort()))
        .thenReturn(
            buildSort(buildSort(rescorePropertiesKey, "_rescore"), buildSort("_id", "_obj")));

    sortQueryAdapter =
        new SortQueryAdapter(
            sortParser,
            mock(FilterQueryAdapter.class),
            scriptRemotePropertiesListener,
            elasticsearchSettingsAdapter,
            sortRescoreListener);

    var searchSourceBuilder = new SearchSourceBuilder();
    sortQueryAdapter.apply(searchSourceBuilder, request);
  }

  @Test
  public void shouldApplyRescoreModelWithFunctionRescore() {
    var properties = new ArrayList();

    var model = "model-v2";
    var rescoreType = "ltr_rescore";

    var rescoreDefaultProperties = new LinkedHashMap();
    rescoreDefaultProperties.put("model", model);
    rescoreDefaultProperties.put("rescore_type", rescoreType);

    var rescoreFunctionProperties = new LinkedHashMap();
    var weight = 10;
    var script = new HashMap<String, Object>();
    var source = "";
    var params = new HashMap<String, Object>();
    var defaultValue = 0.001;

    params.put("default_value", defaultValue);
    script.put("source", source);
    script.put("params", params);

    rescoreFunctionProperties.put("rescore_type", "function_score");
    rescoreFunctionProperties.put("weight", weight);
    rescoreFunctionProperties.put("script", script);

    var rescorePropertiesKey = "rescore_default";
    var rescoreProperties = new LinkedHashMap<String, ArrayList>();

    properties.add(rescoreDefaultProperties);
    properties.add(rescoreFunctionProperties);

    rescoreProperties.put(rescorePropertiesKey, properties);
    ES_SORT_RESCORE.setValue(INDEX_NAME, rescoreProperties);

    when(this.sortRescoreListener.getRescorerOrders(INDEX_NAME))
        .thenReturn(build(rescoreProperties));

    var request = fullRequest.build();
    request.setSort(rescorePropertiesKey);

    var sortParser = mock(SortParser.class);
    when(sortParser.parse(request.getSort()))
        .thenReturn(buildSort(rescorePropertiesKey, "_rescore"));

    sortQueryAdapter =
        new SortQueryAdapter(
            sortParser,
            mock(FilterQueryAdapter.class),
            scriptRemotePropertiesListener,
            elasticsearchSettingsAdapter,
            sortRescoreListener);

    var searchSourceBuilder = new SearchSourceBuilder();
    sortQueryAdapter.apply(searchSourceBuilder, request);

    var rescores = searchSourceBuilder.rescores();
    assertEquals(2, rescores.size());
    assertEquals(
        LtrQueryBuilder.class,
        ((QueryRescorerBuilder) rescores.get(0)).getRescoreQuery().getClass());
    assertEquals(
        FunctionScoreQueryBuilder.class,
        ((QueryRescorerBuilder) rescores.get(1)).getRescoreQuery().getClass());
  }

  private Map<String, List<SortRescore>> build(Map<String, ArrayList> rescore) {
    return SortRescoreListener.getConvertedRescores(rescore);
  }

  private Sort buildSort(final String fields, final String type) {
    var typesByName = new LinkedMap<String, String>();
    typesByName.put(fields, type);
    return new Sort(new Field(typesByName), DESC);
  }

  private Sort buildSort(Sort... sorts) {
    return new Sort(newArrayList(sorts));
  }
}

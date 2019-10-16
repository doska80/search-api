package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SORT;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_SORT_DISABLE;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.grupozap.search.api.model.mapping.MappingType.FIELD_TYPE_SCRIPT;
import static com.grupozap.search.api.utils.MapperUtils.convertValue;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.script.ScriptType.STORED;
import static org.elasticsearch.search.sort.ScriptSortBuilder.ScriptSortType.NUMBER;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.grupozap.search.api.listener.ScriptRemotePropertiesListener;
import com.grupozap.search.api.listener.ScriptRemotePropertiesListener.ScriptField;
import com.grupozap.search.api.listener.SortRescoreListener;
import com.grupozap.search.api.model.parser.OperatorParser;
import com.grupozap.search.api.model.parser.SortParser;
import com.grupozap.search.api.model.parser.ValueParser;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.*;
import org.junit.Before;
import org.junit.Test;

public class SortQueryAdapterTest extends SearchTransportClientMock {

  private final SortQueryAdapter sortQueryAdapter;
  private final ScriptRemotePropertiesListener scriptRemotePropertiesListener;
  private final ElasticsearchSettingsAdapter elasticsearchSettingsAdapter;
  private final SortRescoreListener sortRescoreListener;

  public SortQueryAdapterTest() {
    var sortParser =
        new SortParser(
            fieldParserFixture(), new OperatorParser(), new ValueParser(), queryParserFixture());

    scriptRemotePropertiesListener = mock(ScriptRemotePropertiesListener.class);
    elasticsearchSettingsAdapter = mock(ElasticsearchSettingsAdapter.class);
    sortRescoreListener = mock(SortRescoreListener.class);

    this.sortQueryAdapter =
        new SortQueryAdapter(
            sortParser,
            mock(FilterQueryAdapter.class),
            scriptRemotePropertiesListener,
            elasticsearchSettingsAdapter,
            sortRescoreListener);
  }

  @Before
  public void setup() {
    ES_DEFAULT_SORT.setValue(INDEX_NAME, "id ASC");
    ES_SORT_DISABLE.setValue(INDEX_NAME, false);
  }

  @Test
  public void shouldApplySortByRequest() {
    var fieldName1 = "id";
    var sortOrder1 = ASC;

    var fieldName2 = "nested.field";
    var sortOrder2 = DESC;

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
    var sortOrder2 = DESC;
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

    assertEquals(DESC, scriptSortBuilder.order());
    assertEquals(NUMBER, scriptSortBuilder.type());
    assertNull(scriptSortBuilder.script().getLang());
    assertEquals(scriptSortId, scriptSortBuilder.script().getIdOrCode());
    assertEquals(STORED, scriptSortBuilder.script().getType());
    assertTrue(scriptSortBuilder.script().getParams().containsKey("score_factor"));
    assertEquals("2", scriptSortBuilder.script().getParams().get("score_factor"));
  }
}

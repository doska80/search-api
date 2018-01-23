package com.vivareal.search.api.adapter;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SORT;
import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import com.vivareal.search.api.model.http.SearchApiRequest;
import com.vivareal.search.api.model.parser.OperatorParser;
import com.vivareal.search.api.model.parser.SortParser;
import java.util.List;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.BeforeClass;
import org.junit.Test;

public class SortQueryAdapterTest extends SearchTransportClientMock {

  private SortQueryAdapter sortQueryAdapter;

  public SortQueryAdapterTest() {
    SortParser sortParser =
        new SortParser(fieldParserFixture(), new OperatorParser(), queryParserFixture());
    this.sortQueryAdapter = new SortQueryAdapter(sortParser, mock(FilterQueryAdapter.class));
  }

  @BeforeClass
  public static void setup() {
    ES_DEFAULT_SORT.setValue(INDEX_NAME, "id ASC");
  }

  @Test
  public void shouldApplySortByDefaultProperty() {
    SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
    SearchApiRequest request = fullRequest.build();

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sorts = (List) requestBuilder.request().source().sorts();

    assertEquals("id", sorts.get(0).getFieldName());
    assertEquals(ASC, sorts.get(0).order());
    assertNull(sorts.get(0).getNestedPath());
    assertNull(sorts.get(0).getNestedPath());

    assertEquals("_uid", sorts.get(1).getFieldName());
    assertEquals(DESC, sorts.get(1).order());
    assertNull(sorts.get(1).getNestedPath());
    assertNull(sorts.get(1).getNestedFilter());
    assertNull(sorts.get(1).getNestedPath());
  }

  @Test
  public void shouldApplySortByRequest() {
    String fieldName1 = "id";
    SortOrder sortOrder1 = ASC;

    String fieldName2 = "nested.field";
    SortOrder sortOrder2 = DESC;

    SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
    SearchApiRequest request = fullRequest.build();
    request.setSort(
        fieldName1 + " " + sortOrder1.name() + ", " + fieldName2 + " " + sortOrder2.name());

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sorts = (List) requestBuilder.request().source().sorts();

    assertEquals(fieldName1, sorts.get(0).getFieldName());
    assertEquals(sortOrder1, sorts.get(0).order());
    assertNull(sorts.get(0).getNestedPath());

    assertEquals(fieldName2, sorts.get(1).getFieldName());
    assertEquals(sortOrder2, sorts.get(1).order());
    assertEquals("nested", sorts.get(1).getNestedPath());
    assertNull(sorts.get(1).getNestedFilter());

    assertEquals("_uid", sorts.get(2).getFieldName());
    assertEquals(DESC, sorts.get(2).order());
    assertNull(sorts.get(2).getNestedPath());
  }

  @Test
  public void shouldApplySortByScore() {
    String fieldName = "_score";

    SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
    SearchApiRequest request = fullRequest.build();
    request.setSort(fieldName);

    sortQueryAdapter.apply(requestBuilder, request);
    List<SortBuilder> sorts = (List) requestBuilder.request().source().sorts();

    assertEquals(SortOrder.DESC, sorts.get(0).order());
    assertEquals(ScoreSortBuilder.class, sorts.get(0).getClass());
  }

  @Test
  public void shouldApplySortFilterWhenExplicit() {
    String fieldName1 = "id";
    SortOrder sortOrder1 = ASC;

    String fieldName2 = "nested.field";
    SortOrder sortOrder2 = DESC;
    String sortFilter2 = "sortFilter: fieldName EQ \"value\"";

    SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
    SearchApiRequest request = fullRequest.build();
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

    BoolQueryBuilder boolQueryBuilder = boolQuery();

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sorts = (List) requestBuilder.request().source().sorts();

    assertEquals(fieldName1, sorts.get(0).getFieldName());
    assertEquals(sortOrder1, sorts.get(0).order());
    assertNull(sorts.get(0).getNestedPath());

    assertEquals(fieldName2, sorts.get(1).getFieldName());
    assertEquals(sortOrder2, sorts.get(1).order());
    assertEquals("nested", sorts.get(1).getNestedPath());
    assertEquals(boolQueryBuilder, sorts.get(1).getNestedFilter());

    assertEquals("_uid", sorts.get(2).getFieldName());
    assertEquals(DESC, sorts.get(2).order());
    assertNull(sorts.get(2).getNestedPath());
  }

  @Test
  public void mustApplyDefaultSortWhenClientInputSortEmptyOnRequest() {
    SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
    SearchApiRequest request = fullRequest.build();
    request.setSort("");

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sortFields = (List) requestBuilder.request().source().sorts();

    assertEquals(2, sortFields.size());

    assertEquals("id", sortFields.get(0).getFieldName());
    assertEquals(ASC, sortFields.get(0).order());

    assertEquals("_uid", sortFields.get(1).getFieldName());
    assertEquals(DESC, sortFields.get(1).order());
  }

  @Test
  public void mustApplyDefaultSortWhenClientInputAnInvalidFieldSortOnRequest() {
    SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
    SearchApiRequest request = fullRequest.build();
    request.setSort("invalid.field ASC");

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sortFields = (List) requestBuilder.request().source().sorts();

    assertEquals(2, sortFields.size());

    assertEquals("id", sortFields.get(0).getFieldName());
    assertEquals(ASC, sortFields.get(0).order());

    assertEquals("_uid", sortFields.get(1).getFieldName());
    assertEquals(DESC, sortFields.get(1).order());
  }
}

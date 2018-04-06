package com.vivareal.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static com.vivareal.search.api.fixtures.model.parser.ParserTemplateLoader.*;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.vivareal.search.api.model.query.LogicalOperator.AND;
import static com.vivareal.search.api.model.query.RelationalOperator.*;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.elasticsearch.index.query.Operator.OR;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Sets;
import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import com.vivareal.search.api.model.mapping.MappingType;
import com.vivareal.search.api.service.parser.factory.DefaultFilterFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.util.Lists;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ElasticsearchQueryAdapterTest extends SearchTransportClientMock {

  private QueryAdapter<GetRequestBuilder, SearchRequestBuilder> queryAdapter;
  private FilterQueryAdapter filterQueryAdapter;
  private DefaultFilterFactory defaultFilterFactory;
  private PageQueryAdapter pageQueryAdapter;
  private QueryStringAdapter queryStringAdapter;
  private FunctionScoreAdapter functionScoreAdapter;
  private FacetQueryAdapter facetQueryAdapter;

  @Mock private ElasticsearchSettingsAdapter settingsAdapter;

  @Mock private SearchAfterQueryAdapter searchAfterQueryAdapter;

  @Mock private SortQueryAdapter sortQueryAdapter;

  @Before
  public void setup() {
    initMocks(this);

    QS_MM.setValue(INDEX_NAME, "75%");
    QS_DEFAULT_FIELDS.setValue(INDEX_NAME, newArrayList("field", "field1"));
    ES_QUERY_TIMEOUT_VALUE.setValue(INDEX_NAME, 100);
    ES_QUERY_TIMEOUT_UNIT.setValue(INDEX_NAME, "MILLISECONDS");
    ES_DEFAULT_SIZE.setValue(INDEX_NAME, 20);
    ES_MAX_SIZE.setValue(INDEX_NAME, 200);
    ES_FACET_SIZE.setValue(INDEX_NAME, 20);
    ES_MAPPING_META_FIELDS_ID.setValue(INDEX_NAME, "id");

    ESClient esClient = new ESClient(transportClient);
    SourceFieldAdapter sourceFieldAdapter = mock(SourceFieldAdapter.class);

    this.pageQueryAdapter = new PageQueryAdapter();
    this.queryStringAdapter = new QueryStringAdapter(fieldCacheFixture());
    this.functionScoreAdapter = new FunctionScoreAdapter(fieldParserFixture());
    this.facetQueryAdapter = new FacetQueryAdapter(facetParserFixture());
    this.filterQueryAdapter = new FilterQueryAdapter(queryParserFixture());
    this.defaultFilterFactory =
        new DefaultFilterFactory(queryParserWithOutValidationFixture(), filterQueryAdapter);

    this.queryAdapter =
        new ElasticsearchQueryAdapter(
            esClient,
            sourceFieldAdapter,
            pageQueryAdapter,
            searchAfterQueryAdapter,
            sortQueryAdapter,
            queryStringAdapter,
            functionScoreAdapter,
            queryParserFixture(),
            filterQueryAdapter,
            defaultFilterFactory,
            facetQueryAdapter);

    doNothing().when(sourceFieldAdapter).apply(any(SearchRequestBuilder.class), any());
    doNothing().when(sourceFieldAdapter).apply(any(GetRequestBuilder.class), any());
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
  public void shouldReturnGetRequestBuilderByGetId() {
    String id = "123456";

    newArrayList(basicRequest, filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              BaseApiRequest searchApiRequest = request.build();
              GetRequestBuilder requestBuilder = queryAdapter.getById(searchApiRequest, id);

              assertEquals(id, requestBuilder.request().id());
              assertEquals(searchApiRequest.getIndex(), requestBuilder.request().index());
              assertEquals(searchApiRequest.getIndex(), requestBuilder.request().type());
            });
  }

  @Test
  public void shouldApplyTimeoutOnQueryBody() {
    SearchApiRequest request = fullRequest.build();
    SearchRequestBuilder searchRequestBuilder = queryAdapter.query(request);
    assertEquals(
        new TimeValue(100, TimeUnit.MILLISECONDS),
        searchRequestBuilder.request().source().timeout());
  }

  @Test
  public void shouldReturnSimpleSearchRequestBuilderWithBasicRequestPagination() {
    SearchApiRequest request = fullRequest.build();
    SearchRequestBuilder searchRequestBuilder = queryAdapter.query(request);
    SearchSourceBuilder source = searchRequestBuilder.request().source();

    assertEquals(request.getIndex(), searchRequestBuilder.request().indices()[0]);
    assertEquals(request.getFrom(), source.from());
    assertEquals(request.getSize(), source.size());
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSimpleNestedObject() {
    final String field = "nested.field";
    final Object value = "Lorem Ipsum";

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              SearchRequestBuilder searchRequestBuilder =
                  queryAdapter.query(request.filter(format(field, value, EQUAL.name())).build());

              NestedQueryBuilder nestedQueryBuilder =
                  (NestedQueryBuilder)
                      ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                          .filter()
                          .get(0);
              assertNotNull(nestedQueryBuilder);
              assertTrue(
                  nestedQueryBuilder
                      .toString()
                      .contains("\"path\" : \"" + field.split("\\.")[0] + "\""));

              MatchQueryBuilder filter =
                  (MatchQueryBuilder)
                      ((BoolQueryBuilder) nestedQueryBuilder.query()).filter().get(0);
              assertNotNull(filter);
              assertEquals(field, filter.fieldName());
              assertEquals(value, filter.value());
            });
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleFilterDifferent() {
    final String field = "field1";
    final Object value = "Lorem Ipsum";

    DIFFERENT
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          MatchQueryBuilder mustNot =
                              (MatchQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .mustNot()
                                      .get(0);

                          assertNotNull(mustNot);
                          assertEquals(field, mustNot.fieldName());
                          assertEquals(value, mustNot.value());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleFilterEqual() {
    final String field = "field1";
    final Object value = "Lorem Ipsum";

    EQUAL
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          MatchQueryBuilder filter =
                              (MatchQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .filter()
                                      .get(0);

                          assertNotNull(filter);
                          assertEquals(field, filter.fieldName());
                          assertEquals(value, filter.value());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderByTwoFragmentLevelsUsingOR() {
    EQUAL
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request.filter("(x1:1 AND y1:1) OR (x1:2 AND y2:2)").build());
                          List<QueryBuilder> should =
                              ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                                  .should();

                          assertNotNull(should);
                          assertEquals(2, should.size());
                          assertEquals(2, ((BoolQueryBuilder) should.get(0)).filter().size());
                          assertEquals(2, ((BoolQueryBuilder) should.get(1)).filter().size());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderByTwoFragmentLevelsUsingAND() {
    EQUAL
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request.filter("(x1:1 OR y1:1) AND (x1:2 OR y2:2)").build());
                          List<QueryBuilder> filter =
                              ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                                  .filter();

                          assertNotNull(filter);
                          assertEquals(2, filter.size());
                          assertEquals(2, ((BoolQueryBuilder) filter.get(0)).should().size());
                          assertEquals(2, ((BoolQueryBuilder) filter.get(1)).should().size());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderByTwoFragmentLevelsUsingNOT() {
    EQUAL
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request
                                      .filter("NOT((x1:1 AND y1:1) OR (x1:2 AND y2:2))")
                                      .build());
                          List<QueryBuilder> mustNot =
                              ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                                  .mustNot();

                          assertNotNull(mustNot);
                          assertEquals(1, mustNot.size());

                          List<QueryBuilder> should = ((BoolQueryBuilder) mustNot.get(0)).should();
                          assertNotNull(should);
                          assertEquals(2, should.size());
                          assertEquals(2, ((BoolQueryBuilder) should.get(0)).filter().size());
                          assertEquals(2, ((BoolQueryBuilder) should.get(1)).filter().size());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleFilterGreater() {
    final String field = "field1";
    final Object value = 10;

    GREATER
        .getAlias()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          RangeQueryBuilder range =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .filter()
                                      .get(0);

                          assertEquals(field, range.fieldName());
                          assertEquals(value, range.from());
                          assertNull(range.to());
                          assertFalse(range.includeLower());
                          assertTrue(range.includeUpper());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleFilterGreaterEqual() {
    final String field = "field1";
    final Object value = 10;

    GREATER_EQUAL
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          RangeQueryBuilder range =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .filter()
                                      .get(0);

                          assertEquals(field, range.fieldName());
                          assertEquals(value, range.from());
                          assertNull(range.to());
                          assertTrue(range.includeLower());
                          assertTrue(range.includeUpper());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleFilterLess() {
    final String field = "field1";
    final Object value = 10;

    LESS.getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          RangeQueryBuilder range =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .filter()
                                      .get(0);

                          assertEquals(field, range.fieldName());
                          assertEquals(value, range.to());
                          assertNull(range.from());
                          assertTrue(range.includeLower());
                          assertFalse(range.includeUpper());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleFilterLessEqual() {
    final String field = "field1";
    final Object value = 10;

    LESS_EQUAL
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          RangeQueryBuilder range =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .filter()
                                      .get(0);

                          assertEquals(field, range.fieldName());
                          assertEquals(value, range.to());
                          assertNull(range.from());
                          assertTrue(range.includeLower());
                          assertTrue(range.includeUpper());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderByViewport() {

    String field = "field.location.geo_point";

    // Google nomenclature
    double northEastLat = 42.0;
    double northEastLon = -74.0;
    double southWestLat = -40.0;
    double southWestLon = -72.0;

    VIEWPORT
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request
                                      .filter(
                                          String.format(
                                              "%s %s [[%s,%s],[%s,%s]]",
                                              field,
                                              op,
                                              northEastLon,
                                              northEastLat,
                                              southWestLon,
                                              southWestLat))
                                      .build());
                          GeoBoundingBoxQueryBuilder geoBoundingBoxQueryBuilder =
                              (GeoBoundingBoxQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .filter()
                                      .get(0);

                          int delta = 0;
                          assertNotNull(geoBoundingBoxQueryBuilder);
                          assertEquals(field, geoBoundingBoxQueryBuilder.fieldName());
                          assertEquals(
                              northEastLat, geoBoundingBoxQueryBuilder.topLeft().getLat(), delta);
                          assertEquals(
                              southWestLon, geoBoundingBoxQueryBuilder.topLeft().getLon(), delta);
                          assertEquals(
                              southWestLat,
                              geoBoundingBoxQueryBuilder.bottomRight().getLat(),
                              delta);
                          assertEquals(
                              northEastLon,
                              geoBoundingBoxQueryBuilder.bottomRight().getLon(),
                              delta);
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderByPolygon() {
    String query = "field.location.geo_point POLYGON [[-1.1,2.2],[3.3,-4.4],[5.5,6.6]]";

    POLYGON
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(request.filter(query).build());
                          GeoPolygonQueryBuilder polygon =
                              (GeoPolygonQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .filter()
                                      .get(0);

                          assertNotNull(polygon);
                          assertFalse(polygon.ignoreUnmapped());
                          assertEquals("field.location.geo_point", polygon.fieldName());
                          assertEquals(GeoValidationMethod.STRICT, polygon.getValidationMethod());

                          // if the last point is different of the first, ES add a copy of the first
                          // point in the last array position
                          assertEquals(4, polygon.points().size());

                          // inverted (lat/lon) to adapt GeoJson format (lon/lat)
                          assertEquals(new GeoPoint(2.2, -1.1), polygon.points().get(0));
                          assertEquals(new GeoPoint(-4.4, 3.3), polygon.points().get(1));
                          assertEquals(new GeoPoint(6.6, 5.5), polygon.points().get(2));
                          assertEquals(new GeoPoint(2.2, -1.1), polygon.points().get(3));
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleFilterWithLike() {
    final String field = "field1.keyword";
    String value = "Break line\\nNew line with special chars: % \\% _ \\_ * ? \\a!";
    String expected = "Break line\nNew line with special chars: * % ? _ \\* \\? \\a!";

    LIKE.getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          WildcardQueryBuilder wildcardQueryBuilder =
                              (WildcardQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .filter()
                                      .get(0);

                          assertNotNull(wildcardQueryBuilder);
                          assertEquals(field, wildcardQueryBuilder.fieldName());
                          assertEquals(expected, wildcardQueryBuilder.value());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleFilterWithRange() {
    final String field = "field";
    final int from = 3, to = 5;

    RANGE
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request
                                      .filter(String.format("%s %s [%d,%d]", field, op, from, to))
                                      .build());
                          RangeQueryBuilder rangeQueryBuilder =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .filter()
                                      .get(0);

                          assertNotNull(rangeQueryBuilder);
                          assertEquals(field, rangeQueryBuilder.fieldName());
                          assertEquals(from, rangeQueryBuilder.from());
                          assertEquals(to, rangeQueryBuilder.to());
                          assertTrue(rangeQueryBuilder.includeLower());
                          assertTrue(rangeQueryBuilder.includeUpper());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleFilterWithRangeWhenNot() {
    final String field = "field";
    final int from = 5, to = 10;

    RANGE
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request
                                      .filter(
                                          String.format("NOT %s %s [%d,%d]", field, op, from, to))
                                      .build());
                          RangeQueryBuilder rangeQueryBuilder =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .mustNot()
                                      .get(0);

                          assertNotNull(rangeQueryBuilder);
                          assertEquals(field, rangeQueryBuilder.fieldName());
                          assertEquals(from, rangeQueryBuilder.from());
                          assertEquals(to, rangeQueryBuilder.to());
                          assertTrue(rangeQueryBuilder.includeLower());
                          assertTrue(rangeQueryBuilder.includeUpper());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleFilterIn() {
    final String field = "field1";
    final Object[] values = new Object[] {1, "\"string\"", 1.2, true};

    IN.getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request
                                      .filter(
                                          String.format(
                                              "%s %s %s", field, op, Arrays.toString(values)))
                                      .build());
                          TermsQueryBuilder terms =
                              (TermsQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .filter()
                                      .get(0);

                          assertEquals(field, terms.fieldName());
                          assertTrue(
                              asList(
                                      stream(values)
                                          .map(
                                              value -> {
                                                if (value instanceof String) {
                                                  String s = String.valueOf(value);
                                                  return s.replaceAll("\"", "");
                                                }

                                                return value;
                                              })
                                          .toArray())
                                  .equals(terms.values()));
                        }));
  }

  @Test
  public void shouldValidateQueyUsingInOperatorByIds() {
    final String field = "id";
    final Set<Object> values = newHashSet("\"123\"", 456, "\"7a8b9\"");

    IN.getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request
                                      .filter(
                                          String.format(
                                              "%s %s %s",
                                              field, op, Arrays.toString(values.toArray())))
                                      .build());
                          IdsQueryBuilder idsQueryBuilder =
                              (IdsQueryBuilder)
                                  ((BoolQueryBuilder)
                                          searchRequestBuilder.request().source().query())
                                      .filter()
                                      .get(0);

                          assertEquals("ids", idsQueryBuilder.getName());
                          assertEquals(
                              values
                                  .stream()
                                  .map(value -> value.toString().replaceAll("\"", ""))
                                  .collect(toSet()),
                              idsQueryBuilder.ids());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleOperatorAnd() {
    String fieldName1 = "field1";
    Object fieldValue1 = "string";

    String fieldName2 = "field2";
    Object fieldValue2 = 12345;

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              SearchRequestBuilder searchRequestBuilder =
                  queryAdapter.query(
                      request
                          .filter(
                              String.format(
                                  "%s:\"%s\" AND %s:%s",
                                  fieldName1, fieldValue1, fieldName2, fieldValue2))
                          .build());
              List<QueryBuilder> filter =
                  ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).filter();

              assertNotNull(filter);
              assertTrue(filter.size() == 2);
              assertEquals(fieldName1, ((MatchQueryBuilder) filter.get(0)).fieldName());
              assertEquals(fieldValue1, ((MatchQueryBuilder) filter.get(0)).value());
              assertEquals(fieldName2, ((MatchQueryBuilder) filter.get(1)).fieldName());
              assertEquals(fieldValue2, ((MatchQueryBuilder) filter.get(1)).value());
            });
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleOperatorOr() {
    String fieldName1 = "field1";
    Object fieldValue1 = "string";

    String fieldName2 = "field2";
    Object fieldValue2 = 12345;

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              SearchRequestBuilder searchRequestBuilder =
                  queryAdapter.query(
                      request
                          .filter(
                              String.format(
                                  "%s:\"%s\" OR %s:%s",
                                  fieldName1, fieldValue1, fieldName2, fieldValue2))
                          .build());
              List<QueryBuilder> should =
                  ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).should();

              assertNotNull(should);
              assertTrue(should.size() == 2);
              assertEquals(fieldName1, ((MatchQueryBuilder) should.get(0)).fieldName());
              assertEquals(fieldValue1, ((MatchQueryBuilder) should.get(0)).value());
              assertEquals(fieldName2, ((MatchQueryBuilder) should.get(1)).fieldName());
              assertEquals(fieldValue2, ((MatchQueryBuilder) should.get(1)).value());
            });
  }

  @Test
  public void shouldReturnSearchRequestBuilderWhenValueIsNullOnOperatorIsEqual() {
    String fieldName = "field1";
    List<Object> nullValues = newArrayList("NULL", null, "null");

    nullValues
        .parallelStream()
        .forEach(
            nullValue ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request
                                      .filter(String.format("%s:%s", fieldName, nullValue))
                                      .build());
                          List<QueryBuilder> mustNot =
                              ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                                  .mustNot();

                          ExistsQueryBuilder existsQueryBuilder =
                              (ExistsQueryBuilder) mustNot.get(0);
                          assertNotNull(existsQueryBuilder);
                          assertEquals(fieldName, existsQueryBuilder.fieldName());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWhenValueIsNullOnOperatorIsEqualWithNot() {
    String fieldName = "field1";
    List<Object> nullValues = newArrayList("NULL", null, "null");

    nullValues
        .parallelStream()
        .forEach(
            nullValue ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request
                                      .filter(String.format("NOT %s:%s", fieldName, nullValue))
                                      .build());
                          List<QueryBuilder> filter =
                              ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                                  .filter();

                          ExistsQueryBuilder existsQueryBuilder =
                              (ExistsQueryBuilder) filter.get(0);
                          assertNotNull(existsQueryBuilder);
                          assertEquals(fieldName, existsQueryBuilder.fieldName());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWhenValueIsNullOnOperatorIsDifferent() {
    String fieldName = "field1";
    List<Object> nullValues = newArrayList("NULL", null, "null");

    nullValues
        .parallelStream()
        .forEach(
            nullValue ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request
                                      .filter(String.format("%s<>%s", fieldName, nullValue))
                                      .build());
                          List<QueryBuilder> filter =
                              ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                                  .filter();

                          ExistsQueryBuilder existsQueryBuilder =
                              (ExistsQueryBuilder) filter.get(0);
                          assertNotNull(existsQueryBuilder);
                          assertEquals(fieldName, existsQueryBuilder.fieldName());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWhenValueIsNullOnOperatorIsDifferentWithNot() {
    String fieldName = "field1";
    List<Object> nullValues = newArrayList("NULL", null, "null");

    nullValues
        .parallelStream()
        .forEach(
            nullValue ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          SearchRequestBuilder searchRequestBuilder =
                              queryAdapter.query(
                                  request
                                      .filter(String.format("NOT %s<>%s", fieldName, nullValue))
                                      .build());
                          List<QueryBuilder> mustNot =
                              ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                                  .mustNot();

                          ExistsQueryBuilder existsQueryBuilder =
                              (ExistsQueryBuilder) mustNot.get(0);
                          assertNotNull(existsQueryBuilder);
                          assertEquals(fieldName, existsQueryBuilder.fieldName());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestBuilderWithSingleOperatorNot() {
    String fieldName1 = "field1";
    Object fieldValue1 = 1234324;

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              SearchRequestBuilder searchRequestBuilder =
                  queryAdapter.query(
                      request.filter(String.format("NOT %s:%s", fieldName1, fieldValue1)).build());
              MatchQueryBuilder mustNot =
                  (MatchQueryBuilder)
                      ((BoolQueryBuilder) searchRequestBuilder.request().source().query())
                          .mustNot()
                          .get(0);

              assertNotNull(mustNot);
              assertEquals(fieldName1, mustNot.fieldName());
              assertEquals(fieldValue1, mustNot.value());
            });
  }

  @Test
  public void shouldThrowExceptionWhenMinimalShouldMatchIsInvalid() {
    String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
    List<String> invalidMMs =
        Lists.newArrayList("-101%", "101%", "75%.1", "75%,1", "75%123", "75%a");

    invalidMMs.forEach(
        mm ->
            newArrayList(filterableRequest, fullRequest)
                .parallelStream()
                .forEach(
                    request -> {
                      boolean throwsException = false;
                      try {
                        queryAdapter.query(request.q(q).mm(mm).build());
                      } catch (IllegalArgumentException e) {
                        throwsException = true;
                      }
                      assertTrue(throwsException);
                    }));
  }

  /**
   * Full Tree Objects Representation (Recursive)
   *
   * <p>Request: SearchApiRequest { index=my_index, mm=50%, fields=[field1, field2:2.0, field3:5.0],
   * includeFields=[field1, field2], excludeFields=[field3, field4], sort=field1 ASC field2 DESC
   * field3 ASC, facets=[field1, field2], facetSize=10, q=Lorem Ipsum is simply dummy text of the
   * printing and typesetting, from=0, size=20, filter= (field1 EQUAL "string" OR field2 DIFFERENT
   * 5432 AND (field3 GREATER 3 AND (field4 LESS 8 OR field5 IN [1, "string", 1.2, true] AND
   * (field6.location VIEWPORT [[42.0, -74.0], [-40.0, -72.0]])))) }
   *
   * <p>1 + BoolQueryBuilder + filter - MultiMatchQueryBuilder 2 + BoolQueryBuilder + filter -
   * RangeQueryBuilder (field3) 3 + BoolQueryBuilder + filter - TermsQueryBuilder (field5) 4 +
   * BoolQueryBuilder + filter - GeoBoundingBoxQueryBuilder (field6) + should 3 - RangeQueryBuilder
   * (field4) + must_not - MatchQueryBuilder (field2) + should - MatchQueryBuilder (field1)
   */
  @Test
  public void shouldReturnSimpleSearchRequestBuilderWithRecursiveRequest() {

    // QueryString
    String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
    String fieldName1 = "field1";
    float boostValue1 = 1.0f; // default boost value

    String fieldName2 = "field2";
    float boostValue2 = 2.0f;

    String fieldName3 = "field3";
    float boostValue3 = 5.0f;
    Set<String> fields =
        Sets.newLinkedHashSet(
            newArrayList(
                String.format("%s", fieldName1),
                String.format("%s:%s", fieldName2, boostValue2),
                String.format("%s:%s", fieldName3, boostValue3)));
    String mm = "50%";

    // Filters
    String field1Name = "field1";
    String field1RelationalOperator = EQUAL.name();
    Object field1Value = "\"string\"";

    String field2Name = "field2";
    String field2RelationalOperator = DIFFERENT.name();
    Object field2Value = 5432;

    String field3Name = "field3";
    String field3RelationalOperator = GREATER.name();
    Object field3Value = 3;

    String field4Name = "field4";
    String field4RelationalOperator = LESS.name();
    Object field4Value = 8;

    String field5Name = "field5";
    String field5RelationalOperator = IN.name();
    Object[] field5Value = new Object[] {1, "\"string\"", 1.2, true};

    String field6Name = "field6.location.geo_point";
    String field6RelationalOperator = VIEWPORT.name();
    double northEastLat = 42.0;
    double northEastLon = -74.0;
    double southWestLat = -40.0;
    double southWestLon = -72.0;

    when(settingsAdapter.isTypeOf(INDEX_NAME, field6Name, MappingType.FIELD_TYPE_GEOPOINT))
        .thenReturn(true);

    String filter =
        String.format(
            "%s %s %s %s %s %s %s %s (%s %s %s %s (%s %s %s %s %s %s %s %s (%s %s [[%s,%s],[%s,%s]])))",
            field1Name,
            field1RelationalOperator,
            field1Value,
            OR.name(),
            field2Name,
            field2RelationalOperator,
            field2Value,
            AND.name(),
            field3Name,
            field3RelationalOperator,
            field3Value,
            AND.name(),
            field4Name,
            field4RelationalOperator,
            field4Value,
            OR.name(),
            field5Name,
            field5RelationalOperator,
            Arrays.toString(field5Value),
            AND.name(),
            field6Name,
            field6RelationalOperator,
            northEastLon,
            northEastLat,
            southWestLon,
            southWestLat);

    SearchApiRequest searchApiRequest =
        fullRequest.filter(filter).q(q).fields(fields).mm(mm).build();

    SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
    SearchSourceBuilder source = searchRequestBuilder.request().source();

    // index
    assertEquals(searchApiRequest.getIndex(), searchRequestBuilder.request().indices()[0]);

    // filters
    List<QueryBuilder> filterFirstLevel = ((BoolQueryBuilder) source.query()).filter();
    List<QueryBuilder> mustFirstLevel = ((BoolQueryBuilder) source.query()).must();
    List<QueryBuilder> mustNotFirstLevel = ((BoolQueryBuilder) source.query()).mustNot();
    List<QueryBuilder> shouldFirstLevel = ((BoolQueryBuilder) source.query()).should();

    assertNotNull(filterFirstLevel);
    assertNotNull(mustFirstLevel);
    assertNotNull(mustNotFirstLevel);
    assertNotNull(shouldFirstLevel);
    assertEquals(1, filterFirstLevel.size());
    assertEquals(1, mustFirstLevel.size());
    assertEquals(1, mustNotFirstLevel.size());
    assertEquals(1, shouldFirstLevel.size());

    // querystring
    MultiMatchQueryBuilder multiMatchQueryBuilder = (MultiMatchQueryBuilder) mustFirstLevel.get(0);
    Map<String, Float> fieldsAndWeights = new HashMap<>(3);
    fieldsAndWeights.put(fieldName1, boostValue1);
    fieldsAndWeights.put(fieldName2, boostValue2);
    fieldsAndWeights.put(fieldName3, boostValue3);
    assertNotNull(multiMatchQueryBuilder);
    assertEquals(q, multiMatchQueryBuilder.value());
    assertEquals(mm, multiMatchQueryBuilder.minimumShouldMatch());
    assertEquals(OR, multiMatchQueryBuilder.operator());
    assertTrue(fieldsAndWeights.equals(multiMatchQueryBuilder.fields()));

    // field 1
    MatchQueryBuilder shouldMatchField1 = (MatchQueryBuilder) shouldFirstLevel.get(0);
    assertEquals(field1Name, shouldMatchField1.fieldName());
    assertEquals(String.valueOf(field1Value).replaceAll("\"", ""), shouldMatchField1.value());
    assertEquals(OR, shouldMatchField1.operator());

    // field 2
    MatchQueryBuilder mustNotMatchField2 = (MatchQueryBuilder) mustNotFirstLevel.get(0);
    assertEquals(field2Name, mustNotMatchField2.fieldName());
    assertEquals(field2Value, mustNotMatchField2.value());

    // Second Level
    List<QueryBuilder> filterSecondLevel = ((BoolQueryBuilder) filterFirstLevel.get(0)).filter();
    assertTrue(filterSecondLevel.size() == 2);

    // field 3
    RangeQueryBuilder filterRangeSecondLevelField3 = (RangeQueryBuilder) filterSecondLevel.get(0);
    assertEquals(field3Name, filterRangeSecondLevelField3.fieldName());
    assertEquals(field3Value, filterRangeSecondLevelField3.from());
    assertNull(filterRangeSecondLevelField3.to());
    assertFalse(filterRangeSecondLevelField3.includeLower());
    assertTrue(filterRangeSecondLevelField3.includeUpper());

    BoolQueryBuilder queryBuilderThirdLevel = (BoolQueryBuilder) filterSecondLevel.get(1);
    List<QueryBuilder> shouldThirdLevel = queryBuilderThirdLevel.should(); // 1
    List<QueryBuilder> filterThirdLevel = queryBuilderThirdLevel.filter(); // 2

    assertTrue(shouldThirdLevel.size() == 1);
    assertTrue(filterThirdLevel.size() == 2);

    // field 4
    RangeQueryBuilder shouldRangeThirdLevelField4 = (RangeQueryBuilder) shouldThirdLevel.get(0);
    assertEquals(field4Name, shouldRangeThirdLevelField4.fieldName());
    assertEquals(field4Value, shouldRangeThirdLevelField4.to());
    assertNull(shouldRangeThirdLevelField4.from());
    assertTrue(shouldRangeThirdLevelField4.includeLower());
    assertFalse(shouldRangeThirdLevelField4.includeUpper());

    // field 5
    TermsQueryBuilder filterTermsThirdLevelField5 = (TermsQueryBuilder) filterThirdLevel.get(0);
    assertEquals(field5Name, filterTermsThirdLevelField5.fieldName());
    assertTrue(
        asList(
                stream(field5Value)
                    .map(
                        value -> {
                          if (value instanceof String) {
                            String s = String.valueOf(value);
                            return s.replaceAll("\"", "");
                          }

                          return value;
                        })
                    .toArray())
            .equals(filterTermsThirdLevelField5.values()));

    // field 6
    GeoBoundingBoxQueryBuilder filterViewportFouthLevelField6 =
        (GeoBoundingBoxQueryBuilder) ((BoolQueryBuilder) filterThirdLevel.get(1)).filter().get(0);
    int delta = 0;
    assertEquals(field6Name, filterViewportFouthLevelField6.fieldName());
    assertEquals(northEastLat, filterViewportFouthLevelField6.topLeft().getLat(), delta);
    assertEquals(southWestLon, filterViewportFouthLevelField6.topLeft().getLon(), delta);
    assertEquals(southWestLat, filterViewportFouthLevelField6.bottomRight().getLat(), delta);
    assertEquals(northEastLon, filterViewportFouthLevelField6.bottomRight().getLon(), delta);
  }

  @Test
  public void testPreparationQuery() {
    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              SearchRequestBuilder builder = queryAdapter.query(request.build());

              assertEquals(request.build().getIndex(), builder.request().indices()[0]);
              assertThat(builder.request().source().query(), instanceOf(BoolQueryBuilder.class));
            });
  }

  @Test
  public void shouldCreateContainsAllQuery() {
    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              final SearchRequestBuilder builder =
                  queryAdapter.query(request.filter("x CONTAINS_ALL [1,2,3]").build());
              final QueryBuilder query = builder.request().source().query();
              assertThat(query, instanceOf(BoolQueryBuilder.class));
              final BoolQueryBuilder boolQuery = (BoolQueryBuilder) query;
              final AtomicInteger counter = new AtomicInteger(1);
              assertEquals(3, boolQuery.filter().size());
              boolQuery
                  .filter()
                  .forEach(
                      filter -> {
                        assertThat(filter, instanceOf(MatchQueryBuilder.class));
                        final MatchQueryBuilder match = (MatchQueryBuilder) filter;
                        assertEquals("x", match.fieldName());
                        assertEquals(counter.getAndIncrement(), match.value());
                      });
            });
  }

  @Test
  public void shouldCreateNotContainsAllQuery() {
    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .forEach(
            request -> {
              final SearchRequestBuilder builder =
                  queryAdapter.query(request.filter("NOT x CONTAINS_ALL [1,2,3]").build());
              final QueryBuilder query = builder.request().source().query();
              assertThat(query, instanceOf(BoolQueryBuilder.class));
              final BoolQueryBuilder boolQuery = (BoolQueryBuilder) query;
              final AtomicInteger counter = new AtomicInteger(1);
              assertEquals(3, boolQuery.mustNot().size());
              boolQuery
                  .mustNot()
                  .forEach(
                      filter -> {
                        assertThat(filter, instanceOf(MatchQueryBuilder.class));
                        final MatchQueryBuilder match = (MatchQueryBuilder) filter;
                        assertEquals("x", match.fieldName());
                        assertEquals(counter.getAndIncrement(), match.value());
                      });
            });
  }

  private String format(final String field, final Object value, final String relationalOperator) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(field).append(" ").append(relationalOperator).append(" ");

    if (value instanceof String) {
      stringBuilder.append("\"").append(value).append("\"");
    } else {
      stringBuilder.append(value);
    }

    return stringBuilder.toString();
  }
}

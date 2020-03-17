package com.grupozap.search.api.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.grupozap.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SIZE;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_FACET_SIZE;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_MAPPING_META_FIELDS_ID;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_MAX_SIZE;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_QUERY_TIMEOUT_UNIT;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_QUERY_TIMEOUT_VALUE;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.QS_DEFAULT_FIELDS;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.QS_MM;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.facetParserFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldCacheFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserWithOutValidationFixture;
import static com.grupozap.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.grupozap.search.api.model.mapping.MappingType.FIELD_TYPE_GEOPOINT;
import static com.grupozap.search.api.model.query.LogicalOperator.AND;
import static com.grupozap.search.api.model.query.RelationalOperator.DIFFERENT;
import static com.grupozap.search.api.model.query.RelationalOperator.EQUAL;
import static com.grupozap.search.api.model.query.RelationalOperator.GREATER;
import static com.grupozap.search.api.model.query.RelationalOperator.GREATER_EQUAL;
import static com.grupozap.search.api.model.query.RelationalOperator.IN;
import static com.grupozap.search.api.model.query.RelationalOperator.LESS;
import static com.grupozap.search.api.model.query.RelationalOperator.LESS_EQUAL;
import static com.grupozap.search.api.model.query.RelationalOperator.LIKE;
import static com.grupozap.search.api.model.query.RelationalOperator.POLYGON;
import static com.grupozap.search.api.model.query.RelationalOperator.RANGE;
import static com.grupozap.search.api.model.query.RelationalOperator.VIEWPORT;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.elasticsearch.index.query.Operator.OR;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Sets;
import com.grupozap.search.api.listener.ESSortListener;
import com.grupozap.search.api.model.http.SearchApiRequestBuilder.BasicRequestBuilder;
import com.grupozap.search.api.model.listener.SearchSort;
import com.grupozap.search.api.model.mapping.MappingType;
import com.grupozap.search.api.service.parser.factory.DefaultFilterFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.GeoBoundingBoxQueryBuilder;
import org.elasticsearch.index.query.GeoPolygonQueryBuilder;
import org.elasticsearch.index.query.GeoValidationMethod;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ElasticsearchQueryAdapterTest extends SearchTransportClientMock {

  private QueryAdapter<GetRequest, SearchRequest> queryAdapter;
  private FilterQueryAdapter filterQueryAdapter;
  private DefaultFilterFactory defaultFilterFactory;
  private PageQueryAdapter pageQueryAdapter;
  private QueryStringAdapter queryStringAdapter;
  private FunctionScoreAdapter functionScoreAdapter;
  private FacetQueryAdapter facetQueryAdapter;
  private RankFeatureQueryAdapter rankFeatureQueryAdapter;
  private ESSortListener ESSortListener;

  @Mock private ElasticsearchSettingsAdapter settingsAdapter;

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

    var sourceFieldAdapter = mock(SourceFieldAdapter.class);

    this.pageQueryAdapter = new PageQueryAdapter();
    this.queryStringAdapter = new QueryStringAdapter(fieldCacheFixture());
    this.functionScoreAdapter = new FunctionScoreAdapter(fieldParserFixture());
    this.facetQueryAdapter = new FacetQueryAdapter(facetParserFixture());
    this.filterQueryAdapter = new FilterQueryAdapter(queryParserFixture());
    this.defaultFilterFactory =
        new DefaultFilterFactory(queryParserWithOutValidationFixture(), filterQueryAdapter);
    this.ESSortListener = mock(ESSortListener.class);
    this.rankFeatureQueryAdapter = new RankFeatureQueryAdapter(ESSortListener);

    this.queryAdapter =
        new ElasticsearchQueryAdapter(
            sourceFieldAdapter,
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

    doNothing().when(sourceFieldAdapter).apply(any(SearchSourceBuilder.class), any());
    doNothing().when(sourceFieldAdapter).apply(any(GetRequest.class), any());
    doNothing().when(settingsAdapter).checkIndex(any());
    doNothing().when(sortQueryAdapter).apply(any(), any());

    when(settingsAdapter.settingsByKey(INDEX_NAME, SHARDS)).thenReturn("8");
    when(settingsAdapter.isTypeOf(anyString(), anyString(), any(MappingType.class)))
        .thenReturn(false);
    when(this.ESSortListener.getSearchSort(INDEX_NAME))
        .thenReturn(new SearchSort.SearchSortBuilder().disabled(false).build());
  }

  @Test
  public void shouldReturnGetRequestBuilderByGetId() {
    var id = "123456";

    newArrayList(basicRequest, filterableRequest, fullRequest)
        .parallelStream()
        .map(BasicRequestBuilder::build)
        .forEach(
            searchApiRequest -> {
              var requestBuilder = queryAdapter.getById(searchApiRequest, id);
              assertEquals(id, requestBuilder.id());
              assertFalse(requestBuilder.realtime());
              assertEquals(searchApiRequest.getIndex(), requestBuilder.index());
            });
  }

  @Test
  public void shouldApplyTimeoutOnQueryBody() {
    var request = fullRequest.build();
    var searchRequest = queryAdapter.query(request);
    assertEquals(new TimeValue(100, TimeUnit.MILLISECONDS), searchRequest.source().timeout());
  }

  @Test
  public void shouldReturnSimpleSearchRequestWithBasicRequestPagination() {
    var request = fullRequest.build();
    var searchRequest = queryAdapter.query(request);
    var source = searchRequest.source();

    assertEquals(request.getIndex(), searchRequest.indices()[0]);
    assertEquals(request.getFrom(), source.from());
    assertEquals(request.getSize(), source.size());
  }

  @Test
  public void shouldReturnSearchRequestWithSimpleNestedObject() {
    final var field = "nested.field";
    final Object value = "Lorem Ipsum";

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .map(
            request ->
                queryAdapter.query(request.filter(format(field, value, EQUAL.name())).build()))
        .map(
            searchRequest ->
                (NestedQueryBuilder)
                    ((BoolQueryBuilder) searchRequest.source().query()).filter().get(0))
        .forEach(
            nestedQueryBuilder -> {
              assertNotNull(nestedQueryBuilder);
              assertTrue(
                  nestedQueryBuilder
                      .toString()
                      .contains("\"path\" : \"" + field.split("\\.")[0] + "\""));
              var filter =
                  (MatchQueryBuilder)
                      ((BoolQueryBuilder) nestedQueryBuilder.query()).filter().get(0);
              assertNotNull(filter);
              assertEquals(field, filter.fieldName());
              assertEquals(value, filter.value());
            });
  }

  @Test
  public void shouldReturnSearchRequestWithSingleFilterDifferent() {
    final var field = "field1";
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
                          var searchRequest =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          var mustNot =
                              (MatchQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
                                      .mustNot()
                                      .get(0);

                          assertNotNull(mustNot);
                          assertEquals(field, mustNot.fieldName());
                          assertEquals(value, mustNot.value());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestWithSingleFilterEqual() {
    final var field = "field1";
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
                          var searchRequest =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          var filter =
                              (MatchQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
                                      .filter()
                                      .get(0);

                          assertNotNull(filter);
                          assertEquals(field, filter.fieldName());
                          assertEquals(value, filter.value());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestByTwoFragmentLevelsUsingOR() {
    EQUAL
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(
                                  request.filter("(x1:1 AND y1:1) OR (x1:2 AND y2:2)").build());
                          var should = ((BoolQueryBuilder) searchRequest.source().query()).should();

                          assertNotNull(should);
                          assertEquals(2, should.size());
                          assertEquals(2, ((BoolQueryBuilder) should.get(0)).filter().size());
                          assertEquals(2, ((BoolQueryBuilder) should.get(1)).filter().size());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestByTwoFragmentLevelsUsingAND() {
    EQUAL
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(
                                  request.filter("(x1:1 OR y1:1) AND (x1:2 OR y2:2)").build());
                          var filter = ((BoolQueryBuilder) searchRequest.source().query()).filter();

                          assertNotNull(filter);
                          assertEquals(2, filter.size());
                          assertEquals(2, ((BoolQueryBuilder) filter.get(0)).should().size());
                          assertEquals(2, ((BoolQueryBuilder) filter.get(1)).should().size());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestByTwoFragmentLevelsUsingNOT() {
    EQUAL
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(
                                  request
                                      .filter("NOT((x1:1 AND y1:1) OR (x1:2 AND y2:2))")
                                      .build());
                          var mustNot =
                              ((BoolQueryBuilder) searchRequest.source().query()).mustNot();

                          assertNotNull(mustNot);
                          assertEquals(1, mustNot.size());

                          var should = ((BoolQueryBuilder) mustNot.get(0)).should();
                          assertNotNull(should);
                          assertEquals(2, should.size());
                          assertEquals(2, ((BoolQueryBuilder) should.get(0)).filter().size());
                          assertEquals(2, ((BoolQueryBuilder) should.get(1)).filter().size());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestWithSingleFilterGreater() {
    final var field = "field1";
    final Object value = 10L;

    GREATER
        .getAlias()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          var range =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
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
  public void shouldReturnSearchRequestWithSingleFilterGreaterEqual() {
    final var field = "field1";
    final Object value = 10L;

    GREATER_EQUAL
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          var range =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
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
  public void shouldReturnSearchRequestWithSingleFilterLess() {
    final var field = "field1";
    final Object value = 10L;

    LESS.getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          var range =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
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
  public void shouldReturnSearchRequestWithSingleFilterLessEqual() {
    final var field = "field1";
    final Object value = 10L;

    LESS_EQUAL
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          var range =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
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
  public void shouldReturnSearchRequestByViewport() {

    var field = "field.location.geo_point";

    // Google nomenclature
    var northEastLat = 42.0;
    var northEastLon = -74.0;
    var southWestLat = -40.0;
    var southWestLon = -72.0;

    VIEWPORT
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
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
                          var geoBoundingBoxQueryBuilder =
                              (GeoBoundingBoxQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
                                      .filter()
                                      .get(0);

                          var delta = 0;
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
  public void shouldReturnSearchRequestByPolygon() {
    var query = "field.location.geo_point POLYGON [[-1.1,2.2],[3.3,-4.4],[5.5,6.6]]";

    POLYGON
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest = queryAdapter.query(request.filter(query).build());
                          var polygon =
                              (GeoPolygonQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
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
  public void shouldReturnSearchRequestWithSingleFilterWithLike() {
    final var field = "field1.keyword";
    var value = "Break line\\nNew line with special chars: % \\% _ \\_ * ? \\a!";
    var expected = "Break line\nNew line with special chars: * % ? _ \\* \\? \\a!";

    LIKE.getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(request.filter(format(field, value, op)).build());
                          var wildcardQueryBuilder =
                              (WildcardQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
                                      .filter()
                                      .get(0);

                          assertNotNull(wildcardQueryBuilder);
                          assertEquals(field, wildcardQueryBuilder.fieldName());
                          assertEquals(expected, wildcardQueryBuilder.value());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestWithSingleFilterWithRange() {
    final var field = "field";
    final long from = 3L, to = 5L;

    RANGE
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(
                                  request
                                      .filter(String.format("%s %s [%d,%d]", field, op, from, to))
                                      .build());
                          var rangeQueryBuilder =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
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
  public void shouldReturnSearchRequestWithSingleFilterWithRangeWhenNot() {
    final var field = "field";
    final long from = 5L, to = 10L;

    RANGE
        .getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(
                                  request
                                      .filter(
                                          String.format("NOT %s %s [%d,%d]", field, op, from, to))
                                      .build());
                          var rangeQueryBuilder =
                              (RangeQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
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
  public void shouldReturnSearchRequestWithSingleFilterIn() {
    final var field = "field1";
    final var values = new Object[] {1L, "\"string\"", 1.2, true};

    IN.getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(
                                  request
                                      .filter(
                                          String.format(
                                              "%s %s %s", field, op, Arrays.toString(values)))
                                      .build());
                          var terms =
                              (TermsQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
                                      .filter()
                                      .get(0);

                          assertEquals(field, terms.fieldName());
                          getValue(values, terms);
                        }));
  }

  private void getValue(Object[] values, TermsQueryBuilder terms) {
    assertEquals(
        asList(
            stream(values)
                .map(
                    value -> {
                      if (value instanceof String) {
                        var s = String.valueOf(value);
                        return s.replaceAll("\"", "");
                      }

                      return value;
                    })
                .toArray()),
        terms.values());
  }

  @Test
  public void shouldValidateQueyUsingInOperatorByIds() {
    final var field = "id";
    final Set<Object> values = newHashSet("\"123\"", 456, "\"7a8b9\"");

    IN.getAlias()
        .parallelStream()
        .forEach(
            op ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(
                                  request
                                      .filter(
                                          String.format(
                                              "%s %s %s",
                                              field, op, Arrays.toString(values.toArray())))
                                      .build());
                          var idsQueryBuilder =
                              (IdsQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
                                      .filter()
                                      .get(0);

                          assertEquals("ids", idsQueryBuilder.getName());
                          assertEquals(
                              values.stream()
                                  .map(value -> value.toString().replaceAll("\"", ""))
                                  .collect(toSet()),
                              idsQueryBuilder.ids());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestWithSingleOperatorAnd() {
    var fieldName1 = "field1";
    Object fieldValue1 = "string";

    var fieldName2 = "field2";
    Object fieldValue2 = 12345L;

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .map(
            request ->
                queryAdapter.query(
                    request
                        .filter(
                            String.format(
                                "%s:\"%s\" AND %s:%s",
                                fieldName1, fieldValue1, fieldName2, fieldValue2))
                        .build()))
        .map(searchRequest -> ((BoolQueryBuilder) searchRequest.source().query()).filter())
        .forEach(
            filter -> {
              assertNotNull(filter);
              assertEquals(2, filter.size());
              assertEquals(fieldName1, ((MatchQueryBuilder) filter.get(0)).fieldName());
              assertEquals(fieldValue1, ((MatchQueryBuilder) filter.get(0)).value());
              assertEquals(fieldName2, ((MatchQueryBuilder) filter.get(1)).fieldName());
              assertEquals(fieldValue2, ((MatchQueryBuilder) filter.get(1)).value());
            });
  }

  @Test
  public void shouldReturnSearchRequestWithSingleOperatorOr() {
    var fieldName1 = "field1";
    Object fieldValue1 = "string";

    var fieldName2 = "field2";
    Object fieldValue2 = 12345L;

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .map(
            request ->
                queryAdapter.query(
                    request
                        .filter(
                            String.format(
                                "%s:\"%s\" OR %s:%s",
                                fieldName1, fieldValue1, fieldName2, fieldValue2))
                        .build()))
        .map(searchRequest -> ((BoolQueryBuilder) searchRequest.source().query()).should())
        .forEach(
            should -> {
              assertNotNull(should);
              assertEquals(2, should.size());
              assertEquals(fieldName1, ((MatchQueryBuilder) should.get(0)).fieldName());
              assertEquals(fieldValue1, ((MatchQueryBuilder) should.get(0)).value());
              assertEquals(fieldName2, ((MatchQueryBuilder) should.get(1)).fieldName());
              assertEquals(fieldValue2, ((MatchQueryBuilder) should.get(1)).value());
            });
  }

  @Test
  public void shouldReturnSearchRequestWhenValueIsNullOnOperatorIsEqual() {
    var fieldName = "field1";
    List<Object> nullValues = newArrayList("NULL", null, "null");

    nullValues
        .parallelStream()
        .forEach(
            nullValue ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(
                                  request
                                      .filter(String.format("%s:%s", fieldName, nullValue))
                                      .build());
                          var mustNot =
                              ((BoolQueryBuilder) searchRequest.source().query()).mustNot();

                          var existsQueryBuilder = (ExistsQueryBuilder) mustNot.get(0);
                          assertNotNull(existsQueryBuilder);
                          assertEquals(fieldName, existsQueryBuilder.fieldName());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestWhenValueIsNullOnOperatorIsEqualWithNot() {
    var fieldName = "field1";
    List<Object> nullValues = newArrayList("NULL", null, "null");

    nullValues
        .parallelStream()
        .forEach(
            nullValue ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(
                                  request
                                      .filter(String.format("NOT %s:%s", fieldName, nullValue))
                                      .build());
                          var filter = ((BoolQueryBuilder) searchRequest.source().query()).filter();

                          var existsQueryBuilder = (ExistsQueryBuilder) filter.get(0);
                          assertNotNull(existsQueryBuilder);
                          assertEquals(fieldName, existsQueryBuilder.fieldName());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestWhenValueIsNullOnOperatorIsDifferent() {
    var fieldName = "field1";
    List<Object> nullValues = newArrayList("NULL", null, "null");

    nullValues
        .parallelStream()
        .forEach(
            nullValue ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(
                                  request
                                      .filter(String.format("%s<>%s", fieldName, nullValue))
                                      .build());
                          var filter = ((BoolQueryBuilder) searchRequest.source().query()).filter();

                          var existsQueryBuilder = (ExistsQueryBuilder) filter.get(0);
                          assertNotNull(existsQueryBuilder);
                          assertEquals(fieldName, existsQueryBuilder.fieldName());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestWhenValueIsNullOnOperatorIsDifferentWithNot() {
    var fieldName = "field1";
    List<Object> nullValues = newArrayList("NULL", null, "null");

    nullValues
        .parallelStream()
        .forEach(
            nullValue ->
                newArrayList(filterableRequest, fullRequest)
                    .parallelStream()
                    .forEach(
                        request -> {
                          var searchRequest =
                              queryAdapter.query(
                                  request
                                      .filter(String.format("NOT %s<>%s", fieldName, nullValue))
                                      .build());
                          var mustNot =
                              ((BoolQueryBuilder) searchRequest.source().query()).mustNot();

                          var existsQueryBuilder = (ExistsQueryBuilder) mustNot.get(0);
                          assertNotNull(existsQueryBuilder);
                          assertEquals(fieldName, existsQueryBuilder.fieldName());
                        }));
  }

  @Test
  public void shouldReturnSearchRequestWithSingleOperatorNot() {
    var fieldName1 = "field1";
    Object fieldValue1 = 1234324L;

    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .map(
            request ->
                queryAdapter.query(
                    request.filter(String.format("NOT %s:%s", fieldName1, fieldValue1)).build()))
        .map(
            searchRequest ->
                (MatchQueryBuilder)
                    ((BoolQueryBuilder) searchRequest.source().query()).mustNot().get(0))
        .forEach(
            mustNot -> {
              assertNotNull(mustNot);
              assertEquals(fieldName1, mustNot.fieldName());
              assertEquals(fieldValue1, mustNot.value());
            });
  }

  @Test
  public void shouldThrowExceptionWhenMinimalShouldMatchIsInvalid() {
    var q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
    List<String> invalidMMs = newArrayList("-101%", "101%", "75%.1", "75%,1", "75%123", "75%a");

    invalidMMs.forEach(
        mm ->
            newArrayList(filterableRequest, fullRequest)
                .parallelStream()
                .forEach(
                    request -> {
                      var throwsException = false;
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
  public void shouldReturnSimpleSearchRequestWithRecursiveRequest() {

    // QueryString
    var q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
    var fieldName1 = "field1";
    var boostValue1 = 1.0f; // default boost value

    var fieldName2 = "field2";
    var boostValue2 = 2.0f;

    var fieldName3 = "field3";
    var boostValue3 = 5.0f;
    Set<String> fields =
        Sets.newLinkedHashSet(
            newArrayList(
                String.format("%s", fieldName1),
                String.format("%s:%s", fieldName2, boostValue2),
                String.format("%s:%s", fieldName3, boostValue3)));
    var mm = "50%";

    // Filters
    var field1Name = "field1";
    var field1RelationalOperator = EQUAL.name();
    Object field1Value = "\"string\"";

    var field2Name = "field2";
    var field2RelationalOperator = DIFFERENT.name();
    Object field2Value = 5432L;

    var field3Name = "field3";
    var field3RelationalOperator = GREATER.name();
    Object field3Value = 3L;

    var field4Name = "field4";
    var field4RelationalOperator = LESS.name();
    Object field4Value = 8L;

    var field5Name = "field5";
    var field5RelationalOperator = IN.name();
    var field5Value = new Object[] {1L, "\"string\"", 1.2, true};

    var field6Name = "field6.location.geo_point";
    var field6RelationalOperator = VIEWPORT.name();
    var northEastLat = 42.0;
    var northEastLon = -74.0;
    var southWestLat = -40.0;
    var southWestLon = -72.0;

    when(settingsAdapter.isTypeOf(INDEX_NAME, field6Name, FIELD_TYPE_GEOPOINT)).thenReturn(true);

    var filter =
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

    var searchApiRequest = fullRequest.filter(filter).q(q).fields(fields).mm(mm).build();

    var searchRequest = queryAdapter.query(searchApiRequest);
    var source = searchRequest.source();

    // index
    assertEquals(searchApiRequest.getIndex(), searchRequest.indices()[0]);

    // filters
    var filterFirstLevel = ((BoolQueryBuilder) source.query()).filter();
    var mustFirstLevel = ((BoolQueryBuilder) source.query()).must();
    var mustNotFirstLevel = ((BoolQueryBuilder) source.query()).mustNot();
    var shouldFirstLevel = ((BoolQueryBuilder) source.query()).should();

    assertNotNull(filterFirstLevel);
    assertNotNull(mustFirstLevel);
    assertNotNull(mustNotFirstLevel);
    assertNotNull(shouldFirstLevel);
    assertEquals(1, filterFirstLevel.size());
    assertEquals(1, mustFirstLevel.size());
    assertEquals(1, mustNotFirstLevel.size());
    assertEquals(1, shouldFirstLevel.size());

    // querystring
    var multiMatchQueryBuilder = (MultiMatchQueryBuilder) mustFirstLevel.get(0);
    Map<String, Float> fieldsAndWeights = new HashMap<>(3);
    fieldsAndWeights.put(fieldName1, boostValue1);
    fieldsAndWeights.put(fieldName2, boostValue2);
    fieldsAndWeights.put(fieldName3, boostValue3);
    assertNotNull(multiMatchQueryBuilder);
    assertEquals(q, multiMatchQueryBuilder.value());
    assertEquals(mm, multiMatchQueryBuilder.minimumShouldMatch());
    assertEquals(OR, multiMatchQueryBuilder.operator());
    assertEquals(fieldsAndWeights, multiMatchQueryBuilder.fields());

    // field 1
    var shouldMatchField1 = (MatchQueryBuilder) shouldFirstLevel.get(0);
    assertEquals(field1Name, shouldMatchField1.fieldName());
    assertEquals(String.valueOf(field1Value).replaceAll("\"", ""), shouldMatchField1.value());
    assertEquals(OR, shouldMatchField1.operator());

    // field 2
    var mustNotMatchField2 = (MatchQueryBuilder) mustNotFirstLevel.get(0);
    assertEquals(field2Name, mustNotMatchField2.fieldName());
    assertEquals(field2Value, mustNotMatchField2.value());

    // Second Level
    var filterSecondLevel = ((BoolQueryBuilder) filterFirstLevel.get(0)).filter();
    assertEquals(2, filterSecondLevel.size());

    // field 3
    var filterRangeSecondLevelField3 = (RangeQueryBuilder) filterSecondLevel.get(0);
    assertEquals(field3Name, filterRangeSecondLevelField3.fieldName());
    assertEquals(field3Value, filterRangeSecondLevelField3.from());
    assertNull(filterRangeSecondLevelField3.to());
    assertFalse(filterRangeSecondLevelField3.includeLower());
    assertTrue(filterRangeSecondLevelField3.includeUpper());

    var queryBuilderThirdLevel = (BoolQueryBuilder) filterSecondLevel.get(1);
    var shouldThirdLevel = queryBuilderThirdLevel.should(); // 1
    var filterThirdLevel = queryBuilderThirdLevel.filter(); // 2

    assertEquals(1, shouldThirdLevel.size());
    assertEquals(2, filterThirdLevel.size());

    // field 4
    var shouldRangeThirdLevelField4 = (RangeQueryBuilder) shouldThirdLevel.get(0);
    assertEquals(field4Name, shouldRangeThirdLevelField4.fieldName());
    assertEquals(field4Value, shouldRangeThirdLevelField4.to());
    assertNull(shouldRangeThirdLevelField4.from());
    assertTrue(shouldRangeThirdLevelField4.includeLower());
    assertFalse(shouldRangeThirdLevelField4.includeUpper());

    // field 5
    var filterTermsThirdLevelField5 = (TermsQueryBuilder) filterThirdLevel.get(0);
    assertEquals(field5Name, filterTermsThirdLevelField5.fieldName());
    getValue(field5Value, filterTermsThirdLevelField5);

    // field 6
    var filterViewportFouthLevelField6 =
        (GeoBoundingBoxQueryBuilder) ((BoolQueryBuilder) filterThirdLevel.get(1)).filter().get(0);
    var delta = 0;
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
              var builder = queryAdapter.query(request.build());

              assertEquals(request.build().getIndex(), builder.indices()[0]);
              assertThat(builder.source().query(), instanceOf(BoolQueryBuilder.class));
            });
  }

  @Test
  public void shouldCreateContainsAllQuery() {
    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .map(request -> queryAdapter.query(request.filter("x CONTAINS_ALL [1,2,3]").build()))
        .map(builder -> builder.source().query())
        .forEach(
            query -> {
              assertThat(query, instanceOf(BoolQueryBuilder.class));
              final var boolQuery = (BoolQueryBuilder) query;
              final var counter = new AtomicLong(1);
              assertEquals(3, boolQuery.filter().size());
              boolQuery
                  .filter()
                  .forEach(
                      filter -> {
                        assertThat(filter, instanceOf(MatchQueryBuilder.class));
                        final var match = (MatchQueryBuilder) filter;
                        assertEquals("x", match.fieldName());
                        assertEquals(counter.getAndIncrement(), match.value());
                      });
            });
  }

  @Test
  public void shouldCreateNotContainsAllQuery() {
    newArrayList(filterableRequest, fullRequest)
        .parallelStream()
        .map(request -> queryAdapter.query(request.filter("NOT x CONTAINS_ALL [1,2,3]").build()))
        .map(builder -> builder.source().query())
        .forEach(
            query -> {
              assertThat(query, instanceOf(BoolQueryBuilder.class));
              final var boolQuery = (BoolQueryBuilder) query;
              final var counter = new AtomicLong(1);
              assertEquals(3, boolQuery.mustNot().size());
              boolQuery
                  .mustNot()
                  .forEach(
                      filter -> {
                        assertThat(filter, instanceOf(MatchQueryBuilder.class));
                        final var match = (MatchQueryBuilder) filter;
                        assertEquals("x", match.fieldName());
                        assertEquals(counter.getAndIncrement(), match.value());
                      });
            });
  }

  @Test
  public void shouldReturnSearchRequestWithSingleFilterWithAnAliasEqual() {
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
                          var searchRequest =
                              queryAdapter.query(
                                  request.filter(format("field_before_alias", value, op)).build());
                          var filter =
                              (MatchQueryBuilder)
                                  ((BoolQueryBuilder) searchRequest.source().query())
                                      .filter()
                                      .get(0);

                          assertNotNull(filter);
                          assertEquals("field_after_alias", filter.fieldName());
                          assertEquals(value, filter.value());
                        }));
  }

  private String format(final String field, final Object value, final String relationalOperator) {
    var stringBuilder = new StringBuilder();
    stringBuilder.append(field).append(" ").append(relationalOperator).append(" ");

    if (value instanceof String) {
      stringBuilder.append("\"").append(value).append("\"");
    } else {
      stringBuilder.append(value);
    }

    return stringBuilder.toString();
  }
}

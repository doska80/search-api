package com.vivareal.search.api.adapter;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.vivareal.search.api.fixtures.model.SearchApiRequestBuilder;
import com.vivareal.search.api.model.SearchApiRequest;
import org.assertj.core.util.Lists;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.MockTransportClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.fixtures.model.SearchApiRequestBuilder.INDEX_NAME;
import static com.vivareal.search.api.model.query.LogicalOperator.AND;
import static com.vivareal.search.api.model.query.RelationalOperator.*;
import static org.elasticsearch.index.query.Operator.OR;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * Created by leandropereirapinto on 7/3/17.
 */
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
public class ElasticsearchQueryAdapterTest {

    private QueryAdapter<GetRequestBuilder, SearchRequestBuilder> queryAdapter;

    private TransportClient transportClient;

    @Mock
    private SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;

    @Before
    public void setup() {
        initMocks(this);

        this.queryAdapter = spy(new ElasticsearchQueryAdapter());

        this.transportClient = new MockTransportClient(Settings.EMPTY);

        // initialize variables
        setField(this.queryAdapter, "transportClient", transportClient);
        setField(this.queryAdapter, "settingsAdapter", settingsAdapter);
        setField(this.queryAdapter, "queryDefaultFields", "title.raw,description.raw:2,address.street.raw:5");
        setField(this.queryAdapter, "queryDefaultMM", "75%");
        setField(this.queryAdapter, "facetSize", 20);

        doNothing().when(settingsAdapter).checkIndex(any(SearchApiRequest.class));
        when(settingsAdapter.settingsByKey(INDEX_NAME, SHARDS)).thenReturn("8");
    }

    @After
    public void closeClient() {
        this.transportClient.close();
    }

    @Test
    public void shouldReturnGetRequestBuilderByGetId() {
        String id = "123456";

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().basicRequest();
        GetRequestBuilder requestBuilder = queryAdapter.getById(searchApiRequest, id);

        assertEquals(id, requestBuilder.request().id());
        assertEquals(searchApiRequest.getIndex(), requestBuilder.request().index());
        assertEquals(searchApiRequest.getIndex(), requestBuilder.request().type());
    }

    @Test
    public void shouldReturnSimpleSearchRequestBuilderWithBasicRequest() {
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        SearchSourceBuilder source = searchRequestBuilder.request().source();

        assertEquals(searchApiRequest.getIndex(), searchRequestBuilder.request().indices()[0]);
        assertEquals(searchApiRequest.getFrom().intValue(), source.from());
        assertEquals(searchApiRequest.getSize().intValue(), source.size());
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleFilterDifferent() {
        final String field = "field1";
        final Object value = "Lorem Ipsum";

        getOperators(DIFFERENT).forEach(
            op -> {
                SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(format(field, value, op)).basicRequest();
                SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
                MatchQueryBuilder mustNot = (MatchQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).mustNot().get(0);

                assertNotNull(mustNot);
                assertEquals(field, mustNot.fieldName());
                assertEquals(value, mustNot.value());
            }
        );
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleFilterEqual() {
        final String field = "field1";
        final Object value = "Lorem Ipsum";

        getOperators(EQUAL).forEach(
            op -> {
                SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(format(field, value, op)).basicRequest();
                SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
                MatchQueryBuilder must = (MatchQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

                assertNotNull(must);
                assertEquals(field, must.fieldName());
                assertEquals(value, must.value());
            }
        );
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleFilterGreater() {
        final String field = "field1";
        final Object value = 10;

        getOperators(GREATER).forEach(
            op -> {
                SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(format(field, value, op)).basicRequest();
                SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
                RangeQueryBuilder range = (RangeQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

                assertEquals(field, range.fieldName());
                assertEquals(value, range.from());
                assertNull(range.to());
                assertEquals(false, range.includeLower());
                assertEquals(true, range.includeUpper());
            }
        );
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleFilterGreaterEqual() {
        final String field = "field1";
        final Object value = 10;

        getOperators(GREATER_EQUAL).forEach(
            op -> {
                SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(format(field, value, op)).basicRequest();
                SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
                RangeQueryBuilder range = (RangeQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

                assertEquals(field, range.fieldName());
                assertEquals(value, range.from());
                assertNull(range.to());
                assertEquals(true, range.includeLower());
                assertEquals(true, range.includeUpper());
            }
        );
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleFilterLess() {
        final String field = "field1";
        final Object value = 10;

        getOperators(LESS).forEach(
            op -> {
                SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(format(field, value, op)).basicRequest();
                SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
                RangeQueryBuilder range = (RangeQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

                assertEquals(field, range.fieldName());
                assertEquals(value, range.to());
                assertNull(range.from());
                assertEquals(true, range.includeLower());
                assertEquals(false, range.includeUpper());
            }
        );
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleFilterLessEqual() {
        final String field = "field1";
        final Object value = 10;

        getOperators(LESS_EQUAL).forEach(
            op -> {
                SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(format(field, value, op)).basicRequest();
                SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
                RangeQueryBuilder range = (RangeQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

                assertEquals(field, range.fieldName());
                assertEquals(value, range.to());
                assertNull(range.from());
                assertEquals(true, range.includeLower());
                assertEquals(true, range.includeUpper());
            }
        );
    }

    @Test
    public void shouldReturnSearchRequestBuilderByViewport() {

        String field = "field.location";

        // Google nomenclature
        double northEastLat = 42.0;
        double northEastLon = -74.0;
        double southWestLat = -40.0;
        double southWestLon = -72.0;

        getOperators(VIEWPORT).forEach(
            op -> {
                SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(String.format("%s %s [%s,%s;%s,%s]", field, op, northEastLat, northEastLon, southWestLat, southWestLon)).basicRequest();
                SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
                GeoBoundingBoxQueryBuilder geoBoundingBoxQueryBuilder = (GeoBoundingBoxQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

                int delta = 0;
                assertNotNull(geoBoundingBoxQueryBuilder);
                assertEquals(field, geoBoundingBoxQueryBuilder.fieldName());
                assertEquals(northEastLat, geoBoundingBoxQueryBuilder.topLeft().getLat(), delta);
                assertEquals(southWestLon, geoBoundingBoxQueryBuilder.topLeft().getLon(), delta);
                assertEquals(southWestLat, geoBoundingBoxQueryBuilder.bottomRight().getLat(), delta);
                assertEquals(northEastLon, geoBoundingBoxQueryBuilder.bottomRight().getLon(), delta);
            }
        );
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleFilterIn() {
        final String field = "field1";
        final Object[] values = new Object[]{1, "\"string\"", 1.2, true};

        getOperators(IN).forEach(
            op -> {
                SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(String.format("%s %s %s", field, op, Arrays.toString(values))).basicRequest();
                SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
                TermsQueryBuilder terms = (TermsQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

                assertEquals(field, terms.fieldName());
                assertTrue(Arrays.asList(Arrays.stream(values).map(value -> {

                    if (value instanceof String) {
                        String s = String.valueOf(value);
                        return s.replaceAll("\"", "");
                    }

                    return value;
                }).toArray()).equals(terms.values()));
            }
        );
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleOperatorAnd() {
        String fieldName1 = "field1";
        Object fieldValue1 = "string";

        String fieldName2 = "field2";
        Object fieldValue2 = 12345;

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(String.format("%s:\"%s\" AND %s:%s", fieldName1, fieldValue1, fieldName2, fieldValue2)).basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        List<QueryBuilder> must = ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must();

        assertNotNull(must);
        assertTrue(must.size() == 2);
        assertEquals(fieldName1, ((MatchQueryBuilder) must.get(0)).fieldName());
        assertEquals(fieldValue1, ((MatchQueryBuilder) must.get(0)).value());
        assertEquals(fieldName2, ((MatchQueryBuilder) must.get(1)).fieldName());
        assertEquals(fieldValue2, ((MatchQueryBuilder) must.get(1)).value());
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleOperatorOr() {
        String fieldName1 = "field1";
        Object fieldValue1 = "string";

        String fieldName2 = "field2";
        Object fieldValue2 = 12345;

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(String.format("%s:\"%s\" OR %s:%s", fieldName1, fieldValue1, fieldName2, fieldValue2)).basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        List<QueryBuilder> should = ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).should();

        assertNotNull(should);
        assertTrue(should.size() == 2);
        assertEquals(fieldName1, ((MatchQueryBuilder) should.get(0)).fieldName());
        assertEquals(fieldValue1, ((MatchQueryBuilder) should.get(0)).value());
        assertEquals(fieldName2, ((MatchQueryBuilder) should.get(1)).fieldName());
        assertEquals(fieldValue2, ((MatchQueryBuilder) should.get(1)).value());
    }

    @Test
    public void shouldReturnSearchRequestBuilderWhenValueIsNull() {
        String fieldName = "field1";
        List<Object> nullValues = newArrayList("NULL", null, "null");

        nullValues.forEach(
            nullValue -> {
                SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(String.format("%s:%s", fieldName, nullValue)).basicRequest();
                SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
                List<QueryBuilder> mustNot = ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).mustNot();

                List<RangeQueryBuilder> shouldClauses = (List) ((BoolQueryBuilder) mustNot.get(0)).should();

                assertNotNull(shouldClauses);
                assertTrue(shouldClauses.size() == 2);

                assertEquals(fieldName, shouldClauses.get(0).fieldName());
                assertEquals(0, shouldClauses.get(0).to());
                assertNull(shouldClauses.get(0).from());
                assertEquals(true, shouldClauses.get(0).includeLower());
                assertEquals(true, shouldClauses.get(0).includeUpper());

                assertEquals(fieldName, shouldClauses.get(1).fieldName());
                assertEquals(0, shouldClauses.get(1).from());
                assertNull(shouldClauses.get(1).to());
                assertEquals(true, shouldClauses.get(1).includeLower());
                assertEquals(true, shouldClauses.get(1).includeUpper());
            }
        );
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleOperatorNot() {
        String fieldName1 = "field1";
        Object fieldValue1 = 1234324;

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(String.format("NOT %s:%s", fieldName1, fieldValue1)).basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        MatchQueryBuilder mustNot = (MatchQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).mustNot().get(0);

        assertNotNull(mustNot);
        assertEquals(fieldName1, mustNot.fieldName());
        assertEquals(fieldValue1, mustNot.value());
    }

    @Test
    public void shouldReturnSearchRequestBuilderByFacets() {
        ArrayList<String> facets = Lists.newArrayList("field1", "field2", "field3");

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().facets(facets).facetSize(10).basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        List<AggregationBuilder> aggregations = searchRequestBuilder.request().source().aggregations().getAggregatorFactories();

        assertNotNull(aggregations);
        assertTrue(aggregations.size() == facets.size());

        assertTrue(searchRequestBuilder.toString().contains("\"size\" : 10"));
        assertTrue(searchRequestBuilder.toString().contains("\"shard_size\" : 8"));

        int facet1 = 0;
        assertEquals(facets.get(facet1), ((TermsAggregationBuilder) aggregations.get(facet1)).field());
        assertFalse(Terms.Order.count(true) == (((TermsAggregationBuilder) aggregations.get(facet1)).order()));

        int facet2 = 1;
        assertEquals(facets.get(facet2), ((TermsAggregationBuilder) aggregations.get(facet2)).field());
        assertFalse(Terms.Order.count(true) == (((TermsAggregationBuilder) aggregations.get(facet2)).order()));

        int facet3 = 2;
        assertEquals(facets.get(facet3), ((TermsAggregationBuilder) aggregations.get(facet3)).field());
        assertFalse(Terms.Order.count(true) == (((TermsAggregationBuilder) aggregations.get(facet3)).order()));

    }

    @Test
    public void shouldReturnSearchRequestBuilderSortedBy() {
        String fieldName1 = "field1";
        SortOrder fieldValue1 = ASC;

        String fieldName2 = "field2";
        SortOrder fieldValue2 = DESC;

        String fieldName3 = "field3";
        SortOrder fieldValue3 = ASC;

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().sort(String.format("%s %s, %s %s, %s %s", fieldName1, fieldValue1.name(), fieldName2, fieldValue2.name(), fieldName3, fieldValue3.name())).basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        List<FieldSortBuilder> sorts = (List) searchRequestBuilder.request().source().sorts();

        assertNotNull(sorts);
        assertTrue(sorts.size() == 3);

        assertEquals(fieldName1, sorts.get(0).getFieldName());
        assertEquals(fieldValue1, sorts.get(0).order());

        assertEquals(fieldName2, sorts.get(1).getFieldName());
        assertEquals(fieldValue2, sorts.get(1).order());

        assertEquals(fieldName3, sorts.get(2).getFieldName());
        assertEquals(fieldValue3, sorts.get(2).order());
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSpecifiedFieldSources() {
        ArrayList<String> includeFields = Lists.newArrayList("field1", "field2", "field3");
        ArrayList<String> excludeFields = Lists.newArrayList("field3", "field4");

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().includeFields(includeFields).excludeFields(excludeFields).basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        FetchSourceContext fetchSourceContext = searchRequestBuilder.request().source().fetchSource();

        assertNotNull(fetchSourceContext);

        assertEquals(includeFields.size(), fetchSourceContext.includes().length);
        assertTrue(includeFields.containsAll(Arrays.asList(fetchSourceContext.includes())));

        assertEquals((excludeFields.size() - 1), fetchSourceContext.excludes().length);
        assertTrue(Arrays.asList(fetchSourceContext.excludes()).contains(excludeFields.get(1)));
    }

    @Test
    public void shouldReturnSimpleSearchRequestBuilderByQueryString() {
        String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().q(q).basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

        assertNotNull(queryStringQueryBuilder);
        assertEquals(q, queryStringQueryBuilder.queryString());
        assertEquals(OR, queryStringQueryBuilder.defaultOperator());
    }

    @Test
    public void shouldReturnSimpleSearchRequestBuilderByQueryStringWithSpecifiedFieldToSearch() {
        String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";

        String fieldName1 = "field1";
        float boostValue1 = 1.0f; // default boost value

        String fieldName2 = "field2.raw";
        float boostValue2 = 2.0f;

        String fieldName3 = "field3";
        float boostValue3 = 5.0f;

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().q(q).fields(newArrayList(String.format("%s", fieldName1), String.format("%s:%s", fieldName2, boostValue2), String.format("%s:%s", fieldName3, boostValue3))).basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

        assertNotNull(queryStringQueryBuilder);
        assertEquals(q, queryStringQueryBuilder.queryString());

        Map<String, Float> fieldsAndWeights = new HashMap<>(3);
        fieldsAndWeights.put(fieldName1, boostValue1);
        fieldsAndWeights.put(fieldName2, boostValue2);
        fieldsAndWeights.put(fieldName3, boostValue3);

        assertTrue(fieldsAndWeights.equals(queryStringQueryBuilder.fields()));
    }

    @Test
    public void shouldReturnSearchRequestBuilderByQueryStringWithValidMinimalShouldMatch() {
        String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
        List<String> validMMs = Lists.newArrayList("-100%", "100%", "75%", "-2");

        validMMs.forEach(
            mm -> {
                SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().q(q).mm(mm).basicRequest();
                SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
                QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

                assertNotNull(queryStringQueryBuilder);
                assertEquals(q, queryStringQueryBuilder.queryString());
                assertEquals(mm, queryStringQueryBuilder.minimumShouldMatch());
                assertEquals(OR, queryStringQueryBuilder.defaultOperator());
            }
        );
    }

    @Test
    public void shouldThrowExceptionWhenMinimalShouldMatchIsInvalid() {
        String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
        List<String> invalidMMs = Lists.newArrayList("-101%", "101%", "75%.1", "75%,1", "75%123", "75%a");

        invalidMMs.forEach(
            mm -> {
                boolean throwsException = false;
                try {
                    SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().q(q).mm(mm).basicRequest();
                    queryAdapter.query(searchApiRequest);
                } catch (IllegalArgumentException e) {
                    throwsException = true;
                }
                assertTrue(throwsException);
            }
        );
    }

    /*
    * Full Tree Objects Representation (Recursive)
    *
    * Request: SearchApiRequest {
    *   index=my_index,
    *   mm=50%,
    *   fields=[field1, field2.raw:2.0, field3:5.0],
    *   includeFields=[field1, field2],
    *   excludeFields=[field3, field4],
    *   sort=field1 ASC field2 DESC field3 ASC,
    *   facets=[field1, field2],
    *   facetSize=10,
    *   q=Lorem Ipsum is simply dummy text of the printing and typesetting,
    *   from=0,
    *   size=20,
    *   filter=
    *       (field1 EQUAL "string" OR field2 DIFFERENT 5432 AND
    *           (field3 GREATER 3 AND
    *               (field4 LESS 8 OR field5 IN [1, "string", 1.2, true] AND
    *                   (field6.location VIEWPORT [[42.0, -74.0], [-40.0, -72.0]]))))
    * }
    *
    * 1 + BoolQueryBuilder
    * 	+ must
    * 		- QueryStringQueryBuilder
    * 		2 + BoolQueryBuilder
    * 			+ must
    * 				- RangeQueryBuilder (field3)
    * 				3 + BoolQueryBuilder
    * 					+ must
    * 						- TermsQueryBuilder (field5)
    * 						4 + BoolQueryBuilder
    * 							+ must
    * 								- GeoBoundingBoxQueryBuilder (field6)
    * 					+ should
    * 						3 - RangeQueryBuilder (field4)
    * 	+ must_not
    * 		- MatchQueryBuilder (field2)
    * 	+ should
    * 		- MatchQueryBuilder (field1)
    */
    @Test
    public void shouldReturnSimpleSearchRequestBuilderWithRecursiveRequest() {

        // Display results
        List<String> includeFields  = Lists.newArrayList("field1", "field2");
        List<String> excludeFields = Lists.newArrayList("field3", "field4");

        // Sort
        String sortFieldName1 = "field1";
        SortOrder sortFieldValue1 = ASC;

        String sortFieldName2 = "field2";
        SortOrder sortFieldValue2 = DESC;

        String sortFieldName3 = "field3";
        SortOrder sortFieldValue3 = ASC;

        String sort = String.format("%s %s, %s %s, %s %s", sortFieldName1, sortFieldValue1.name(), sortFieldName2, sortFieldValue2.name(), sortFieldName3, sortFieldValue3.name());

        // Facets
        List<String> facets = Lists.newArrayList("field1", "field2");
        Integer facetSize = 10;

        // QueryString
        String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
        String fieldName1 = "field1";
        float boostValue1 = 1.0f; // default boost value

        String fieldName2 = "field2.raw";
        float boostValue2 = 2.0f;

        String fieldName3 = "field3";
        float boostValue3 = 5.0f;
        List<String> fields = newArrayList(String.format("%s", fieldName1), String.format("%s:%s", fieldName2, boostValue2), String.format("%s:%s", fieldName3, boostValue3));
        String mm = "50%";

        // Pagination
        Integer from = 40;
        Integer size = 20;

        // Filters
        String field1Name = "field1";
        String field1RelationalOperator = getOperators(EQUAL).get(1);
        Object field1Value = "\"string\"";

        String field2Name = "field2";
        String field2RelationalOperator = getOperators(DIFFERENT).get(1);
        Object field2Value = 5432;

        String field3Name = "field3";
        String field3RelationalOperator = getOperators(GREATER).get(1);
        Object field3Value = 3;

        String field4Name = "field4";
        String field4RelationalOperator = getOperators(LESS).get(1);
        Object field4Value = 8;

        String field5Name = "field5";
        String field5RelationalOperator = getOperators(IN).get(0);
        Object[] field5Value = new Object[]{1, "\"string\"", 1.2, true};

        String field6Name = "field6.location";
        String field6RelationalOperator = getOperators(VIEWPORT).get(0);
        double northEastLat = 42.0;
        double northEastLon = -74.0;
        double southWestLat = -40.0;
        double southWestLon = -72.0;

        String filter = String.format("%s %s %s %s %s %s %s %s (%s %s %s %s (%s %s %s %s %s %s %s %s (%s %s [%s,%s;%s,%s])))",
            field1Name, field1RelationalOperator, field1Value, OR.name(),
            field2Name, field2RelationalOperator, field2Value, AND.name(),
            field3Name, field3RelationalOperator, field3Value, AND.name(),
            field4Name, field4RelationalOperator, field4Value, OR.name(),
            field5Name, field5RelationalOperator, Arrays.toString(field5Value), AND.name(),
            field6Name, field6RelationalOperator, northEastLat, northEastLon, southWestLat, southWestLon
        );

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder()
            .includeFields(includeFields)
            .excludeFields(excludeFields)
            .filter(filter)
            .sort(sort)
            .facets(facets)
            .facetSize(facetSize)
            .q(q)
            .fields(fields)
            .mm(mm)
            .from(from)
            .size(size)
            .basicRequest();

        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        SearchSourceBuilder source = searchRequestBuilder.request().source();

        // index
        assertEquals(searchApiRequest.getIndex(), searchRequestBuilder.request().indices()[0]);

        // pagination
        assertEquals(searchApiRequest.getFrom().intValue(), source.from());
        assertEquals(searchApiRequest.getSize().intValue(), source.size());

        // display
        FetchSourceContext fetchSourceContext = source.fetchSource();
        assertNotNull(fetchSourceContext);
        assertEquals(includeFields.size(), fetchSourceContext.includes().length);
        assertTrue(includeFields.containsAll(Arrays.asList(fetchSourceContext.includes())));
        assertEquals(excludeFields.size(), fetchSourceContext.excludes().length);
        assertTrue(excludeFields.containsAll(Arrays.asList(fetchSourceContext.excludes())));

        // sort
        List<FieldSortBuilder> sorts = (List) source.sorts();
        assertNotNull(sorts);
        assertTrue(sorts.size() == 3);
        assertEquals(sortFieldName1, sorts.get(0).getFieldName());
        assertEquals(sortFieldValue1, sorts.get(0).order());
        assertEquals(sortFieldName2, sorts.get(1).getFieldName());
        assertEquals(sortFieldValue2, sorts.get(1).order());
        assertEquals(sortFieldName3, sorts.get(2).getFieldName());
        assertEquals(sortFieldValue3, sorts.get(2).order());

        //facets
        List<AggregationBuilder> aggregations = source.aggregations().getAggregatorFactories();
        assertNotNull(aggregations);
        assertTrue(aggregations.size() == facets.size());
        assertTrue(searchRequestBuilder.toString().contains("\"size\" : " + facetSize));
        assertTrue(searchRequestBuilder.toString().contains("\"shard_size\" : 8"));
        int facet1 = 0;
        assertEquals(facets.get(facet1), ((TermsAggregationBuilder) aggregations.get(facet1)).field());
        assertFalse(Terms.Order.count(true) == (((TermsAggregationBuilder) aggregations.get(facet1)).order()));
        int facet2 = 1;
        assertEquals(facets.get(facet2), ((TermsAggregationBuilder) aggregations.get(facet2)).field());
        assertFalse(Terms.Order.count(true) == (((TermsAggregationBuilder) aggregations.get(facet2)).order()));

        // filters
        List<QueryBuilder> mustFirstLevel = ((BoolQueryBuilder) source.query()).must();
        List<QueryBuilder> mustNotFirstLevel = ((BoolQueryBuilder) source.query()).mustNot();
        List<QueryBuilder> shouldFirstLevel = ((BoolQueryBuilder) source.query()).should();

        assertNotNull(mustFirstLevel);
        assertNotNull(mustNotFirstLevel);
        assertNotNull(shouldFirstLevel);
        assertTrue(mustFirstLevel.size() == 2);
        assertTrue(mustNotFirstLevel.size() == 1);
        assertTrue(shouldFirstLevel.size() == 1);

        // querystring
        QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) mustFirstLevel.get(0);
        Map<String, Float> fieldsAndWeights = new HashMap<>(3);
        fieldsAndWeights.put(fieldName1, boostValue1);
        fieldsAndWeights.put(fieldName2, boostValue2);
        fieldsAndWeights.put(fieldName3, boostValue3);
        assertNotNull(queryStringQueryBuilder);
        assertEquals(q, queryStringQueryBuilder.queryString());
        assertEquals(mm, queryStringQueryBuilder.minimumShouldMatch());
        assertEquals(OR, queryStringQueryBuilder.defaultOperator());
        assertTrue(fieldsAndWeights.equals(queryStringQueryBuilder.fields()));

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
        List<QueryBuilder> mustSecondLevel = ((BoolQueryBuilder) mustFirstLevel.get(1)).must();
        assertTrue(mustSecondLevel.size() == 2);

        // field 3
        RangeQueryBuilder mustRangeSecondLevelField3 = (RangeQueryBuilder) mustSecondLevel.get(0);
        assertEquals(field3Name, mustRangeSecondLevelField3.fieldName());
        assertEquals(field3Value, mustRangeSecondLevelField3.from());
        assertNull(mustRangeSecondLevelField3.to());
        assertEquals(false, mustRangeSecondLevelField3.includeLower());
        assertEquals(true, mustRangeSecondLevelField3.includeUpper());

        BoolQueryBuilder queryBuilderThirdLevel = (BoolQueryBuilder) mustSecondLevel.get(1);
        List<QueryBuilder> shouldThirdLevel = queryBuilderThirdLevel.should(); //1
        List<QueryBuilder> mustThirdLevel = queryBuilderThirdLevel.must(); //2

        assertTrue(shouldThirdLevel.size() == 1);
        assertTrue(mustThirdLevel.size() == 2);

        // field 4
        RangeQueryBuilder shouldRangeThirdLevelField4 = (RangeQueryBuilder) shouldThirdLevel.get(0);
        assertEquals(field4Name, shouldRangeThirdLevelField4.fieldName());
        assertEquals(field4Value, shouldRangeThirdLevelField4.to());
        assertNull(shouldRangeThirdLevelField4.from());
        assertEquals(true, shouldRangeThirdLevelField4.includeLower());
        assertEquals(false, shouldRangeThirdLevelField4.includeUpper());

        // field 5
        TermsQueryBuilder mustTermsThirdLevelField5 = (TermsQueryBuilder) mustThirdLevel.get(0);
        assertEquals(field5Name, mustTermsThirdLevelField5.fieldName());
        assertTrue(Arrays.asList(Arrays.stream(field5Value).map(value -> {

            if (value instanceof String) {
                String s = String.valueOf(value);
                return s.replaceAll("\"", "");
            }

            return value;
        }).toArray()).equals(mustTermsThirdLevelField5.values()));

        // field 6
        GeoBoundingBoxQueryBuilder mustViewportFouthLevelField6 = (GeoBoundingBoxQueryBuilder) ((BoolQueryBuilder) mustThirdLevel.get(1)).must().get(0);
        int delta = 0;
        assertEquals(field6Name, mustViewportFouthLevelField6.fieldName());
        assertEquals(northEastLat, mustViewportFouthLevelField6.topLeft().getLat(), delta);
        assertEquals(southWestLon, mustViewportFouthLevelField6.topLeft().getLon(), delta);
        assertEquals(southWestLat, mustViewportFouthLevelField6.bottomRight().getLat(), delta);
        assertEquals(northEastLon, mustViewportFouthLevelField6.bottomRight().getLon(), delta);
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

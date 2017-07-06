package com.vivareal.search.api.adapter;

import com.vivareal.search.api.exception.IndexNotFoundException;
import com.vivareal.search.api.fixtures.model.SearchApiRequestBuilder;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.query.RelationalOperator;
import com.vivareal.search.api.model.query.Value;
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
import static org.elasticsearch.index.query.Operator.AND;
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
        setField(this.queryAdapter, "queryDefaultOperator", "AND");
        setField(this.queryAdapter, "queryDefaultMM", "75%");
        setField(this.queryAdapter, "facetSize", 20);

        doNothing().when(settingsAdapter).checkIndex(any(SearchApiRequest.class));
    }

    @After
    public void closeClient() {
        this.transportClient.close();
    }

    @Test(expected = IndexNotFoundException.class)
    public void shouldThrowExceptionWhenIndexIsInvalidToGetById() {
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().basicRequest();
        doThrow(new IndexNotFoundException(searchApiRequest.getIndex())).when(settingsAdapter).checkIndex(searchApiRequest);
        queryAdapter.getById(searchApiRequest, "12345");
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

    @Test(expected = IndexNotFoundException.class)
    public void shouldThrowExceptionWhenIndexIsInvalidToQuery() {
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().basicRequest();
        doThrow(new IndexNotFoundException(searchApiRequest.getIndex())).when(settingsAdapter).checkIndex(searchApiRequest);
        queryAdapter.query(searchApiRequest);
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

        RelationalOperator.getOperators(RelationalOperator.DIFFERENT).forEach(
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

        RelationalOperator.getOperators(RelationalOperator.EQUAL).forEach(
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

        RelationalOperator.getOperators(RelationalOperator.GREATER).forEach(
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

        RelationalOperator.getOperators(RelationalOperator.GREATER_EQUAL).forEach(
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

        RelationalOperator.getOperators(RelationalOperator.LESS).forEach(
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

        RelationalOperator.getOperators(RelationalOperator.LESS_EQUAL).forEach(
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

        RelationalOperator.getOperators(RelationalOperator.VIEWPORT).forEach(
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
        final Object[] value = new Object[]{1, "\"string\"", 1.2, true};

        RelationalOperator.getOperators(RelationalOperator.IN).forEach(
            op -> {
                SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter(String.format("%s %s %s", field, op, Arrays.toString(value))).basicRequest();
                SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
                TermsQueryBuilder terms = (TermsQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

                assertEquals(field, terms.fieldName());
                assertTrue(
                    Arrays.equals(((List<Value>) terms.values().get(0)).stream().map(contents -> {
                        Object obj = contents.getContents(0);

                        if (obj instanceof String)
                            obj = String.format("\"%s\"", obj);

                        return obj;
                    }).toArray(), value)
                );
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
    public void shouldReturnSearchRequestBuilderByFacets() {
        ArrayList<String> facets = Lists.newArrayList("field1", "field2", "field3");

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().facets(facets).facetSize(10).basicRequest();
        when(settingsAdapter.settingsByKey(searchApiRequest.getIndex(), SHARDS)).thenReturn("8");

        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        List<AggregationBuilder> aggregations = searchRequestBuilder.request().source().aggregations().getAggregatorFactories();
        System.out.println(aggregations);

        assertNotNull(aggregations);
        assertTrue(aggregations.size() == facets.size());

        assertTrue(searchRequestBuilder.toString().contains("\"size\" : 10"));
        assertTrue(searchRequestBuilder.toString().contains("\"shard_size\" : 8"));

        int facet1 = 0;
        assertEquals(facets.get(facet1), ((TermsAggregationBuilder) aggregations.get(facet1)).field());
        assertFalse(Terms.Order.count(true) == (((TermsAggregationBuilder) aggregations.get(facet1)).order()));
        assertTrue(Terms.Order.count(false) == (((TermsAggregationBuilder) aggregations.get(facet1)).order()));

        int facet2 = 1;
        assertEquals(facets.get(facet2), ((TermsAggregationBuilder) aggregations.get(facet2)).field());
        assertFalse(Terms.Order.count(true) == (((TermsAggregationBuilder) aggregations.get(facet2)).order()));
        assertTrue(Terms.Order.count(false) == (((TermsAggregationBuilder) aggregations.get(facet2)).order()));

        int facet3 = 2;
        assertEquals(facets.get(facet3), ((TermsAggregationBuilder) aggregations.get(facet3)).field());
        assertFalse(Terms.Order.count(true) == (((TermsAggregationBuilder) aggregations.get(facet3)).order()));
        assertTrue(Terms.Order.count(false) == (((TermsAggregationBuilder) aggregations.get(facet3)).order()));

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
        ArrayList<String> excludeFields = Lists.newArrayList("field3");

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().includeFields(includeFields).excludeFields(excludeFields).basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        FetchSourceContext fetchSourceContext = searchRequestBuilder.request().source().fetchSource();

        assertNotNull(fetchSourceContext);

        assertEquals(includeFields.size(), fetchSourceContext.includes().length);
        assertTrue(includeFields.containsAll(Arrays.asList(fetchSourceContext.includes())));

        assertEquals(excludeFields.size(), fetchSourceContext.excludes().length);
        assertTrue(excludeFields.containsAll(Arrays.asList(fetchSourceContext.excludes())));
    }

    @Test
    public void shouldReturnSimpleSearchRequestBuilderByQueryString() {
        String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().q(q).basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

        assertNotNull(queryStringQueryBuilder);
        assertEquals(q, queryStringQueryBuilder.queryString());
        assertEquals(AND, queryStringQueryBuilder.defaultOperator());
    }

    @Test
    public void shouldReturnSimpleSearchRequestBuilderByQueryStringWithOperator() {
        String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
        String op = "OR";

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().q(q).op(op).basicRequest();
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
    public void shouldReturnSearchRequestBuilderByQueryStringWithMinimalShouldMatch() {
        String q = "Lorem Ipsum is simply dummy text of the printing and typesetting";
        String mm = "50%";

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().q(q).mm(mm).basicRequest();
        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) ((BoolQueryBuilder) searchRequestBuilder.request().source().query()).must().get(0);

        assertNotNull(queryStringQueryBuilder);
        assertEquals(q, queryStringQueryBuilder.queryString());
        assertEquals(mm, queryStringQueryBuilder.minimumShouldMatch());
        assertEquals(OR, queryStringQueryBuilder.defaultOperator());
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

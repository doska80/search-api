package com.vivareal.search.api.adapter;

import com.vivareal.search.api.exception.IndexNotFoundException;
import com.vivareal.search.api.model.SearchApiRequest;
import org.assertj.core.util.Lists;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static org.apache.commons.lang3.ObjectUtils.allNotNull;
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
    }

    @After
    public void closeClient() {
        this.transportClient.close();
    }

    @Test(expected = IndexNotFoundException.class)
    public void shouldThrowExceptionWhenIndexIsInvalidToGetById() throws Exception {
        doThrow(new IndexNotFoundException("my_index")).when(settingsAdapter).checkIndex(any());
        queryAdapter.getById(any(), anyString());
    }

    @Test
    public void shouldReturnGetRequestBuilderByGetId() throws Exception {
        String id = "123456";

        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().basicRequest();
        doNothing().when(settingsAdapter).checkIndex(searchApiRequest);

        GetRequestBuilder requestBuilder = queryAdapter.getById(searchApiRequest, id);
        assertEquals(id, requestBuilder.request().id());
        assertEquals(searchApiRequest.getIndex(), requestBuilder.request().index());
        assertEquals(searchApiRequest.getIndex(), requestBuilder.request().type());
    }

    @Test(expected = IndexNotFoundException.class)
    public void shouldThrowExceptionWhenIndexIsInvalidToQuery() throws Exception {
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().basicRequest();
        doThrow(new IndexNotFoundException(searchApiRequest.getIndex())).when(settingsAdapter).checkIndex(searchApiRequest);
        queryAdapter.query(searchApiRequest);
    }

    @Test
    public void shouldReturnSimpleSearchRequestBuilderWithBasicRequest() throws Exception {
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().basicRequest();
        doNothing().when(settingsAdapter).checkIndex(searchApiRequest);

        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        SearchSourceBuilder source = searchRequestBuilder.request().source();

        assertEquals(searchApiRequest.getIndex(), searchRequestBuilder.request().indices()[0]);
        assertEquals(searchApiRequest.getFrom().intValue(), source.from());
        assertEquals(searchApiRequest.getSize().intValue(), source.size());
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleFilter() throws Exception {
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter("a:1234").basicRequest();
        doNothing().when(settingsAdapter).checkIndex(searchApiRequest);

        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);

        SearchRequest request = searchRequestBuilder.request();
        SearchSourceBuilder source = request.source();
        List<QueryBuilder> must = ((BoolQueryBuilder) source.query()).must();

        assertNotNull(must);
        assertTrue(must.get(0) instanceof MatchQueryBuilder);
        assertEquals("a", ((MatchQueryBuilder) must.get(0)).fieldName());
        assertEquals(1234, ((MatchQueryBuilder) must.get(0)).value());
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleANDOperator() throws Exception {
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter("a:123 AND b:432").basicRequest();
        doNothing().when(settingsAdapter).checkIndex(searchApiRequest);

        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);

        SearchRequest request = searchRequestBuilder.request();
        SearchSourceBuilder source = request.source();
        List<QueryBuilder> must = ((BoolQueryBuilder) source.query()).must();

        assertNotNull(must);
        assertTrue(must.size() == 2);
        assertEquals("a", ((MatchQueryBuilder) must.get(0)).fieldName());
        assertEquals(123, ((MatchQueryBuilder) must.get(0)).value());
        assertEquals("b", ((MatchQueryBuilder) must.get(1)).fieldName());
        assertEquals(432, ((MatchQueryBuilder) must.get(1)).value());
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSingleOROperator() throws Exception {
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter("a:123 OR b:432").basicRequest();
        doNothing().when(settingsAdapter).checkIndex(searchApiRequest);

        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);

        SearchRequest request = searchRequestBuilder.request();
        SearchSourceBuilder source = request.source();
        List<QueryBuilder> should = ((BoolQueryBuilder) source.query()).should();

        assertNotNull(should);
        assertTrue(should.size() == 2);
        assertEquals("a", ((MatchQueryBuilder) should.get(0)).fieldName());
        assertEquals(123, ((MatchQueryBuilder) should.get(0)).value());
        assertEquals("b", ((MatchQueryBuilder) should.get(1)).fieldName());
        assertEquals(432, ((MatchQueryBuilder) should.get(1)).value());
    }

    @Test
    public void shouldReturnSearchRequestBuilderWhenValueIsNull() throws Exception {
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().filter("a:NULL").basicRequest();
        doNothing().when(settingsAdapter).checkIndex(searchApiRequest);

        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);

        SearchRequest request = searchRequestBuilder.request();
        SearchSourceBuilder source = request.source();
        List<QueryBuilder> mustNot = ((BoolQueryBuilder) source.query()).mustNot();



        BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) mustNot.get(0);
        List<RangeQueryBuilder> shouldClauses = (List) boolQueryBuilder.should();

        assertNotNull(shouldClauses);
        assertTrue(shouldClauses.size() == 2);

        assertEquals("a", shouldClauses.get(0).fieldName());
        assertEquals(0, shouldClauses.get(0).to());
        assertNull(shouldClauses.get(0).from());
        assertEquals(true, shouldClauses.get(0).includeLower());
        assertEquals(true, shouldClauses.get(0).includeUpper());

        assertEquals("a", shouldClauses.get(1).fieldName());
        assertEquals(0, shouldClauses.get(1).from());
        assertNull(shouldClauses.get(1).to());
        assertEquals(true, shouldClauses.get(1).includeLower());
        assertEquals(true, shouldClauses.get(1).includeUpper());
    }

    @Test
    public void shouldReturnSearchRequestBuilderByFacets() throws Exception {
        ArrayList<String> facets = Lists.newArrayList("field1", "field2", "field3");
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().facets(facets).facetSize(10).basicRequest();
        doNothing().when(settingsAdapter).checkIndex(searchApiRequest);
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
    public void shouldReturnSearchRequestBuilderSortedBy() throws Exception {
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().sort("field1 ASC, field2 DESC, field3 ASC").basicRequest();
        doNothing().when(settingsAdapter).checkIndex(searchApiRequest);

        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        List<FieldSortBuilder> sorts = (List) searchRequestBuilder.request().source().sorts();

        assertNotNull(sorts);
        assertTrue(sorts.size() == 3);

        int field1 = 0;
        assertEquals("field1", sorts.get(field1).getFieldName());
        assertEquals(SortOrder.ASC, sorts.get(field1).order());

        int field2 = 1;
        assertEquals("field2", sorts.get(field2).getFieldName());
        assertEquals(SortOrder.DESC, sorts.get(field2).order());

        int field3 = 2;
        assertEquals("field3", sorts.get(field3).getFieldName());
        assertEquals(SortOrder.ASC, sorts.get(field3).order());
    }

    @Test
    public void shouldReturnSearchRequestBuilderWithSpecifiedFieldSources() throws Exception {
        ArrayList<String> includeFields = Lists.newArrayList("field1", "field2", "field3");
        ArrayList<String> excludeFields = Lists.newArrayList("field3");
        SearchApiRequest searchApiRequest = new SearchApiRequestBuilder().includeFields(includeFields).excludeFields(excludeFields).basicRequest();
        doNothing().when(settingsAdapter).checkIndex(searchApiRequest);

        SearchRequestBuilder searchRequestBuilder = queryAdapter.query(searchApiRequest);
        FetchSourceContext fetchSourceContext = searchRequestBuilder.request().source().fetchSource();

        assertNotNull(fetchSourceContext);

        assertEquals(includeFields.size(), fetchSourceContext.includes().length);
        assertTrue(includeFields.containsAll(Arrays.asList(fetchSourceContext.includes())));

        assertEquals(excludeFields.size(), fetchSourceContext.excludes().length);
        assertTrue(excludeFields.containsAll(Arrays.asList(fetchSourceContext.excludes())));
    }

    public static class SearchApiRequestBuilder {

        private String index;
        private String operator;
        private String mm;
        private List<String> fields;
        private List<String> includeFields;
        private List<String> excludeFields;
        private String filter;
        private String sort;
        private List<String> facets;
        private int facetSize;
        private String q;
        private int from;
        private int size;

        public SearchApiRequest basicRequest() {
            return index("my_index").from(0).size(20).builder();
        }

        private SearchApiRequest builder() {
            SearchApiRequest searchApiRequest = new SearchApiRequest();

            if (allNotNull(index))
                searchApiRequest.setIndex(index);

            if (allNotNull(operator))
                searchApiRequest.setOperator(operator);

            if (allNotNull(mm))
                searchApiRequest.setMm(mm);

            if (allNotNull(fields))
                searchApiRequest.setFields(fields);

            if (allNotNull(includeFields))
                searchApiRequest.setIncludeFields(includeFields);

            if (allNotNull(excludeFields))
                searchApiRequest.setExcludeFields(excludeFields);

            if (allNotNull(filter))
                searchApiRequest.setFilter(filter);

            if (allNotNull(sort))
                searchApiRequest.setSort(sort);

            if (allNotNull(facets))
                searchApiRequest.setFacets(facets);

            if (allNotNull(facetSize))
                searchApiRequest.setFacetSize(facetSize);

            if (allNotNull(q))
                searchApiRequest.setQ(q);

            if (allNotNull(from))
                searchApiRequest.setFrom(from);

            if (allNotNull(size))
                searchApiRequest.setSize(size);

            return searchApiRequest;
        }

        public SearchApiRequestBuilder index(final String index) {
            this.index = index;
            return this;
        }

        public SearchApiRequestBuilder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public SearchApiRequestBuilder mm(String mm) {
            this.mm = mm;
            return this;
        }

        public SearchApiRequestBuilder fields(List<String> fields) {
            this.fields = fields;
            return this;
        }

        public SearchApiRequestBuilder includeFields(List<String> includeFields) {
            this.includeFields = includeFields;
            return this;
        }

        public SearchApiRequestBuilder excludeFields(List<String> excludeFields) {
            this.excludeFields = excludeFields;
            return this;
        }

        public SearchApiRequestBuilder filter(String filter) {
            this.filter = filter;
            return this;
        }

        public SearchApiRequestBuilder sort(String sort) {
            this.sort = sort;
            return this;
        }

        public SearchApiRequestBuilder facets(List<String> facets) {
            this.facets = facets;
            return this;
        }

        public SearchApiRequestBuilder facetSize(int facetSize) {
            this.facetSize = facetSize;
            return this;
        }

        public SearchApiRequestBuilder q(String q) {
            this.q = q;
            return this;
        }

        public SearchApiRequestBuilder from(int from) {
            this.from = from;
            return this;
        }

        public SearchApiRequestBuilder size(int size) {
            this.size = size;
            return this;
        }
    }

}

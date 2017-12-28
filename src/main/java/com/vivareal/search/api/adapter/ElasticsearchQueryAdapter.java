package com.vivareal.search.api.adapter;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.FilterableApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import com.vivareal.search.api.model.parser.FacetParser;
import com.vivareal.search.api.model.search.Facetable;
import com.vivareal.search.api.model.search.Queryable;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_STRING;
import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;
import static org.apache.commons.lang3.math.NumberUtils.toInt;
import static org.apache.lucene.search.join.ScoreMode.None;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@Scope(SCOPE_SINGLETON)
@Qualifier("ElasticsearchQuery")
@DependsOn("searchApiEnv")
public class ElasticsearchQueryAdapter implements QueryAdapter<GetRequestBuilder, SearchRequestBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchQueryAdapter.class);

    private static final String NOT_NESTED = "not_nested";
    private static final String MM_ERROR_MESSAGE = "Minimum Should Match (mm) should be a valid integer number (-100 <> +100)";

    private final ESClient esClient;

    private final SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;
    private final SourceFieldAdapter sourceFieldAdapter;
    private final SearchAfterQueryAdapter searchAfterQueryAdapter;
    private final SortQueryAdapter sortQueryAdapter;
    private final FilterQueryAdapter filterQueryAdapter;
    private final FacetParser facetParser;

    @Autowired
    public ElasticsearchQueryAdapter(ESClient esClient,
                                     @Qualifier("elasticsearchSettings") SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter,
                                     SourceFieldAdapter sourceFieldAdapter,
                                     SearchAfterQueryAdapter searchAfterQueryAdapter,
                                     SortQueryAdapter sortQueryAdapter,
                                     FilterQueryAdapter filterQueryAdapter,
                                     FacetParser facetParser) {
        this.esClient = esClient;
        this.settingsAdapter = settingsAdapter;
        this.sourceFieldAdapter = sourceFieldAdapter;
        this.searchAfterQueryAdapter = searchAfterQueryAdapter;
        this.sortQueryAdapter = sortQueryAdapter;
        this.filterQueryAdapter = filterQueryAdapter;
        this.facetParser = facetParser;
    }

    @Override
    @Trace
    public GetRequestBuilder getById(BaseApiRequest request, String id) {
        settingsAdapter.checkIndex(request);

        GetRequestBuilder requestBuilder = esClient.prepareGet(request, id)
            .setRealtime(false)
            .setOperationThreaded(false);

        sourceFieldAdapter.apply(requestBuilder, request);

        LOG.debug("Query getById {}", requestBuilder.request());

        return requestBuilder;
    }

    @Override
    @Trace
    public SearchRequestBuilder query(FilterableApiRequest request) {
        return prepareQuery(request, (searchBuilder, queryBuilder) -> buildQueryByFilterableApiRequest(request, searchBuilder, queryBuilder));
    }

    @Override
    @Trace
    public SearchRequestBuilder query(SearchApiRequest request) {
        return prepareQuery(request, (searchBuilder, queryBuilder) -> buildQueryBySearchApiRequest(request, searchBuilder, queryBuilder));
    }

    private SearchRequestBuilder prepareQuery(BaseApiRequest request, BiConsumer<SearchRequestBuilder, BoolQueryBuilder> builder) {
        settingsAdapter.checkIndex(request);
        SearchRequestBuilder searchBuilder = esClient.prepareSearch(request)
            .setTimeout(
                new TimeValue(ES_QUERY_TIMEOUT_VALUE.getValue(request.getIndex()),
                TimeUnit.valueOf(ES_QUERY_TIMEOUT_UNIT.getValue(request.getIndex())))
            );

        BoolQueryBuilder queryBuilder = boolQuery();
        builder.accept(searchBuilder, queryBuilder);
        searchBuilder.setQuery(queryBuilder);

        LOG.debug("Request: {} - Query: {}", request, searchBuilder);
        return searchBuilder;
    }

    private void buildQueryByFilterableApiRequest(FilterableApiRequest request, SearchRequestBuilder searchBuilder, BoolQueryBuilder queryBuilder) {
        applyPage(searchBuilder, request);
        sourceFieldAdapter.apply(searchBuilder, request);
        applyPage(searchBuilder, request);
        applyQueryString(queryBuilder, request);
        filterQueryAdapter.apply(queryBuilder, request);
        sortQueryAdapter.apply(searchBuilder, request);
        searchAfterQueryAdapter.apply(searchBuilder, request);
    }

    private void buildQueryBySearchApiRequest(SearchApiRequest request, SearchRequestBuilder searchBuilder, BoolQueryBuilder queryBuilder) {
        buildQueryByFilterableApiRequest(request, searchBuilder, queryBuilder);
        applyFacets(searchBuilder, request);
    }

    private void applyPage(SearchRequestBuilder searchBuilder, FilterableApiRequest request) {
        String index = request.getIndex();
        request.setPaginationValues(ES_DEFAULT_SIZE.getValue(index), ES_MAX_SIZE.getValue(index));
        searchBuilder.setFrom(request.getFrom());
        searchBuilder.setSize(request.getSize());
    }

    private void applyQueryString(BoolQueryBuilder queryBuilder, final Queryable request) {
        if (isEmpty(request.getQ()))
            return;

        String indexName = request.getIndex();

        String mm = isEmpty(request.getMm()) ? QS_MM.getValue(indexName) : request.getMm();
        checkMM(mm);

        Map<String, AbstractQueryBuilder> queryStringQueries = new HashMap<>();

        QS_DEFAULT_FIELDS.getValue(request.getFields(), indexName).forEach(field -> {
            String[] boostFieldValues = field.split(":");
            String fieldName = boostFieldValues[0];

            if (settingsAdapter.isTypeOf(indexName, fieldName.split("\\.")[0], FIELD_TYPE_NESTED)) {
                String nestedField = fieldName.split("\\.")[0];

                if (queryStringQueries.containsKey(nestedField)) {
                    buildQueryStringQuery((QueryStringQueryBuilder) ((NestedQueryBuilder) queryStringQueries.get(nestedField)).query(), indexName, request.getQ(), boostFieldValues, mm);
                } else {
                    queryStringQueries.put(nestedField, nestedQuery(nestedField, buildQueryStringQuery(null, indexName, request.getQ(), boostFieldValues, mm), None));
                }
            } else {
                if (queryStringQueries.containsKey(NOT_NESTED)) {
                    buildQueryStringQuery((QueryStringQueryBuilder) queryStringQueries.get(NOT_NESTED), indexName, request.getQ(), boostFieldValues, mm);
                } else {
                    queryStringQueries.put(NOT_NESTED, buildQueryStringQuery(null, indexName, request.getQ(), boostFieldValues, mm));
                }
            }
        });
        queryStringQueries.forEach((nestedPath, nestedQuery) -> queryBuilder.filter().add(nestedQuery));
    }

    private QueryStringQueryBuilder buildQueryStringQuery(QueryStringQueryBuilder queryStringQueryBuilder, final String indexName, final String q, final String[] boostFieldValues, final String mm) {
        if (queryStringQueryBuilder == null)
            queryStringQueryBuilder = queryStringQuery(q);

        String fieldName = boostFieldValues[0];

        if (settingsAdapter.isTypeOf(indexName, fieldName, FIELD_TYPE_STRING) && !fieldName.contains(".raw")) {
            fieldName = fieldName.concat(".raw");
        }

        float boost = (boostFieldValues.length == 2 ? Float.parseFloat(boostFieldValues[1]) : 1.0f);
        queryStringQueryBuilder.field(fieldName, boost).minimumShouldMatch(mm).tieBreaker(0.2f).phraseSlop(2);
        return queryStringQueryBuilder;
    }

    private void checkMM(final String mm) {
        if (mm.contains(".") || mm.contains("%") && ((mm.length() - 1) > mm.indexOf('%')))
            throw new NumberFormatException(MM_ERROR_MESSAGE);

        String mmNumber = mm.replace("%", "");

        if (!isCreatable(mmNumber))
            throw new NumberFormatException(MM_ERROR_MESSAGE);

        int number = toInt(mmNumber);

        if (number < -100 || number > 100)
            throw new IllegalArgumentException(MM_ERROR_MESSAGE);
    }

    private void applyFacets(SearchRequestBuilder searchRequestBuilder, final Facetable request) {
        Set<String> value = request.getFacets();
        if (isEmpty(value))
            return;

        final String indexName = request.getIndex();
        request.setFacetingValues(ES_FACET_SIZE.getValue(indexName));

        final int facetSize = request.getFacetSize();
        final int shardSize = parseInt(String.valueOf(settingsAdapter.settingsByKey(request.getIndex(), SHARDS)));

        facetParser.parse(value.stream().collect(joining(","))).forEach(facet -> {
            final String fieldName = facet.getName();
            final String firstName = facet.firstName();
            settingsAdapter.checkFieldName(indexName, fieldName, false);

            AggregationBuilder agg = terms(fieldName)
                .field(fieldName)
                .size(facetSize)
                .shardSize(shardSize)
                .order(Terms.Order.count(false));

            if (settingsAdapter.isTypeOf(indexName, firstName, FIELD_TYPE_NESTED)) {
                applyFacetsByNestedFields(searchRequestBuilder, firstName, agg);
            } else {
                searchRequestBuilder.addAggregation(agg);
            }
        });
    }

    private void applyFacetsByNestedFields(SearchRequestBuilder searchRequestBuilder, final String name, final AggregationBuilder agg) {
        Optional<AggregationBuilder> nestedAgg = ofNullable(searchRequestBuilder.request().source().aggregations())
        .flatMap(builder ->
            builder.getAggregatorFactories()
            .stream()
            .filter(aggregationBuilder -> aggregationBuilder instanceof NestedAggregationBuilder && name.equals(aggregationBuilder.getName()))
            .findFirst()
        );

        if (nestedAgg.isPresent()) {
            nestedAgg.get().subAggregation(agg);
        } else {
            searchRequestBuilder.addAggregation(nested(name, name).subAggregation(agg));
        }
    }
}

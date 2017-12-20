package com.vivareal.search.api.adapter;

import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.exception.UnsupportedFieldException;
import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.FilterableApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import com.vivareal.search.api.model.parser.FacetParser;
import com.vivareal.search.api.model.parser.QueryParser;
import com.vivareal.search.api.model.query.*;
import com.vivareal.search.api.model.search.Facetable;
import com.vivareal.search.api.model.search.Filterable;
import com.vivareal.search.api.model.search.Queryable;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.vivareal.search.api.adapter.ElasticsearchQueryAdapter.QueryType.*;
import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static com.vivareal.search.api.model.mapping.MappingType.*;
import static com.vivareal.search.api.model.query.LogicalOperator.AND;
import static com.vivareal.search.api.model.query.RelationalOperator.*;
import static java.lang.Integer.parseInt;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
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
    private final QueryParser queryParser;
    private final FacetParser facetParser;

    @Autowired
    public ElasticsearchQueryAdapter(ESClient esClient,
                                     @Qualifier("elasticsearchSettings") SettingsAdapter<Map<String,
                                     Map<String, Object>>, String> settingsAdapter,
                                     SourceFieldAdapter sourceFieldAdapter,
                                     SearchAfterQueryAdapter searchAfterQueryAdapter,
                                     SortQueryAdapter sortQueryAdapter,
                                     QueryParser queryParser,
                                     FacetParser facetParser) {
        this.esClient = esClient;
        this.settingsAdapter = settingsAdapter;
        this.sourceFieldAdapter = sourceFieldAdapter;
        this.searchAfterQueryAdapter = searchAfterQueryAdapter;
        this.sortQueryAdapter = sortQueryAdapter;
        this.queryParser = queryParser;
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
        applyFilterQuery(queryBuilder, request);
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

    public void applyFilterQuery(BoolQueryBuilder queryBuilder, final Filterable filter) {
        ofNullable(filter.getFilter()).ifPresent(f -> applyFilterQuery(queryBuilder, queryParser.parse(f), filter.getIndex(), new HashMap<>()));
    }

    private void applyFilterQuery(BoolQueryBuilder queryBuilder, final QueryFragment queryFragment, final String indexName, Map<QueryType, Map<String, BoolQueryBuilder>> nestedMap) {
        if (queryFragment == null || !(queryFragment instanceof QueryFragmentList))
            return;

        QueryFragmentList queryFragmentList = (QueryFragmentList) queryFragment;
        LogicalOperator logicalOperator = AND;

        for (int index = 0; index < queryFragmentList.size(); index++) {
            QueryFragment queryFragmentFilter = queryFragmentList.get(index);

            if (queryFragmentFilter instanceof QueryFragmentList) {
                BoolQueryBuilder recursiveQueryBuilder = boolQuery();
                logicalOperator = getLogicalOperatorByQueryFragmentList(queryFragmentList, index, logicalOperator);
                Map<QueryType, Map<String, BoolQueryBuilder>> innerNestedMap = addFilterQuery(new HashMap<>(), queryBuilder, recursiveQueryBuilder, logicalOperator, isNotBeforeCurrentQueryFragment(queryFragmentList, index), false, null);
                applyFilterQuery(recursiveQueryBuilder, queryFragmentFilter, indexName, innerNestedMap);

            } else if (queryFragmentFilter instanceof QueryFragmentItem) {
                QueryFragmentItem queryFragmentItem = (QueryFragmentItem) queryFragmentFilter;
                Filter filter = queryFragmentItem.getFilter();

                String fieldName = filter.getField().getName();
                String fieldFirstName = filter.getField().firstName();
                settingsAdapter.checkFieldName(indexName, fieldName, false);
                boolean nested = settingsAdapter.isTypeOf(indexName, fieldFirstName, FIELD_TYPE_NESTED);

                final boolean not = isNotBeforeCurrentQueryFragment(queryFragmentList, index);
                logicalOperator = getLogicalOperatorByQueryFragmentList(queryFragmentList, index, logicalOperator);
                RelationalOperator operator = filter.getRelationalOperator();

                if (filter.getValue().isEmpty()) {
                    addFilterQuery(nestedMap, queryBuilder, existsQuery(fieldName), logicalOperator, DIFFERENT.equals(operator) == not, nested, fieldFirstName);
                    continue;
                }

                Value filterValue = filter.getValue();

                switch (operator) {
                    case DIFFERENT:
                        addFilterQuery(nestedMap, queryBuilder, matchQuery(fieldName, filterValue.value()), logicalOperator, !not, nested, fieldFirstName);
                        break;

                    case EQUAL:
                        addFilterQuery(nestedMap, queryBuilder, matchQuery(fieldName, filterValue.value()), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case RANGE:
                        addFilterQuery(nestedMap, queryBuilder, rangeQuery(fieldName).from(filterValue.value(0)).to(filterValue.value(1)).includeLower(true).includeUpper(true), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case GREATER:
                        addFilterQuery(nestedMap, queryBuilder, rangeQuery(fieldName).from(filterValue.value()).includeLower(false), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case GREATER_EQUAL:
                        addFilterQuery(nestedMap, queryBuilder, rangeQuery(fieldName).from(filterValue.value()).includeLower(true), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case LESS:
                        addFilterQuery(nestedMap, queryBuilder, rangeQuery(fieldName).to(filterValue.value()).includeUpper(false), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case LESS_EQUAL:
                        addFilterQuery(nestedMap, queryBuilder, rangeQuery(fieldName).to(filterValue.value()).includeUpper(true), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case LIKE:
                        if (!settingsAdapter.isTypeOf(indexName, fieldName, FIELD_TYPE_KEYWORD))
                            throw new UnsupportedFieldException(fieldName, settingsAdapter.getFieldType(indexName, fieldName), FIELD_TYPE_KEYWORD.toString(), LIKE);

                        addFilterQuery(nestedMap, queryBuilder, wildcardQuery(fieldName, filter.getValue().first()), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case IN:
                        if (fieldName.equals(ES_MAPPING_META_FIELDS_ID.getValue(indexName))) {
                            String[] values = filterValue.stream().map(contents -> ((Value) contents).value(0)).map(Object::toString).toArray(String[]::new);
                            addFilterQuery(nestedMap, queryBuilder, idsQuery().addIds(values), logicalOperator, not, nested, fieldFirstName);
                        } else {
                            Object[] values = filterValue.stream().map(contents -> ((Value) contents).value(0)).toArray();
                            addFilterQuery(nestedMap, queryBuilder, termsQuery(fieldName, values), logicalOperator, not, nested, fieldFirstName);
                        }
                        break;

                    case POLYGON:
                        if (!settingsAdapter.isTypeOf(indexName, fieldName, FIELD_TYPE_GEOPOINT))
                            throw new UnsupportedFieldException(fieldName, settingsAdapter.getFieldType(indexName, fieldName), FIELD_TYPE_GEOPOINT.toString(), POLYGON);

                        List<GeoPoint> points = filterValue
                            .stream()
                            .map(point -> new GeoPoint(((Value) point).<Double>value(1), ((Value) point).<Double>value(0)))
                            .collect(toList());

                        addFilterQuery(nestedMap, queryBuilder, geoPolygonQuery(fieldName, points), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case VIEWPORT:
                        if (!settingsAdapter.isTypeOf(indexName, fieldName, FIELD_TYPE_GEOPOINT))
                            throw new UnsupportedFieldException(fieldName, settingsAdapter.getFieldType(indexName, fieldName), FIELD_TYPE_GEOPOINT.toString(), VIEWPORT);

                        GeoPoint topRight = new GeoPoint(filterValue.value(0, 1), filterValue.value(0, 0));
                        GeoPoint bottomLeft = new GeoPoint(filterValue.value(1, 1), filterValue.value(1, 0));

                        addFilterQuery(nestedMap, queryBuilder, geoBoundingBoxQuery(fieldName).setCornersOGC(bottomLeft, topRight), logicalOperator, not, nested, fieldFirstName);
                        break;

                    case CONTAINS_ALL:
                        Object[] values = filterValue.stream().map(contents -> ((Value) contents).value(0)).toArray();
                        Stream.of(values).forEach((value) -> addFilterQuery(nestedMap, queryBuilder, matchQuery(fieldName, value), AND, not, nested, fieldFirstName));
                        break;

                    default:
                        throw new UnsupportedOperationException("Unknown Relational Operator " + operator.name());
                }
            }
        }
    }

    private Map<QueryType, Map<String, BoolQueryBuilder>> addFilterQuery(Map<QueryType, Map<String, BoolQueryBuilder>> nestedMap, BoolQueryBuilder boolQueryBuilder, final QueryBuilder queryBuilder, final LogicalOperator logicalOperator, final boolean not, final boolean nested, final String fieldFirstName) {
        QueryType queryType = getQueryType(logicalOperator, not);
        QueryBuilder query = queryBuilder;

        if (nested) {
            Optional<QueryBuilder> nestedQuery = addNestedQuery(queryType, nestedMap, fieldFirstName, queryBuilder, logicalOperator);
            if (nestedQuery.isPresent()) {
                query = nestedQuery.get();
            } else {
                return nestedMap;
            }
        }

        switch (queryType) {
            case FILTER:
                boolQueryBuilder.filter(query);
                break;

            case MUST_NOT:
                boolQueryBuilder.mustNot(query);
                break;

            case SHOULD:
                boolQueryBuilder.should(query);
                break;

            case SHOULD_NOT:
                boolQueryBuilder.should(boolQuery().mustNot(query));
                break;
        }
        return nestedMap;
    }

    enum QueryType {
        FILTER, MUST_NOT, SHOULD, SHOULD_NOT
    }

    private QueryType getQueryType(final LogicalOperator logicalOperator, final boolean not) {
        if(logicalOperator.equals(AND)) {
            if (!not) {
                return FILTER;
            } else {
                return MUST_NOT;
            }
        } else {
            if (!not) {
                return SHOULD;
            } else {
                return SHOULD_NOT;
            }
        }
    }

    private Optional<QueryBuilder> addNestedQuery(final QueryType queryType, Map<QueryType, Map<String, BoolQueryBuilder>> nestedMap, final String fieldFirstName, final QueryBuilder queryBuilder, final LogicalOperator logicalOperator) {
        final boolean nested = false;
        final boolean not = false;

        if (!nestedMap.containsKey(queryType)) {
            Map<String, BoolQueryBuilder> m = new HashMap<>();
            BoolQueryBuilder bq = boolQuery();
            m.put(fieldFirstName, bq);
            nestedMap.put(queryType, m);

            addFilterQuery(nestedMap, bq, queryBuilder, logicalOperator, not, nested, fieldFirstName);
            return of(nestedQuery(fieldFirstName, bq, None));

        } else if (!nestedMap.get(queryType).containsKey(fieldFirstName)) {
            BoolQueryBuilder bq = boolQuery();
            nestedMap.get(queryType).put(fieldFirstName, bq);

            addFilterQuery(nestedMap, bq, queryBuilder, logicalOperator, not, nested, fieldFirstName);
            return of(nestedQuery(fieldFirstName, bq, None));

        } else {
            addFilterQuery(nestedMap, nestedMap.get(queryType).get(fieldFirstName), queryBuilder, logicalOperator, not, nested, fieldFirstName);
        }

        return empty();
    }

    private boolean isNotBeforeCurrentQueryFragment(final QueryFragmentList queryFragmentList, final int index) {
        if (index - 1 >= 0) {
            QueryFragment before = queryFragmentList.get(index - 1);
            if (before instanceof QueryFragmentNot)
                return ((QueryFragmentNot) before).isNot();
        }
        return false;
    }

    private LogicalOperator getLogicalOperatorByQueryFragmentList(final QueryFragmentList queryFragmentList, int index, LogicalOperator logicalOperator) {
        if (index + 1 < queryFragmentList.size()) {
            QueryFragment next = queryFragmentList.get(index + 1);
            if (next instanceof QueryFragmentItem)
                return ((QueryFragmentItem) next).getLogicalOperator();

            if (next instanceof QueryFragmentOperator)
                return ((QueryFragmentOperator) next).getOperator();
        }
        return logicalOperator;
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

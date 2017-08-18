package com.vivareal.search.api.adapter;

import com.vivareal.search.api.exception.UnsupportableFieldException;
import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import com.vivareal.search.api.model.parser.FacetParser;
import com.vivareal.search.api.model.parser.QueryParser;
import com.vivareal.search.api.model.parser.SortParser;
import com.vivareal.search.api.model.query.*;
import com.vivareal.search.api.model.search.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.google.common.collect.Maps.newHashMap;
import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.FIELD_TYPE_TEXT;
import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static com.vivareal.search.api.model.query.LogicalOperator.AND;
import static com.vivareal.search.api.model.query.RelationalOperator.*;
import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.lucene.search.join.ScoreMode.None;
import static org.elasticsearch.index.query.Operator.OR;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@Scope(SCOPE_SINGLETON)
@Qualifier("ElasticsearchQuery")
public class ElasticsearchQueryAdapter implements QueryAdapter<GetRequestBuilder, SearchRequestBuilder> {

    private static Logger LOG = LoggerFactory.getLogger(ElasticsearchQueryAdapter.class);

    @Autowired
    private TransportClient transportClient;

    @Autowired
    @Qualifier("elasticsearchSettings")
    private SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;

    @Override
    public GetRequestBuilder getById(BaseApiRequest request, String id) {
        settingsAdapter.checkIndex(request);

        Pair<String[], String[]> fetchSourceFields = getFetchSourceFields(request);
        GetRequestBuilder requestBuilder = transportClient.prepareGet()
            .setIndex(request.getIndex())
            .setType(request.getIndex())
            .setId(id)
            .setFetchSource(fetchSourceFields.getLeft(), fetchSourceFields.getRight());
        LOG.debug("Query getById {}", requestBuilder != null ? requestBuilder.request() : null);

        return requestBuilder;
    }

    @Override
    public SearchRequestBuilder query(BaseApiRequest request) {
        return prepareQuery(request, (searchBuilder, boolQueryBuilder) -> addFieldList(searchBuilder, request));
    }

    @Override
    public SearchRequestBuilder query(SearchApiRequest request) {
        return prepareQuery(request, (searchBuilder, queryBuilder) -> {
            searchBuilder.setFrom(request.getFrom());
            searchBuilder.setSize(request.getSize());
            addFieldList(searchBuilder, request);
            applySort(searchBuilder, request);
            applyFacetFields(searchBuilder, request);

            applyQueryString(queryBuilder, request);

            Map<String, BoolQueryBuilder> nestedQueries = newHashMap();
            applyFilterQuery(queryBuilder, request, nestedQueries);
        });
    }

    private void applyFilterQuery(BoolQueryBuilder queryBuilder, final Filterable filter, Map<String, BoolQueryBuilder> nestedQueries) {
        ofNullable(filter.getFilter()).ifPresent(f -> applyFilterQuery(queryBuilder, QueryParser.get().parse(f), filter.getIndex(), nestedQueries));
    }

    private void applyFilterQuery(BoolQueryBuilder queryBuilder, final QueryFragment queryFragment, final String indexName, Map<String, BoolQueryBuilder> nestedQueries) {
        if (queryFragment != null && queryFragment instanceof QueryFragmentList) {
            QueryFragmentList queryFragmentList = (QueryFragmentList) queryFragment;
            LogicalOperator logicalOperator = AND;

            for (int index = 0; index < queryFragmentList.size(); index++) {
                QueryFragment queryFragmentFilter = queryFragmentList.get(index);

                if (queryFragmentFilter instanceof QueryFragmentList) {
                    BoolQueryBuilder recursiveQueryBuilder = boolQuery();
                    addFilterQueryByLogicalOperator(queryBuilder, recursiveQueryBuilder, getLogicalOperatorByQueryFragmentList(queryFragmentList, index, logicalOperator), isNotBeforeCurrentQueryFragment(queryFragmentList, index), false, null, nestedQueries);
                    applyFilterQuery(recursiveQueryBuilder, queryFragmentFilter, indexName, newHashMap());

                } else if (queryFragmentFilter instanceof QueryFragmentItem) {
                    QueryFragmentItem queryFragmentItem = (QueryFragmentItem) queryFragmentFilter;
                    Filter filter = queryFragmentItem.getFilter();

                    String fieldName = filter.getField().getName();
                    settingsAdapter.checkFieldName(indexName, fieldName);
                    boolean nested = settingsAdapter.isTypeOfNested(indexName, fieldName);

                    final boolean not = isNotBeforeCurrentQueryFragment(queryFragmentList, index);
                    logicalOperator = getLogicalOperatorByQueryFragmentList(queryFragmentList, index, logicalOperator);
                    RelationalOperator operator = filter.getRelationalOperator();

                    if (!isEmpty(filter.getValue().getContents())) {

                        List<Object> multiValues = filter.getValue().getContents();
                        Object singleValue = filter.getValue().getContents(0);

                        switch (operator) {
                            case DIFFERENT:
                                addFilterQueryByLogicalOperator(queryBuilder, matchQuery(fieldName, singleValue), logicalOperator, !not, nested, fieldName, nestedQueries);
                                break;

                            case EQUAL:
                                addFilterQueryByLogicalOperator(queryBuilder, matchQuery(fieldName, singleValue), logicalOperator, not, nested, fieldName, nestedQueries);
                                break;

                            case GREATER:
                                addFilterQueryByLogicalOperator(queryBuilder, rangeQuery(fieldName).from(singleValue).includeLower(false), logicalOperator, not, nested, fieldName, nestedQueries);
                                break;

                            case GREATER_EQUAL:
                                addFilterQueryByLogicalOperator(queryBuilder, rangeQuery(fieldName).from(singleValue).includeLower(true), logicalOperator, not, nested, fieldName, nestedQueries);
                                break;

                            case LESS:
                                addFilterQueryByLogicalOperator(queryBuilder, rangeQuery(fieldName).to(singleValue).includeUpper(false), logicalOperator, not, nested, fieldName, nestedQueries);
                                break;

                            case LESS_EQUAL:
                                addFilterQueryByLogicalOperator(queryBuilder, rangeQuery(fieldName).to(singleValue).includeUpper(true), logicalOperator, not, nested, fieldName, nestedQueries);
                                break;

                            case STARTS_WITH:
                                if(!settingsAdapter.isTypeOfString(indexName, fieldName))
                                    throw new UnsupportableFieldException(settingsAdapter.getFieldType(indexName, fieldName), FIELD_TYPE_TEXT, STARTS_WITH);

                                addFilterQueryByLogicalOperator(queryBuilder, matchPhrasePrefixQuery(fieldName.concat(".raw"), singleValue), logicalOperator, not, nested, fieldName, nestedQueries);
                                break;

                            case IN:
                                Object[] values = multiValues.stream().map(contents -> ((com.vivareal.search.api.model.query.Value) contents).getContents(0)).toArray();
                                addFilterQueryByLogicalOperator(queryBuilder, termsQuery(fieldName, values), logicalOperator, not, nested, fieldName, nestedQueries);
                                break;

                            case VIEWPORT:
                                if(!settingsAdapter.isTypeOfGeoPoint(indexName, fieldName))
                                    throw new UnsupportableFieldException(settingsAdapter.getFieldType(indexName, fieldName), FIELD_TYPE_TEXT, VIEWPORT);

                                GeoPoint topRight = createGeoPointFromRawCoordinates((List) multiValues.get(0));
                                GeoPoint bottomLeft = createGeoPointFromRawCoordinates((List) multiValues.get(1));
                                addFilterQueryByLogicalOperator(queryBuilder, geoBoundingBoxQuery(filter.getField().getName()).setCornersOGC(bottomLeft, topRight), logicalOperator, not, nested, fieldName, nestedQueries);
                                break;

                            default:
                                throw new UnsupportedOperationException("Unknown Relational Operator " + operator.name());
                        }
                    } else {
                        addFilterQueryByLogicalOperator(queryBuilder, existsQuery(fieldName), logicalOperator, DIFFERENT.equals(operator) == not, nested, fieldName, nestedQueries);
                    }
                }
            }
        }
    }

    private void addFilterQueryByLogicalOperator(BoolQueryBuilder boolQueryBuilder, final QueryBuilder queryBuilder, final LogicalOperator logicalOperator, final boolean not, final boolean nested, final String fieldName, Map<String, BoolQueryBuilder> nestedQueries) {

        QueryBuilder query = (nested ? buildNestedQuery(fieldName, queryBuilder, logicalOperator, not, nestedQueries) : queryBuilder);

        if (query == null)
            return;

        if(logicalOperator.equals(AND)) {
            if (!not || nested)
                boolQueryBuilder.must(query);
            else
                boolQueryBuilder.mustNot(query);
        } else if(logicalOperator.equals(LogicalOperator.OR)) {
            if (!not || nested)
                boolQueryBuilder.should(query);
            else
                boolQueryBuilder.should(boolQuery().mustNot(query));
        }
    }

    private QueryBuilder buildNestedQuery(final String fieldName, final QueryBuilder query, final LogicalOperator logicalOperator, final boolean not, Map<String, BoolQueryBuilder> nestedQueries) {

        String parentFieldName = fieldName.split("\\.")[0];

        BoolQueryBuilder boolQueryBuilder;
        boolean found = false;

        if (nestedQueries.containsKey(parentFieldName)) {
            boolQueryBuilder = nestedQueries.get(parentFieldName);
            found = true;
        } else {
            boolQueryBuilder = boolQuery();
            nestedQueries.put(parentFieldName, boolQueryBuilder);
        }

        if(logicalOperator.equals(AND)) {
            if (!not)
                boolQueryBuilder.must(query);
            else
                boolQueryBuilder.mustNot(query);
        } else if(logicalOperator.equals(LogicalOperator.OR)) {
            if (!not)
                boolQueryBuilder.should(query);
            else
                boolQueryBuilder.should(boolQuery().mustNot(query));
        }

        return found ? null : nestedQuery(parentFieldName, boolQueryBuilder, None);
    }

    private GeoPoint createGeoPointFromRawCoordinates(List<com.vivareal.search.api.model.query.Value> viewPortLatLon) {
        return new GeoPoint(viewPortLatLon.get(0).value(), viewPortLatLon.get(1).value());
    }

    private boolean isNotBeforeCurrentQueryFragment(final QueryFragmentList queryFragmentList, final int index) {
        if (index - 1 >= 0) {
            QueryFragment before = queryFragmentList.get(index - 1);
            if (before instanceof QueryFragmentNot) {
                QueryFragmentNot before1 = (QueryFragmentNot) before;
                return before1.isNot();
            }
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
        if (!isEmpty(request.getQ())) {
            QueryStringQueryBuilder queryStringBuilder = queryStringQuery(request.getQ());
            String index = request.getIndex();

            String mm = isEmpty(request.getMm()) ? QS_MM.getValue(index) : request.getMm();
            checkMM(mm, request);

            QS_DEFAULT_FIELDS.getValue(request.getFields(), index).forEach(boostField -> addFieldToSearchOnQParameter(queryStringBuilder, index, boostField));

            queryStringBuilder.minimumShouldMatch(mm).tieBreaker(0.2f).phraseSlop(2).defaultOperator(OR);
            queryBuilder.must().add(queryStringBuilder);
        }
    }

    private void checkMM(final String mm, final Queryable request) {
        String errorMessage = ("Minimum Should Match (mm) should be a valid integer number (-100 <> +100). Request: " + request.toString());

        if (mm.contains(".") || mm.contains("%") && ((mm.length() - 1) > mm.indexOf('%'))) {
            LOG.error(errorMessage);
            throw new NumberFormatException(errorMessage);
        }

        String mmNumber = mm.replace("%", "");

        if (!NumberUtils.isCreatable(mmNumber)) {
            LOG.error(errorMessage);
            throw new NumberFormatException(errorMessage);
        }

        int number = NumberUtils.toInt(mmNumber);

        if (number < -100 || number > 100) {
            LOG.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void applySort(SearchRequestBuilder searchRequestBuilder, final Sortable request) {
        Set<String> value = ES_DEFAULT_SORT.getValue(request.getSort(), request.getIndex());
        if (!isEmpty(value)) {
            Sort sort = SortParser.get().parse(value.stream().collect(joining(",")));
            sort.forEach(item -> {
                String fieldName = item.getField().getName();
                settingsAdapter.checkFieldName(request.getIndex(), fieldName);
                searchRequestBuilder.addSort(fieldName, SortOrder.valueOf(item.getOrderOperator().name()));
            });
        }
    }

    private void applyFacetFields(SearchRequestBuilder searchRequestBuilder, final Facetable request) {
        Set<String> value = request.getFacets();
        if (!isEmpty(value)) {
            List<Field> facets = FacetParser.get().parse(value.stream().collect(joining(",")));
            facets.forEach(facet -> {

                final String indexName = request.getIndex();
                final String fieldName = facet.getName();
                final int facetSize = ES_FACET_SIZE.getValue(request.getFacetSize(), request.getIndex());
                final int shardSize = parseInt(valueOf(settingsAdapter.settingsByKey(request.getIndex(), SHARDS)));

                settingsAdapter.checkFieldName(indexName, fieldName);

                AggregationBuilder agg = terms(fieldName)
                    .field(fieldName)
                    .size(facetSize)
                    .shardSize(shardSize)
                    .order(Terms.Order.count(false));

                if (settingsAdapter.isTypeOfNested(indexName, fieldName)) {
                    applyFacetsByNestedFields(searchRequestBuilder, fieldName, agg);
                } else {
                    searchRequestBuilder.addAggregation(agg);
                }
            });
        }
    }

    private void applyFacetsByNestedFields(SearchRequestBuilder searchRequestBuilder, final String fieldName, final AggregationBuilder agg) {
        final String name = fieldName.split("\\.")[0];

        Optional<AggregationBuilder> nestedAgg = ofNullable(searchRequestBuilder.request().source().aggregations())
            .map(builder -> builder.getAggregatorFactories().stream()
                .filter(aggregationBuilder -> aggregationBuilder instanceof NestedAggregationBuilder)
                .filter(aggregationBuilder -> name.equals(aggregationBuilder.getName()))
                .findFirst()
            ).orElse(empty());

        if (nestedAgg.isPresent()) {
            nestedAgg.get().subAggregation(agg);
        } else {
            searchRequestBuilder.addAggregation(nested(name, name).subAggregation(agg));
        }
    }

    private void addFieldToSearchOnQParameter(QueryStringQueryBuilder queryStringBuilder, final String indexName, final String boostField) {
        String[] boostFieldValues = boostField.split(":");
        String fieldName = boostFieldValues[0];
        settingsAdapter.checkFieldName(indexName, fieldName);
        queryStringBuilder.field(fieldName, boostFieldValues.length == 2 ? Float.parseFloat(boostFieldValues[1]) : 1.0f);
    }

    private void addFieldList(SearchRequestBuilder searchRequestBuilder, final Fetchable request) {
        Pair<String[], String[]> fetchSource = getFetchSourceFields(request);
        searchRequestBuilder.setFetchSource(fetchSource.getLeft(), fetchSource.getRight());
    }

    private Pair<String[], String[]> getFetchSourceFields(Fetchable request) {
        String[] includes = SOURCE_INCLUDES.getValue(request.getIncludeFields(), request.getIndex()).toArray(new String[0]);
        String[] excludes = SOURCE_EXCLUDES.getValue(request.getExcludeFields(), request.getIndex())
            .stream()
            .filter(field -> !contains(includes, field))
            .toArray(String[]::new);

        Stream.of(includes).forEach(field -> settingsAdapter.checkFieldName(request.getIndex(), field));
        Stream.of(excludes).forEach(field -> settingsAdapter.checkFieldName(request.getIndex(), field));

        return Pair.of(includes, excludes);
    }

    private SearchRequestBuilder prepareQuery(BaseApiRequest request, BiConsumer<SearchRequestBuilder, BoolQueryBuilder> builder) {
        settingsAdapter.checkIndex(request);

        SearchRequestBuilder searchBuilder = transportClient.prepareSearch(request.getIndex());
        searchBuilder.setPreference("_replica_first"); // <3

        BoolQueryBuilder queryBuilder = boolQuery();

        builder.accept(searchBuilder, queryBuilder);

        searchBuilder.setQuery(queryBuilder);

        LOG.debug("Request: {}", request);
        LOG.debug("Query: {}", searchBuilder);

        return searchBuilder;
    }
}

package com.vivareal.search.api.adapter;

import com.vivareal.search.api.configuration.SearchApiEnv.RemoteProperties;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.query.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.configuration.SearchApiEnv.RemoteProperties.*;
import static com.vivareal.search.api.model.query.LogicalOperator.AND;
import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.elasticsearch.index.query.Operator.OR;
import static org.elasticsearch.index.query.QueryBuilders.*;
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
    public GetRequestBuilder getById(SearchApiRequest request, String id) {
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
    public SearchRequestBuilder query(SearchApiRequest request) {
        settingsAdapter.checkIndex(request);

        SearchRequestBuilder searchBuilder = transportClient.prepareSearch(request.getIndex());
        searchBuilder.setPreference("_replica_first"); // <3
        searchBuilder.setFrom(request.getFrom());
        searchBuilder.setSize(request.getSize());
        addFieldList(searchBuilder, request);
        applySort(searchBuilder, request);
        applyFacetFields(searchBuilder, request);

        BoolQueryBuilder queryBuilder = boolQuery();
        searchBuilder.setQuery(queryBuilder);
        applyQueryString(queryBuilder, request);
        applyFilterQuery(queryBuilder, request.getFilter());

        LOG.debug("Request: {}", request);
        LOG.debug("Query: {}", searchBuilder);

        return searchBuilder;
    }

    private void applyFilterQuery(BoolQueryBuilder queryBuilder, final QueryFragment queryFragment) {
        if (queryFragment != null && queryFragment instanceof QueryFragmentList) {
            QueryFragmentList queryFragmentList = (QueryFragmentList) queryFragment;
            LogicalOperator logicalOperator = AND;

            for (int index = 0; index < queryFragmentList.size(); index++) {
                QueryFragment queryFragmentFilter = queryFragmentList.get(index);

                if (queryFragmentFilter instanceof QueryFragmentList) {
                    BoolQueryBuilder recursiveQueryBuilder = boolQuery();
                    addFilterQueryByLogicalOperator(queryBuilder, recursiveQueryBuilder, getLogicalOperatorByQueryFragmentList(queryFragmentList, index, logicalOperator), isNotBeforeCurrentQueryFragment(queryFragmentList, index));
                    applyFilterQuery(recursiveQueryBuilder, queryFragmentFilter);

                } else if (queryFragmentFilter instanceof QueryFragmentItem) {
                    QueryFragmentItem queryFragmentItem = (QueryFragmentItem) queryFragmentFilter;
                    Filter filter = queryFragmentItem.getFilter();

                    String fieldName = filter.getField().getName();
                    final boolean not = isNotBeforeCurrentQueryFragment(queryFragmentList, index);
                    logicalOperator = getLogicalOperatorByQueryFragmentList(queryFragmentList, index, logicalOperator);

                    if (!isEmpty(filter.getValue().getContents())) {
                        RelationalOperator operator = filter.getRelationalOperator();

                        List<Object> multiValues = filter.getValue().getContents();
                        Object singleValue = filter.getValue().getContents(0);

                        switch (operator) {
                            case DIFFERENT:
                                addFilterQueryByLogicalOperator(queryBuilder, matchQuery(fieldName, singleValue), logicalOperator, !not);
                                break;
                            case EQUAL:
                                addFilterQueryByLogicalOperator(queryBuilder, matchQuery(fieldName, singleValue), logicalOperator, not);
                                break;
                            case GREATER:
                                addFilterQueryByLogicalOperator(queryBuilder, rangeQuery(fieldName).from(singleValue).includeLower(false), logicalOperator, not);
                                break;
                            case GREATER_EQUAL:
                                addFilterQueryByLogicalOperator(queryBuilder, rangeQuery(fieldName).from(singleValue).includeLower(true), logicalOperator, not);
                                break;
                            case LESS:
                                addFilterQueryByLogicalOperator(queryBuilder, rangeQuery(fieldName).to(singleValue).includeUpper(false), logicalOperator, not);
                                break;
                            case LESS_EQUAL:
                                addFilterQueryByLogicalOperator(queryBuilder, rangeQuery(fieldName).to(singleValue).includeUpper(true), logicalOperator, not);
                                break;
                            case IN:
                                Object[] values = multiValues.stream().map(contents -> ((com.vivareal.search.api.model.query.Value) contents).getContents(0)).toArray();
                                addFilterQueryByLogicalOperator(queryBuilder, termsQuery(fieldName, values), logicalOperator, not);
                                break;
                            case VIEWPORT:
                                GeoPoint topRight = createGeoPointFromRawCoordinates((List) multiValues.get(0));
                                GeoPoint bottomLeft = createGeoPointFromRawCoordinates((List) multiValues.get(1));
                                addFilterQueryByLogicalOperator(queryBuilder, geoBoundingBoxQuery(filter.getField().getName()).setCornersOGC(bottomLeft, topRight), logicalOperator, not);
                                break;
                            default:
                                throw new UnsupportedOperationException("Unknown Relational Operator " + operator.name());
                        }
                    } else {
                        RangeQueryBuilder lte = rangeQuery(fieldName).to(0).includeUpper(true);
                        RangeQueryBuilder gte = rangeQuery(fieldName).from(0).includeLower(true);
                        addFilterQueryByLogicalOperator(queryBuilder, boolQuery().should(lte).should(gte), logicalOperator, !not);
                    }
                }
            }
        }
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

    private void addFilterQueryByLogicalOperator(BoolQueryBuilder queryBuilder, final QueryBuilder query, final LogicalOperator logicalOperator, final boolean not) {
        if(logicalOperator.equals(AND)) {
            if (!not)
                queryBuilder.must(query);
            else
                queryBuilder.mustNot(query);
        } else if(logicalOperator.equals(LogicalOperator.OR)) {
            if (!not)
                queryBuilder.should(query);
            else
                queryBuilder.should(boolQuery().mustNot(query));
        }
    }

    private void applyQueryString(BoolQueryBuilder queryBuilder, final SearchApiRequest request) {
        if (!isEmpty(request.getQ())) {
            QueryStringQueryBuilder queryStringBuilder = queryStringQuery(request.getQ());

            String mm = isEmpty(request.getMm()) ? QS_MM.getValue() : request.getMm();
            checkMM(mm, request);

            Map<String, Float> fields = new HashMap<>();

            if (isEmpty(request.getFields())) {
                stream(QS_DEFAULT_FIELDS.getValue().split(",")).forEach(bf -> addFieldToSearchOnQParameter(queryStringBuilder, bf));
            } else {
                request.getFields().forEach(boostField -> addFieldToSearchOnQParameter(queryStringBuilder, boostField));
            }
            queryStringBuilder.fields(fields).minimumShouldMatch(mm).tieBreaker(0.2f).phraseSlop(2).defaultOperator(OR);
            queryBuilder.must().add(queryStringBuilder);
        }
    }

    private void checkMM(final String mm, final SearchApiRequest request) {
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

    private void applySort(SearchRequestBuilder searchRequestBuilder, final SearchApiRequest request) {
        if (!isEmpty(request.getSort()))
            request.getSort().forEach(s -> searchRequestBuilder.addSort(s.getField().getName(), SortOrder.valueOf(s.getOrderOperator().name())));
    }

    private void applyFacetFields(SearchRequestBuilder searchRequestBuilder, final SearchApiRequest request) {
        if (!isEmpty(request.getFacets()))
            request.getFacets().forEach(facetField -> searchRequestBuilder.addAggregation(AggregationBuilders.terms(facetField.getName())
                    .field(facetField.getName())
                    .order(Terms.Order.count(false))
                    .size(request.getFacetSize() != null ? request.getFacetSize() : parseInt(ES_FACET_SIZE.getValue()))
                    .shardSize(parseInt(valueOf(settingsAdapter.settingsByKey(request.getIndex(), SHARDS))))));
    }

    private void addFieldToSearchOnQParameter(QueryStringQueryBuilder queryStringBuilder, final String boostField) {
        String[] boostFieldValues = boostField.split(":");
        queryStringBuilder.field(boostFieldValues[0], boostFieldValues.length == 2 ? Float.parseFloat(boostFieldValues[1]) : 1.0f);
    }

    private void addFieldList(SearchRequestBuilder searchRequestBuilder, final SearchApiRequest request) {
        Pair<String[], String[]> fetchSource = getFetchSourceFields(request);
        searchRequestBuilder.setFetchSource(fetchSource.getLeft(), fetchSource.getRight());
    }

    private Pair<String[], String[]> getFetchSourceFields(SearchApiRequest request) {
        String[] includes = getDefaultFieldConfig(SOURCE_INCLUDES);
        if (!isEmpty(request.getIncludeFields())) {
            includes = list2Array(request.getIncludeFields());
        }

        String[] excludes = getDefaultFieldConfig(SOURCE_EXCLUDES);
        if (!isEmpty(request.getExcludeFields())) {
            excludes = list2Array(request.getExcludeFields());
        }

        if (!ArrayUtils.isEmpty(includes) && !ArrayUtils.isEmpty(excludes)) {
            Set<String> includesList = Stream.of(includes).collect(Collectors.toSet());
            excludes = stream(excludes).filter(ex -> !includesList.contains(ex)).toArray(String[]::new);
        }

        return Pair.of(includes, excludes);
    }

    private String[] getDefaultFieldConfig(RemoteProperties sourceDefaultIncludes) {
        return ofNullable(sourceDefaultIncludes)
            .map(RemoteProperties::getValue)
            .filter(StringUtils::isNotBlank)
            .map(defaultFields -> defaultFields.split(","))
            .orElse(new String[]{});
    }

    private String[] list2Array(List<String> rawList) {
        List<String> list = firstNonNull(rawList, emptyList());
        return list.toArray(new String[list.size()]);
    }
}

package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;
import com.vivareal.search.api.model.parser.SortParser;
import com.vivareal.search.api.model.query.*;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.vivareal.search.api.model.SearchApiResponse.builder;
import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.elasticsearch.index.query.Operator.OR;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@Scope(SCOPE_SINGLETON)
@Qualifier("ElasticsearchQuery")
public class ElasticsearchQueryAdapter extends AbstractQueryAdapter<SearchRequestBuilder, List<QueryFragment>, Sort> {

    private static Logger LOG = LoggerFactory.getLogger(ElasticsearchQueryAdapter.class);

    private final TransportClient transportClient;

    @Autowired
    @Qualifier("ElasticsearchSettings")
    private SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;

    @Value("${querystring.listings.default.fields}")
    private String queryListingsDefaultFields;

    @Value("${querystring.listings.default.operator}")
    private String queryListingsDefaultOperator;

    @Value("${querystring.listings.default.mm}")
    private String queryListingsDefaultMM;

    @Value("${es.controller.search.timeout}")
    private Integer timeout;

    @Value("${es.facet.size}")
    private Integer facetSize;

    public ElasticsearchQueryAdapter(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    @Override
    public Optional<SearchApiResponse> getById(SearchApiRequest request, String id) {
        settingsAdapter.isValidIndex(request);
        GetRequestBuilder requestBuilder = transportClient.prepareGet().setIndex(request.getIndex()).setType(request.getIndex()).setId(id);

        try {
            GetResponse response = requestBuilder.execute().get(timeout, TimeUnit.MILLISECONDS);
            return ofNullable(builder().result(request.getIndex(), response.getSource()).totalCount(response.getSource() != null ? 1 : 0));
        } catch (Exception e) {
            LOG.error("Getting id={}, request: {}, error: {}", id, request, e);
        }
        return empty();
    }

    @Override
    public SearchRequestBuilder query(SearchApiRequest request) {
        settingsAdapter.isValidIndex(request);

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

        LOG.debug("Query: {}", searchBuilder);

        return searchBuilder;
    }

    private void applyFilterQuery(BoolQueryBuilder queryBuilder, final QueryFragment queryFragment) {
        if (queryFragment != null && queryFragment instanceof QueryFragmentList) {
            QueryFragmentList queryFragmentList = (QueryFragmentList) queryFragment;
            LogicalOperator logicalOperator = LogicalOperator.AND;

            for (int index = 0; index < queryFragmentList.size(); index++) {
                QueryFragment queryFragmentFilter = queryFragmentList.get(index);

                if (queryFragmentFilter instanceof QueryFragmentList) {
                    BoolQueryBuilder recursiveQueryBuilder = boolQuery();
                    addFilterQueryByLogicalOperator(queryBuilder, recursiveQueryBuilder, getLogicalOperatorByQueryFragmentList(queryFragmentList, index, logicalOperator), isNotBeforeCurrentQueryFragment(queryFragmentList, index));
                    applyFilterQuery(recursiveQueryBuilder, queryFragmentFilter);

                } else if (queryFragmentFilter instanceof QueryFragmentItem) {
                    QueryFragmentItem queryFragmentItem = (QueryFragmentItem) queryFragmentFilter;
                    Filter filter = queryFragmentItem.getFilter();

                    if (!isEmpty(filter.getValue().getContents())) {
                        RelationalOperator operator = filter.getRelationalOperator();
                        String fieldName = filter.getField().getName();

                        List<Object> multiValues = filter.getValue().getContents();
                        Object singleValue = filter.getValue().getContents(0);

                        final boolean not = isNotBeforeCurrentQueryFragment(queryFragmentList, index);
                        logicalOperator = getLogicalOperatorByQueryFragmentList(queryFragmentList, index, logicalOperator);

                        switch (operator) {
                            case DIFFERENT:
                                addFilterQueryByLogicalOperator(queryBuilder, matchQuery(fieldName, singleValue), logicalOperator, not);
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
                                addFilterQueryByLogicalOperator(queryBuilder, termsQuery(fieldName, multiValues), logicalOperator, not);
                                break;
                            default:
                                throw new UnsupportedOperationException("Unknown Relational Operator " + operator.name());
                        }
                    }
                }
            }
        }
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
            if (next instanceof QueryFragmentItem) {
                logicalOperator = ((QueryFragmentItem) next).getLogicalOperator();

            } else if (next instanceof QueryFragmentOperator) {
                logicalOperator = ((QueryFragmentOperator) next).getOperator();
            }
        }
        return logicalOperator;
    }

    private void addFilterQueryByLogicalOperator(BoolQueryBuilder queryBuilder, final QueryBuilder query, final LogicalOperator logicalOperator, final boolean not) {

        switch (logicalOperator) {
            case AND:
                if (!not) {
                    queryBuilder.must(query);
                } else {
                    queryBuilder.mustNot(query);
                }
                break;

            case OR:
                if (!not) {
                    queryBuilder.should(query);
                } else {
                    queryBuilder.should(boolQuery().mustNot(query));
                }
                break;
        }
    }

    private void applyQueryString(BoolQueryBuilder queryBuilder, final SearchApiRequest request) {
        if (!isEmpty(request.getQ())) {
            QueryStringQueryBuilder queryStringBuilder = queryStringQuery(request.getQ());
            if ("listings".equals(request.getIndex())) {
                Map<String, Float> fields = new HashMap<>();
                if (isEmpty(request.getFields())) {
                    String[] boostFields = queryListingsDefaultFields.split(",");
                    for (final String boostField : boostFields) {
                        addFieldToSearchOnQParameter(queryStringBuilder, boostField);
                    }
                } else {
                    request.getFields().forEach(boostField -> {
                        addFieldToSearchOnQParameter(queryStringBuilder, boostField);
                    });
                }

                String operator = ofNullable(request.getOperator())
                .filter(op -> op.equalsIgnoreCase(OR.name()))
                .map(op -> OR.name())
                .orElse(queryListingsDefaultOperator);

                // if client specify mm on the request, the default operator is OR
                operator = ofNullable(request.getMm()).map(op -> OR.name()).orElse(operator);

                queryStringBuilder.fields(fields).minimumShouldMatch(isEmpty(request.getMm()) ? queryListingsDefaultMM : request.getMm()).tieBreaker(0.2f).phraseSlop(2).defaultOperator(Operator.valueOf(operator));
            }
            queryBuilder.must().add(queryStringBuilder);
        }
    }

    private void applySort(SearchRequestBuilder searchRequestBuilder, final SearchApiRequest request) {
        if (!isEmpty(request.getSort()))
            request.getSort().forEach(s -> {
                searchRequestBuilder.addSort(s.getField().getName(), SortOrder.valueOf(s.getOrderOperator().name()));
            });
    }

    private void applyFacetFields(SearchRequestBuilder searchRequestBuilder, final SearchApiRequest request) {
        if (!isEmpty(request.getFacets()))
            request.getFacets().forEach(facetField -> {
                searchRequestBuilder.addAggregation(AggregationBuilders.terms(facetField.getName()).field(facetField.getName()).order(Terms.Order.count(false)).size(request.getFacetSize() != null ? request.getFacetSize() : facetSize).shardSize(parseInt(valueOf(settingsAdapter.settingsByKey(request.getIndex(), SHARDS)))));
            });
    }

    private void addFieldToSearchOnQParameter(QueryStringQueryBuilder queryStringBuilder, final String boostField) {
        String[] boostFieldValues = boostField.split(":");
        queryStringBuilder.field(boostFieldValues[0], boostFieldValues.length == 2 ? Float.parseFloat(boostFieldValues[1]) : 1.0f);
    }

    private void addFieldList(SearchRequestBuilder searchRequestBuilder, final SearchApiRequest request) {
        if (!isEmpty(request.getIncludeFields()) || !isEmpty(request.getExcludeFields())) {
            searchRequestBuilder.setFetchSource(request.getIncludeFields().toArray(new String[request.getIncludeFields().size()]), request.getExcludeFields().toArray(new String[request.getExcludeFields().size()]));
        }
    }

    @Override
    protected List<QueryFragment> getFilter(List<String> filter) {
        return null;
    }

    @Override
    protected Sort getSort(List<String> sort) {
        return SortParser.get().parse(sort.stream().collect(Collectors.joining(" ")));
    }

}

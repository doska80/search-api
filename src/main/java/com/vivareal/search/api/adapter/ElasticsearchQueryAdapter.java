package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiIndex;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;
import com.vivareal.search.api.model.query.Sort;
import com.vivareal.search.api.parser.Filter;
import com.vivareal.search.api.parser.QueryFragment;
import com.vivareal.search.api.parser.RelationalOperator;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
public class ElasticsearchQueryAdapter extends AbstractQueryAdapter<SearchRequestBuilder, List<QueryFragment>, List<Sort>> {

    private static Logger LOG = LoggerFactory.getLogger(ElasticsearchQueryAdapter.class);

    private final TransportClient transportClient;

    @Value("${querystring.listings.default.fields}")
    private String queryListingsDefaultFields;

    @Value("${querystring.listings.default.operator}")
    private String queryListingsDefaultOperator;

    @Value("${querystring.listings.default.mm}")
    private String queryListingsDefaultMM;

    public ElasticsearchQueryAdapter(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    @Override
    public Optional<SearchApiResponse> getById(SearchApiRequest request, String id) {
        SearchApiIndex index = SearchApiIndex.of(request);
        GetRequestBuilder requestBuilder = transportClient.prepareGet().setIndex(index.getIndex()).setType(index.getIndex()).setId(id);

        try {
            GetResponse response = requestBuilder.execute().get(1, TimeUnit.SECONDS);
            return ofNullable(SearchApiResponse.builder().result(index.getIndex(), response.getSource()).totalCount(response.getSource() != null ? 1 : 0));
        } catch (Exception e) {
            LOG.error("Getting id={}, request: {}, error: {}", id, request, e);
        }
        return empty();
    }

    @Override
    public SearchRequestBuilder query(SearchApiRequest request) {
        SearchApiIndex index = SearchApiIndex.of(request);

        SearchRequestBuilder searchBuilder = transportClient.prepareSearch(index.getIndex());
        searchBuilder.setPreference("_replica_first"); // <3
        searchBuilder.setFrom(request.getFrom());
        searchBuilder.setSize(request.getSize());
        addFieldList(searchBuilder, request);

        BoolQueryBuilder queryBuilder = boolQuery();
        searchBuilder.setQuery(queryBuilder);
        applyQueryString(queryBuilder, request);

//        if (!request.getFilter().isEmpty()) {
//            request.getFilter().forEach(filterFragment -> {
//                Filter filter = null;
//                RelationalOperator operator = filter.getRelationalOperator();
//                String fieldName = filter.getField().getName();
//                List<Object> values = filter.getValue().getContents();
//                if (values == null || values.isEmpty())
//                    return;
//                if (values.size() == 1) {
//                    Object firstValue = values.get(0);
//                    switch (operator) {
//                        case DIFFERENT:
//                            queryBuilder.mustNot().add(matchQuery(fieldName, firstValue));
//                            break;
//                        case EQUAL:
//                            queryBuilder.must().add(matchQuery(fieldName, firstValue));
//                            break;
//                        case GREATER:
//                            queryBuilder.must().add(rangeQuery(fieldName).from(firstValue).includeLower(false));
//                            break;
//                        case GREATER_EQUAL:
//                            queryBuilder.must().add(rangeQuery(fieldName).from(firstValue).includeLower(true));
//                            break;
//                        case LESS:
//                            queryBuilder.must().add(rangeQuery(fieldName).to(firstValue).includeUpper(false));
//                            break;
//                        case LESS_EQUAL:
//                            queryBuilder.must().add(rangeQuery(fieldName).to(firstValue).includeUpper(true));
//                            break;
//                        default:
//                            throw new UnsupportedOperationException("Unknown Relational Operator " + operator.name());
//                    }
//                } else {
//                    queryBuilder.must().add(QueryBuilders.termsQuery(fieldName, values));
//                }
//            });
//        }

        LOG.debug("Query: {}", searchBuilder);

        return searchBuilder;
    }

    private void applyQueryString(BoolQueryBuilder queryBuilder, final SearchApiRequest request) {
        if (!isEmpty(request.getQ())) {
            QueryStringQueryBuilder queryStringBuilder = queryStringQuery(request.getQ());
            if (SearchApiIndex.SearchIndex.LISTINGS.index().equals(request.getIndex())) {
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
    protected List<Sort> getSort(List<String> sort) {
        return null;
    }

}

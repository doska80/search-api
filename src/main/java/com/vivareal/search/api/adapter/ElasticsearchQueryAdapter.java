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
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Component
@Scope(SCOPE_SINGLETON)
@Qualifier("ElasticsearchQuery")
public class ElasticsearchQueryAdapter extends AbstractQueryAdapter<SearchHit, List<QueryFragment>, List<Sort>> {

    private static Logger LOG = LoggerFactory.getLogger(ElasticsearchQueryAdapter.class);

    private final TransportClient transportClient;

    public ElasticsearchQueryAdapter(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    @Override
    public Object getById(SearchApiRequest request, String id) {
        SearchApiIndex index = SearchApiIndex.of(request);

        GetRequestBuilder requestBuilder = transportClient.prepareGet().setIndex(index.getIndex()).setType(index.getIndex()).setId(id);
        try {
            GetResponse response = requestBuilder.execute().get(1, TimeUnit.SECONDS);
            return response.getSource();
        } catch (Exception e) {
            LOG.error("Getting id={} error: {}", id, request);
        }
        return null;
    }

    @Override
    public SearchApiResponse query(SearchApiRequest request) {
        SearchApiIndex index = SearchApiIndex.of(request);
        List<Object> response = new ArrayList<>();
        SearchRequestBuilder searchBuilder = transportClient.prepareSearch(index.getIndex());
        searchBuilder.setPreference("_replica_first"); // <3

        BoolQueryBuilder query = new BoolQueryBuilder();

        if (!request.getFilter().isEmpty()) {
            BoolQueryBuilder filterQuery = new BoolQueryBuilder();
            request.getFilter().forEach(filterFragment -> {
                QueryFragment.Type type = filterFragment.getType();
                if (QueryFragment.Type.EXPRESSION_LIST.equals(type)) { // TODO sub query, still not workiung :(
                    throw new UnsupportedOperationException("Subqueries aren't supported yet :("); // TODO TUDO!
                } else if (QueryFragment.Type.FILTER.equals(type)) {
                    Filter filter = filterFragment.get();
                    RelationalOperator operator = filter.getRelationalOperator();
                    String fieldName = filter.getField().getName();
                    List<String> values = filter.getValue().getContents();
                    if (values == null || values.isEmpty())
                        return;
                    if (values.size() == 1) {
                        String firstValue = values.get(0);
                        if (RelationalOperator.DIFFERENT.equals(operator))
                            filterQuery.mustNot().add(QueryBuilders.matchQuery(fieldName, firstValue));
                        else if (RelationalOperator.EQUAL.equals(operator))
                            filterQuery.must().add(QueryBuilders.matchQuery(fieldName, firstValue));
                        else if (RelationalOperator.GREATER.equals(operator))
                            filterQuery.must().add(QueryBuilders.rangeQuery(fieldName).from(firstValue).includeLower(false));
                        else if (RelationalOperator.GREATER_EQUAL.equals(operator))
                            filterQuery.must().add(QueryBuilders.rangeQuery(fieldName).from(firstValue).includeLower(true));
                        else if (RelationalOperator.LESS.equals(operator))
                            filterQuery.must().add(QueryBuilders.rangeQuery(fieldName).to(firstValue).includeUpper(false));
                        else if (RelationalOperator.LESS_EQUAL.equals(operator))
                            filterQuery.must().add(QueryBuilders.rangeQuery(fieldName).to(firstValue).includeUpper(true));
                        else
                            throw new UnsupportedOperationException("Unknown Relational Operator " + operator.name());
                    } else {
                        filterQuery.must().add(QueryBuilders.termsQuery(fieldName, values));
                    }
                } else {
                    // FIXME AND / OR between query fragments. everything is working as AND now... :/
                }
            });
            query.filter(filterQuery);
        }
        searchBuilder.setQuery(query);
        LOG.debug("Query: {}", searchBuilder);
        SearchResponse esResponse = searchBuilder.execute().actionGet();
        esResponse.getHits().forEach(hit -> {  // FIXME should be async if possible
            response.add(hit.getSource()); // FIXME avoid iterating twice!
        });

        return new SearchApiResponse(esResponse.getTookInMillis(), esResponse.getHits().getTotalHits(), response);
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

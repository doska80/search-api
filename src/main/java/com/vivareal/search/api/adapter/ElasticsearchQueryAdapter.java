package com.vivareal.search.api.adapter;


import com.vivareal.search.api.model.SearchApiIndex;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.model.query.Sort;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;

import org.elasticsearch.search.SearchHit;

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Component
@Scope(SCOPE_SINGLETON)
@Qualifier("ElasticsearchQuery")
public class ElasticsearchQueryAdapter extends AbstractQueryAdapter<SearchHit,List<Field>,List<Sort>> {

    public static final String INDEX = "inmuebles";

    private final TransportClient transportClient;

    public ElasticsearchQueryAdapter(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    @Override
    public Object getById(SearchApiRequest request, String id) {
        SearchApiIndex index = SearchApiIndex.of(request);

        // FIXME we need to receive index and type separately somehow
        GetRequestBuilder requestBuilder = transportClient.prepareGet().setIndex(index.getIndex()).setType(index.getIndex()).setId(id);
        try {
            GetResponse response = requestBuilder.execute().get(1, TimeUnit.SECONDS);
            return response.getSource();
        } catch (Exception e) {
            e.printStackTrace(); // FIXME seriously, fix me.
        }
        return null;
    }

    @Override
    public List<SearchHit> getQuery(SearchApiRequest request) {
        SearchRequestBuilder searchBuilder = transportClient.prepareSearch("inmuebles"); // FIXME parameter
        request.getFilter().forEach(filter -> {
            List<Field> x = this.parseFilter(filter);
            System.out.println(x);
        });
        return Arrays.asList(searchBuilder.execute().actionGet().getHits().getHits()); // FIXME should be async if possible
    }

    public List<SearchHit> getQueryMamud(SearchApiRequest request) {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        QueryStringQueryBuilder queryString = new QueryStringQueryBuilder(request.getQ());
        boolQuery.must().add(queryString);

        SearchRequestBuilder searchBuilder = transportClient.prepareSearch(INDEX)
                .setSize(100) // TODO we must configure timeouts
                .setScroll(new TimeValue(60000));
        searchBuilder.setQuery(boolQuery);

        return null;
    }

    @Override
    protected List<Field> getFilter(List<String> filter) {
        return null;
    }

    @Override
    protected List<Sort> getSort(List<String> sort) {
        return null;
    }

//
//    public Object getFilter(SearchApiRequest searchApiRequest, String... indices) {
//        SearchRequestBuilder builder = this.transportClient.prepareSearch(indices);
//        return builder;
//    }


//    public

}

package com.vivareal.search.api.adapter;


import com.vivareal.search.api.controller.v2.stream.ResponseStream;
import com.vivareal.search.api.model.SearchApiIndex;
import com.vivareal.search.api.model.SearchApiIterator;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.query.Sort;
import com.vivareal.search.api.parser.QueryFragment;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Component
@Scope(SCOPE_SINGLETON)
@Qualifier("ElasticsearchQuery")
public class ElasticsearchQueryAdapter extends AbstractQueryAdapter<SearchHit,List<QueryFragment>,List<Sort>> {

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
    public List<Map<String, Object>> getQueryMarcao(SearchApiRequest request) {
        List<Map<String, Object>> response = new ArrayList<>();
        SearchRequestBuilder searchBuilder = transportClient.prepareSearch("inmuebles"); // FIXME parameter
        searchBuilder.setPreference("_replica_first"); // <3

        if (request.getFilter().isEmpty()) {
            BoolQueryBuilder filterQuery = new BoolQueryBuilder();
            request.getFilter().forEach(filter -> {
                if (filter.size() == 1) {
//                    QueryFragment orFilter = filter.get(0);
//                    filterQuery.should().add(QueryBuilders.matchQuery(orFilter.getName(), orFilter.getValue()));
                } else {
//                    BoolQueryBuilder andFilterQuery = new BoolQueryBuilder();
//                    filter.forEach(andFilter -> {
//                        andFilterQuery.must().add(QueryBuilders.matchQuery(andFilter.getName(), andFilter.getValue()));
//                    });
//                    filterQuery.should().add(andFilterQuery);
                }
            });
            searchBuilder.setQuery(filterQuery);
        }
        System.out.println(searchBuilder);
        searchBuilder.execute().actionGet().getHits().forEach(hit -> {  // FIXME should be async if possible
            response.add(hit.getSource()); // FIXME avoid iterating twice!
        });
        return response;
    }

    @Override
    public void stream(SearchApiRequest request, OutputStream stream) {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        QueryStringQueryBuilder queryString = new QueryStringQueryBuilder(request.getQ());
        boolQuery.must().add(queryString);

        SearchRequestBuilder core = transportClient.prepareSearch(SearchApiIndex.of(request).getIndex())
                .setSearchType(SearchType.QUERY_THEN_FETCH)
                .setSize(ofNullable(request.getSize()).map(Integer::parseInt).orElse(10))
                .setFrom(ofNullable(request.getFrom()).map(Integer::parseInt).orElse(0))
                .setScroll(new TimeValue(200)); // TODO we must configure timeouts

        core.setQuery(boolQuery);

        ResponseStream.create(stream)
                .withIterator(new SearchApiIterator<>(transportClient, core.get()), SearchHit::source);
    }

    @Override
    protected List<QueryFragment> getFilter(List<String> filter) {
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

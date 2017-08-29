package com.vivareal.search.api.service;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.controller.stream.ElasticSearchStream;
import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import com.vivareal.search.api.model.http.SearchApiResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static java.util.Optional.ofNullable;

@Component
public class SearchService {

    private static Logger LOG = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    @Qualifier("ElasticsearchQuery")
    private QueryAdapter<GetRequestBuilder, SearchRequestBuilder> queryAdapter;

    @Autowired
    private ElasticSearchStream elasticSearch;

    public Optional<Object> getById(BaseApiRequest request, String id) throws InterruptedException, ExecutionException, TimeoutException {
        return ofNullable(this.queryAdapter.getById(request, id).execute().get(ES_CONTROLLER_SEARCH_TIMEOUT.getValue(request.getIndex()), TimeUnit.MILLISECONDS).getSource());
    }

    public SearchApiResponse search(SearchApiRequest request) {
        String index = request.getIndex();
        request.setPaginationValues(ES_DEFAULT_SIZE.getValue(index), ES_MAX_SIZE.getValue(index));

        SearchRequestBuilder requestBuilder = this.queryAdapter.query(request);
        SearchResponse esResponse = requestBuilder.execute().actionGet((Long) ES_CONTROLLER_SEARCH_TIMEOUT.getValue(index));

        List<Object> data = new ArrayList<>(esResponse.getHits().getHits().length);
        esResponse.getHits().forEach(hit -> data.add(hit.getSource()));

        return new SearchApiResponse()
                .time(esResponse.getTookInMillis())
                .totalCount(esResponse.getHits().getTotalHits())
                .result(request.getIndex(), data)
                .facets(esResponse.getAggregations());
    }

    public void stream(BaseApiRequest request, OutputStream stream) {
        elasticSearch.stream(request, stream);
    }
}

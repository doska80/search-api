package com.vivareal.search.api.service;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.controller.stream.ElasticSearchStream;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static com.vivareal.search.api.model.SearchApiResponse.builder;
import static java.util.Collections.synchronizedList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;

@Component
public class SearchService {

    private static Logger LOG = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    @Qualifier("ElasticsearchQuery")
    private QueryAdapter<GetRequestBuilder, SearchRequestBuilder> queryAdapter;

    @Autowired
    private ElasticSearchStream elasticSearch;

    public Optional<Object> getById(SearchApiRequest request, String id) {
        String index = request.getIndex();
        request.setPaginationValues(ES_DEFAULT_SIZE.getValue(index), ES_MAX_SIZE.getValue(index));

        try {
            GetResponse response = this.queryAdapter.getById(request, id).execute().get(ES_CONTROLLER_SEARCH_TIMEOUT.getValue(index), TimeUnit.MILLISECONDS);
            if (response.isExists())
                return ofNullable(response.getSource());

        } catch (Exception e) {
            LOG.error("Getting id={}, request: {}, error: {}", id, request, e);
        }
        return empty();
    }

    public SearchApiResponse search(SearchApiRequest request) {
        String index = request.getIndex();
        request.setPaginationValues(ES_DEFAULT_SIZE.getValue(index), ES_MAX_SIZE.getValue(index));

        SearchRequestBuilder requestBuilder = this.queryAdapter.query(request);
        SearchResponse esResponse = requestBuilder.execute().actionGet((Long) ES_CONTROLLER_SEARCH_TIMEOUT.getValue(index));

        return builder()
                .time(esResponse.getTookInMillis())
                .totalCount(esResponse.getHits().getTotalHits())
                .result(request.getIndex(),
                        Arrays.stream(esResponse.getHits().getHits())
                        .map(SearchHit::getSource)
                        .collect(toCollection(() -> synchronizedList(new ArrayList<>(esResponse.getHits().getHits().length)))))
                .facets(ofNullable(esResponse.getAggregations()));
    }

    public void stream(SearchApiRequest request, OutputStream stream) {
        String index = request.getIndex();
        request.setPaginationValues(ES_DEFAULT_SIZE.getValue(index), ES_MAX_SIZE.getValue(index));
        elasticSearch.stream(request, stream);
    }
}

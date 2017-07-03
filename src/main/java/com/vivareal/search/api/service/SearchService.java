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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
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

    @Value("${es.default.size}")
    private Integer defaultSize;

    @Value("${es.max.size}")
    private Integer maxSize;

    @Value("${es.controller.search.timeout}")
    private Integer timeout;

    @Autowired
    private ElasticSearchStream elasticSearch;

    public Optional<SearchApiResponse> getById(SearchApiRequest request, String id) {
        request.setPaginationValues(defaultSize, maxSize);

        try {
            GetResponse response = this.queryAdapter.getById(request, id).execute().get(timeout, TimeUnit.MILLISECONDS);
            if (response.isExists())
                return ofNullable(builder().result(request.getIndex(), newArrayList(response.getSource())).totalCount(1));

        } catch (Exception e) {
            LOG.error("Getting id={}, request: {}, error: {}", id, request, e);
        }
        return empty();
    }

    public SearchApiResponse search(SearchApiRequest request) {
        request.setPaginationValues(defaultSize, maxSize);

        SearchRequestBuilder requestBuilder = this.queryAdapter.query(request);
        SearchResponse esResponse = requestBuilder.execute().actionGet(timeout);

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
        request.setPaginationValues(defaultSize, maxSize);
        elasticSearch.stream(request, stream);
    }
}

package com.vivareal.search.api.service;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.controller.stream.ElasticSearchStream;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Collections.synchronizedList;
import static java.util.stream.Collectors.toCollection;

@Component
public class SearchService {

    @Autowired
    @Qualifier("ElasticsearchQuery")
    private QueryAdapter<SearchRequestBuilder> queryAdapter;

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
        return this.queryAdapter.getById(request, id);
    }

    public SearchApiResponse search(SearchApiRequest request) {
        request.setPaginationValues(defaultSize, maxSize);

        SearchRequestBuilder requestBuilder = this.queryAdapter.query(request);
        SearchResponse esResponse = requestBuilder.execute().actionGet(timeout);

        return SearchApiResponse.builder()
        .time(esResponse.getTookInMillis())
        .totalCount(esResponse.getHits().getTotalHits())
        .result(request.getIndex(),
            Arrays.stream(esResponse.getHits().getHits()).map(SearchHit::getSource).collect(
                toCollection(() -> synchronizedList(new ArrayList<>(esResponse.getHits().getHits().length)))
            ))
        .facets(Optional.ofNullable(esResponse.getAggregations()));
    }

    public void stream(SearchApiRequest request, OutputStream stream) {
        request.setPaginationValues(defaultSize, maxSize);
        elasticSearch.stream(request, stream);
    }
}

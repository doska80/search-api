package com.vivareal.search.api.service;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.controller.v2.stream.ElasticSearchStream;
import com.vivareal.search.api.model.SearchApiIndex;
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
import java.util.Arrays;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Component
public class SearchService {

    @Autowired
    @Qualifier("ElasticsearchQuery")
    protected QueryAdapter queryAdapter;

    @Value("${es.default.size}")
    private Integer defaultSize;

    @Value("${es.max.size}")
    private Integer maxSize;

    @Autowired
    private ElasticSearchStream elasticSearch;

    public Optional<SearchApiResponse> getById(SearchApiRequest request, String id) {
        request.setPaginationValues(defaultSize, maxSize);
        return this.queryAdapter.getById(request, id);
    }

    public SearchApiResponse search(SearchApiRequest request) {
        request.setPaginationValues(defaultSize, maxSize);

        SearchRequestBuilder requestBuilder = (SearchRequestBuilder) this.queryAdapter.query(request);
        SearchResponse esResponse = requestBuilder.execute().actionGet();

        return SearchApiResponse.builder()
            .time(esResponse.getTookInMillis())
            .totalCount(esResponse.getHits().getTotalHits())
            .result(SearchApiIndex.of(request).getIndex(),
                Arrays.stream(esResponse.getHits().hits()).map(SearchHit::getSource).collect(toList()));
    }

    public void stream(SearchApiRequest request, OutputStream stream) {
        request.setPaginationValues(defaultSize, maxSize);
        elasticSearch.stream(request, stream);
    }
}

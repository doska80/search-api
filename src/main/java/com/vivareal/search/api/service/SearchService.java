package com.vivareal.search.api.service;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.controller.stream.ElasticSearchStream;
import com.vivareal.search.api.exception.QueryPhaseExecutionException;
import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.FilterableApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

@Component
public class SearchService {

    @Autowired
    @Qualifier("ElasticsearchQuery")
    private QueryAdapter<GetRequestBuilder, SearchRequestBuilder> queryAdapter;

    @Autowired
    private ElasticSearchStream elasticSearch;

    public Optional<Object> getById(BaseApiRequest request, String id) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return ofNullable(this.queryAdapter.getById(request, id).execute().get(ES_CONTROLLER_SEARCH_TIMEOUT.getValue(request.getIndex()), TimeUnit.MILLISECONDS).getSource());
        } catch (Exception e) {
            if (getRootCause(e) instanceof IllegalArgumentException)
                throw new IllegalArgumentException(e);
            if (e instanceof ElasticsearchException)
                throw new QueryPhaseExecutionException(e);
            throw e;
        }
    }

    public SearchResponse search(SearchApiRequest request) {
        String index = request.getIndex();
        request.setPaginationValues(ES_DEFAULT_SIZE.getValue(index), ES_MAX_SIZE.getValue(index));

        SearchRequestBuilder searchRequestBuilder = null;

        try {
            searchRequestBuilder = this.queryAdapter.query(request);
            return searchRequestBuilder.execute().actionGet((Long) ES_CONTROLLER_SEARCH_TIMEOUT.getValue(index));
        } catch (Exception e) {
            if (getRootCause(e) instanceof IllegalArgumentException)
                throw new IllegalArgumentException(e);
            if (e instanceof ElasticsearchException)
                throw new QueryPhaseExecutionException(ofNullable(searchRequestBuilder).map(SearchRequestBuilder::toString).orElse("{}"), e);
            throw e;
        }
    }

    public void stream(FilterableApiRequest request, OutputStream stream) {
        elasticSearch.stream(request, stream);
    }
}

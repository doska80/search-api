package com.grupozap.search.api.service;

import static com.grupozap.search.api.service.CircuitBreakerService.CircuitType.GET_BY_ID;
import static com.grupozap.search.api.service.CircuitBreakerService.CircuitType.SEARCH;
import static java.lang.String.format;
import static java.util.Optional.of;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

import com.grupozap.search.api.adapter.QueryAdapter;
import com.grupozap.search.api.controller.stream.ElasticSearchStream;
import com.grupozap.search.api.exception.QueryPhaseExecutionException;
import com.grupozap.search.api.exception.QueryTimeoutException;
import com.grupozap.search.api.model.http.BaseApiRequest;
import com.grupozap.search.api.model.http.FilterableApiRequest;
import com.grupozap.search.api.model.http.SearchApiRequest;
import datadog.trace.api.Trace;
import java.io.IOException;
import java.io.OutputStream;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

  private final Boolean requestCache;
  @Autowired private CircuitBreakerService circuitBreakerService;
  @Autowired private QueryAdapter<GetRequest, SearchRequest> queryAdapter;
  @Autowired private ElasticSearchStream elasticSearch;
  @Autowired private RestHighLevelClient restHighLevelClient;

  public SearchService(@Value("${es.index.requests.cache.enable}") Boolean requestCache) {
    this.requestCache = requestCache;
  }

  @Trace
  public GetResponse getById(BaseApiRequest request, String id) {
    return circuitBreakerService.execute(GET_BY_ID, request.getIndex(), () -> doGet(request, id));
  }

  @Trace
  public SearchResponse search(SearchApiRequest request) {
    return circuitBreakerService.execute(SEARCH, request.getIndex(), () -> doSearch(request));
  }

  public void stream(FilterableApiRequest request, OutputStream stream) {
    // Default value for stream size: return all results
    if (request.getSize() == Integer.MAX_VALUE) request.setSize(0);

    // Default value for stream sort: no sorting
    if (request.getSort() == null) request.setDisableSort(true);

    elasticSearch.stream(request, stream);
  }

  private GetResponse doGet(BaseApiRequest baseApiRequest, String id) {
    final var request = this.queryAdapter.getById(baseApiRequest, id);
    try {
      return restHighLevelClient.get(request, DEFAULT);
    } catch (ElasticsearchException e) {
      throw new QueryPhaseExecutionException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private SearchResponse doSearch(SearchApiRequest searchRequest) {
    final var request = this.queryAdapter.query(searchRequest).requestCache(requestCache);
    try {
      final var response = restHighLevelClient.search(request, DEFAULT);
      if (response.getFailedShards() != 0)
        throw new QueryPhaseExecutionException(
            format("%d of %d shards failed", response.getFailedShards(), response.getTotalShards()),
            request.source().toString());
      if (response.isTimedOut()) throw new QueryTimeoutException(request.source().toString());
      return response;
    } catch (ElasticsearchException e) {
      throw new QueryPhaseExecutionException(
          of(request).map(r -> r.source().toString()).orElse("{}"), e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

package com.grupozap.search.api.service;

import static java.lang.String.format;
import static java.util.Optional.of;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
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
import org.springframework.stereotype.Component;

@Component
public class SearchService {

  private static final long FILTER_THRESHOLD = 5000000000L; // 5 seconds

  @Autowired private QueryAdapter<GetRequest, SearchRequest> queryAdapter;

  @Autowired private ElasticSearchStream elasticSearch;

  @Autowired private RestHighLevelClient restHighLevelClient;

  private final Boolean requestCache;

  public SearchService(@Value("${es.index.requests.cache.enable}") Boolean requestCache) {
    this.requestCache = requestCache;
  }

  @Trace
  public GetResponse getById(BaseApiRequest request, String index, String id) {
    try {
      return restHighLevelClient.get(this.queryAdapter.getById(request, index, id), DEFAULT);
    } catch (Exception e) {
      if (getRootCause(e) instanceof IllegalArgumentException)
        throw new IllegalArgumentException(e);
      if (e instanceof ElasticsearchException) throw new QueryPhaseExecutionException(e);
      throw new RuntimeException(e);
    }
  }

  @Trace
  public SearchResponse search(SearchApiRequest request) {
    var searchRequest = this.queryAdapter.query(request).requestCache(requestCache);
    try {
      var searchResponse = restHighLevelClient.search(searchRequest, DEFAULT);
      if (searchResponse.getFailedShards() != 0)
        throw new QueryPhaseExecutionException(
            format(
                "%d of %d shards failed",
                searchResponse.getFailedShards(), searchResponse.getTotalShards()),
            searchRequest.source().toString());
      if (searchResponse.isTimedOut())
        throw new QueryTimeoutException(searchRequest.source().toString());
      return searchResponse;

    } catch (ElasticsearchException e) {
      throw new QueryPhaseExecutionException(
          of(searchRequest).map(r -> r.source().toString()).orElse("{}"), e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void stream(FilterableApiRequest request, OutputStream stream) {
    // Default value for stream size: return all results
    if (request.getSize() == Integer.MAX_VALUE) request.setSize(0);

    // Default value for stream sort: no sorting
    if (request.getSort() == null) request.setDisableSort(true);

    elasticSearch.stream(request, stream);
  }
}

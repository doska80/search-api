package com.grupozap.search.api.service;

import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

import com.grupozap.search.api.adapter.QueryAdapter;
import com.grupozap.search.api.controller.stream.ElasticSearchStream;
import com.grupozap.search.api.exception.QueryPhaseExecutionException;
import com.grupozap.search.api.exception.QueryTimeoutException;
import com.grupozap.search.api.model.http.BaseApiRequest;
import com.grupozap.search.api.model.http.FilterableApiRequest;
import com.grupozap.search.api.model.http.SearchApiRequest;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import java.io.OutputStream;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SearchService {

  private static final long FILTER_THRESHOLD = 5000000000L; // 5 seconds

  @Autowired private QueryAdapter<GetRequest, SearchRequest> queryAdapter;

  @Autowired private ElasticSearchStream elasticSearch;

  @Autowired private RestHighLevelClient restHighLevelClient;

  @Trace
  public GetResponse getById(BaseApiRequest request, String id) {
    try {
      return restHighLevelClient.get(this.queryAdapter.getById(request, id));
    } catch (Exception e) {
      if (getRootCause(e) instanceof IllegalArgumentException)
        throw new IllegalArgumentException(e);
      if (e instanceof ElasticsearchException) throw new QueryPhaseExecutionException(e);
      throw new RuntimeException(e);
    }
  }

  @Trace
  public SearchResponse search(SearchApiRequest request) {
    return search(request, 1);
  }

  @Trace
  public SearchResponse search(SearchApiRequest request, final int retries) {
    SearchRequest searchRequest = this.queryAdapter.query(request);
    try {
      SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
      if (searchResponse.getFailedShards() != 0)
        throw new QueryPhaseExecutionException(
            format(
                "%d of %d shards failed",
                searchResponse.getFailedShards(), searchResponse.getTotalShards()),
            searchRequest.toString());
      if (searchResponse.isTimedOut()) throw new QueryTimeoutException(searchRequest.toString());
      return searchResponse;

    } catch (Exception e) {
      if (getRootCause(e) instanceof IllegalArgumentException)
        throw new IllegalArgumentException(e);
      if (e instanceof ElasticsearchException) {
        if (retries != 0) return search(request, retries - 1);
        throw new QueryPhaseExecutionException(
            ofNullable(searchRequest).map(SearchRequest::toString).orElse("{}"), e);
      }
      throw new RuntimeException(e);
    }
  }

  public void stream(FilterableApiRequest request, OutputStream stream) {
    final long startTime = nanoTime();

    // Default value for stream size: return all results
    if (request.getSize() == Integer.MAX_VALUE) request.setSize(0);

    // Default value for stream sort: no sorting
    if (request.getSort() == null) request.setDisableSort(true);

    elasticSearch.stream(request, stream);

    if ((nanoTime() - startTime) > FILTER_THRESHOLD) NewRelic.ignoreTransaction();
  }
}

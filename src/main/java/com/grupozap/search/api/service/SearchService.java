package com.grupozap.search.api.service;

import static com.grupozap.search.api.service.CircuitBreakerService.CircuitType.GET_BY_ID;
import static com.grupozap.search.api.service.CircuitBreakerService.CircuitType.SEARCH;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_BY_ID_ERRORS;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_BY_ID_FOUND;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_BY_ID_LATENCY;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_BY_ID_NOT_FOUND;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_BY_ID_REQUESTS;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_ERRORS;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_LATENCY;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_REQUESTS;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_WITHOUT_RESULTS;
import static com.grupozap.search.api.service.MetricsCollector.Metric.SEARCH_WITH_RESULTS;
import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
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
  @Autowired private MetricsCollector collector;

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
    final var start = nanoTime();
    final var index = baseApiRequest.getIndex();
    final var request = this.queryAdapter.getById(baseApiRequest, id);
    collector.record(SEARCH_BY_ID_REQUESTS, index);
    try {
      final var response = restHighLevelClient.get(request, DEFAULT);
      collector.record(response.isExists() ? SEARCH_BY_ID_FOUND : SEARCH_BY_ID_NOT_FOUND, index);
      return response;
    } catch (ElasticsearchException e) {
      collector.record(SEARCH_BY_ID_ERRORS, index);
      throw new QueryPhaseExecutionException(e);
    } catch (IOException e) {
      collector.record(SEARCH_BY_ID_ERRORS, index);
      throw new RuntimeException(e);
    } finally {
      collector.record(SEARCH_BY_ID_LATENCY, index, NANOSECONDS.toMillis(nanoTime() - start));
    }
  }

  private SearchResponse doSearch(SearchApiRequest searchRequest) {
    final var start = nanoTime();
    final var index = searchRequest.getIndex();
    final var request = this.queryAdapter.query(searchRequest).requestCache(requestCache);
    collector.record(SEARCH_REQUESTS, index);
    try {
      final var response = restHighLevelClient.search(request, DEFAULT);
      if (response.getFailedShards() != 0) {
        collector.record(SEARCH_ERRORS, index);
        throw new QueryPhaseExecutionException(
            format("%d of %d shards failed", response.getFailedShards(), response.getTotalShards()),
            request.source().toString());
      }
      if (response.isTimedOut()) {
        collector.record(SEARCH_ERRORS, index);
        throw new QueryTimeoutException(request.source().toString());
      }
      collector.record(
          response.getHits().getTotalHits().value > 0
              ? SEARCH_WITH_RESULTS
              : SEARCH_WITHOUT_RESULTS,
          index);
      return response;
    } catch (ElasticsearchException e) {
      collector.record(SEARCH_ERRORS, index);
      throw new QueryPhaseExecutionException(
          of(request).map(r -> r.source().toString()).orElse("{}"), e);
    } catch (IOException e) {
      collector.record(SEARCH_ERRORS, index);
      throw new RuntimeException(e);
    } finally {
      collector.record(SEARCH_LATENCY, index, NANOSECONDS.toMillis(nanoTime() - start));
    }
  }
}

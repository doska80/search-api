package com.vivareal.search.api.service;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_CONTROLLER_SEARCH_TIMEOUT;
import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.controller.stream.ElasticSearchStream;
import com.vivareal.search.api.exception.QueryPhaseExecutionException;
import com.vivareal.search.api.exception.QueryTimeoutException;
import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.FilterableApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import java.io.OutputStream;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SearchService {

  private static final long FILTER_THRESHOLD = 5000000000L; // 5 seconds

  @Autowired
  @Qualifier("ElasticsearchQuery")
  private QueryAdapter<GetRequestBuilder, SearchRequestBuilder> queryAdapter;

  @Autowired private ElasticSearchStream elasticSearch;

  @Trace
  public GetResponse getById(BaseApiRequest request, String id) {
    try {
      return this.queryAdapter
          .getById(request, id)
          .get((TimeValue) ES_CONTROLLER_SEARCH_TIMEOUT.getValue(request.getIndex()));
    } catch (Exception e) {
      if (getRootCause(e) instanceof IllegalArgumentException)
        throw new IllegalArgumentException(e);
      if (e instanceof ElasticsearchException) throw new QueryPhaseExecutionException(e);
      throw e;
    }
  }

  @Trace
  public SearchResponse search(SearchApiRequest request) {
    return search(request, 1);
  }

  @Trace
  public SearchResponse search(SearchApiRequest request, final int retries) {
    SearchRequestBuilder searchRequestBuilder = null;

    try {

      searchRequestBuilder = this.queryAdapter.query(request);
      SearchResponse searchResponse =
          searchRequestBuilder.get(
              (TimeValue) ES_CONTROLLER_SEARCH_TIMEOUT.getValue(request.getIndex()));

      if (searchResponse.getFailedShards() != 0)
        throw new QueryPhaseExecutionException(
            format(
                "%d of %d shards failed",
                searchResponse.getFailedShards(), searchResponse.getTotalShards()),
            searchRequestBuilder.toString());

      if (searchResponse.isTimedOut())
        throw new QueryTimeoutException(searchRequestBuilder.toString());

      return searchResponse;
    } catch (Exception e) {
      if (getRootCause(e) instanceof IllegalArgumentException)
        throw new IllegalArgumentException(e);
      if (e instanceof ElasticsearchException) {
        if (retries != 0) return search(request, retries - 1);
        throw new QueryPhaseExecutionException(
            ofNullable(searchRequestBuilder).map(SearchRequestBuilder::toString).orElse("{}"), e);
      }
      throw e;
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

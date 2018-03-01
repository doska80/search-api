package com.vivareal.search.api.model;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.vivareal.search.api.exception.FailedShardsException;
import com.vivareal.search.api.exception.QueryTimeoutException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.search.Scroll;

public class SearchApiIterator<T> implements Iterator<T[]> {

  private TransportClient client;
  private SearchResponse response;
  private final Scroll scroll;
  private final int size;
  private int count;
  private final long requestStreamTimeout;

  public SearchApiIterator(
      TransportClient client,
      SearchResponse response,
      final Scroll scroll,
      final int size,
      final long requestStreamTimeout) {

    if ((this.response = response) == null)
      throw new IllegalArgumentException("response can not be null");

    if (response.getFailedShards() > 0)
      throw new FailedShardsException(response.getFailedShards(), response.getTotalShards());

    if (response.isTimedOut()) throw new QueryTimeoutException();

    if ((this.client = client) == null)
      throw new IllegalArgumentException("client can not be null");

    this.scroll = scroll;
    this.size = size;
    this.count = hits();
    this.requestStreamTimeout = requestStreamTimeout;
  }

  @Override
  public boolean hasNext() {
    return hits() > 0 && count <= size;
  }

  @Override
  public T[] next() {

    @SuppressWarnings("unchecked")
    T[] result = (T[]) response.getHits().getHits();

    try {
      SearchScrollRequest scrollRequest = new SearchScrollRequest(response.getScrollId());
      scrollRequest.scroll(scroll);

      response = client.searchScroll(scrollRequest).get(requestStreamTimeout, MILLISECONDS);

      if (response.getFailedShards() > 0)
        throw new FailedShardsException(response.getFailedShards(), response.getTotalShards());

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException("Error to Iterate stream", e);
    }

    this.count += hits();
    return result;
  }

  private int hits() {
    return response.getHits().getHits().length;
  }

  public int getCount() {
    return count;
  }
}

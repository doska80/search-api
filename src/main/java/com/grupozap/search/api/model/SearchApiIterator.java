package com.grupozap.search.api.model;

import static org.elasticsearch.client.RequestOptions.DEFAULT;

import com.grupozap.search.api.exception.FailedShardsException;
import com.grupozap.search.api.exception.QueryTimeoutException;
import java.io.IOException;
import java.util.Iterator;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.Scroll;

public class SearchApiIterator<T> implements Iterator<T[]> {

  private RestHighLevelClient client;
  private SearchResponse response;
  private final Scroll scroll;
  private final int size;
  private int count;

  public SearchApiIterator(
      RestHighLevelClient client, SearchResponse response, final Scroll scroll, final int size) {

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

      response = client.scroll(scrollRequest, DEFAULT);

      if (response.getFailedShards() > 0)
        throw new FailedShardsException(response.getFailedShards(), response.getTotalShards());

    } catch (IOException e) {
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

package com.vivareal.search.api.controller.stream;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static com.vivareal.search.api.controller.stream.ResponseStream.iterate;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.elasticsearch.common.bytes.BytesReference.toBytes;
import static org.elasticsearch.common.unit.TimeValue.timeValueMillis;
import static org.slf4j.LoggerFactory.getLogger;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.model.SearchApiIterator;
import com.vivareal.search.api.model.http.FilterableApiRequest;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticSearchStream {

  private static final Logger LOG = getLogger(ElasticSearchStream.class);

  @Autowired private TransportClient client;

  @Autowired private QueryAdapter<?, SearchRequestBuilder> queryAdapter;

  public void stream(FilterableApiRequest request, OutputStream stream) {
    String index = request.getIndex();
    final long requestStreamTimeout = ES_CONTROLLER_STREAM_TIMEOUT.getValue(index);

    final Scroll scroll = new Scroll(timeValueMillis(ES_SCROLL_TIMEOUT.getValue(index)));
    SearchRequest searchRequest = new SearchRequest(index);
    searchRequest.scroll(scroll);

    SearchRequestBuilder requestBuilder =
        this.queryAdapter.query(request).setSize(ES_STREAM_SIZE.getValue(index));

    int size = MAX_VALUE;
    if (request.getSize() != MAX_VALUE && request.getSize() != 0) {
      size = request.getSize();
      requestBuilder.setSize(min(request.getSize(), ES_STREAM_SIZE.getValue(index)));
      requestBuilder.setTerminateAfter(size);
    }

    searchRequest.source(requestBuilder.request().source());

    try {
      SearchResponse response =
          client.search(searchRequest).get(requestStreamTimeout, MILLISECONDS);

      SearchApiIterator<SearchHit> searchApiIterator =
          new SearchApiIterator<>(client, response, scroll, size, requestStreamTimeout);

      iterate(stream, searchApiIterator, (SearchHit sh) -> toBytes(sh.getSourceRef()));

      LOG.info(
          "Stream - Total hits {} - Total sent {}",
          response.getHits().totalHits,
          searchApiIterator.getCount());

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(
          format("Error to get stream from Request: %s", request.toString()), e);
    }
  }
}

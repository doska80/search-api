package com.vivareal.search.api.controller.stream;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static com.vivareal.search.api.controller.stream.ResponseStream.iterate;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.min;
import static java.lang.String.format;
import static org.elasticsearch.common.bytes.BytesReference.toBytes;
import static org.elasticsearch.common.unit.TimeValue.timeValueMillis;
import static org.slf4j.LoggerFactory.getLogger;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.model.SearchApiIterator;
import com.vivareal.search.api.model.http.FilterableApiRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticSearchStream {

  private static final Logger LOG = getLogger(ElasticSearchStream.class);

  @Autowired private RestHighLevelClient client;

  @Autowired private QueryAdapter<?, SearchRequest> queryAdapter;

  public void stream(FilterableApiRequest request, OutputStream stream) {
    String index = request.getIndex();

    final Scroll scroll = new Scroll(timeValueMillis(ES_SCROLL_KEEP_ALIVE.getValue(index)));
    SearchRequest searchRequest = new SearchRequest(index);
    searchRequest.scroll(scroll);

    SearchSourceBuilder searchSourceBuilder =
        this.queryAdapter
            .query(request)
            .source()
            .size(ES_STREAM_SIZE.getValue(index))
            .timeout(
                new TimeValue(
                    ES_CONTROLLER_STREAM_TIMEOUT.getValue(index),
                    TimeUnit.valueOf(ES_QUERY_TIMEOUT_UNIT.getValue(request.getIndex()))));

    int size = MAX_VALUE;
    if (request.getSize() != MAX_VALUE && request.getSize() != 0) {
      size = request.getSize();
      searchSourceBuilder.size(min(request.getSize(), ES_STREAM_SIZE.getValue(index)));
      searchSourceBuilder.terminateAfter(size);
    }

    searchRequest.source(searchSourceBuilder);

    try {
      SearchResponse response = client.search(searchRequest);

      SearchApiIterator<SearchHit> searchApiIterator =
          new SearchApiIterator<>(client, response, scroll, size);

      iterate(stream, searchApiIterator, (SearchHit sh) -> toBytes(sh.getSourceRef()));

      LOG.info(
          "Stream - Total hits {} - Total sent {}",
          response.getHits().totalHits,
          searchApiIterator.getCount());

    } catch (IOException e) {
      throw new RuntimeException(
          format("Error to get stream from Request: %s", request.toString()), e);
    }
  }
}

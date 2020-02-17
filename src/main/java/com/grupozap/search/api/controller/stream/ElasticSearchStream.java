package com.grupozap.search.api.controller.stream;

import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_CONTROLLER_STREAM_TIMEOUT;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_QUERY_TIMEOUT_UNIT;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_SCROLL_KEEP_ALIVE;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_STREAM_SIZE;
import static com.grupozap.search.api.controller.stream.ResponseStream.iterate;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.min;
import static java.lang.String.format;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.bytes.BytesReference.toBytes;
import static org.elasticsearch.common.unit.TimeValue.timeValueMillis;
import static org.slf4j.LoggerFactory.getLogger;

import com.grupozap.search.api.adapter.QueryAdapter;
import com.grupozap.search.api.model.SearchApiIterator;
import com.grupozap.search.api.model.http.FilterableApiRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ElasticSearchStream {

  private static final Logger LOG = getLogger(ElasticSearchStream.class);

  @Autowired
  @Qualifier("restHighLevelClientForStream")
  private RestHighLevelClient client;

  @Autowired private QueryAdapter<?, SearchRequest> queryAdapter;

  public void stream(FilterableApiRequest request, OutputStream stream) {
    var index = request.getIndex();

    final var scroll = new Scroll(timeValueMillis(ES_SCROLL_KEEP_ALIVE.getValue(index)));
    var searchRequest = new SearchRequest(index);
    searchRequest.scroll(scroll);

    var searchSourceBuilder =
        this.queryAdapter
            .query(request)
            .source()
            .size(ES_STREAM_SIZE.getValue(index))
            .timeout(
                new TimeValue(
                    ES_CONTROLLER_STREAM_TIMEOUT.getValue(index),
                    TimeUnit.valueOf(ES_QUERY_TIMEOUT_UNIT.getValue(request.getIndex()))));

    var size = MAX_VALUE;
    if (request.getSize() != MAX_VALUE && request.getSize() != 0) {
      size = request.getSize();
      searchSourceBuilder.size(min(request.getSize(), ES_STREAM_SIZE.getValue(index)));
      searchSourceBuilder.terminateAfter(size);
    }

    searchRequest.source(searchSourceBuilder);

    try {
      var response = client.search(searchRequest, DEFAULT);

      var searchApiIterator = new SearchApiIterator<SearchHit>(client, response, scroll, size);

      iterate(stream, searchApiIterator, (SearchHit sh) -> toBytes(sh.getSourceRef()));

      LOG.info(
          "Stream - Total hits {} - Total sent {}",
          response.getHits().getTotalHits().value,
          searchApiIterator.getCount());

    } catch (IOException e) {
      throw new RuntimeException(
          format("Error to get stream from Request: %s", request.toString()), e);
    }
  }
}

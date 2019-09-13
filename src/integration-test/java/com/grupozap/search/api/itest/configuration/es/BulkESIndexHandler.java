package com.grupozap.search.api.itest.configuration.es;

import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy.WAIT_UNTIL;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.xcontent.XContentType.JSON;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BulkESIndexHandler {

  private static final Logger LOG = LoggerFactory.getLogger(BulkESIndexHandler.class);

  private final RestHighLevelClient restHighLevelClient;

  private static final Gson gson = new Gson();

  @Autowired
  public BulkESIndexHandler(RestHighLevelClient restHighLevelClient) {
    this.restHighLevelClient = restHighLevelClient;
  }

  public void bulkInsert(final String index, List<Map> sources) {
    var request = new BulkRequest();
    request.timeout(new TimeValue(30, SECONDS));
    request.setRefreshPolicy(WAIT_UNTIL);
    sources.forEach(
        source ->
            request.add(
                new IndexRequest(index)
                    .id(valueOf(source.get("id")))
                    .source(gson.toJson(source), JSON)));
    try {
      var response = restHighLevelClient.bulk(request, DEFAULT);
      if (response.hasFailures()) {
        response
            .iterator()
            .forEachRemaining(
                bulkItemResponse -> {
                  if (bulkItemResponse.isFailed()) {
                    var failure = bulkItemResponse.getFailure();
                    LOG.info(
                        "Document {} not indexed! Message: {}",
                        bulkItemResponse.getId(),
                        failure.getMessage());
                  }
                });
      } else {
        LOG.info("{} documents indexed with success!", sources.size());
      }
    } catch (IOException e) {
      LOG.error("Error to process bulk insert!", e);
    }
  }
}

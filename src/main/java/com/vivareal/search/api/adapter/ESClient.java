package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.http.BaseApiRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ESClient {

  private final TransportClient transportClient;

  @Autowired
  public ESClient(TransportClient transportClient) {
    this.transportClient = transportClient;
  }

  public GetIndexResponse getIndexResponse() {
    return transportClient.admin().indices().prepareGetIndex().get();
  }

  public GetRequestBuilder prepareGet(BaseApiRequest request, String id) {
    return transportClient.prepareGet(request.getIndex(), request.getIndex(), id);
  }

  public SearchRequestBuilder prepareSearch(BaseApiRequest request) {
    return transportClient.prepareSearch(request.getIndex());
  }
}

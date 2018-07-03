package com.vivareal.search.api.adapter;

import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
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
}

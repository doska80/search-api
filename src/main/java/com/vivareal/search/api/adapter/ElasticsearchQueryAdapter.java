package com.vivareal.search.api.adapter;


import com.vivareal.search.api.model.SearchApiRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;

public class ElasticsearchQueryAdapter {

    private final TransportClient transportClient;

    public ElasticsearchQueryAdapter(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    public Object getQuery(String type, SearchApiRequest searchApiRequest) {
        SearchRequestBuilder searchBuilder = this.transportClient.prepareSearch(type);
        return null;
    }


//    public

}

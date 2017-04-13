package com.vivareal.search.api.adapter;


import com.vivareal.search.api.model.SearchApiRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;

public class ElasticsearchQueryAdapter extends AbstractQueryAdapter {

    private final TransportClient transportClient;

    public ElasticsearchQueryAdapter(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    public Object getQuery(SearchApiRequest searchApiRequest, String... indices) {
        SearchRequestBuilder builder = this.transportClient.prepareSearch(indices);
        return builder;
    }


//    public

}

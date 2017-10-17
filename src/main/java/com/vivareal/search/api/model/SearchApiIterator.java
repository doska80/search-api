package com.vivareal.search.api.model;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;

import java.util.Iterator;
import java.util.function.Function;

public class SearchApiIterator<T> implements Iterator<T[]> {
    private TransportClient client;

    private SearchResponse response;

    private Function<SearchScrollRequestBuilder, SearchResponse> loop;

    private final Integer size;

    private int count;

    public SearchApiIterator(TransportClient client, SearchResponse response, Function<SearchScrollRequestBuilder, SearchResponse> loop, Integer size) {
        if (response == null) throw new IllegalArgumentException("response can not be null");
        this.response = response;

        if (client == null) throw new IllegalArgumentException("client can not be null");
        this.client = client;

        if (loop == null) throw new IllegalArgumentException("loop can not be null");
        this.loop = loop;

        this.size = size;
        this.count = hits();
    }

    @Override
    public boolean hasNext() {
        return hits() > 0 && count <= size;
    }

    @Override
    public T[] next() {
        T[] result = (T[]) response.getHits().getHits();

        response = loop.apply(client.prepareSearchScroll(response.getScrollId()));

        this.count += hits();

        return result;
    }

    private int hits() {
        return response.getHits().getHits().length;
    }
}

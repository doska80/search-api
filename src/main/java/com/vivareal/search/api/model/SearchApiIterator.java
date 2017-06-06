package com.vivareal.search.api.model;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;

import java.util.Iterator;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class SearchApiIterator<T> implements Iterator<T[]> {
    private TransportClient client;

    private SearchResponse response;

    private Function<SearchScrollRequestBuilder, SearchResponse> loop;

    public SearchApiIterator(TransportClient client, SearchResponse response, Function<SearchScrollRequestBuilder, SearchResponse> loop) {
        if (response == null) throw new IllegalArgumentException("response can not be null");
        this.response = response;

        if (client == null) throw new IllegalArgumentException("client can not be null");
        this.client = client;

        if (loop == null) throw new IllegalArgumentException("loop can not be null");
        this.loop = loop;
    }

    @Override
    public boolean hasNext() {
        return ofNullable(response.getHits())
                .map(e -> e.getHits().length)
                .orElse(0) > 0;
    }

    @Override
    public T[] next() {
        T[] result = (T[]) ofNullable(response.getHits())
                .flatMap(e -> ofNullable(e.getHits()))
                .orElse(null);

        response = loop.apply(client.prepareSearchScroll(response.getScrollId()));

        return result;
    }
}

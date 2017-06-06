package com.vivareal.search.api.model;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;

import java.util.Iterator;

import static java.util.Optional.ofNullable;

public class SearchApiIterator<T> implements Iterator<T[]> {
    private TransportClient client;

    private SearchResponse response;

    public SearchApiIterator(TransportClient client, SearchResponse response) {
        if (response == null) throw new IllegalArgumentException("response can not be null");
        this.response = response;

        if (client == null) throw new IllegalArgumentException("client can not be null");
        this.client = client;
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

        response = client.prepareSearchScroll(response.getScrollId())
                .setScroll(new TimeValue(60000)) // TODO we must configure timeouts
                .execute()
                .actionGet();

        return result;
    }
}

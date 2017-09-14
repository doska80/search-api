package com.vivareal.search.api.model.serializer;

public class SearchResponseEnvelope<T> {

    private final String indexName;
    private final T searchResponse;

    public SearchResponseEnvelope(String indexName, T searchResponse) {
        this.indexName = indexName;
        this.searchResponse = searchResponse;
    }

    public String getIndexName() {
        return indexName;
    }

    public T getSearchResponse() {
        return searchResponse;
    }
}

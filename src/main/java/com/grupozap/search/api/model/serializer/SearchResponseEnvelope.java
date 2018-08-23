package com.grupozap.search.api.model.serializer;

import static org.apache.commons.lang3.Validate.notNull;

public class SearchResponseEnvelope<T> {

  private final String indexName;
  private final T searchResponse;

  public SearchResponseEnvelope(String indexName, T searchResponse) {
    notNull(indexName, "The object indexName must not be null");
    notNull(searchResponse, "The object searchResponse must not be null");

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

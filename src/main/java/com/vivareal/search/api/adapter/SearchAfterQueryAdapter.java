package com.vivareal.search.api.adapter;

import static java.util.stream.Stream.of;

import com.vivareal.search.api.model.http.FilterableApiRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.springframework.stereotype.Component;

@Component
public class SearchAfterQueryAdapter {

  public static final String SORT_SEPARATOR = "_";

  void apply(SearchRequestBuilder searchBuilder, FilterableApiRequest request) {
    if (request.getCursorId() != null) {
      searchBuilder.setFrom(0);
      searchBuilder.searchAfter(
          of(request.getCursorId().split(SORT_SEPARATOR))
              .map(str -> str.replaceAll("%5f", "_"))
              .toArray());
    }
  }
}

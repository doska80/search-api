package com.vivareal.search.api.adapter;

import static java.util.stream.Stream.of;

import com.vivareal.search.api.model.http.FilterableApiRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;

@Component
public class SearchAfterQueryAdapter {

  public static final String SORT_SEPARATOR = "_";

  void apply(SearchSourceBuilder searchBuilder, FilterableApiRequest request) {
    if (request.getCursorId() == null) return;

    searchBuilder.from(0);
    searchBuilder.searchAfter(
        of(request.getCursorId().split(SORT_SEPARATOR))
            .map(str -> str.replaceAll("%5f", "_"))
            .toArray());
  }
}

package com.vivareal.search.api.adapter;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SIZE;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_MAX_SIZE;

import com.vivareal.search.api.model.http.FilterableApiRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.springframework.stereotype.Component;

@Component
public class PageQueryAdapter {

  public PageQueryAdapter() {}

  public void apply(SearchRequestBuilder searchBuilder, FilterableApiRequest request) {
    String index = request.getIndex();
    request.setPaginationValues(ES_DEFAULT_SIZE.getValue(index), ES_MAX_SIZE.getValue(index));
    searchBuilder.setFrom(request.getFrom());
    searchBuilder.setSize(request.getSize());
  }
}

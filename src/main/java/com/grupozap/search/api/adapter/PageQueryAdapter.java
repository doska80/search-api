package com.grupozap.search.api.adapter;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SIZE;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_MAX_SIZE;

import com.grupozap.search.api.configuration.environment.RemoteProperties;
import com.grupozap.search.api.model.http.FilterableApiRequest;
import com.vivareal.search.api.model.http.FilterableApiRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;

@Component
public class PageQueryAdapter {

  public PageQueryAdapter() {}

  public void apply(SearchSourceBuilder searchBuilder, FilterableApiRequest request) {
    String index = request.getIndex();
    request.setPaginationValues(
        RemoteProperties.ES_DEFAULT_SIZE.getValue(index), RemoteProperties.ES_MAX_SIZE.getValue(index));
    searchBuilder.from(request.getFrom());
    searchBuilder.size(request.getSize());
  }
}

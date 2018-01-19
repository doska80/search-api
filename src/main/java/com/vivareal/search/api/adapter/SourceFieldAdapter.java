package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.search.Fetchable;
import java.util.Map;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SourceFieldAdapter {

  private final SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;

  @Autowired
  public SourceFieldAdapter(
      @Qualifier("elasticsearchSettings")
          SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter) {
    this.settingsAdapter = settingsAdapter;
  }

  public void apply(SearchRequestBuilder searchRequestBuilder, final Fetchable request) {
    String[] includeFields = settingsAdapter.getFetchSourceIncludeFields(request);
    searchRequestBuilder.setFetchSource(
        includeFields, settingsAdapter.getFetchSourceExcludeFields(request, includeFields));
  }

  public void apply(GetRequestBuilder getRequestBuilder, final Fetchable request) {
    String[] includeFields = settingsAdapter.getFetchSourceIncludeFields(request);
    getRequestBuilder.setFetchSource(
        includeFields, settingsAdapter.getFetchSourceExcludeFields(request, includeFields));
  }
}

package com.grupozap.search.api.service.parser;

import static com.grupozap.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.DEFAULT_INDEX;
import static java.lang.Integer.parseInt;

import com.grupozap.search.api.adapter.SettingsAdapter;
import com.grupozap.search.api.model.search.Indexable;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope
@Component
public class IndexSettings {

  private final SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;
  private String index;

  @Autowired
  public IndexSettings(SettingsAdapter settingsAdapter) {
    this.settingsAdapter = settingsAdapter;
  }

  public String getIndex() {
    return index;
  }

  public void validateIndex(Indexable indexable) {
    if (!DEFAULT_INDEX.equals(indexable.getIndex())) {
      settingsAdapter.checkIndex(indexable);
    }
    this.index = indexable.getIndex();
  }

  public String getIndexByAlias() {
    return settingsAdapter.getIndexByAlias(index);
  }

  public int getShards() {
    return parseInt(settingsAdapter.settingsByKey(index, SHARDS));
  }
}

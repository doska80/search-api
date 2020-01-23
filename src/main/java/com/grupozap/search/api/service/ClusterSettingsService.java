package com.grupozap.search.api.service;

import com.grupozap.search.api.adapter.SettingsAdapter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ClusterSettingsService {

  @Autowired
  @Qualifier("elasticsearchSettings")
  private SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;

  public Map<String, Map<String, Object>> settings() {
    return settingsAdapter.settings();
  }
}

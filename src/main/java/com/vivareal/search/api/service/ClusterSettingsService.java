package com.vivareal.search.api.service;

import com.vivareal.search.api.adapter.SettingsAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ClusterSettingsService {

    @Autowired
    @Qualifier("elasticsearchSettings")
    private SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;

    public Map<String, Map<String, Object>> settings() {
        return settingsAdapter.settings();
    }
}

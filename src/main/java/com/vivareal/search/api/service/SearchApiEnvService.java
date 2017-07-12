package com.vivareal.search.api.service;

import com.vivareal.search.api.configuration.SearchApiEnv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SearchApiEnvService {

    @Autowired
    private SearchApiEnv searchApiEnv;

    public Map<String, Object> getLocalProperties() {
        return searchApiEnv.getLocalProperties();
    }

    public Map<String, Object> getRemoteProperties() {
        return searchApiEnv.getRemoteProperties();
    }
}

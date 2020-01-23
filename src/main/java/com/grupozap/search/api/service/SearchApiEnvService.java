package com.grupozap.search.api.service;

import com.grupozap.search.api.configuration.environment.SearchApiEnv;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SearchApiEnvService {

  @Autowired private SearchApiEnv searchApiEnv;

  public Map<String, Object> getLocalProperties() {
    return searchApiEnv.getLocalProperties();
  }

  public Map<String, Object> getRemoteProperties() {
    return searchApiEnv.getRemoteProperties();
  }
}

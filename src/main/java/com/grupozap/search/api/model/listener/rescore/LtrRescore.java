package com.grupozap.search.api.model.listener.rescore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.grupozap.search.api.query.LtrQueryBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.index.query.QueryBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(SnakeCaseStrategy.class)
public class LtrRescore extends SortRescore {

  private String model;
  private Map<String, Object> params = new HashMap<>();
  private List<String> activeFeatures = new ArrayList<>();

  public String getModel() {
    return model;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public List<String> getActiveFeatures() {
    return activeFeatures;
  }

  @Override
  public QueryBuilder getQueryBuilder() {
    return new LtrQueryBuilder.Builder(this.model)
        .params(this.params)
        .activeFeatures(this.activeFeatures)
        .build();
  }
}

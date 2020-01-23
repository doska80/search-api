package com.grupozap.search.api.listener;

import static com.google.common.collect.Maps.newHashMap;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_SORT_RESCORE;
import static com.grupozap.search.api.listener.SortRescoreListener.RescoreType.fromString;
import static com.grupozap.search.api.utils.MapperUtils.convertValue;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.grupozap.search.api.query.LtrQueryBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component("sortRescoreListener")
public class SortRescoreListener implements ApplicationListener<RemotePropertiesUpdatedEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(SortRescoreListener.class);

  private final Map<String, Map<String, SortRescore>> rescorerOrders;

  public SortRescoreListener() {
    this.rescorerOrders = new ConcurrentHashMap<>();
  }

  public Map<String, SortRescore> getRescorerOrders(String index) {
    return this.rescorerOrders.getOrDefault(index, newHashMap());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onApplicationEvent(RemotePropertiesUpdatedEvent event) {
    Map<String, Map> rescore = ES_SORT_RESCORE.getValue(event.getIndex());
    if (nonNull(rescore)) {
      Map<String, SortRescore> sortRescoreByType =
          rescore.keySet().stream()
              .collect(
                  toMap(
                      identity(),
                      key ->
                          convertValue(
                              rescore.get(key),
                              fromString((String) rescore.get(key).get("rescore_type"))
                                  .getRescoreClass())));
      rescorerOrders.put(event.getIndex(), sortRescoreByType);
      LOG.info("Refreshing es.sort.rescore. {}", this.rescorerOrders.toString());
    } else {
      this.rescorerOrders.remove(event.getIndex());
    }
  }

  public enum RescoreType {
    LTR_RESCORE("ltr_rescore", LtrRescore.class),
    RANDOM_RESCORE("random_rescore", RandomRescore.class);

    private final String alias;
    private final Class<? extends SortRescore> rescoreClass;

    RescoreType(String alias, Class<? extends SortRescore> rescoreClass) {
      this.alias = alias;
      this.rescoreClass = rescoreClass;
    }

    public static RescoreType fromString(String alias) {
      return Arrays.stream(RescoreType.values())
          .filter(r -> r.alias.equalsIgnoreCase(alias))
          .findFirst()
          .orElseThrow();
    }

    public String getAlias() {
      return alias;
    }

    public Class<? extends SortRescore> getRescoreClass() {
      return rescoreClass;
    }
  }

  public abstract static class SortRescore {

    private int windowSize = 500;
    private float queryWeight = 1.0f;
    private float rescoreQueryWeight = 1.0f;
    private String scoreMode = "total";

    public int getWindowSize() {
      return windowSize;
    }

    public float getQueryWeight() {
      return queryWeight;
    }

    public float getRescoreQueryWeight() {
      return rescoreQueryWeight;
    }

    public String getScoreMode() {
      return scoreMode;
    }

    public abstract QueryBuilder getQueryBuilder();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(SnakeCaseStrategy.class)
  public static class RandomRescore extends SortRescore {

    private int seed;
    private String field = "_seq_no";

    public int getSeed() {
      return seed;
    }

    public String getField() {
      return field;
    }

    @Override
    public QueryBuilder getQueryBuilder() {
      return new FunctionScoreQueryBuilder(
          new RandomScoreFunctionBuilder().seed(this.seed).setField(this.field));
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(SnakeCaseStrategy.class)
  public static class LtrRescore extends SortRescore {

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
}

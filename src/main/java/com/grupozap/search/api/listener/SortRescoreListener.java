package com.grupozap.search.api.listener;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.grupozap.search.api.configuration.environment.RemoteProperties.ES_SORT_RESCORE;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.Objects.nonNull;

import com.grupozap.search.api.model.event.RemotePropertiesUpdatedEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
  public void onApplicationEvent(RemotePropertiesUpdatedEvent event) {
    Map<String, Map> rescore = ES_SORT_RESCORE.getValue(event.getIndex());
    if (nonNull(rescore)) {
      this.rescorerOrders.put(event.getIndex(), SortRescore.build(rescore));
      LOG.info("Refreshing es.sort.rescore. {}", this.rescorerOrders.toString());
    } else {
      this.rescorerOrders.remove(event.getIndex());
    }
  }

  public static final class SortRescore {
    private final String model;

    public static final int DEFAULT_WINDOW_SIZE = 500;
    public static final float DEFAULT_QUERY_WEIGHT = 1.0f;
    public static final float DEFAULT_RESCORE_QUERY_WEIGHT = 1.0f;
    public static final String DEFAULT_SCORE_MODE = "total";
    public static final Map<String, Object> DEFAULT_PARAMS = newHashMap();
    public static final List<String> DEFAULT_ACTIVE_FEATURES = newArrayList();

    private final int windowSize;
    private final float queryWeight;
    private final float rescoreQueryWeight;
    private final String scoreMode;
    private final Map<String, Object> params;
    private final List<String> activeFeatures;

    @SuppressWarnings("unchecked")
    public static Map<String, SortRescore> build(Map<String, Map> rescoreProperties) {
      Map<String, SortRescore> rescorerOrders = new HashMap<>(rescoreProperties.size());
      rescoreProperties.forEach(
          (rescorePropertiesKey, rescorePropertiesValues) -> {
            var rescorerOrder =
                new SortRescore(
                    valueOf(rescorePropertiesValues.get("model")),
                    rescorePropertiesValues.get("window_size") != null
                        ? parseInt(valueOf(rescorePropertiesValues.get("window_size")))
                        : DEFAULT_WINDOW_SIZE,
                    rescorePropertiesValues.get("query_weight") != null
                        ? parseFloat(valueOf(rescorePropertiesValues.get("query_weight")))
                        : DEFAULT_QUERY_WEIGHT,
                    rescorePropertiesValues.get("rescore_query_weight") != null
                        ? parseFloat(valueOf(rescorePropertiesValues.get("rescore_query_weight")))
                        : DEFAULT_RESCORE_QUERY_WEIGHT,
                    rescorePropertiesValues.get("score_mode") != null
                        ? valueOf(rescorePropertiesValues.get("score_mode"))
                        : DEFAULT_SCORE_MODE,
                    rescorePropertiesValues.get("params") != null
                        ? (Map<String, Object>) rescorePropertiesValues.get("params")
                        : DEFAULT_PARAMS,
                    rescorePropertiesValues.get("active_features") != null
                        ? (List<String>) rescorePropertiesValues.get("active_features")
                        : DEFAULT_ACTIVE_FEATURES);
            rescorerOrders.put(rescorePropertiesKey, rescorerOrder);
          });
      return rescorerOrders;
    }

    SortRescore(
        String model,
        int windowSize,
        float queryWeight,
        float rescoreQueryWeight,
        String scoreMode,
        Map<String, Object> params,
        List<String> activeFeatures) {
      this.model = model;
      this.windowSize = windowSize;
      this.queryWeight = queryWeight;
      this.rescoreQueryWeight = rescoreQueryWeight;
      this.scoreMode = scoreMode;
      this.params = params;
      this.activeFeatures = activeFeatures;
    }

    public String getModel() {
      return model;
    }

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

    public Map<String, Object> getParams() {
      return params;
    }

    public List<String> getActiveFeatures() {
      return activeFeatures;
    }
  }
}

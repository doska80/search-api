package com.grupozap.search.api.model.listener.rescore;

import static com.grupozap.search.api.model.listener.rescore.RescoreType.fromString;
import static com.grupozap.search.api.utils.MapperUtils.convertValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.elasticsearch.index.query.QueryBuilder;

public abstract class SortRescore {

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

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static List<SortRescore> build(Map<String, Map<String, Object>> entryValue) {
    var rescores = new ArrayList<SortRescore>();
    if (entryValue.containsKey("rescores")) {
      ((List) entryValue.get("rescores"))
          .forEach(
              rescore ->
                  rescores.add(
                      convertValue(
                          rescore,
                          fromString((String) ((Map) rescore).get("rescore_type"))
                              .getRescoreClass())));
    }
    return rescores;
  }
}

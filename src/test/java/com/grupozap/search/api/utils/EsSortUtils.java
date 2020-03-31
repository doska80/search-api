package com.grupozap.search.api.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EsSortUtils {

  public static final boolean DISABLED_SORT = false;
  public static final String DEFAULT_SORT = "rescore_default";

  private static final Map<String, Object> RFQ =
      Map.of(
          "field",
          "field1",
          "function",
          "log",
          "boost",
          1.0,
          "scaling_factor",
          (float) 4.0,
          "pivot",
          0.0,
          "exponent",
          0.0);

  private static final Map<String, Object> SCRIPT_SORT =
      Map.of(
          "id", "index_scriptid",
          "scriptType", "stored",
          "scriptSortType", "number",
          "lang", "painless",
          "params", emptyMap());

  private static final Map<String, Object> RESCORE_LTR =
      Map.of(
          "rescore_type",
          "ltr_rescore",
          "window_size",
          500,
          "score_mode",
          "total",
          "rescore_query_weight",
          1,
          "model",
          "model_v2",
          "active_features",
          emptyList(),
          "query_weight",
          1);

  private static final Map<String, Object> RESCORE_RANDOM =
      Map.of(
          "rescore_type", "random_rescore",
          "window_size", 1000,
          "score_mode", "total",
          "field", "_seq_no",
          "seed", 1,
          "rescore_query_weight", 1,
          "query_weight", 1);

  private static final Map<String, Object> RESCORE_FUNCTION =
      Map.of(
          "rescore_type", "function_score",
          "window_size", 1000,
          "rescore_query_weight", 1,
          "weight", 1,
          "query_weight", 1,
          "script", Map.of("source", "doc['default'].value"));

  public Map<String, Object> buildEsSort(String sortName, Map<String, Object> sort) {
    var map = new LinkedHashMap<String, Object>();
    map.put("disabled", DISABLED_SORT);
    map.put("default_sort", DEFAULT_SORT);
    map.put("sorts", List.of(Map.of(sortName, sort)));
    return map;
  }

  public Map<String, Object> buildSortTypeWithCustomProperties(
      Map<String, Object> customProperties, SortType sortType) {
    var properties = new LinkedHashMap<String, Object>();
    properties.putAll(sortType.getSortType());
    properties.putAll(customProperties);
    return properties;
  }

  public enum SortType {
    RQF_TYPE(RFQ),
    SCRIPT_SORT_TYPE(SCRIPT_SORT),
    LTR_RESCORE_TYPE(RESCORE_LTR),
    RANDOM_RESCORE_TYPE(RESCORE_RANDOM),
    FUNCTION_RESCORE_TYPE(RESCORE_FUNCTION);

    private Map<String, Object> sortType;

    SortType(Map<String, Object> sortType) {
      this.sortType = sortType;
    }

    public Map<String, Object> getSortType() {
      return sortType;
    }
  }
}

package com.grupozap.search.api.model.listener.rfq;

import static java.lang.Float.parseFloat;
import static java.lang.String.valueOf;
import static org.apache.commons.collections4.MapUtils.isEmpty;

import java.util.Map;

public class Rfq {

  private String field;
  private String function;
  private float boost;
  private float scalingFactor;
  private float pivot;
  private float exponent;

  private Rfq() {}

  public String getField() {
    return field;
  }

  public String getFunction() {
    return function;
  }

  public float getBoost() {
    return boost;
  }

  public float getScalingFactor() {
    return scalingFactor;
  }

  public float getPivot() {
    return pivot;
  }

  public float getExponent() {
    return exponent;
  }

  public static Rfq build(Map<String, Map<String, Object>> entryValue) {
    if (entryValue.containsKey("rfq") && !isEmpty(entryValue.get("rfq"))) {
      var rfqMap = entryValue.get("rfq");
      var rfq = new Rfq();
      rfq.field = valueOf(rfqMap.get("field"));
      rfq.function = valueOf(rfqMap.get("function"));
      rfq.boost = parseFloat(valueOf(rfqMap.getOrDefault("boost", 0.0)));
      rfq.scalingFactor = parseFloat(valueOf(rfqMap.getOrDefault("scaling_factor", 0.0)));
      rfq.pivot = parseFloat(valueOf(rfqMap.getOrDefault("pivot", 0.0)));
      rfq.exponent = parseFloat(valueOf(rfqMap.getOrDefault("exponent", 0.0)));
      return rfq;
    }
    return null;
  }
}

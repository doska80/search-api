package com.grupozap.search.api.model.listener.rescore;

import static java.util.Arrays.stream;

public enum RescoreType {
  LTR_RESCORE("ltr_rescore", LtrRescore.class),
  RANDOM_RESCORE("random_rescore", RandomRescore.class),
  FUNCTION_SCORE_RESCORE("function_score", FunctionScoreRescore.class);

  private final String alias;
  private final Class<? extends SortRescore> rescoreClass;

  RescoreType(String alias, Class<? extends SortRescore> rescoreClass) {
    this.alias = alias;
    this.rescoreClass = rescoreClass;
  }

  public static RescoreType fromString(String alias) {
    return stream(values()).filter(r -> r.alias.equalsIgnoreCase(alias)).findFirst().orElseThrow();
  }

  public String getAlias() {
    return alias;
  }

  public Class<? extends SortRescore> getRescoreClass() {
    return rescoreClass;
  }
}

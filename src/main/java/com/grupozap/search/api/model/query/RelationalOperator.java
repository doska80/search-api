package com.grupozap.search.api.model.query;

import static com.vivareal.search.api.model.query.RelationalOperator.RelationalOperatorMap.OPERATORS;
import static java.util.Optional.ofNullable;

import java.util.*;

public enum RelationalOperator {
  DIFFERENT("NE", "<>"),
  EQUAL("EQ", ":", "="),
  GREATER("GT", ">"),
  GREATER_EQUAL("GTE", ">="),
  IN("INTO"),
  LESS("LT", "<"),
  LESS_EQUAL("LTE", "<="),
  VIEWPORT("@"),
  LIKE("LK"),
  RANGE("RG"),
  POLYGON("PG"),
  CONTAINS_ALL("CA");

  private final Set<String> alias;

  RelationalOperator(String... alias) {
    this.alias = new HashSet<>();
    this.alias.addAll(Arrays.asList(alias));
    this.alias.add(name());

    this.alias.forEach(label -> OPERATORS.put(label, this));
  }

  public static String[] getOperators() {
    return OPERATORS.keySet().toArray(new String[OPERATORS.size()]);
  }

  public static RelationalOperator get(final String relation) {
    return ofNullable(relation)
        .map(OPERATORS::get)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Relational Operator \"" + relation + "\" is not recognized!"));
  }

  public Set<String> getAlias() {
    return alias;
  }

  static class RelationalOperatorMap {
    static final Map<String, RelationalOperator> OPERATORS = new HashMap<>();
  }
}

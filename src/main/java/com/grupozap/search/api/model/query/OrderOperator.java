package com.grupozap.search.api.model.query;

import static java.util.Optional.ofNullable;

import java.util.stream.Stream;

public enum OrderOperator {
  ASC,
  DESC;

  public static OrderOperator get(String order) {
    String value = ofNullable(order).map(String::toUpperCase).orElse("");

    return Stream.of(OrderOperator.values())
        .filter(o -> o.name().equals(value))
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException("OrderOperator \"" + order + "\" is not recognized!"));
  }

  public static String[] getOperators() {
    return Stream.of(OrderOperator.values()).map(OrderOperator::name).toArray(String[]::new);
  }
}

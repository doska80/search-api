package com.grupozap.search.api.model.query;

import com.grupozap.search.api.exception.InvalidFieldException;
import com.grupozap.search.api.service.parser.factory.FieldFactory;
import java.util.Optional;

public class Facet {
  public static final String _KEY = "_key";
  public static final String _COUNT = "_count";
  private static final Sort DEFAULT_SORT =
      new Sort(FieldFactory.createField("_count"), OrderOperator.DESC);
  private final Field field;

  private final Sort sort;

  public Facet(Field field, Optional<Sort> sort) {
    this.field = field;

    this.sort = sort.orElse(DEFAULT_SORT);

    this.sort.stream()
        .filter(this::hasInvalidFieldName)
        .findFirst()
        .ifPresent(
            item -> {
              throw new InvalidFieldException(
                  String.format(
                      "Facet field [ %s ] not allowed. You can use [ _key ] or [ _count ]",
                      item.getField().getName()));
            });
  }

  private boolean hasInvalidFieldName(Item item) {
    return !_KEY.equals(item.getField().getName()) && !_COUNT.equals(item.getField().getName());
  }

  public Field getField() {
    return field;
  }

  public Sort getSort() {
    return sort;
  }

  @Override
  public String toString() {
    return String.format("%s %s", field.toString(), sort.toString()).trim();
  }
}

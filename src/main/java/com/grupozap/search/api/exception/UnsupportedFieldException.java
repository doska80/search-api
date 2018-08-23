package com.grupozap.search.api.exception;

import com.vivareal.search.api.model.query.RelationalOperator;

@FunctionalInterface
interface InvalidMessageFunction {
  String apply(String field, String invalidType, String validType, RelationalOperator operator);
}

public class UnsupportedFieldException extends IllegalArgumentException {

  private static final InvalidMessageFunction ERROR_MESSAGE =
      (field, invalidType, validType, operator) ->
          String.format(
              "Field [%s] is a type of [%s] and to operation [%s] the correct field type is %s",
              field, invalidType, operator.name(), validType);

  public UnsupportedFieldException(
      final String field,
      final String invalidType,
      final String validType,
      RelationalOperator operator) {
    super(ERROR_MESSAGE.apply(field, invalidType, validType, operator));
  }

  public UnsupportedFieldException(
      final String field,
      final String invalidType,
      final String validType,
      RelationalOperator operator,
      Throwable cause) {
    super(ERROR_MESSAGE.apply(field, invalidType, validType, operator), cause);
  }

  public UnsupportedFieldException(Throwable cause) {
    super(cause);
  }
}

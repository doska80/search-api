package com.grupozap.search.api.exception;

import static java.lang.String.format;

import java.util.function.BiFunction;

public class InvalidFieldException extends IllegalArgumentException {

  private static final BiFunction<String, String, String> ERROR_MESSAGE =
      (field, index) -> format("Field [ %s ] not found for index [ %s ]", field, index);

  public InvalidFieldException(final String field, final String index) {
    super(ERROR_MESSAGE.apply(field, index));
  }

  public InvalidFieldException(String field, final String index, Throwable cause) {
    super(ERROR_MESSAGE.apply(field, index), cause);
  }

  public InvalidFieldException(final String text) {
    super(text);
  }

  public InvalidFieldException(Throwable cause) {
    super(cause);
  }
}

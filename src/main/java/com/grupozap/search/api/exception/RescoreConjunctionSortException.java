package com.grupozap.search.api.exception;

import java.util.function.Function;

public class RescoreConjunctionSortException extends IllegalArgumentException {
  private static final Function<String, String> ERROR_MESSAGE =
      (field) -> String.format("Cannot use [%s] option in conjunction with [rescore].", field);

  public RescoreConjunctionSortException(final String field) {
    super(ERROR_MESSAGE.apply(field));
  }

  public RescoreConjunctionSortException(String field, Throwable cause) {
    super(ERROR_MESSAGE.apply(field), cause);
  }

  public RescoreConjunctionSortException(Throwable cause) {
    super(cause);
  }
}

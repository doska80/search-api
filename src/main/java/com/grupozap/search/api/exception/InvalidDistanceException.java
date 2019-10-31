package com.grupozap.search.api.exception;

import static java.lang.String.format;

import java.util.List;
import java.util.function.BiFunction;

public class InvalidDistanceException extends IllegalArgumentException {

  private static final BiFunction<String, List<String>, String> ERROR_MESSAGE =
      (distance, allowedDistances) ->
          format(
              "The distance informed [ %s ] is incorrect. The allowed distance is like this: [500m, 1km, etc]. The allowed unit distances are: %s",
              distance, allowedDistances);

  public InvalidDistanceException(final String distance, final List<String> allowedDistances) {
    super(ERROR_MESSAGE.apply(distance, allowedDistances));
  }
}

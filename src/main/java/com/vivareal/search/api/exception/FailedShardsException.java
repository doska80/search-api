package com.vivareal.search.api.exception;

import static java.lang.String.format;

import java.util.function.BiFunction;

public class FailedShardsException extends RuntimeException {

  private static final BiFunction<Integer, Integer, String> ERROR_MESSAGE =
      (failedShards, totalShards) ->
          format("Failed on %d from total of %d shards", failedShards, totalShards);

  public FailedShardsException(final int failedShards, final int totalShards) {
    super(ERROR_MESSAGE.apply(failedShards, totalShards));
  }
}

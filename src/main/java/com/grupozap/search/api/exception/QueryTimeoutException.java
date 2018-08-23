package com.grupozap.search.api.exception;

public class QueryTimeoutException extends QueryPhaseExecutionException {

  private static final String MESSAGE = "Timeout occurred when query was executed on server";

  public QueryTimeoutException() {
    this("");
  }

  public QueryTimeoutException(final String query) {
    super(MESSAGE, query);
  }
}

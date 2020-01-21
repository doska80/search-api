package com.grupozap.search.api.exception;

import static com.grupozap.search.api.utils.MapperUtils.parser;
import static java.lang.String.valueOf;

import com.fasterxml.jackson.databind.JsonNode;

public class QueryPhaseExecutionException extends RuntimeException {

  private static final String MESSAGE = "Error on executing the query by remote server";
  private final String query;

  public QueryPhaseExecutionException(final Throwable t) {
    super(MESSAGE, t);
    this.query = "{}";
  }

  public QueryPhaseExecutionException(final String message, final String query) {
    super(message);
    this.query = query;
  }

  public QueryPhaseExecutionException(final String query, final Throwable t) {
    super(MESSAGE, t);
    this.query = query;
  }

  @Override
  public String getMessage() {
    return super.getMessage() + " - query: [" + getQuery() + "]";
  }

  public String getQuery() {
    return valueOf(parser(query, JsonNode.class));
  }
}

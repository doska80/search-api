package com.vivareal.search.api.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class QueryPhaseExecutionException extends RuntimeException {

    private static Logger LOG = LoggerFactory.getLogger(QueryPhaseExecutionException.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
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

    public String getQuery() {
        try {
            return objectMapper.readValue(query, JsonNode.class).toString();
        } catch (IOException e) {
            LOG.warn("Error to read query {}", query, e);
            return "{}";
        }
    }
}

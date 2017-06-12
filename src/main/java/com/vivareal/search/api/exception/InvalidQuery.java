package com.vivareal.search.api.exception;

import java.util.function.Function;

public class InvalidQuery extends RuntimeException {

    private static final Function<String, String> QUERY_MESSAGE_FN = query -> String.format("Invalid query: %s", query);

    public InvalidQuery(String query) {
        super(QUERY_MESSAGE_FN.apply(query));
    }

    public InvalidQuery(String query, Throwable cause) {
        super(QUERY_MESSAGE_FN.apply(query), cause);
    }
}

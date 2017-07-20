package com.vivareal.search.api.model.http;

import com.fasterxml.jackson.annotation.JsonInclude;

import static java.time.ZonedDateTime.now;
import static java.util.Date.from;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SearchApiResponseError {

    private final long timestamp;
    private final String errorMessage;
    private final String request;

    public SearchApiResponseError(String errorMessage, String request) {
        this.timestamp = from(now().toInstant()).getTime();
        this.errorMessage = errorMessage;
        this.request = request;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRequest() {
        return request;
    }
}

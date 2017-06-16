package com.vivareal.search.api.model;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SearchApiResponse {

    private long time;
    private long totalCount;
    private Map<String, List<Object>> result;

    private SearchApiResponse() {
        super();
        this.result = new HashMap<>();
    }

    public static SearchApiResponse builder() {
        return new SearchApiResponse();
    }

    public SearchApiResponse time(final long time) {
        this.time = time;
        return this;
    }

    public SearchApiResponse totalCount(final long totalCount) {
        this.totalCount = totalCount;
        return this;
    }

    public SearchApiResponse result(final String resultName, Object result) {
        return result(resultName, result != null ? Lists.newArrayList(result) : null);
    }

    public SearchApiResponse result(final String resultName, List<Object> result) {
        this.result.put(resultName, result != null ? result : new ArrayList<>());
        return this;
    }

    public long getTime() {
        return time;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public Map<String, List<Object>> getResult() {
        return result;
    }
}

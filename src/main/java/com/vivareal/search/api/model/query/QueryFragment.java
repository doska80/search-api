package com.vivareal.search.api.model.query;

public interface QueryFragment {
    default QueryFragment get() {
        return this;
    }
}

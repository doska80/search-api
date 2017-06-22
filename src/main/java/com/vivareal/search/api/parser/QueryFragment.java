package com.vivareal.search.api.parser;

public interface QueryFragment {
    default QueryFragment get() {
        return this;
    }
}
package com.vivareal.search.api.model.query;

public interface QueryFragment {
  int MAX_FRAGMENTS = 64;

  default QueryFragment get() {
    return this;
  }
}

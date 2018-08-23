package com.grupozap.search.api.model.query;

import java.util.HashSet;
import java.util.Set;

public interface QueryFragment {
  int MAX_FRAGMENTS = 64;

  default QueryFragment get() {
    return this;
  }

  default Set<String> getFieldNames() {
    return getFieldNames(true);
  }

  default Set<String> getFieldNames(boolean includeRootFields) {
    return new HashSet<>();
  }
}

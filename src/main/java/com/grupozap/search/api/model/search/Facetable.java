package com.grupozap.search.api.model.search;

import java.util.Set;

public interface Facetable extends Indexable {
  Set<String> getFacets();

  int getFacetSize();

  void setFacetSize(int facetSize);

  default void setFacetingValues(final int defaultSize) {
    if (getFacetSize() != Integer.MAX_VALUE) {
      if (getFacetSize() <= 0)
        throw new IllegalArgumentException("Parameter [facetSize] must be a positive integer");

      setFacetSize(getFacetSize());
    } else setFacetSize(defaultSize);
  }
}

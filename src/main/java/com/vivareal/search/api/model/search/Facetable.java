package com.vivareal.search.api.model.search;

import java.util.Set;

public interface Facetable extends Indexable {
    Set<String> getFacets();
    Integer getFacetSize();
}

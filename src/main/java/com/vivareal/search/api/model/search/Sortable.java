package com.vivareal.search.api.model.search;

import java.util.Set;

public interface Sortable extends Indexable {
    Set<String> getSort();
}

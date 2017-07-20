package com.vivareal.search.api.model.search;

import java.util.Set;

public interface Fetchable extends Indexable {
    Set<String> getIncludeFields();
    Set<String> getExcludeFields();
}

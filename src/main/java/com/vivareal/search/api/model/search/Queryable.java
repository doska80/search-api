package com.vivareal.search.api.model.search;

import java.util.Set;

public interface Queryable extends Indexable {
    String getQ();
    String getMm();
    Set<String> getFields();
}

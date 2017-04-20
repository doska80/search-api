package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;

public interface QueryAdapter<Q, F, S> {

    Object getById(String collection, String id);
    Q getQuery(SearchApiRequest request);

}

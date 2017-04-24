package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;

import java.util.List;

public interface QueryAdapter<Q, F, S> {

    Object getById(String collection, String id);
    List<Q> getQuery(SearchApiRequest request);

}

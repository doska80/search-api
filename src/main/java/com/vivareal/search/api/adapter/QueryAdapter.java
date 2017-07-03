package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;

/**
 *
 * @param <Q1>
 * @param <Q2>
 */
public interface QueryAdapter<Q1, Q2> {

    Q1 getById(SearchApiRequest request, String id);

    Q2 query(SearchApiRequest request);
}

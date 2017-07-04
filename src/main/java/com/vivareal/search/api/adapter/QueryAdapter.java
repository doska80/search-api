package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;

/**
 * This interface is responsible to generate a queryBuilder to realize a search operation
 *
 * @param <Q1> Implementation of QueryAdapter expected as a result of search by id
 * @param <Q2> Implementation of QueryAdapter expected as a result of the generic query
 */
public interface QueryAdapter<Q1, Q2> {

    Q1 getById(SearchApiRequest request, String id);

    Q2 query(SearchApiRequest request);
}

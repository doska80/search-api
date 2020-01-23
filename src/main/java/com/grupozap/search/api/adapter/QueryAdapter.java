package com.grupozap.search.api.adapter;

import com.grupozap.search.api.model.http.BaseApiRequest;
import com.grupozap.search.api.model.http.FilterableApiRequest;
import com.grupozap.search.api.model.http.SearchApiRequest;

/**
 * This interface is responsible to generate a queryBuilder to realize a search operation
 *
 * @param <Q1> Implementation of QueryAdapter expected as a result of search by id
 * @param <Q2> Implementation of QueryAdapter expected as a result of the generic query
 */
public interface QueryAdapter<Q1, Q2> {

  Q1 getById(BaseApiRequest request, String id);

  Q2 query(FilterableApiRequest request);

  Q2 query(SearchApiRequest request);
}

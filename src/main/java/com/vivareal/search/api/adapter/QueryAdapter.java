package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;

public interface QueryAdapter<Q, F, S> {

    Object getById(SearchApiRequest request, String id);

    SearchApiResponse getQueryMarcao(SearchApiRequest request);
}

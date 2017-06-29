package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;

import java.util.Optional;

public interface QueryAdapter<Q> {

    Optional<SearchApiResponse> getById(SearchApiRequest request, String id);

    Q query(SearchApiRequest request);
}

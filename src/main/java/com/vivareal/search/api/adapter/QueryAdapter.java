package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;

import java.util.List;
import java.util.Map;

public interface QueryAdapter<Q, F, S> {

    Object getById(SearchApiRequest request, String id);
    List<Map<String, Object>> getQueryMarcao(SearchApiRequest request);
}

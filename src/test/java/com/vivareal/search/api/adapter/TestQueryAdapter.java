package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;

public class TestQueryAdapter extends AbstractQueryAdapter<Void, Void, Void> {

    @Override
    public Optional<Object> getById(SearchApiRequest request, String id) {
        return empty();
    }

    @Override
    public SearchApiResponse query(SearchApiRequest request) {
        return null;
    }

    @Override
    protected Void getFilter(List<String> filter) {
        return null;
    }

    @Override
    protected Void getSort(List<String> sort) {
        return null;
    }

}

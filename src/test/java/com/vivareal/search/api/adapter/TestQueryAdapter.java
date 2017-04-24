package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;
import org.elasticsearch.search.SearchHit;

import java.util.List;

public class TestQueryAdapter extends AbstractQueryAdapter<Void, Void, Void> {

    @Override
    public Object getById(SearchApiRequest request, String id) {
        return null;
    }

    @Override
    public List<Void> getQueryMarcao(SearchApiRequest request) {
        return null;
    }

    @Override
    public List<Void> getQueryMamud(SearchApiRequest request) {
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

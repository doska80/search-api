package com.vivareal.search.api.service;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.model.SearchApiRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Component
public class ListingService {

    @Autowired
    @Qualifier("ElasticsearchQuery")
    protected QueryAdapter queryAdapter;

    public Map<String, Object> getListingById(SearchApiRequest request, String id) {
        return (Map<String, Object>) this.queryAdapter.getById(request, id);
    }

    public List<Object> getListings(SearchApiRequest request) {
        List<Object> response = this.queryAdapter.getQueryMarcao(request);
        return response;
    }

    public void stream(SearchApiRequest request, OutputStream stream) {
        this.queryAdapter.stream(request, stream);
    }
}

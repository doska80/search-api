package com.vivareal.search.api.service;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

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

    public SearchApiResponse query(SearchApiRequest request) {
        queryAdapter.getQuery(request);

        return null;
    }

    public List<Object> getListings(SearchApiRequest request) {
        List<Object> response = this.queryAdapter.getQueryMarcao(request);
        return response;
    }
}

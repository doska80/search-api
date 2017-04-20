package com.vivareal.search.api.service;

import com.vivareal.search.api.adapter.ElasticsearchQueryAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ListingService {

    @Autowired
    @Qualifier("ElasticsearchQuery")
    protected ElasticsearchQueryAdapter queryAdapter;

    public Map<String, Object> getListingById(String id) {
        return (Map<String, Object>) this.queryAdapter.getById("inmuebles", id);
    }

}

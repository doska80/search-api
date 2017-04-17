package com.vivareal.search.api.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class SearchApiResponse {
    private final List<Object> listings;

    public SearchApiResponse() {
        this(new ArrayList<>());
    }

    public SearchApiResponse(List<Object> listings) {
        this.listings = listings;
    }

    public SearchApiResponse(String listings) {
        List<Object> list = new ArrayList<>();
        list.add(listings);
        this.listings = list;
    }

    public Object getListings() {
        return listings;
    }

    public void addList(Object source) {
        this.listings.add(source);
    }
}

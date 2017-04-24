package com.vivareal.search.api.model;


import java.util.ArrayList;
import java.util.List;

public final class SearchApiResponse {
    private final List<Object> listings;

    public SearchApiResponse() {
        this(new ArrayList<>());
    }

    public SearchApiResponse(List<Object> listings) {
        this.listings = listings;
    }

    public Object getListings() {
        return listings;
    }

    public void addListing(Object source) {
        this.listings.add(source);
    }

    public void addListings(List<Object> listings) {
        this.listings.addAll(listings);
    }
}

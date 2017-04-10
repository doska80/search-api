package com.vivareal.search.api.model;

public final class SearchResponse {
    private final String listings;

    public SearchResponse() {
        this("{}");
    }

    public SearchResponse(String listings) {
        this.listings = listings;
    }

    public String getListings() {
        return listings;
    }
}

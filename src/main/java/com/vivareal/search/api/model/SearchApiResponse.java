package com.vivareal.search.api.model;

public final class SearchApiResponse {
    private final String listings;

    public SearchApiResponse() {
        this("{}");
    }

    public SearchApiResponse(String listings) {
        this.listings = listings;
    }

    public String getListings() {
        return listings;
    }
}

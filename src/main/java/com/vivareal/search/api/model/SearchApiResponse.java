package com.vivareal.search.api.model;


import java.util.ArrayList;
import java.util.List;

public final class SearchApiResponse {
    private final long time;
    private final long totalCount;
    private final List<Object> listings;

    public SearchApiResponse() {
        this(0l, 0l, new ArrayList<>());
    }

    public SearchApiResponse(long time, long totalCount, List<Object> listings) {
        this.time = time;
        this.totalCount = totalCount;
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

    public long getTotalCount() {
        return totalCount;
    }

    public long getTime() {
        return time;
    }
}

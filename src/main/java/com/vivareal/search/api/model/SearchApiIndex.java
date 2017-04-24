package com.vivareal.search.api.model;

public class SearchApiIndex {

    private SearchApiRequest request;

    public SearchApiIndex(SearchApiRequest request) {
        this.request = request;
    }

    public static SearchApiIndex of(SearchApiRequest request) {
        return new SearchApiIndex(request);
    }

    public String getIndex() {
        return "inmuebles"; // TODO creates a logic to returns the index according to request
    }
}

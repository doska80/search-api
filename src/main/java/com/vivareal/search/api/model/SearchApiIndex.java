package com.vivareal.search.api.model;

import com.vivareal.search.api.exception.IndexNotFoundException;

import java.util.Arrays;

public class SearchApiIndex {

    private SearchApiRequest request;

    public SearchApiIndex(SearchApiRequest request) {
        this.request = request;
    }

    public static SearchApiIndex of(SearchApiRequest request) {
        return new SearchApiIndex(request);
    }

    public String getIndex() {
        return SearchIndex.getSearchIndexByIndexName(request.getIndex()).index();
    }

    public enum SearchIndex {

        LISTINGS("listing", "listings") {
            @Override
            public String index() {
                return "listings";
            }
        },
        PUBLISHERS("publisher", "publishers") {
            @Override
            public String index() {
                return "publishers";
            }
        };

        public static SearchIndex getSearchIndexByIndexName(final String indexName) {
            for (SearchIndex index : SearchIndex.values()) {
                if (Arrays.asList(index.indexNames).contains(indexName)) {
                    return index;
                }
            }
            throw new IndexNotFoundException(indexName);
        }

        public static String[] getIndexNames() {
            String[] indexNames = new String[SearchIndex.values().length];
            int i = 0;
            for (SearchIndex index : SearchIndex.values()) {
                indexNames[i++] = index.index();
            }
            return indexNames;
        }

        private String[] indexNames;

        public abstract String index();

        SearchIndex(String... indexNames) {
            this.indexNames = indexNames;
        }

    }
}

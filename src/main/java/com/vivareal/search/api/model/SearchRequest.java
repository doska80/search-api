package com.vivareal.search.api.model;

import java.util.List;

public final class SearchRequest {

    private List<String> fields;
    private String q;

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        if (q != null) this.q = q.trim();
    }
}

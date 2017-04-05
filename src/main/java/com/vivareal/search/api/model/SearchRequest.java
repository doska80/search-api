package com.vivareal.search.api.model;

import java.util.List;

public class SearchRequest {

    protected List<String> fields;
    protected String q;

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
        if (q == null) return;
        this.q = q.trim();
    }
}

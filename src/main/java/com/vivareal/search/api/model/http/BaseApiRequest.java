package com.vivareal.search.api.model.http;

import com.vivareal.search.api.model.search.Fetchable;

import java.util.Set;

public class BaseApiRequest implements Fetchable {

    private String index;

    private Set<String> includeFields;
    private Set<String> excludeFields;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public Set<String> getIncludeFields() {
        return includeFields;
    }

    public void setIncludeFields(Set<String> includeFields) {
        this.includeFields = includeFields;
    }

    public Set<String> getExcludeFields() {
        return excludeFields;
    }

    public void setExcludeFields(Set<String> excludeFields) {
        this.excludeFields = excludeFields;
    }

}

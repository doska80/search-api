package com.vivareal.search.api.model.query;

import org.springframework.util.CollectionUtils;

import java.util.List;

public class QueryFragmentNot implements QueryFragment {
    private final boolean not;

    public QueryFragmentNot(List<Boolean> nots) {
        if (nots.size() > 1) throw new IllegalArgumentException("Cannot have consecutive NOTs");
        this.not = CollectionUtils.isEmpty(nots) ? false : nots.get(0);
    }

    @Override
    public String toString() {
        return not ? "NOT" : "";
    }
}

package com.vivareal.search.api.model.query;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

public class QueryFragmentNot implements QueryFragment {

    private final boolean not;

    public QueryFragmentNot(List<Boolean> nots) {
        if (nots.size() > 1)
            throw new IllegalArgumentException("Cannot have consecutive NOTs");

        this.not = isEmpty(nots) || nots.get(0) == null ? false : nots.get(0);
    }

    public boolean isNot() {
        return not;
    }

    @Override
    public String toString() {
        return not ? "NOT" : "";
    }
}

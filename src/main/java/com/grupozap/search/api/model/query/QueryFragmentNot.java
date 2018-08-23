package com.grupozap.search.api.model.query;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;

public class QueryFragmentNot implements QueryFragment {

  private final boolean not;

  public QueryFragmentNot(List<Boolean> nots) {
    if (nots.size() > 1) throw new IllegalArgumentException("Cannot have consecutive NOTs");

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

package com.vivareal.search.api.model.search;

public interface Sortable extends Indexable {
  String getSort();

  boolean isDisableSort();
}

package com.grupozap.search.api.adapter;

import com.grupozap.search.api.model.mapping.MappingType;
import com.grupozap.search.api.model.search.Indexable;

public interface SettingsAdapter<T, U> {

  T settings();

  U settingsByKey(String index, String property);

  void checkIndex(Indexable request);

  String getFieldType(String index, String fieldName);

  String getIndexByAlias(String index);

  boolean isTypeOf(final String index, final String fieldName, final MappingType type);
}

package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.mapping.MappingType;
import com.vivareal.search.api.model.search.Indexable;

public interface SettingsAdapter<T, U> {

  T settings();

  U settingsByKey(String index, String property);

  void checkIndex(Indexable request);

  boolean checkFieldName(String index, String fieldName, boolean acceptAsterisk);

  String getFieldType(String index, String fieldName);

  boolean isTypeOf(final String index, final String fieldName, final MappingType type);
}

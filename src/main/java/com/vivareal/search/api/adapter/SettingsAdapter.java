package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.search.Indexable;
import com.vivareal.search.api.model.mapping.MappingType;

public interface SettingsAdapter<T, U> {

    T settings();

    U settingsByKey(String index, String property);

    void checkIndex(Indexable request);

    void checkFieldName(String index, String fieldName);

    String getFieldType(String index, String fieldName);

    boolean isTypeOf(final String index, final String fieldName, final MappingType type);
}

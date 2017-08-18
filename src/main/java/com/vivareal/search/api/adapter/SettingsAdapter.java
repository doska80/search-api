package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.search.Indexable;

public interface SettingsAdapter<T, U> {

    T settings();

    U settingsByKey(String index, String property);

    void checkIndex(Indexable request);

    void checkFieldName(String index, String fieldName);

    String getFieldType(String index, String fieldName);

    boolean isTypeOfNested(String index, String fieldName);

    boolean isTypeOfText(String index, String fieldName);

    boolean isTypeOfBoolean(String index, String fieldName);

    boolean isTypeOfGeoPoint(String index, String fieldName);

    boolean isTypeOfKeyword(String index, String fieldName);

    boolean isTypeOfDate(String index, String fieldName);

    boolean isTypeOfLong(String index, String fieldName);

    boolean isTypeOfFloat(String index, String fieldName);

    default boolean isTypeOfString(String index, String fieldName) {
        return isTypeOfText(index, fieldName) || isTypeOfKeyword(index, fieldName);
    }
}

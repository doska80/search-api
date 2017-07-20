package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.search.Indexable;

public interface SettingsAdapter<T, U> {

    T settings();

    U settingsByKey(String index, String key);

    void checkIndex(Indexable request);
}

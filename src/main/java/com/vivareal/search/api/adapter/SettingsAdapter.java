package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;

/**
 * Created by leandropereirapinto on 6/29/17.
 */
public interface SettingsAdapter<T, U> {

    T settings();

    U settingsByKey(String index, String key);

    void checkIndex(SearchApiRequest request);
}

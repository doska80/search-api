package com.vivareal.search.api.adapter;

/**
 * Created by leandropereirapinto on 6/29/17.
 */
public interface SettingsAdapter<T, U> {

    T settings();

    U settingsByKey(String index, String key);
}

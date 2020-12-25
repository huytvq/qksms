package com.moez.QKSMS.feature;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceHelper {

    public static final String SHARED_PREF_NAME = "MESSAGES";
    private static SharedPreferenceHelper mInstance;
    private SharedPreferences mSharedPreferences;

    private SharedPreferenceHelper(Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    public static SharedPreferenceHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPreferenceHelper(context);
        }
        return mInstance;
    }

    public void set(String key, String value) {
        mSharedPreferences.edit().putString(key, value).apply();
    }

    public String get(String key) {
        return mSharedPreferences.getString(key, null);
    }

    public void removeKey(String key) {
        mSharedPreferences.edit().remove(key).apply();
    }

    public void clearSharePrefs() {
        mSharedPreferences.edit().clear().commit();
    }

    public void setInt(String key, int value) {
        mSharedPreferences.edit().putInt(key, value).apply();
    }

    public int getInt(String key) {
        return mSharedPreferences.getInt(key, 0);
    }

    public void setBool(String key, boolean value) {
        mSharedPreferences.edit().putBoolean(key, value).apply();
    }

    public boolean getBool(String key) {
        return mSharedPreferences.getBoolean(key, false);
    }
}
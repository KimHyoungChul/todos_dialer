package com.todosdialer.todosdialer.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Gson 객체를 관라하는 객체
 */
public class GsonManager {
    private static Gson mGson;

    public static Gson getGson() {
        if (mGson == null) {
            mGson = new GsonBuilder()
                    .serializeNulls()
                    .setLenient()
                    .create();
        }
        return mGson;
    }
}

package com.todosdialer.todosdialer.api.body;

import android.content.Context;

import com.google.gson.JsonObject;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;

public class LoadOrderBody {
    private ArrayList<JsonObject> AppOrderList = new ArrayList<>();

    public LoadOrderBody(Context context, String email) {
        JsonObject obj = new JsonObject();
        obj.addProperty("langCode", "KR");
        obj.addProperty("id", email);
        obj.addProperty("uuid", Utils.getUUID(context));

        AppOrderList.add(obj);
    }
}

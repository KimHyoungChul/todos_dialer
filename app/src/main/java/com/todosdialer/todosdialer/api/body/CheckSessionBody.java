package com.todosdialer.todosdialer.api.body;

import android.content.Context;

import com.google.gson.JsonObject;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;

public class CheckSessionBody {
    private ArrayList<JsonObject> AppSessionInfo = new ArrayList<>();

    public CheckSessionBody(Context context, String email) {
        JsonObject obj = new JsonObject();
        obj.addProperty("langCode", "KR");
        obj.addProperty("id", email);
        obj.addProperty("uuid", Utils.getUUID(context));

        AppSessionInfo.add(obj);
    }
}

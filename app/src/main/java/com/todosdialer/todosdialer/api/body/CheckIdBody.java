package com.todosdialer.todosdialer.api.body;

import com.google.gson.JsonObject;

import java.util.ArrayList;

public class CheckIdBody {
    private ArrayList<JsonObject> AppCheckId = new ArrayList<>();

    public CheckIdBody(String id) {
        JsonObject obj = new JsonObject();
        obj.addProperty("langCode", "KR");
        obj.addProperty("id", id);
        AppCheckId.add(obj);
    }
}

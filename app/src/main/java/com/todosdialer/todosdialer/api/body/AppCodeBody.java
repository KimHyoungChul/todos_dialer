package com.todosdialer.todosdialer.api.body;

import com.google.gson.JsonObject;

import java.util.ArrayList;

public class AppCodeBody {
    private ArrayList<JsonObject> AppCode = new ArrayList<>();

    public AppCodeBody(String codeGroup, String code, String getCodeGroup) {
        JsonObject obj = new JsonObject();
        obj.addProperty("langCode", "KR");
        obj.addProperty("codeGroup", codeGroup);
        obj.addProperty("code", code);
        obj.addProperty("getCodeGroup", getCodeGroup);
        AppCode.add(obj);
    }
}

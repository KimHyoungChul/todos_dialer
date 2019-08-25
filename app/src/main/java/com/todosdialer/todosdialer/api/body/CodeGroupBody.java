package com.todosdialer.todosdialer.api.body;

import com.google.gson.JsonObject;

import java.util.ArrayList;

public class CodeGroupBody {
    private ArrayList<JsonObject> AppCodeGroup = new ArrayList<>();

    public CodeGroupBody(String codeGroup) {
        JsonObject obj = new JsonObject();
        obj.addProperty("langCode", "KR");
        obj.addProperty("codeGroup", codeGroup);
        AppCodeGroup.add(obj);
    }
}

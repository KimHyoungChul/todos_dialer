package com.todosdialer.todosdialer.api.body;

import android.content.Context;

import com.google.gson.JsonObject;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;


public class FindingPwBody {
    private ArrayList<JsonObject> AppFindPw = new ArrayList<>();

    public FindingPwBody(Context context, String email) {
        JsonObject obj = new JsonObject();
        obj.addProperty("langCode", "KR");
        obj.addProperty("id", email);
        obj.addProperty("uuid", Utils.getUUID(context));

        AppFindPw.add(obj);
    }
}

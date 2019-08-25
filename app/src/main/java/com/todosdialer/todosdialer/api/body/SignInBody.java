package com.todosdialer.todosdialer.api.body;

import android.content.Context;

import com.google.gson.JsonObject;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;


public class SignInBody {
    private ArrayList<JsonObject> AppMemberLogin = new ArrayList<>();

    public SignInBody(Context context, String id, String pw) {
        JsonObject obj = new JsonObject();
        obj.addProperty("langCode", "KR");
        obj.addProperty("id", id);
        obj.addProperty("pw", pw);
        obj.addProperty("uuid", Utils.getUUID(context));

        AppMemberLogin.add(obj);
    }
}

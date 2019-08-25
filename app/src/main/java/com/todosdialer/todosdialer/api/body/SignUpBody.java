package com.todosdialer.todosdialer.api.body;

import android.content.Context;

import com.google.gson.JsonObject;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;


public class SignUpBody {
    private ArrayList<JsonObject> AppMemberReg = new ArrayList<>();

    public SignUpBody(Context context, String id, String name, String birthday, String phone, String pw) {
        JsonObject obj = new JsonObject();
        obj.addProperty("langCode", "KR");
        obj.addProperty("id", id);
        obj.addProperty("pw", pw);
        obj.addProperty("name", name);
        obj.addProperty("phone", phone);
        obj.addProperty("birthday", birthday);
        obj.addProperty("uuid", Utils.getUUID(context));

        AppMemberReg.add(obj);
    }
}

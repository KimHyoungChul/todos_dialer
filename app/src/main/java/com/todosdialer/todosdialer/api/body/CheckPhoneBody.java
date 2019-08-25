package com.todosdialer.todosdialer.api.body;

import com.google.gson.JsonObject;

import java.util.ArrayList;


public class CheckPhoneBody {
    private ArrayList<JsonObject> AppCheckPhone = new ArrayList<>();
    public CheckPhoneBody(String phoneNumber) {
        JsonObject obj = new JsonObject();
        obj.addProperty("langCode", "KR");
        obj.addProperty("phone", phoneNumber);
        AppCheckPhone.add(obj);
    }
}

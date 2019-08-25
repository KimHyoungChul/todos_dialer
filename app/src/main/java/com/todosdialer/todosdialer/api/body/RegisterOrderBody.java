package com.todosdialer.todosdialer.api.body;


import android.content.Context;

import com.google.gson.JsonObject;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;

public class RegisterOrderBody {
    public ArrayList<JsonObject> AppOrderReg = new ArrayList<>();

    public RegisterOrderBody(Context context,
                             String id,
                             String basicNationCode,
                             String basicTelecomCode,
                             String outNationCode,
                             String outDate,
                             String outTime,
                             String outAirPortCode,
                             String inDate,
                             String inTime,
                             String inAirPortCode) {
        JsonObject obj = new JsonObject();
        obj.addProperty("langCode", "KR");
        obj.addProperty("id", id);
        obj.addProperty("uuid", Utils.getUUID(context));
        obj.addProperty("basicNationCode", basicNationCode);
        obj.addProperty("basicTelecomCode", basicTelecomCode);
        obj.addProperty("outNationCode", outNationCode);
        obj.addProperty("outDate", outDate);
        obj.addProperty("outTime", outTime);
        obj.addProperty("outAirPortCode", outAirPortCode);
        obj.addProperty("inDate", inDate);
        obj.addProperty("inTime", inTime);
        obj.addProperty("inAirPortCode", inAirPortCode);
        AppOrderReg.add(obj);
    }
}

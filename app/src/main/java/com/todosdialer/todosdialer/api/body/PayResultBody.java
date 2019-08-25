package com.todosdialer.todosdialer.api.body;

import android.content.Context;

import com.google.gson.JsonObject;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;

public class PayResultBody {
    private ArrayList<JsonObject> AppPayResult = new ArrayList<>();

    public PayResultBody(Context context,
                         String email,
                         String orderNo,
                         String payID,
                         String payMethod,
                         String payPg,
                         float payPrice,
                         String payResult,
                         String payDate,
                         int payState) {
        JsonObject obj = new JsonObject();
        obj.addProperty("langCode", "KR");
        obj.addProperty("id", email);
        obj.addProperty("uuid", Utils.getUUID(context));
        obj.addProperty("orderNo", orderNo);
        obj.addProperty("payID", payID);

        obj.addProperty("payMethod", payMethod);
        obj.addProperty("payPg", payPg);
        obj.addProperty("payPrice", payPrice);
        obj.addProperty("payResult", payResult);
        obj.addProperty("payDate", payDate);
        obj.addProperty("payState", payState);

        AppPayResult.add(obj);
    }
}

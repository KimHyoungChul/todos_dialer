package com.todosdialer.todosdialer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;
import com.todosdialer.todosdialer.TodosApplication;
import com.todosdialer.todosdialer.service.TodosService;

import java.lang.reflect.Method;

public class TodosReceiver extends BroadcastReceiver {
    private static final String ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE";
    private static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_PHONE_STATE.equals(intent.getAction())) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                if (TodosApplication.onCalling()) {
                    killCall(context);
                }
            }
        } else if (ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, TodosService.class));
            } else {
                context.startService(new Intent(context, TodosService.class));
            }
        }
    }

    public void killCall(Context context) {
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony != null) {
            try {
                Class c = Class.forName(telephony.getClass().getName());
                Method m = c.getDeclaredMethod("getITelephony");
                m.setAccessible(true);
                ITelephony telephonyService = (ITelephony) m.invoke(telephony);
                telephonyService.endCall();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}

package com.todosdialer.todosdialer.sip;

import android.util.Log;

import org.pjsip.pjsua2.Buddy;

public class TodosSipBuddy extends Buddy {
    public void onBuddyState() {
        super.onBuddyState();
        Log.d("TodosSipBuddy", "onBuddyState");
    }
}

package com.todosdialer.todosdialer.sip;

import android.content.Intent;
import android.util.Log;

import com.todosdialer.todosdialer.manager.RealmManager;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnIncomingSubscribeParam;
import org.pjsip.pjsua2.OnInstantMessageParam;
import org.pjsip.pjsua2.OnInstantMessageStatusParam;
import org.pjsip.pjsua2.OnMwiInfoParam;
import org.pjsip.pjsua2.OnRegStartedParam;
import org.pjsip.pjsua2.OnRegStateParam;
import org.pjsip.pjsua2.OnTypingIndicationParam;
import org.pjsip.pjsua2.pjsip_status_code;

public class TodosSipAccount extends Account {
    public void onIncomingCall(OnIncomingCallParam paramOnIncomingCallParam) {
        super.onIncomingCall(paramOnIncomingCallParam);
        Log.e("TodosSipAccount", "-- incoming call  onIncomingCall");
        SipInstance sipInstance = SipInstance.getInstance();

        int callId = paramOnIncomingCallParam.getCallId();
        TodosSipCall newIncomingCall = new TodosSipCall(sipInstance.getTodosAccount(), callId);

        Intent localIntent = new Intent(TodosSip.ACTION_INCOMING_CALLING);
        String phoneNumber = paramOnIncomingCallParam.getRdata().getWholeMsg().split("From: \"")[1].split("\"")[0];
        localIntent.putExtra(TodosSip.EXTRA_PHONE_NUMBER, phoneNumber);
        sipInstance.sendBroadcast(localIntent);

        sipInstance.setSipCall(newIncomingCall);
    }

    public void onIncomingSubscribe(OnIncomingSubscribeParam paramOnIncomingSubscribeParam) {
        super.onIncomingSubscribe(paramOnIncomingSubscribeParam);
        Log.e("TodosSipAccount", "-- onIncomingSubscribe");
    }

    public void onInstantMessage(OnInstantMessageParam param) {
        super.onInstantMessage(param);
        String phoneNumber = param.getFromUri().split("sip:")[1].split("@")[0];
        String msgBody = param.getMsgBody();

        RealmManager.newInstance().writeLog("Receiving sms: " + phoneNumber);
        RealmManager.newInstance().writeLog("Receiving msgBody: " + msgBody);

        Intent intent = new Intent(TodosSip.ACTION_RECEIVE_MESSAGE);
        intent.putExtra(TodosSip.EXTRA_PHONE_NUMBER, phoneNumber);
        intent.putExtra(TodosSip.EXTRA_MESSAGE_BODY, msgBody);
        SipInstance.getInstance().sendBroadcast(intent);
    }

    public void onInstantMessageStatus(OnInstantMessageStatusParam param) {
        super.onInstantMessageStatus(param);
        pjsip_status_code code = param.getCode();
        Log.e("TodosSipAccount", "onInstantMessageStatus, code: " + code);
        RealmManager.newInstance().writeLog("onInstantMessageStatus, code: " + code);
        if (pjsip_status_code.PJSIP_SC_ACCEPTED.equals(code)) {
            String phoneNumber = param.getToUri().split("sip:")[1].split("@")[0];
            String msgBody = param.getMsgBody();

            Intent intent = new Intent(TodosSip.ACTION_SEND_MESSAGE);
            intent.putExtra(TodosSip.EXTRA_PHONE_NUMBER, phoneNumber);
            intent.putExtra(TodosSip.EXTRA_MESSAGE_BODY, msgBody);
            SipInstance.getInstance().sendBroadcast(intent);
        }
    }

    public void onMwiInfo(OnMwiInfoParam paramOnMwiInfoParam) {
        super.onMwiInfo(paramOnMwiInfoParam);
        Log.e("TodosSipAccount", "onMwiInfo");
    }

    public void onRegStarted(OnRegStartedParam paramOnRegStartedParam) {
        super.onRegStarted(paramOnRegStartedParam);
        Log.e("TodosSipAccount", "onRegStarted");
    }

    public void onRegState(OnRegStateParam paramOnRegStateParam) {
        RealmManager.newInstance().writeLog("handleRegisterSip status: " + paramOnRegStateParam.getStatus());

        SipInstance.getInstance().setOnAccountRegState(paramOnRegStateParam.getCode());
        int code = paramOnRegStateParam.getStatus();
        String reason = paramOnRegStateParam.getReason();

        Intent intent = new Intent(TodosSip.ACTION_REGISTER_SIP);
        intent.putExtra(TodosSip.EXTRA_REGISTER_CODE, code);
        intent.putExtra(TodosSip.EXTRA_REGISTER_REASON, reason);
        SipInstance.getInstance().sendBroadcast(intent);

        Log.e("TodosSipAccount", "On registration state : " + paramOnRegStateParam.getCode());
        Log.e("TodosSipAccount", paramOnRegStateParam.getReason());
        Log.e("TodosSipAccount", "prm.getStatus():" + paramOnRegStateParam.getStatus());

        RealmManager.newInstance().writeLog("On registration state : " + paramOnRegStateParam.getCode());
        RealmManager.newInstance().writeLog("reason: " + paramOnRegStateParam.getReason());
        RealmManager.newInstance().writeLog("prm.getStatus():" + paramOnRegStateParam.getStatus());
    }

    public void onTypingIndication(OnTypingIndicationParam paramOnTypingIndicationParam) {
        super.onTypingIndication(paramOnTypingIndicationParam);
    }
}
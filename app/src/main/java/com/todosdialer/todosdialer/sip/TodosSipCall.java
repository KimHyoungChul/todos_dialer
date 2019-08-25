package com.todosdialer.todosdialer.sip;


import android.content.Intent;
import android.util.Log;

import com.todosdialer.todosdialer.manager.RealmManager;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.AudioMedia;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallMediaInfo;
import org.pjsip.pjsua2.CallMediaInfoVector;
import org.pjsip.pjsua2.Media;
import org.pjsip.pjsua2.OnCallMediaStateParam;
import org.pjsip.pjsua2.OnCallStateParam;
import org.pjsip.pjsua2.pjmedia_type;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsua_call_media_status;


public class TodosSipCall extends Call {

    TodosSipCall(Account paramAccount) {
        super(paramAccount);
    }

    TodosSipCall(Account paramAccount, int paramInt) {
        super(paramAccount, paramInt);
    }


    @Override
    public void onCallMediaState(OnCallMediaStateParam prm) {
        CallInfo ci;
        try {
            ci = getInfo();
        } catch (Exception e) {
            e.printStackTrace();
            RealmManager.newInstance().writeLog("[Todos] Call Exception: " + Log.getStackTraceString(e));
            return;
        }

        CallMediaInfoVector cmiv = ci.getMedia();
        for (int i = 0; i < cmiv.size(); i++) {
            CallMediaInfo cmi = cmiv.get(i);
            if (cmi.getType() == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
                    (cmi.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE)) {
                Media m = getMedia(i);
                AudioMedia am = AudioMedia.typecastFromMedia(m);
                AudDevManager audDevManager = SipInstance.getInstance().getAudDevManager();
                try {
                    //통화 소리의 노이즈(울림 현상 포함) 조정
                    audDevManager.getCaptureDevMedia().adjustRxLevel(0.6f);
                    audDevManager.getCaptureDevMedia().adjustTxLevel(0.6f);

                    if (am != null) {
                        am.startTransmit(audDevManager.getPlaybackDevMedia());
                    }
                    //통화 시작
                    audDevManager.getCaptureDevMedia().startTransmit(am);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onCallState(OnCallStateParam onCallStateParam) {
        super.onCallState(onCallStateParam);
        SipInstance sipInstance = SipInstance.getInstance();
        try {
            Log.e(getClass().getSimpleName(), "onCallState State: " + getInfo().getState());
            if (pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED.equals(getInfo().getState())) {
                delete();
                sipInstance.sendBroadcast(new Intent(TodosSip.ACTION_STATUS_DISCONNECTED));
            } else if (pjsip_inv_state.PJSIP_INV_STATE_CONNECTING.equals(getInfo().getState())) {
                sipInstance.sendBroadcast(new Intent(TodosSip.ACTION_STATUS_CONNECTING));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

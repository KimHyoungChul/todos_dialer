package com.todosdialer.todosdialer.sip;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AccountSipConfig;
import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.CallSendRequestParam;
import org.pjsip.pjsua2.CodecInfo;
import org.pjsip.pjsua2.CodecInfoVector;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.SipTxOption;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.pj_qos_type;
import org.pjsip.pjsua2.pjsip_status_code;
import org.pjsip.pjsua2.pjsip_transport_type_e;

public class SipInstance {
    private static final long PJMEDIA_ECHO_WEBRTC = 3,
            PJMEDIA_ECHO_USE_NOISE_SUPPRESSOR = 128,
            PJMEDIA_ECHO_AGGRESSIVENESS_AGGRESSIVE = 0x300;

    private static final int PRIORITY_MAX = 130;
    private static final int PRIORITY_DISABLED = 120;
    private static final String PREFIX_CODEC_PCM = "PCM";
    private static final String PREFIX_CODEC_PCMA = "PCMA";
    private static final String PREFIX_CODEC_PCMU = "PCMU";
    private static final String PREFIX_CODEC_G722 = "G722";
    private static final String PREFIX_CODEC_G726 = "G726";

    private static final String ACCOUNT_FORMAT = "sip:%s@%s";

    private static SipInstance mInstance;

    private Context mContext;
    private TodosSipAccount mTodosAccount;

    private Endpoint mEndpoint;
    private String mDomain = "";
    private int mPort = 0;

    private Handler mHandler;
    private pjsip_status_code mOnAccountRegState;

    private TodosSipCall mSipCall = null;

    private SipInstance(Context paramContext) {
        this.mContext = paramContext.getApplicationContext();
        this.mHandler = new Handler(paramContext.getMainLooper());
    }

    public static SipInstance getInstance() {
        return mInstance;
    }

    public static SipInstance getInstance(Context paramContext) {
        if (mInstance == null) {
            mInstance = newInstance(paramContext);
        }
        return mInstance;
    }

    public static SipInstance newInstance(Context paramContext) {
        if (mInstance != null) {
            mInstance.unregisterAccount();
        }
        mInstance = new SipInstance(paramContext);
        return mInstance;
    }

    public void enqueue(Runnable paramRunnable) {
        mHandler.post(paramRunnable);
    }

    TodosSipAccount getTodosAccount() {
        return mTodosAccount;
    }

    private pjsip_status_code getOnAccountRegState() {
        return mOnAccountRegState;
    }

    public TodosSipCall getSipCall() {
        return mSipCall;
    }

    void setOnAccountRegState(pjsip_status_code parampjsip_status_code) {
        mOnAccountRegState = parampjsip_status_code;
    }

    void setSipCall(TodosSipCall todosSipCall) {
        mSipCall = todosSipCall;
    }

    public void initEndpoint() {
        mEndpoint = new Endpoint();
        try {
            mEndpoint.libCreate();

            EpConfig epConfig = new EpConfig();
            epConfig.getUaConfig().setUserAgent("Pjsua2 Android " + mEndpoint.libVersion().getFull());
            epConfig.getMedConfig().setHasIoqueue(true);
            epConfig.getMedConfig().setEcOptions(PJMEDIA_ECHO_WEBRTC |
                    PJMEDIA_ECHO_USE_NOISE_SUPPRESSOR |
                    PJMEDIA_ECHO_AGGRESSIVENESS_AGGRESSIVE);
            epConfig.getMedConfig().setQuality(8);
            epConfig.getMedConfig().setEcTailLen(50);
            epConfig.getMedConfig().setThreadCnt(1);
            mEndpoint.libInit(epConfig);
            mEndpoint.libStart();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setCodecPriority();
    }

    private void setCodecPriority() {
        try {
            CodecInfoVector codecs = mEndpoint.codecEnum();
            if (codecs != null) {
                for (int i = 0; i < ((int) codecs.size()); i++) {
                    CodecInfo codecInfo = codecs.get(i);
                    if (codecInfo.getCodecId().startsWith(PREFIX_CODEC_PCMU)) {
                        mEndpoint.codecSetPriority(codecInfo.getCodecId(), (short) PRIORITY_MAX);
                    } else if (codecInfo.getCodecId().startsWith(PREFIX_CODEC_PCMA)) {
                        mEndpoint.codecSetPriority(codecInfo.getCodecId(), (short) (PRIORITY_MAX));
                    } else if (codecInfo.getCodecId().startsWith(PREFIX_CODEC_G722)) {
                        mEndpoint.codecSetPriority(codecInfo.getCodecId(), (short) (PRIORITY_MAX - 1));
                    } else if (codecInfo.getCodecId().startsWith(PREFIX_CODEC_G726)) {
                        mEndpoint.codecSetPriority(codecInfo.getCodecId(), (short) (PRIORITY_MAX - 1));
                    } else {
                        mEndpoint.codecSetPriority(codecInfo.getCodecId(), (short) PRIORITY_DISABLED);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetEndPoint() {
        if (!android.os.Build.MODEL.startsWith("SM-G920")) {
            return;
        }

        try {
            if (mEndpoint != null) {
                mEndpoint.libDestroy();
                initEndpoint();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerPjsip(String domain, int port, String userId, String pw) throws Exception {
        mDomain = domain;
        mPort = port;

        TransportConfig udpTransport = new TransportConfig();
        udpTransport.setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);
        udpTransport.setPort(mPort);
        try {
            mEndpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, udpTransport);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TransportConfig tcpTransport = new TransportConfig();
        tcpTransport.setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);
        tcpTransport.setPort(mPort);
        try {
            mEndpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, tcpTransport);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String account = String.format(ACCOUNT_FORMAT, userId, mDomain);
        String registrarUri = String.format("sip:%s", mDomain) + ":" + mPort;

        AccountSipConfig accountSipConfig = new AccountSipConfig();
        accountSipConfig.getAuthCreds().add(new AuthCredInfo("digest", "*", userId, 0, pw));

        AccountConfig accountConfig = new AccountConfig();
        accountConfig.getNatConfig().setIceEnabled(true);
        accountConfig.setIdUri(account);
        accountConfig.setSipConfig(accountSipConfig);
        accountConfig.getRegConfig().setRegistrarUri(registrarUri);

        if (mTodosAccount != null) {
            try {
                mTodosAccount.delete();
                mTodosAccount = null;
            } catch (Exception e) {
                e.printStackTrace();
                mTodosAccount = null;
            }
        }
        mTodosAccount = new TodosSipAccount();
        mTodosAccount.create(accountConfig);
        Log.d("SipInstance", "-- registerd");
    }

    public void hangup() {
        if (mSipCall != null && mSipCall.isActive()) {
            CallOpParam callOpParam = new CallOpParam(true);
            callOpParam.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
            try {
                mSipCall.hangup(callOpParam);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d("SipInstance", "-- Action Hangup () : Sip call is null");
    }

    public boolean isAccountAvailable() {
        return pjsip_status_code.PJSIP_SC_OK.equals(getOnAccountRegState());
    }

    public void makeCall(String phoneNumber) throws Exception {
        phoneNumber = phoneNumber.replace("-", "");
        phoneNumber = phoneNumber.replace(" ", "");

        CallOpParam callOpParam = new CallOpParam(true);
        String dstUri = String.format(ACCOUNT_FORMAT, phoneNumber, mDomain) + ":" + mPort;

        TodosSipCall todosSipCall = new TodosSipCall(mTodosAccount);
        todosSipCall.makeCall(dstUri, callOpParam);
        setSipCall(todosSipCall);

        Log.e(getClass().getSimpleName(), "makeCall...");
        Intent intent = new Intent(TodosSip.ACTION_OUTGOING_CALLING);
        intent.putExtra(TodosSip.EXTRA_PHONE_NUMBER, phoneNumber);
        sendBroadcast(intent);
    }

    public void answer() {
        if (getSipCall() == null) {
            Log.d("SipInstance", "-- can't answer ()");
            return;
        }

        try {
            CallOpParam callOpParam = new CallOpParam(true);
            callOpParam.setStatusCode(pjsip_status_code.PJSIP_SC_OK);
            getSipCall().answer(callOpParam);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendBroadcast(final Intent paramIntent) {
        if (this.mContext != null) {
            enqueue(new Runnable() {
                public void run() {
                    SipInstance.this.mContext.sendBroadcast(paramIntent);
                }
            });
            return;
        }
        Log.d("SipInstance", "-- Sip Service is not registerd");
    }

    public void unregisterAccount() {
        if (mSipCall != null) {
            mSipCall.delete();
            mSipCall = null;
        }

        if ((mTodosAccount != null) && (mTodosAccount.isValid())) {
            mTodosAccount.delete();
        }

        mOnAccountRegState = pjsip_status_code.PJSIP_SC_GONE;

        Log.d("SipInstance", "-- unregistered");
    }

    synchronized AudDevManager getAudDevManager() {
        return mEndpoint.audDevManager();
    }

    public void sendDial(String dial) throws Exception {
        CallSendRequestParam prm = new CallSendRequestParam();
        prm.setMethod("INFO");
        SipTxOption txo = new SipTxOption();
        txo.setContentType(" application/dtmf-relay");
        txo.setMsgBody("Signal=" + String.valueOf(dial) + "\n" + "Duration=160");
        prm.setTxOption(txo);
        getSipCall().sendRequest(prm);
    }

    public void destroyEndpoint() {
        mOnAccountRegState = pjsip_status_code.PJSIP_SC_GONE;
        try {
            unregisterAccount();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().gc();

        if (mEndpoint != null) {
            try {
                mEndpoint.libDestroy();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                mEndpoint.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void refreshAccount() {
        if (mTodosAccount != null) {
            try {
                mTodosAccount.setRegistration(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int sendDtmf(int callId, int keyCode) {
        return 0;
    }

    private int sendDtmf(final int callId, String keyPressed) {
        return 0;
    }
}

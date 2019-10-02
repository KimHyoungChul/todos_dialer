package com.todosdialer.todosdialer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.otto.Subscribe;
import com.todosdialer.todosdialer.ChatActivity;
import com.todosdialer.todosdialer.IncomingCallActivity;
import com.todosdialer.todosdialer.OutgoingCallActivity;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.SplashActivity;
import com.todosdialer.todosdialer.TodosApplication;
import com.todosdialer.todosdialer.manager.BusManager;
import com.todosdialer.todosdialer.manager.PushManager;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.manager.SharedPreferenceManager;
import com.todosdialer.todosdialer.model.Friend;
import com.todosdialer.todosdialer.model.Message;
import com.todosdialer.todosdialer.model.SipSessionInfo;
import com.todosdialer.todosdialer.observer.ContactObserver;
import com.todosdialer.todosdialer.sip.SipInstance;
import com.todosdialer.todosdialer.sip.TodosSip;
import com.todosdialer.todosdialer.util.Utils;

import org.pjsip.pjsua2.CallOpParam;

import io.realm.Realm;

public class TodosService extends Service {
    public static final boolean hasToShowLog = false;
    public static final int CALL_NOTI_ONGOING_ID = 927;
    private static final long TERM_WAKE_LOCK_RELEASE = 3 * 1000;
    private static final int FOREGROUND_NOTIFICATION_ID = 95;
    private static final long TERM_CHECKER = 3 * 60 * 1000;

    private TodosReceiver mTodosReceiver = new TodosReceiver();
    private Realm mRealm;
    private RealmManager mRealmManager;
    private SipInstance mSipInstance;

    private ContactObserver mContactObserver;
    private boolean mIsJustInit;
    private Handler mHandler;
    private BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                if (mIsJustInit) {
                    mIsJustInit = false;
                    return;
                }

                if (mRealmManager != null) {
                    mRealmManager.writeLog("Received changing network state action!");
                }

                if (mSipInstance != null) {
                    if (Utils.isNetworkStateFine(getApplicationContext())) {
                        if (mRealmManager != null) {
                            mRealmManager.writeLog("WIFI state is fine, ip: " + Utils.getIp());
                            mRealmManager.writeLog("Call SipInstance.refreshAccount()");
                        }
                        mSipInstance.refreshAccount();

                        if (TodosApplication.onCalling()) {
                            try {
                                mSipInstance.getSipCall().reinvite(new CallOpParam(true));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    };

    private Runnable mChecker = new Runnable() {
        @Override
        public void run() {
            if (mSipInstance != null) {
                if (!mSipInstance.isAccountAvailable()) {
                    if (mRealmManager != null) {
                        mRealmManager.writeLog("[SIP CHECKER] connection lost, ip: " + Utils.getIp());
                        mRealmManager.writeLog("Call SipInstance.refreshAccount()");
                    }
                    mSipInstance.refreshAccount();
                }
            }

            registerChecker();
        }
    };

    public TodosService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mIsJustInit = true;
        mHandler = new Handler();

        Utils.loadNativeLibraries(getApplicationContext());

        BusManager.getInstance().register(this);

        makeForeground("", null);

        mSipInstance = SipInstance.getInstance(this);
        mSipInstance.initEndpoint();

        mRealm = Realm.getDefaultInstance();
        mRealmManager = RealmManager.newInstance();
        mContactObserver = new ContactObserver(getApplicationContext(), new Handler());
        getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, mContactObserver);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(99);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkStateReceiver, intentFilter);

        registerReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        registerReceiver();

        return START_STICKY;
    }

    private void makeForeground(String phoneNumber, Intent intent) {
        String msg = phoneNumber + getString(R.string.msg_calling_with);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification = new NotificationCompat.Builder(getApplicationContext(), PushManager.CHANNEL_SERVICE_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(TextUtils.isEmpty(phoneNumber) ? makeMainIntent(this) : makeCallIntent(this, intent))
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setAutoCancel(false)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(TextUtils.isEmpty(phoneNumber) ? getString(R.string.app_name) : msg)
                    .build();

            startForeground(FOREGROUND_NOTIFICATION_ID, notification);
        } else {
            Notification notification = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(TextUtils.isEmpty(phoneNumber) ? makeMainIntent(this) : makeCallIntent(this, intent))
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setAutoCancel(false)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(TextUtils.isEmpty(phoneNumber) ? getString(R.string.app_name) : msg)
                    .build();

            startForeground(FOREGROUND_NOTIFICATION_ID, notification);
        }
    }

    private static PendingIntent makeMainIntent(Context context) {
        Intent intent = new Intent(context, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent makeCallIntent(Context context, Intent intent) {
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void registerChecker() {
        if (mHandler != null) {
            mHandler.postDelayed(mChecker, TERM_CHECKER);
        }

    }

    private void unregisterChecker() {
        try {
            if (mHandler != null) {
                mHandler.removeCallbacks(mChecker);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onReceiveRequest(Request request) {
        if (Request.REQUEST_REGISTER_SIP.equals(request.request) && request.data != null && (request.data instanceof SipSessionInfo)) {
            Log.i(getClass().getSimpleName(), "REGISTER_SIP");
            SipSessionInfo info = (SipSessionInfo) request.data;
            int port = TextUtils.isEmpty(info.getSipSVRPort()) || !TextUtils.isDigitsOnly(info.getSipSVRPort()) ?
                    0 :
                    Integer.valueOf(info.getSipSVRPort());

            Log.i(getClass().getSimpleName(), "REGISTER_SIP info ip: " + info.getSipSVRIP());
            Log.i(getClass().getSimpleName(), "REGISTER_SIP info port: " + port);
            Log.i(getClass().getSimpleName(), "REGISTER_SIP info id: " + info.getSipID());
            Log.i(getClass().getSimpleName(), "REGISTER_SIP info pw: " + info.getSipPW());

//            SharedPreferenceManager mSP = new SharedPreferenceManager();
//            mSP.setString(getApplicationContext(), "SipID", info.getSipID());

            registerSipServer(info.getSipSVRIP(),
                    port,
                    info.getSipID(),
                    info.getSipPW());
        } else if (Request.REQUEST_UPDATE_FOREGROUND_TO_CALL.equals(request.request) && request.data != null && (request.data instanceof String)) {
            Log.e(getClass().getSimpleName(), "REQUEST_UPDATE_FOREGROUND_TO_CALL, phoneNumber: " + request.intent.getStringExtra(OutgoingCallActivity.EXTRA_KEY_PHONE_NUMBER));
            Log.e(getClass().getSimpleName(), "REQUEST_UPDATE_FOREGROUND_TO_CALL, phoneNumber: " + request.intent.getStringExtra(IncomingCallActivity.EXTRA_KEY_PHONE_NUMBER));
            String phoneNumber = (String) request.data;
            makeForeground(phoneNumber, request.intent);
        } else if (Request.REQUEST_UPDATE_FOREGROUND_TO_MAIN.equals(request.request)) {
            Log.e(getClass().getSimpleName(), "REQUEST_UPDATE_FOREGROUND_TO_MAIN");
            makeForeground("", null);
        }
    }

    private void registerSipServer(String domain, int port, String id, String pw) {
        try {
            if (!TextUtils.isEmpty(domain) &&
                    !TextUtils.isEmpty(id) &&
                    !TextUtils.isEmpty(pw)) {
                String formattedId = id.replace("-", "").replace(" ", "");
                mRealmManager.writeLog("Register account to sip server, current phone ip: " + Utils.getIp());
                if (!mSipInstance.isAccountAvailable()) {
                    mSipInstance.registerPjsip(domain, port, formattedId, pw);
                } else {
                    try {
                        mSipInstance.unregisterAccount();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mSipInstance.registerPjsip(domain, port, formattedId, pw);
                }
            } else {
                BusManager.getInstance().post(new TodosService.Response(Response.RESPONSE_REGISTER_FAIL));
            }
        } catch (Exception e) {
            e.printStackTrace();
            BusManager.getInstance().post(new TodosService.Response(Response.RESPONSE_REGISTER_FAIL));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterChecker();

        SipInstance.getInstance(getApplicationContext()).destroyEndpoint();

        BusManager.getInstance().unregister(this);

        try {
            getContentResolver().unregisterContentObserver(mContactObserver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            unregisterReceiver(mNetworkStateReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        unregisterReceiver();

        mRealm.close();
        mRealmManager = null;

        stopForeground(true);
    }

    private void unregisterReceiver() {
        try {
            unregisterReceiver(mTodosReceiver);
        } catch (Exception e) {
        }
    }

    private void registerReceiver() {
        unregisterReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TodosSip.ACTION_OUTGOING_CALLING);
        intentFilter.addAction(TodosSip.ACTION_INCOMING_CALLING);
        intentFilter.addAction(TodosSip.ACTION_STATUS_CONNECTING);
        intentFilter.addAction(TodosSip.ACTION_STATUS_DISCONNECTED);
        intentFilter.addAction(TodosSip.ACTION_RECEIVE_MESSAGE);
        intentFilter.addAction(TodosSip.ACTION_SEND_MESSAGE);
        intentFilter.addAction(TodosSip.ACTION_REGISTER_SIP);
        intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);

        registerReceiver(this.mTodosReceiver, intentFilter);
    }

    class TodosReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(getClass().getSimpleName(), "Action: " + intent.getAction());
            mRealmManager.writeLog(intent.getAction());
            if (intent.getAction().equals(TodosSip.ACTION_REGISTER_SIP)) {
                handleRegisterSip(intent);
            } else if (intent.getAction().equals(TodosSip.ACTION_SEND_MESSAGE)) {
                handleSendingMsg(intent);
            } else if (intent.getAction().equals(TodosSip.ACTION_RECEIVE_MESSAGE)) {
                handleReceivingMsg(intent);
            } else if (intent.getAction().equals(TodosSip.ACTION_OUTGOING_CALLING)) {
                handleOutgoingCall(intent);
            } else if (intent.getAction().equals(TodosSip.ACTION_INCOMING_CALLING)) {
                handleIncomingCall(intent);
            } else if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
                mSipInstance.resetEndPoint();
            }
        }
    }

    private void handleRegisterSip(Intent intent) {
        int code = intent.getIntExtra(TodosSip.EXTRA_REGISTER_CODE, -1);
        String reason = intent.getStringExtra(TodosSip.EXTRA_REGISTER_REASON);

        Log.i(getClass().getSimpleName(), "handleRegisterSip code: " + code);
        Log.i(getClass().getSimpleName(), "handleRegisterSip reason: " + reason);

        mRealmManager.writeLog("handleRegisterSip code: " + code);
        mRealmManager.writeLog("handleRegisterSip reason: " + reason);
    }

    private void handleSendingMsg(Intent intent) {
        String phoneNumber = intent.getStringExtra(TodosSip.EXTRA_PHONE_NUMBER);
        String messageBody = intent.getStringExtra(TodosSip.EXTRA_MESSAGE_BODY);

        Log.i(getClass().getSimpleName(), "handleSendingMsg phoneNumber: " + phoneNumber);
        Log.i(getClass().getSimpleName(), "handleSendingMsg messageBody: " + messageBody);
    }

    private void handleReceivingMsg(Intent intent) {
        String phoneNumber = intent.getStringExtra(TodosSip.EXTRA_PHONE_NUMBER);
        String messageBody = intent.getStringExtra(TodosSip.EXTRA_MESSAGE_BODY);
        messageBody = convertForUser(messageBody);

        Log.i(getClass().getSimpleName(), "handleReceivingMsg phoneNumber: " + phoneNumber);
        Log.i(getClass().getSimpleName(), "handleReceivingMsg messageBody: " + messageBody);
        Friend friend = mRealmManager.findFriendWithDefault(mRealm, phoneNumber);
        Friend currentChatFriend = ChatActivity.currentChatFriend();
        Log.i(getClass().getSimpleName(), "currentChatFriend != null: " + (currentChatFriend != null));
        int readState = currentChatFriend != null && !currentChatFriend.getNumber().equals(phoneNumber) ?
                Message.READ_STATE_READ :
                Message.READ_STATE_UNREAD;

        Message message = mRealmManager.insertMessage(mRealm,
                SharedPreferenceManager.getString(getApplicationContext(), "SipID"),
                friend,
                messageBody,
                Message.INPUT_STATE_OUT,
                readState,
                Message.SEND_STATE_SUCCESS);
        Log.i(getClass().getSimpleName(), "readState: " + readState);
        if (readState == Message.READ_STATE_READ) {
            mRealmManager.createOrUpdateChatRoom(mRealm, message, 0);
        } else {
            mRealmManager.createOrUpdateChatRoom(mRealm, message);
            int id = 500 + ((int) message.getId());
            PushManager.sendMessageNotification(getApplicationContext(), id, message.getName(), messageBody);
        }

        turnScreenOn();
    }

    private void handleOutgoingCall(Intent intent) {
        String phoneNumber = intent.getStringExtra(TodosSip.EXTRA_PHONE_NUMBER);
        Log.i(getClass().getSimpleName(), "handleOutgoingCall phoneNumber: " + phoneNumber);
    }

    private void handleIncomingCall(Intent intent) {
        String phoneNumber = intent.getStringExtra(TodosSip.EXTRA_PHONE_NUMBER);
        Log.i(getClass().getSimpleName(), "handleIncomingCall phoneNumber: " + phoneNumber);

        turnScreenOn();

        Intent incomingCallIntent = new Intent(getApplicationContext(), IncomingCallActivity.class);
        incomingCallIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        incomingCallIntent.putExtra(IncomingCallActivity.EXTRA_KEY_PHONE_NUMBER, phoneNumber);
        startActivity(incomingCallIntent);
    }

    private void turnScreenOn() {
        if (isScreenOff()) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                try {
                    PowerManager.WakeLock fullWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE, "TtService_full");
                    fullWakeLock.acquire(TERM_WAKE_LOCK_RELEASE);
                } catch (Exception e) {
                    e.printStackTrace();

                    PowerManager.WakeLock partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TtService");
                    partialWakeLock.acquire(TERM_WAKE_LOCK_RELEASE);
                }
            }
        }
    }

    boolean isScreenOff() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return !pm.isScreenOn() || !pm.isInteractive();
        } else {
            return !pm.isScreenOn();
        }
    }

    private String convertForUser(String paramString) {
        String str = TextUtils.isEmpty(paramString) ? "" : paramString;
        if (paramString.contains("Sender:")) {
            str = paramString;
            if (paramString.contains("SMSC:")) {
                str = paramString;
                if (paramString.contains("SCTS:")) {
                    str = paramString.substring(paramString.indexOf("\n", paramString.indexOf("SCTS")) + 2);
                }
            }
        }
        return str.trim();
    }

    public static class Request {
        public static final String REQUEST_REGISTER_SIP = "request_register_sip";
        public static final String REQUEST_UPDATE_FOREGROUND_TO_CALL = "update_foreground_to_call";
        public static final String REQUEST_UPDATE_FOREGROUND_TO_MAIN = "update_foreground_to_main";

        private String request;
        private Object data;
        private String from;
        private Intent intent;

        public Request(String from, String request, Object data) {
            this.from = from;
            this.request = request;
            this.data = data;
        }

        public Request(String from, String request, Object data, Intent intent) {
            this.from = from;
            this.request = request;
            this.data = data;
            this.intent = intent;
        }
    }

    public static class Response {
        public static final String RESPONSE_REGISTER_FAIL = "response_register_fail";
        public String name;

         Response(String response) {
            this.name = response;
        }
    }
}

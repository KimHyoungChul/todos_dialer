package com.todosdialer.todosdialer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.todosdialer.todosdialer.fragment.DialFragment;
import com.todosdialer.todosdialer.manager.PushManager;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.model.CallLog;
import com.todosdialer.todosdialer.model.Friend;
import com.todosdialer.todosdialer.sip.SipInstance;
import com.todosdialer.todosdialer.sip.TodosSip;
import com.todosdialer.todosdialer.worker.ToneWorker;
import com.wang.avi.AVLoadingIndicatorView;

import org.pjsip.pjsua2.OnDtmfDigitParam;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import io.realm.Realm;

import static android.media.AudioManager.STREAM_DTMF;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.media.AudioManager.STREAM_NOTIFICATION;
import static android.media.AudioManager.STREAM_SYSTEM;
import static android.media.AudioManager.STREAM_VOICE_CALL;
import static android.support.v4.app.NotificationCompat.STREAM_DEFAULT;

public class OutgoingCallActivity extends AppCompatActivity implements SensorEventListener, DialFragment.OnClickListener {
    public static final String EXTRA_KEY_PHONE_NUMBER = "OutgoingCallActivity.key_phone_number";

    private TextView mTextName;
    private TextView mTextNumber;
    private TextView mTextTimer;

    private ImageView mImgPhoto;
    private View mContainerBtnCalling;

    private ImageButton mBtnSpeaker;
    private ImageButton mBtnPad;
    private ImageButton mBtnBluetooth;
    private ImageButton mBtnEndCall;
    private AVLoadingIndicatorView mIndicator;

    private SensorManager mSensorManager;
    private PowerManager.WakeLock mProximityWakeLock;
    private float mLastSensorValue = 0;
    private KeyguardManager.KeyguardLock mKeyguardLock;

    private AudioManager mAudioManager;
    private BluetoothAdapter mBluetoothAdapter;

    private Realm mRealm;
    private Friend mFriend;
    private boolean wifiStat;

    private long mCreatedAt;
    private long mStartTime;

    //    private ToneWorker mToneWorker;
    ToneGenerator dtmfGenerator;

    private Handler mHandler = new Handler();
    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long time = (Calendar.getInstance().getTimeInMillis() - mStartTime) / 1000;

            long min = time / 60;
            long sec = time % 60;
            long hour = min / 60;

            String strTime = String.format(Locale.getDefault(), "%02d : %02d : %02d", hour, min, sec);
            mTextTimer.setText(strTime);

            mHandler.postDelayed(mTimerRunnable, 1000L);
        }
    };
    private boolean mNoOngoingNotification = false;
    private BroadcastReceiver mCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(TodosSip.ACTION_STATUS_CONNECTING)) {
                    mIsEnded = false;

                    changeStateToCalling();
                } else if (intent.getAction().equals(TodosSip.ACTION_STATUS_DISCONNECTED)) {
                    mIsEnded = true;

                    mNoOngoingNotification = true;

                    finish();
                }
            }
        }
    };
    private DialFragment mDialFragment;
    private boolean mIsEnded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            TodosApplication.onCalling(true);
            initWindowFlag();

            setContentView(R.layout.activity_outgoing_call);

            mRealm = Realm.getDefaultInstance();
            String phoneNumber = getIntent().getStringExtra(EXTRA_KEY_PHONE_NUMBER);

            if (TextUtils.isEmpty(phoneNumber)) {
                String errorMsg = "[OutgoingCallActivity] Phone number is not valid! Can not calling.";
                RealmManager.newInstance().writeLog(errorMsg);
                finish();
                return;
            }

            setActionbar(getString(R.string.app_name));

            mIndicator = findViewById(R.id.indicator);
            mTextName = findViewById(R.id.text_f_name);
            mTextNumber = findViewById(R.id.text_f_phone_number);
            mImgPhoto = findViewById(R.id.img_f_photo);
            mTextTimer = findViewById(R.id.text_call_timer);
            mTextTimer.setVisibility(View.GONE);

            mContainerBtnCalling = findViewById(R.id.container_btn_calling);
            mBtnSpeaker = findViewById(R.id.btn_speaker);
            mBtnPad = findViewById(R.id.btn_pad);
            mBtnBluetooth = findViewById(R.id.btn_blue_tooth);
            mBtnEndCall = findViewById(R.id.btn_end_call);

            mIndicator.show();
            mContainerBtnCalling.setVisibility(View.INVISIBLE);

            mFriend = RealmManager.newInstance().findFriend(mRealm, phoneNumber);
            if (mFriend == null) {
                mFriend = new Friend();
                mFriend.setName(phoneNumber);
                mFriend.setNumber(phoneNumber);
            }

            //wifi 상태 확인
            registerReceiver(rssiReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));

            initViewByFriend();

            initWakeLock();

            initAudioManager();

            initProximitySensor();

            initListener();

            mCreatedAt = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).getTimeInMillis();

            registerReceiver();

            sendCall();

//            mToneWorker = new ToneWorker(getApplicationContext());
//            mToneWorker.start();

            dtmfGenerator = new ToneGenerator(STREAM_VOICE_CALL, ToneGenerator.MAX_VOLUME);

            //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
            if (savedInstanceState != null) {
                Log.d("OutgoingCallActivity", "savedInstanceState is not null");
                finish();
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private BroadcastReceiver rssiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            WifiManager wman = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wman.getConnectionInfo();

            int _rssi = info.getRssi();
            //Toast.makeText(context, "_rssi = " + _rssi, Toast.LENGTH_LONG).show();
            Log.i("TAG", "_rssi ======================================> " + _rssi);

            if (_rssi < -75) {
                wifiStat = false;
                wman.setWifiEnabled(false);
                Toast.makeText(context, "WIFI 신호가 약해서 3G/Lte로 변경합니다.", Toast.LENGTH_LONG).show();
            }
        }
    };

    private void initWindowFlag() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    private void initViewByFriend() {
        String tmpNumber = "";
        String tmpName = "";
        String area = "";
        String state = "";
        String serial = "";

        if (Pattern.matches("^[0-9]+$", mFriend.getName())) {
            tmpName = mFriend.getName();
            //Log.d("TAG", "==================================>initViewByFriend: " + tmpName.length());
            if (tmpName.length() == 9) {
                area = tmpName.substring(0, 2);
                state = tmpName.substring(2, 5);
                serial = tmpName.substring(5, 9);
                tmpName = area + "-" + state + "-" + serial;
            } else if (tmpName.length() == 10) {
                if (tmpName.startsWith("02")) {
                    area = tmpName.substring(0, 2);
                    state = tmpName.substring(2, 6);
                    serial = tmpName.substring(6, 10);
                    tmpName = area + "-" + state + "-" + serial;
                } else {
                    area = tmpName.substring(0, 3);
                    state = tmpName.substring(3, 6);
                    serial = tmpName.substring(6, 10);
                    tmpName = area + "-" + state + "-" + serial;
                }
            } else if (tmpName.length() == 11) {
                area = tmpName.substring(0, 3);
                state = tmpName.substring(3, 7);
                serial = tmpName.substring(7, 11);
                tmpName = area + "-" + state + "-" + serial;
            }
            mTextName.setText(tmpName);
        } else {
            mTextName.setText(mFriend.getName());
        }

        if (tmpNumber.length() == 8) {
            if (tmpNumber.startsWith("1")) {
                state = tmpNumber.substring(0, 4);
                serial = tmpNumber.substring(4, 8);
                tmpNumber = state + "-" + serial;
            }
        } else if (tmpNumber.length() == 9) {
            if (tmpNumber.startsWith("02")) {
                area = tmpNumber.substring(0, 2);
                state = tmpNumber.substring(2, 5);
                serial = tmpNumber.substring(5, 9);
                tmpNumber = area + "-" + state + "-" + serial;
            }
        } else if (tmpNumber.length() == 10) {
            if (tmpNumber.startsWith("02")) {
                area = tmpNumber.substring(0, 2);
                state = tmpNumber.substring(2, 6);
                serial = tmpNumber.substring(6, 10);
                tmpNumber = area + "-" + state + "-" + serial;
            } else {
                area = tmpNumber.substring(0, 3);
                state = tmpNumber.substring(3, 6);
                serial = tmpNumber.substring(6, 10);
                tmpNumber = area + "-" + state + "-" + serial;
            }
        } else if (tmpNumber.length() == 11) {
            area = tmpNumber.substring(0, 3);
            state = tmpNumber.substring(3, 7);
            serial = tmpNumber.substring(7, 11);
            tmpNumber = area + "-" + state + "-" + serial;
        }
        mTextNumber.setText(tmpNumber);
        //mTextNumber.setText(mFriend.getNumber());
        if (!TextUtils.isEmpty(mFriend.getUriPhoto())) {
            Glide.with(mImgPhoto.getContext())
                    .load(Uri.parse(mFriend.getUriPhoto()))
                    .apply(RequestOptions.centerCropTransform())
                    .into(mImgPhoto);
        } else {
            mImgPhoto.setImageResource(R.drawable.ic_account_circle_48dp);
        }
    }

    private void initAudioManager() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);
            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            mAudioManager.setSpeakerphoneOn(false);

            if (isBluetoothAvailable()) {
                mAudioManager.startBluetoothSco();
                mAudioManager.setBluetoothScoOn(true);

                mBtnBluetooth.setImageResource(R.drawable.ic_bluetooth_24dp);
            } else {
                mBtnBluetooth.setImageResource(R.drawable.ic_bluetooth_white_24dp);
            }
        }
        mBtnBluetooth.setEnabled(isBluetoothAvailable());
    }

    private void initWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (powerManager != null) {
                mProximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, getLocalClassName());
            }
        } else {
            try {
                int proximityScreenOffWakeLock = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
                if (proximityScreenOffWakeLock != 0x0) {
                    if (powerManager != null) {
                        mProximityWakeLock = powerManager.newWakeLock(proximityScreenOffWakeLock, getLocalClassName());
                        mProximityWakeLock.setReferenceCounted(false);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            mKeyguardLock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
            mKeyguardLock.disableKeyguard();
        }
    }

    private void initProximitySensor() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (sensor != null) {
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    private void initListener() {
        mBtnSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeToSpeakMode();
            }
        });

        mBtnPad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialFragment();
            }
        });

        mBtnBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBluetoothAvailable()) {
                    changeToBlueTooth();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.msg_there_is_no_paired_bluetooth, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtnEndCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();

                finish();
            }
        });
    }

    private void changeToSpeakMode() {
        if (mAudioManager != null) {
            if (mAudioManager.isSpeakerphoneOn()) {
                mAudioManager.setSpeakerphoneOn(false);
                mAudioManager.stopBluetoothSco();
                mAudioManager.setBluetoothScoOn(false);

                mBtnSpeaker.setImageResource(R.drawable.ic_call_speaker);
            } else {
                mAudioManager.setSpeakerphoneOn(true);
                mBtnSpeaker.setImageResource(R.drawable.ic_volume_up_24dp);
            }
            mBtnBluetooth.setImageResource(mAudioManager.isBluetoothScoOn() ?
                    R.drawable.ic_bluetooth_24dp :
                    R.drawable.ic_bluetooth_white_24dp);
        }
    }

    private void changeToBlueTooth() {
        if (mAudioManager != null) {
            if (isBluetoothAvailable()) {
                if (mAudioManager.isBluetoothScoOn()) {
                    mAudioManager.setSpeakerphoneOn(false);
                    mAudioManager.stopBluetoothSco();
                    mAudioManager.setBluetoothScoOn(false);

                    mBtnBluetooth.setImageResource(R.drawable.ic_bluetooth_white_24dp);
                } else {
                    mAudioManager.setSpeakerphoneOn(false);
                    mAudioManager.startBluetoothSco();
                    mAudioManager.setBluetoothScoOn(true);

                    mBtnBluetooth.setImageResource(R.drawable.ic_bluetooth_24dp);
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.msg_there_is_no_paired_bluetooth, Toast.LENGTH_SHORT).show();
            }

            mBtnSpeaker.setImageResource(mAudioManager.isSpeakerphoneOn() ?
                    R.drawable.ic_volume_up_24dp :
                    R.drawable.ic_call_speaker);
        }
    }

    private boolean isBluetoothAvailable() {
        return mAudioManager != null && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED && mBluetoothAdapter.getBondedDevices() != null && mBluetoothAdapter.getBondedDevices().size() > 0;
    }

    private void changeStateToCalling() {
        if (mAudioManager != null) {
            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }

        mIndicator.smoothToHide();
        mContainerBtnCalling.setVisibility(View.VISIBLE);
        startTimer();
    }

    private void endCall() {
        if (!mIsEnded) {
            mNoOngoingNotification = true;

            SipInstance.getInstance(getApplicationContext()).hangup();

            stopTimer();
            mIsEnded = true;

            insertCallLog();
        }
    }

    private void startTimer() {
        String strTime = String.format(Locale.getDefault(), "%02d : %02d : %02d", 0, 0, 0);
        mTextTimer.setText(strTime);
        mTextTimer.setVisibility(View.VISIBLE);

        mStartTime = Calendar.getInstance().getTimeInMillis();
        mHandler.postDelayed(mTimerRunnable, 1000L);
    }

    private void stopTimer() {
        try {
            mHandler.removeCallbacks(mTimerRunnable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendCall() {
        mIsEnded = false;

        SipInstance sipInstance = SipInstance.getInstance(getApplicationContext());
        if (sipInstance.isAccountAvailable()) {
            try {
                sipInstance.makeCall(mFriend.getNumber());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), R.string.msg_please_check_account, Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.msg_please_check_account, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void insertCallLog() {
        long callDuration = mStartTime != 0 ? (Calendar.getInstance().getTimeInMillis() - mStartTime) : 0;

        RealmManager.newInstance().insertCallLog(mRealm, mFriend, callDuration, CallLog.STATE_OUTGOING, mCreatedAt);
    }

//    private void generateTone(String dial) {
//        if (mToneWorker != null) {
//            mToneWorker.addDialer(dial);
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        endCall();

        releaseAudioManager();

        unregisterReceiver();

        clearNotification();


        mRealm.close();

        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }

        if (mProximityWakeLock != null) {
            if (mProximityWakeLock.isHeld()) {
                mProximityWakeLock.release();
            }
            mProximityWakeLock = null;
        }
        mKeyguardLock.reenableKeyguard();
        TodosApplication.onCalling(false);

        //wifi 감시 해제
        unregisterReceiver(rssiReceiver);
        //통화 종료 후 wifi 상태가 3G/LTE 라면 wifi로 상태 변경
        if (wifiStat == false) {
            @SuppressLint("WifiManagerLeak") WifiManager wman = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wman.setWifiEnabled(true);
            wifiStat = true;
        }

        if (dtmfGenerator != null) {
            dtmfGenerator.release();
        }

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            Log.i(getClass().getSimpleName(), "sensorEvent.values[0]: " + sensorEvent.values[0]);
            if (sensorEvent.values[0] <= mLastSensorValue) {
                if (mProximityWakeLock != null && !mProximityWakeLock.isHeld()) {
                    mProximityWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
                }
            } else {
                if (mProximityWakeLock != null && mProximityWakeLock.isHeld()) {
                    mProximityWakeLock.release();
                }
            }
            mLastSensorValue = sensorEvent.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onBackPressed() {
        Log.i(getClass().getSimpleName(), "moveTaskToBack!");

        removeDialFragment();

        moveTaskToBack(true);
    }

    private void clearNotification() {
        PushManager.clearOngoingCall(OutgoingCallActivity.this);
    }

    private void makeOngoingNotification() {
        if (mNoOngoingNotification) {
            return;
        }

        Intent intent = new Intent(getApplicationContext(), OutgoingCallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(OutgoingCallActivity.EXTRA_KEY_PHONE_NUMBER, mFriend.getNumber());

        String msg = mFriend.getName() + getString(R.string.msg_calling_with);

        PushManager.sendOngoingCallPush(OutgoingCallActivity.this, intent, mFriend.getNumber(), msg);
    }

    @Override
    protected void onResume() {
        clearNotification();
        super.onResume();
    }

    @Override
    protected void onPause() {
        makeOngoingNotification();
        super.onPause();
    }

    private void unregisterReceiver() {
        try {
            unregisterReceiver(mCallReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TodosSip.ACTION_STATUS_CONNECTING);
        intentFilter.addAction(TodosSip.ACTION_STATUS_DISCONNECTED);
        registerReceiver(this.mCallReceiver, intentFilter);
    }

    private void showDialFragment() {
        mDialFragment = DialFragment.newInstance();
        mDialFragment.setOnDialClickListener(this);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.anim_fragment_enter, R.anim.anim_fragment_exit, R.anim.anim_fragment_enter, R.anim.anim_fragment_exit);
        transaction.add(R.id.pad_fragment_container, mDialFragment);
        transaction.commit();
    }

    @Override
    public void onDialClicked(String dial) {
        Log.i(getClass().getSimpleName(), "Send dial: " + dial);
        try {
            SipInstance.getInstance(getApplicationContext()).sendDial(dial);
//            generateTone(dial);
//            playToneGenerator(dial);
//            SipInstance.getInstance().getSipCall().dialDtmf(dial);
//            SipServiceCommand.sendDTMF(
//                    OutgoingCallActivity.this,
//                    SipInstance.getInstance(getApplicationContext()).getSipCall().getInfo().getLocalUri(), //account id
//                    SipInstance.getInstance().getSipCall().getId(),  //call id
//                    dial);
        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace();
        }
    }

    private void playToneGenerator(String dial) {
        int duration = 1000;


        if (dial.equals("0")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_0, duration);
        } else if (dial.equals("1")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_1, duration);
        } else if (dial.equals("2")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_2, duration);
        } else if (dial.equals("3")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_3, duration);
        } else if (dial.equals("4")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_4, duration);
        } else if (dial.equals("5")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_5, duration);
        } else if (dial.equals("6")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_6, duration);
        } else if (dial.equals("7")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_7, duration);
        } else if (dial.equals("8")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_8, duration);
        } else if (dial.equals("9")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_9, duration);
        } else if (dial.equals("*")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_S, duration); //TONE_DTMF_S for key *
        } else if (dial.equals("#")) {
            dtmfGenerator.startTone(ToneGenerator.TONE_DTMF_P, duration); //TONE_DTMF_P for key #
        } else {
//            dtmfGenerator.startTone(ToneGenerator.TONE_UNKNOWN, duration);
        }

        try {
            Thread.sleep(160);
            dtmfGenerator.stopTone();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onFinishClicked() {
        removeDialFragment();
    }


    private void removeDialFragment() {
        if (mDialFragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.anim_fragment_enter, R.anim.anim_fragment_exit, R.anim.anim_fragment_enter, R.anim.anim_fragment_exit);
            transaction.remove(mDialFragment);
            transaction.commit();
        }
    }

    private void releaseAudioManager() {
        if (mAudioManager != null) {
            if (mAudioManager.isSpeakerphoneOn()) {
                mAudioManager.setSpeakerphoneOn(false);
            }

            mAudioManager.setMode(AudioManager.MODE_NORMAL);
            mAudioManager = null;
        }
    }

    private void setActionbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setContentInsetsAbsolute(0, 0); //좌우 여백 제거
        setSupportActionBar(toolbar);

        try {
            // Get the ActionBar here to configure the way it behaves.
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
//            actionBar.setHomeAsUpIndicator(R.drawable.arrow_left);
//            actionBar.setHomeButtonEnabled(true);

            TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
            toolbarTitle.setText(title);


            findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    endCall();

                    onBackPressed();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: { //for toolbar back button
                endCall();

                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

}

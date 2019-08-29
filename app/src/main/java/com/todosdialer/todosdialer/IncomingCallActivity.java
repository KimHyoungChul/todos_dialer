package com.todosdialer.todosdialer;

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
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
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

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import io.realm.Realm;

public class IncomingCallActivity extends AppCompatActivity implements SensorEventListener, DialFragment.OnClickListener {
    public static final String EXTRA_KEY_PHONE_NUMBER = "IncomingCallActivity.key_phone_number";

    private TextView mTextName;
    private TextView mTextNumber;
    private TextView mTextTimer;

    private ImageView mImgPhoto;
    private View mContainerBtnCalling;
    private View mContainerWaiting;

    private ImageButton mBtnSpeaker;
    private ImageButton mBtnPad;
    private ImageButton mBtnBluetooth;
    private ImageButton mBtnEndCall;
    private ImageView mPanelLeft;
    private ImageView mPanelRight;

    private SensorManager mSensorManager;
    private PowerManager.WakeLock mProximityWakeLock;
    private float mLastSensorValue = 0;
    private KeyguardManager.KeyguardLock mKeyguardLock;

    private AudioManager mAudioManager;
    private BluetoothAdapter mBluetoothAdapter;
    public static boolean bellFlag = true;
    private MediaPlayer mAudio = null;
    private Vibrator vibrator = null;
    private Ringtone bell = null;
    private RingtoneManager bellManager = null;
    boolean isPlay = false;
    private ToneGenerator ringbackPlayer = null;
    public boolean endFlag = false;

    private Realm mRealm;
    private Friend mFriend;

    private long mCreatedAt;
    private long mStartTime;
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
    private int mCallState = CallLog.STATE_MISS;
    private DialFragment mDialFragment;
    private boolean mIsEnded = false;

    private BroadcastReceiver mCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(TodosSip.ACTION_STATUS_CONNECTING)) {
                    ringOff();

                    mIsEnded = false;

                    changeStateToCalling();
                } else if (intent.getAction().equals(TodosSip.ACTION_STATUS_DISCONNECTED)) {
                    ringOff();

                    mIsEnded = true;

                    mNoOngoingNotification = true;

                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TodosApplication.onCalling(true);
        initWindowFlag();
        setContentView(R.layout.activity_incoming_call);
        mRealm = Realm.getDefaultInstance();
        String phoneNumber = getIntent().getStringExtra(EXTRA_KEY_PHONE_NUMBER);

        if (TextUtils.isEmpty(phoneNumber)) {
            String errorMsg = "[IncomingCallActivity] Phone number is not valid! Can not calling.";
            RealmManager.newInstance().writeLog(errorMsg);
            finish();
            return;
        }

        setActionbar(getString(R.string.app_name));

        mTextName = findViewById(R.id.text_f_name);
        mTextNumber = findViewById(R.id.text_f_phone_number);
        mImgPhoto = findViewById(R.id.img_f_photo);
        mTextTimer = findViewById(R.id.text_call_timer);

        mContainerBtnCalling = findViewById(R.id.container_btn_calling);
        mContainerWaiting = findViewById(R.id.container_waiting);

        mBtnSpeaker = findViewById(R.id.btn_speaker);
        mBtnPad = findViewById(R.id.btn_pad);
        mBtnBluetooth = findViewById(R.id.btn_blue_tooth);
        mBtnEndCall = findViewById(R.id.btn_end_call);

        mPanelLeft = findViewById(R.id.panel_left);
        mPanelRight = findViewById(R.id.panel_right);

        mContainerBtnCalling.setVisibility(View.GONE);
        mContainerWaiting.setVisibility(View.VISIBLE);

        phoneNumber.replace("-","");
        Log.d("TAG", "onCreate: " + phoneNumber);
        mFriend = RealmManager.newInstance().findFriend(mRealm, phoneNumber);
        if (mFriend == null) {
            mFriend = new Friend();
            mFriend.setName(phoneNumber);
            mFriend.setNumber(phoneNumber);
        }

        initViewByFriend();

        initWakeLock();

        initAudioManager();

        initProximitySensor();

        initListener();

        registerReceiver();

        mCallState = CallLog.STATE_MISS;
        mCreatedAt = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).getTimeInMillis();

        bellPlay();

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("IncommingCallActivity","savedInstanceState is not null");
            finish();
            return;
        }
    }

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

        if(Pattern.matches("^[0-9]+$", mFriend.getName())){
            tmpName = mFriend.getName();
            if(tmpName.length() == 9){
                area = tmpName.substring(0,2);
                state = tmpName.substring(2,5);
                serial = tmpName.substring(5,9);
                tmpName = area + "-" + state + "-" + serial;
            }else if(tmpName.length() == 10){
                if(tmpName.startsWith("02")){
                    area = tmpName.substring(0,2);
                    state = tmpName.substring(2,6);
                    serial = tmpName.substring(6,10);
                    tmpName = area + "-" + state + "-" + serial;
                }else{
                    area = tmpName.substring(0,3);
                    state = tmpName.substring(3,6);
                    serial = tmpName.substring(6,10);
                    tmpName = area + "-" + state + "-" + serial;
                }
            } else if (tmpName.length() == 11) {
                area = tmpName.substring(0,3);
                state = tmpName.substring(3,7);
                serial = tmpName.substring(7,11);
                tmpName = area + "-" + state + "-" + serial;
            }
            mTextName.setText(tmpName);
        }else {
            mTextName.setText(mFriend.getName());
        }

        if(Pattern.matches("^[0-9]+$", mFriend.getName())) {
            tmpNumber = mFriend.getNumber();
            if (tmpNumber.length() == 9) {
                area = tmpNumber.substring(0, 2);
                state = tmpNumber.substring(2, 5);
                serial = tmpNumber.substring(5, 9);
                tmpNumber = area + "-" + state + "-" + serial;
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
        }else {
            mTextNumber.setText(mFriend.getNumber());
        }
        mTextNumber.setText(tmpNumber);
        //mTextNumber.setText(mFriend.getNumber());
        if (!TextUtils.isEmpty(mFriend.getUriPhoto())) {
            Glide.with(mImgPhoto.getContext())
                    .load(Uri.parse(mFriend.getUriPhoto()))
                    .apply(RequestOptions.centerCropTransform())
                    .into(mImgPhoto);
        } else {
            mImgPhoto.setImageResource(R.drawable.ic_account_circle_white_24dp);
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
                ringOff();

                endCall();

                finish();
            }
        });

        mPanelLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ringOff();

                SipInstance.getInstance(getApplicationContext()).answer();
            }
        });

        mPanelRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallState = CallLog.STATE_INCOMING;

                ringOff();

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
        if (mAudioManager != null) {
            if (mBluetoothAdapter != null &&
                    mBluetoothAdapter.isEnabled() &&
                    mBluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED &&
                    mBluetoothAdapter.getBondedDevices() != null &&
                    mBluetoothAdapter.getBondedDevices().size() > 0) {
                return true;
            }
        }
        return false;
    }

    private void changeStateToCalling() {
        if (mAudioManager != null) {
            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }

        mCallState = CallLog.STATE_INCOMING;
        mContainerBtnCalling.setVisibility(View.VISIBLE);
        mContainerWaiting.setVisibility(View.GONE);
        mTextTimer.setVisibility(View.VISIBLE);

        startTimer();
    }

    private void endCall() {
        if (!mIsEnded) {
            mCallState = CallLog.STATE_INCOMING;

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

    private void insertCallLog() {
        long callDuration = mStartTime != 0 ? (Calendar.getInstance().getTimeInMillis() - mStartTime) : 0;

        CallLog callLog = RealmManager.newInstance().insertCallLog(mRealm,
                mFriend,
                callDuration,
                mCallState,
                mCreatedAt,
                mCallState != CallLog.STATE_MISS);

        if (mCallState == CallLog.STATE_MISS) {
            int id = ((int) callLog.getId());
            String message = callLog.getName() + " " + getString(R.string.msg_call_from);
            PushManager.sendMissCallNotification(getApplicationContext(), id, callLog.getName(), message);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        endCall();

        releaseAudioManager();

        unregisterReceiver();

        clearNotification();

        insertCallLog();

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
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
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

        moveTaskToBack(true);
    }


    private void clearNotification() {
        PushManager.clearOngoingCall(IncomingCallActivity.this);
    }

    private void makeOngoingNotification() {
        if (mNoOngoingNotification) {
            return;
        }
        Intent intent = new Intent(getApplicationContext(), IncomingCallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(IncomingCallActivity.EXTRA_KEY_PHONE_NUMBER, mFriend.getNumber());

        String msg = mFriend.getName() + getString(R.string.msg_calling_with);

        PushManager.sendOngoingCallPush(IncomingCallActivity.this, intent, mFriend.getNumber(), msg);
    }


    @Override
    protected void onResume() {
        clearNotification();
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

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
        } catch (Exception e) {
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
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }

        if (!endFlag) {
            try {
                if (ringbackPlayer == null) {
                    ringbackPlayer = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, ToneGenerator.MAX_VOLUME / 2);
                }
                ringbackPlayer.startTone(ToneGenerator.TONE_PROP_PROMPT, 100);
            } catch (Exception ignored) {
            }
        }

        if (mAudioManager != null) {
            if (mAudioManager.isSpeakerphoneOn()) {
                mAudioManager.setSpeakerphoneOn(false);
            }

            mAudioManager.setMode(AudioManager.MODE_NORMAL);
            mAudioManager = null;
        }
    }

    public void bellPlay() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        if (!isPlay) {
            isPlay = true;
            boolean bellFlag;

            try {
                long[] pattern = {1200, 3 * 1000};
                switch (mAudioManager.getRingerMode()) {
                    case AudioManager.RINGER_MODE_SILENT:
                        break;

                    case AudioManager.RINGER_MODE_VIBRATE:
                        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        if (vibrator != null) {
                            vibrator.vibrate(pattern, 0);
                        }
                        break;

                    case AudioManager.RINGER_MODE_NORMAL:
                        try {
                            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                            if (mAudio == null) {
                                mAudio = new MediaPlayer();
                                mAudio.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        mp.reset();
                                        mp.start();
                                    }
                                });
                                mAudio.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                                    @Override
                                    public boolean onError(MediaPlayer mp, int what, int extra) {
                                        mp.release();
                                        return false;
                                    }
                                });

                            }
                            mAudio.setDataSource(this, alert);

                            if (mAudioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
                                mAudio.setAudioStreamType(AudioManager.STREAM_RING);
                                mAudio.setLooping(true);
                                mAudio.prepare();
                                mAudio.start();
                            }

                            bellFlag = false;
                        } catch (Exception e) {
                            bellFlag = true;
                        }


                        if (bellFlag) {
                            if (mAudio == null)
                                mAudio = new MediaPlayer();

                            if (bellManager == null)
                                bellManager = new RingtoneManager(this);

                            if (mAudioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
                                bell = RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
                                bell.setStreamType(AudioManager.STREAM_RING);
                                bell.play();
                            }
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void ringOff() {
        try {
            bellFlag = false;
            if (mAudio != null) {
                try {
                    mAudio.stop();
                    mAudio.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mAudio = null;
            }


            if (bell != null) {
                bell.stop();
                bell = null;
            }

            if (bellManager != null) {
                bellManager.stopPreviousRingtone();
                bellManager = null;
            }

            if (vibrator != null) {
                vibrator.cancel();
                vibrator = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
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

}

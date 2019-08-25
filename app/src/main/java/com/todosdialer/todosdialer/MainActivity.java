package com.todosdialer.todosdialer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;
import com.todosdialer.todosdialer.fragment.MainCallLogFragment;
import com.todosdialer.todosdialer.fragment.MainChatRoomFragment;
import com.todosdialer.todosdialer.fragment.MainContactFragment;
import com.todosdialer.todosdialer.fragment.MainPadFragment;
import com.todosdialer.todosdialer.manager.BusManager;
import com.todosdialer.todosdialer.manager.PushManager;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.CallLog;
import com.todosdialer.todosdialer.model.Friend;
import com.todosdialer.todosdialer.service.TodosService;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String EXTRA_KEY_TAB = "extra_key_tab";
    private static final int TAB_DIAL_PAD = 0;
    public static final int TAB_CALL_LOG = 1;
    public static final int TAB_CONTACT = 2;
    public static final int TAB_SMS = 3;

    private DrawerLayout mDrawer;

    private ImageButton mBtnPad;
    private ImageButton mBtnLog;
    private TextView mTextUncheckedMissedCall;
    private TextView mTextUnreadMessage;
    private ImageButton mBtnContact;
    private ImageButton mBtnMessage;
    private RealmResults<Friend> mFriends;

    private RealmManager mRealmManager;
    private Realm mRealm;
    private RealmResults<CallLog> mCallLogResults;
    private RealmResults<CallLog> mMissedCallLogResults;

    private RealmChangeListener<Realm> mRealmListener = new RealmChangeListener<Realm>() {
        @Override
        public void onChange(@NonNull Realm realm) {

            refreshMainCallLogFragment();

            setUncheckedSizeText();
        }
    };

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BusManager.getInstance().register(this);
        setContentView(R.layout.activity_main_header);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mDrawer = findViewById(R.id.drawer_layout);

        mBtnPad = findViewById(R.id.btn_pad);
        mBtnLog = findViewById(R.id.btn_call_log);
        mTextUncheckedMissedCall = findViewById(R.id.text_unchecked_missed_call);
        mTextUnreadMessage = findViewById(R.id.text_unread_message);
        mBtnContact = findViewById(R.id.btn_contact);
        mBtnMessage = findViewById(R.id.btn_message);

        mRealm = Realm.getDefaultInstance();
        mRealmManager = RealmManager.newInstance();
        mFriends = mRealmManager.loadFriends(mRealm);
        mCallLogResults = mRealmManager.loadCallLogs(mRealm);
        mMissedCallLogResults = mRealmManager.loadMissedCallLogs(mRealm);

        initListeners();

        setUncheckedSizeText();

        mRealm.addChangeListener(mRealmListener);

        PushManager.clearAll(getApplicationContext());

        switchView(getIntent().getIntExtra(EXTRA_KEY_TAB, 0));

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("MainActivity","savedInstanceState is not null");
            finish();
            return;
        }
    }

    private void initListeners() {
        findViewById(R.id.btn_drawer_toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mDrawer.isDrawerOpen(GravityCompat.START)) {
                    mDrawer.openDrawer(GravityCompat.START);
                }
            }
        });
        mBtnPad.setOnClickListener(this);
        mBtnLog.setOnClickListener(this);
        mBtnContact.setOnClickListener(this);
        mBtnMessage.setOnClickListener(this);

        findViewById(R.id.btn_user_info).setOnClickListener(this);

        findViewById(R.id.btn_reservation).setOnClickListener(this);

        findViewById(R.id.btn_reservation_list).setOnClickListener(this);

        findViewById(R.id.btn_log_out).setOnClickListener(this);

        findViewById(R.id.btn_infomation).setOnClickListener(this);

        TextView btnLog = findViewById(R.id.btn_log);
        String logName = "로그보기 [" + Utils.getAppVersionName(getApplicationContext()) + "]";
        btnLog.setText(logName);
        btnLog.setVisibility(TodosService.hasToShowLog ? View.VISIBLE : View.GONE);
        btnLog.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        mDrawer.closeDrawer(GravityCompat.START);

        switch (view.getId()) {
            case R.id.btn_pad:
                switchView(TAB_DIAL_PAD);
                break;
            case R.id.btn_call_log:
                switchView(TAB_CALL_LOG);
                break;
            case R.id.btn_contact:
                switchView(TAB_CONTACT);
                break;
            case R.id.btn_message:
                switchView(TAB_SMS);
                break;

            case R.id.btn_user_info:
                startActivity(new Intent(this, UserInfoActivity.class));
                break;
            case R.id.btn_reservation:
                showReservationPage();
                break;
            case R.id.btn_reservation_list:
                startActivity(new Intent(this, ReservationInfoActivity.class));
                break;

            case R.id.btn_log_out:
                logOutWithRestartApp();
                break;
            case R.id.btn_log:
                startActivity(new Intent(this, LogActivity.class));
                break;
                /*10.16 정보안내 버튼 추가 */
            case R.id.btn_infomation:
                startInfo();
                break;

        }
    }

    private void showReservationPage() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(RetrofitManager.PRODUCT_URL));
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), R.string.msg_there_is_no_available_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void startInfo() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(RetrofitManager.INFOMATION));
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), R.string.msg_there_is_no_available_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void switchView(int tab) {
        switch (tab) {
            case TAB_DIAL_PAD:
                changeTabIndicator(TAB_DIAL_PAD);
                replaceFragment(MainPadFragment.newInstance(mFriends, mCallLogResults.size() > 0 ? mCallLogResults.first() : null));
                break;
            case TAB_CALL_LOG:
                changeTabIndicator(TAB_CALL_LOG);
                replaceFragment(makeMainCallLogFragment());
                break;
            case TAB_CONTACT:
                changeTabIndicator(TAB_CONTACT);
                replaceFragment(MainContactFragment.newInstance(mFriends));
                break;
            case TAB_SMS:
                changeTabIndicator(TAB_SMS);
                replaceFragment(MainChatRoomFragment.newInstance());
                break;

            default:
                changeTabIndicator(TAB_DIAL_PAD);
                replaceFragment(MainPadFragment.newInstance(mFriends, mCallLogResults.size() > 0 ? mCallLogResults.first() : null));
                break;
        }
    }

    private void refreshMainCallLogFragment() {
        if (mCallLogResults == null || mMissedCallLogResults == null) {
            return;
        }

        Fragment fragmentByTag = getSupportFragmentManager().findFragmentByTag(MainCallLogFragment.class.getSimpleName());
        if (fragmentByTag != null &&
                fragmentByTag.isAdded() &&
                fragmentByTag.isVisible() &&
                (fragmentByTag instanceof MainCallLogFragment)) {
            MainCallLogFragment fragment = (MainCallLogFragment) fragmentByTag;

            ArrayList<CallLog> callLogs = new ArrayList<>();
            for (CallLog log : mCallLogResults) {
                callLogs.add(mRealm.copyFromRealm(log));
            }

            ArrayList<CallLog> missedLogs = new ArrayList<>();
            for (CallLog log : mMissedCallLogResults) {
                missedLogs.add(mRealm.copyFromRealm(log));
            }

            fragment.setCallLogList(callLogs);
            fragment.setMissedCallLogList(missedLogs);
        }
    }

    private MainCallLogFragment makeMainCallLogFragment() {
        ArrayList<CallLog> callLogs = new ArrayList<>();
        for (CallLog log : mCallLogResults) {
            callLogs.add(mRealm.copyFromRealm(log));
        }

        ArrayList<CallLog> missedLogs = new ArrayList<>();
        for (CallLog log : mMissedCallLogResults) {
            missedLogs.add(mRealm.copyFromRealm(log));
        }
        return MainCallLogFragment.newInstance(callLogs, missedLogs);
    }

    private void setUncheckedSizeText() {
        if (mTextUncheckedMissedCall != null) {
            int uncheckedSize = mRealmManager.countUncheckedCallLogs(mMissedCallLogResults);
            if (uncheckedSize == 0) {
                mTextUncheckedMissedCall.setVisibility(View.GONE);
            } else if (uncheckedSize > 99) {
                mTextUncheckedMissedCall.setVisibility(View.VISIBLE);
                String sizeText = "99+";
                mTextUncheckedMissedCall.setText(sizeText);
            } else {
                mTextUncheckedMissedCall.setVisibility(View.VISIBLE);
                mTextUncheckedMissedCall.setText(String.valueOf(uncheckedSize));
            }
        }

        if (mTextUnreadMessage != null) {
            long unreadCount = mRealmManager.countAllUnreadMessageCount(mRealm);
            if (unreadCount == 0) {
                mTextUnreadMessage.setVisibility(View.GONE);
            } else if (unreadCount > 99) {
                mTextUnreadMessage.setVisibility(View.VISIBLE);
                String sizeText = "99+";
                mTextUnreadMessage.setText(sizeText);
            } else {
                mTextUnreadMessage.setVisibility(View.VISIBLE);
                mTextUnreadMessage.setText(String.valueOf(unreadCount));
            }
        }
    }

    private void changeTabIndicator(int position) {
        mBtnPad.setImageResource(R.drawable.ic_dialpad);
        mBtnLog.setImageResource(R.drawable.ic_query_builder);
        mBtnContact.setImageResource(R.drawable.ic_contact_phone);
        mBtnMessage.setImageResource(R.drawable.ic_textsms);

        switch (position) {
            case 0:
                mBtnPad.setImageResource(R.drawable.ic_dialpad_white_24dp);
                break;

            case 1:
                mBtnLog.setImageResource(R.drawable.ic_query_builder_white_24dp);
                break;

            case 2:
                mBtnContact.setImageResource(R.drawable.ic_contact_phone_white_24dp);
                break;

            case 3:
                mBtnMessage.setImageResource(R.drawable.ic_textsms_white_24dp);
                break;
        }
    }

    private void logOutWithRestartApp() {
        mRealmManager.deleteUser(mRealm);

        stopService(new Intent(getApplicationContext(), TodosService.class));

        finish();

        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_content, fragment, fragment.getClass().getSimpleName());
        fragmentTransaction.commit();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onReceiveResponse(TodosService.Response response) {
        if (TodosService.Response.RESPONSE_REGISTER_FAIL.equals(response.name)) {
            Toast.makeText(getApplicationContext(), R.string.msg_fail_to_register_sip, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BusManager.getInstance().unregister(this);
        mRealm.removeChangeListener(mRealmListener);
        mRealm.close();
    }
}

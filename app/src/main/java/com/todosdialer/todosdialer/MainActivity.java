package com.todosdialer.todosdialer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
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
    public static final int TAB_CONTACT = 1;
    public static final int TAB_CALL_LOG = 2;
    public static final int TAB_SMS = 3;

    private DrawerLayout mDrawer;

    //    private ImageButton mBtnPad;
//    private ImageButton mBtnLog;
    private RelativeLayout mBtnPad;
    private RelativeLayout mBtnLog;
    private TextView mTextUncheckedMissedCall;
    private TextView mTextUnreadMessage;
    private RelativeLayout mBtnContact;
    private RelativeLayout mBtnMessage;
    private RealmResults<Friend> mFriends;

    private RealmManager mRealmManager;
    private Realm mRealm;
    private RealmResults<CallLog> mCallLogResults;
    private RealmResults<CallLog> mMissedCallLogResults;

    private View tabUnderLine01;
    private View tabUnderLine02;
    private View tabUnderLine03;
    private View tabUnderLine04;

//    TabLayout tabLayout;

    ViewPager mViewPager;

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

        tabUnderLine01 = findViewById(R.id.tab_under_dot_01);
        tabUnderLine02 = findViewById(R.id.tab_under_dot_02);
        tabUnderLine03 = findViewById(R.id.tab_under_dot_03);
        tabUnderLine04 = findViewById(R.id.tab_under_dot_04);

        mRealm = Realm.getDefaultInstance();
        mRealmManager = RealmManager.newInstance();
        mFriends = mRealmManager.loadFriends(mRealm);
        mCallLogResults = mRealmManager.loadCallLogs(mRealm);
        mMissedCallLogResults = mRealmManager.loadMissedCallLogs(mRealm);

        initListeners();

        setUncheckedSizeText();

        mRealm.addChangeListener(mRealmListener);

        PushManager.clearAll(getApplicationContext());


        mBtnPad.setTag(TAB_DIAL_PAD);
        mBtnContact.setTag(TAB_CONTACT);
        mBtnLog.setTag(TAB_CALL_LOG);
        mBtnMessage.setTag(TAB_SMS);


        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(new pagerAdapter(getSupportFragmentManager()));
        mViewPager.setCurrentItem(TAB_DIAL_PAD);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        tabUnderLine01.setVisibility(View.VISIBLE);
                        tabUnderLine02.setVisibility(View.INVISIBLE);
                        tabUnderLine03.setVisibility(View.INVISIBLE);
                        tabUnderLine04.setVisibility(View.INVISIBLE);

                        mBtnPad.setSelected(true);
                        mBtnContact.setSelected(false);
                        mBtnLog.setSelected(false);
                        mBtnMessage.setSelected(false);
                        break;
                    case 1:
                        tabUnderLine01.setVisibility(View.INVISIBLE);
                        tabUnderLine02.setVisibility(View.VISIBLE);
                        tabUnderLine03.setVisibility(View.INVISIBLE);
                        tabUnderLine04.setVisibility(View.INVISIBLE);

                        mBtnPad.setSelected(false);
                        mBtnContact.setSelected(true);
                        mBtnLog.setSelected(false);
                        mBtnMessage.setSelected(false);
                        break;
                    case 2:
                        tabUnderLine01.setVisibility(View.INVISIBLE);
                        tabUnderLine02.setVisibility(View.INVISIBLE);
                        tabUnderLine03.setVisibility(View.VISIBLE);
                        tabUnderLine04.setVisibility(View.INVISIBLE);

                        mBtnPad.setSelected(false);
                        mBtnContact.setSelected(false);
                        mBtnLog.setSelected(true);
                        mBtnMessage.setSelected(false);

                        mViewPager.getAdapter().notifyDataSetChanged();
                        break;
                    case 3:
                        tabUnderLine01.setVisibility(View.INVISIBLE);
                        tabUnderLine02.setVisibility(View.INVISIBLE);
                        tabUnderLine03.setVisibility(View.INVISIBLE);
                        tabUnderLine04.setVisibility(View.VISIBLE);

                        mBtnPad.setSelected(false);
                        mBtnContact.setSelected(false);
                        mBtnLog.setSelected(false);
                        mBtnMessage.setSelected(true);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //노티피케이션바 항목 클릭해서 접근하는 경우
        if(getIntent() != null) {
            switchView(getIntent().getIntExtra(EXTRA_KEY_TAB, 0));
            mViewPager.setCurrentItem(getIntent().getIntExtra(EXTRA_KEY_TAB, 0));
        }


//        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
//        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_dial_pad_selector));
//        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_contact_selector));
//        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_call_log_selector));
//        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_sms_selector));
//        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
//
//        for(int i=0; i < tabLayout.getTabCount(); i++) {
//            View tab = ((ViewGroup) tabLayout.getChildAt(0)).getChildAt(i);
//            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) tab.getLayoutParams();
//            p.setMargins(0, 0, 50, 0);
//            tab.requestLayout();
//        }

//        final ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
//        final PagerAdapter adapter = new pagerAdapter(getSupportFragmentManager());
//        viewPager.setAdapter(adapter);
//
//        mBtnPad.setSelected(true);

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("MainActivity", "savedInstanceState is not null");
            finish();
            return;
        }

    }

    View.OnClickListener movePageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int tag = (int) v.getTag();
            mViewPager.setCurrentItem(tag);
        }
    };

    private class pagerAdapter extends FragmentStatePagerAdapter {
        public pagerAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            switch (position) {
                case TAB_DIAL_PAD:
                    return MainPadFragment.newInstance(mFriends, mCallLogResults.size() > 0 ? mCallLogResults.first() : null);
                case TAB_CALL_LOG:
                    return makeMainCallLogFragment();
                case TAB_CONTACT:
                    return MainContactFragment.newInstance(mFriends);
                case TAB_SMS:
                    return MainChatRoomFragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public int getItemPosition(Object object) {
            if (object instanceof MainCallLogFragment) {
                return POSITION_NONE;
            } else {
                return super.getItemPosition(object);
            }
        }
    }


    private void initListeners() {
        findViewById(R.id.btn_menu).setOnClickListener(new View.OnClickListener() {
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
//        mDrawer.closeDrawer(GravityCompat.START);

        switch (view.getId()) {
            case R.id.btn_pad:
                switchView(TAB_DIAL_PAD);
                mViewPager.setCurrentItem(TAB_DIAL_PAD);
                break;
            case R.id.btn_call_log:
                switchView(TAB_CALL_LOG);
                mViewPager.setCurrentItem(TAB_CALL_LOG);
                break;
            case R.id.btn_contact:
                switchView(TAB_CONTACT);
                mViewPager.setCurrentItem(TAB_CONTACT);
                break;
            case R.id.btn_message:
                switchView(TAB_SMS);
                mViewPager.setCurrentItem(TAB_SMS);
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
        startActivity(intent);
//        try {
////            startActivity(intent);
//            Intent i = new Intent(MainActivity.this, WebViewActivity.class);
//            i.putExtra("title", getString(R.string.term_reservation));
//            i.putExtra("url", RetrofitManager.PRODUCT_URL);
//            startActivity(i);
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(getApplicationContext(), R.string.msg_there_is_no_available_app, Toast.LENGTH_SHORT).show();
//        }
    }

    private void startInfo() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(RetrofitManager.INFOMATION));
        startActivity(intent);
//        try {
//            Intent i = new Intent(MainActivity.this, WebViewActivity.class);
//            i.putExtra("title", getString(R.string.term_infomation));
//            i.putExtra("url", RetrofitManager.INFOMATION);
//            startActivity(i);
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(getApplicationContext(), R.string.msg_there_is_no_available_app, Toast.LENGTH_SHORT).show();
//        }
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

    public void refreshMainCallLogFragment() {
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
//        mBtnPad.setImageResource(R.drawable.ic_dialpad_white_24dp);
//        mBtnLog.setImageResource(R.drawable.ic_query_builder_white_24dp);
//        mBtnContact.setImageResource(R.drawable.ic_contact_phone_white_24dp);
//        mBtnMessage.setImageResource(R.drawable.ic_textsms_white_24dp);
        mBtnPad.setSelected(false);
        mBtnLog.setSelected(false);
        mBtnContact.setSelected(false);
        mBtnMessage.setSelected(false);

        switch (position) {
            case TAB_DIAL_PAD:
//                mBtnPad.setImageResource(R.drawable.ic_dialpad_white_24dp_pressed);
                mBtnPad.setSelected(true);
                break;

            case TAB_CALL_LOG:
//                mBtnLog.setImageResource(R.drawable.ic_query_builder_white_24dp_pressed);
                mBtnLog.setSelected(true);
                break;

            case TAB_CONTACT:
//                mBtnContact.setImageResource(R.drawable.ic_contact_phone_white_24dp_pressed);
                mBtnContact.setSelected(true);
                break;

            case TAB_SMS:
//                mBtnMessage.setImageResource(R.drawable.ic_textsms_white_24dp_pressed);
                mBtnMessage.setSelected(true);
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



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: { //for toolbar back button
                mDrawer.openDrawer(GravityCompat.START);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mViewPager.getAdapter().notifyDataSetChanged();
    }
}

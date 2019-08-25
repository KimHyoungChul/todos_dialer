package com.todosdialer.todosdialer;

import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.AppMemberInfoBody;
import com.todosdialer.todosdialer.api.response.AppMemberInfoResponse;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.MemberInfo;
import com.todosdialer.todosdialer.model.User;

import io.realm.Realm;

public class UserInfoActivity extends AppCompatActivity {
    private Realm mRealm;
    private TextView mUserPhone;
    private TextView mUserBirthday;
    private TextView mUserExpiredDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        initActionBar();

        setActionbar(getString(R.string.term_user_info));

        mRealm = Realm.getDefaultInstance();

        TextView userEmail = findViewById(R.id.text_user_email);
        mUserPhone = findViewById(R.id.text_user_phone);
        mUserBirthday = findViewById(R.id.text_user_birthday);
        mUserExpiredDate = findViewById(R.id.text_user_expired_date);

        User user = RealmManager.newInstance().loadUserWithDefault(mRealm);
        userEmail.setText(user.getId());
        loadAppMemberInfo(user.getId());

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("UserInfoActivity","savedInstanceState is not null");
            finish();
            return;
        }
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    private void loadAppMemberInfo(String email) {
        RetrofitManager.retrofit(getApplicationContext()).create(Client.Api.class)
                .loadAppMemberInfo(new AppMemberInfoBody(this, email))
                .enqueue(new ApiCallback<AppMemberInfoResponse>() {
                    @Override
                    public void onSuccess(AppMemberInfoResponse response) {
                        if (response.isSuccess()) {
                            setViewByInfo(response.result());
                        } else {
                            Toast.makeText(getApplicationContext(), response.message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void setViewByInfo(MemberInfo memberInfo) {
        if (TextUtils.isEmpty(memberInfo.phone)){
            mUserPhone.setText(getString(R.string.msg_not_available));
            mUserExpiredDate.setText(getString(R.string.msg_not_available));
        }
        else{
            mUserPhone.setText(memberInfo.phone);
            mUserExpiredDate.setText(String.valueOf(memberInfo.dueDay) + getString(R.string.term_day_unit));
        }

        mUserBirthday.setText(memberInfo.birthDate);
    }

    private void setActionbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            // Get the ActionBar here to configure the way it behaves.
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_left);
            actionBar.setHomeButtonEnabled(true);

            TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
            toolbarTitle.setText(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:{ //for toolbar back button
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

}

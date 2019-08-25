package com.todosdialer.todosdialer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.todosdialer.todosdialer.adapter.FriendListAdapter;
import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.CheckSessionBody;
import com.todosdialer.todosdialer.api.body.SignInBody;
import com.todosdialer.todosdialer.api.response.BaseResponse;
import com.todosdialer.todosdialer.api.response.SipSessionInfoResponse;
import com.todosdialer.todosdialer.manager.BusManager;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.Friend;
import com.todosdialer.todosdialer.model.User;
import com.todosdialer.todosdialer.service.TodosService;
import com.todosdialer.todosdialer.sip.SipInstance;
import com.todosdialer.todosdialer.util.KoreanTextMatcher;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

public class SearchFriendActivity extends AppCompatActivity {
    public static final String EXTRA_KEY_KEYWORDS = "extra_key_keyword";
    public static final String DATA_KEY_SEARCH_RESULT = "data_key_search_result";

    private List<Friend> mFriends;
    private ArrayList<String> mSearchKeywords;
    private FriendListAdapter mAdapter;
    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_friend);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initActionBar();

        mRealm = Realm.getDefaultInstance();
        mFriends = RealmManager.newInstance().loadFriends(mRealm);

        mSearchKeywords = getIntent().getStringArrayListExtra(EXTRA_KEY_KEYWORDS);
        if (mSearchKeywords == null) {
            mSearchKeywords = new ArrayList<>();
        }

        RecyclerView recyclerView = findViewById(R.id.friend_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new FriendListAdapter(false);
        mAdapter.setOnFriendClickListener(new FriendListAdapter.OnFriendClickListener() {
            @Override
            public void onCellTouched(String number) {

            }

            @Override
            public void onCellClicked(String number) {
                finishWithResult(number);
            }

            @Override
            public void onMessageClicked(String number) {
            }

            @Override
            public void onCallClicked(String number) {
            }
        });
        recyclerView.setAdapter(mAdapter);

        refreshList();

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("SearchFriendAcivity", "savedInstanceState finish ");
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


    private void refreshList() {
        ArrayList<Friend> refreshList = new ArrayList<>();
        for (int i = 0; i < mFriends.size(); i++) {
            Friend checking = mFriends.get(i);

            for (int j = 0; j < mSearchKeywords.size(); j++) {
                Log.d("TAG", "SearchFriendActivity:mSearchKeyword : " + mSearchKeywords.get(j));
                if (TextUtils.isDigitsOnly(mSearchKeywords.get(j))) {
                    if (checking.getNumber().replace("-", "").contains(mSearchKeywords.get(j))) {
                        refreshList.add(checking);
                    }
                } else {
                    if (TextUtils.isEmpty(mSearchKeywords.get(j)) || KoreanTextMatcher.isMatch(checking.getName(), mSearchKeywords.get(j))) {
                        refreshList.add(checking);
                    }
                }
            }
        }
        mAdapter.setFriendList(refreshList);
    }

    private void finishWithResult(String number) {
        number = number.replace(" ", "");
        Intent data = new Intent();
        data.putExtra(DATA_KEY_SEARCH_RESULT, number);
        setResult(RESULT_OK, data);

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRealm.close();
    }

    public static class SignInActivity extends AppCompatActivity {
        private static final int REQUEST_PERMISSION = 21;
        private static final int REQUEST_SIGN_UP = 33;

        private AppCompatEditText mEditEmail;
        private AppCompatEditText mEditPassword;

        private boolean saveLoginData;
        private String id;
        private SharedPreferences appData;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_sign);
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowCustomEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }

            hasPermissions(Utils.checkPermissions(getApplicationContext()));

            //login 설정값 불러오기
            appData = getSharedPreferences("appData", MODE_PRIVATE);
            load();

            mEditEmail = findViewById(R.id.edit_email);
            mEditPassword = findViewById(R.id.edit_password);

            //이전에 저장된 loging 설정값 불러오기
            if (saveLoginData) {
                mEditEmail.setText(id);
            }
            findViewById(R.id.btn_sign_in).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isAllInputValid()) {
                        signIn(mEditEmail.getText().toString(), mEditPassword.getText().toString());
                    }
                    save();
                }
            });

            findViewById(R.id.btn_sign_up).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivityForResult(new Intent(SignInActivity.this, SignUpActivity.class), REQUEST_SIGN_UP);
                }
            });

            findViewById(R.id.btn_finding_pw).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(SignInActivity.this, FindingPwActivity.class));
                }
            });

            findViewById(R.id.btn_service_info).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://brand.todosdialer.com/?c=106")));
                    Intent i = new Intent(SignInActivity.this, WebViewActivity.class);
                    i.putExtra("title", getString(R.string.term_infomation));
                    i.putExtra("url", "http://brand.todosdialer.com/?c=106");
                    startActivity(i);
                }
            });

            //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
            if (savedInstanceState != null) {
                finish();
                return;
            }
        }

        //login 저장에 사용되는 함수
        private void save() {
            SharedPreferences.Editor editor = appData.edit();
            editor.putBoolean("SAVE_LOGIN_DATA", true);
            editor.putString("ID", mEditEmail.getText().toString().trim());
            editor.apply();
        }

        private void load() {
            saveLoginData = appData.getBoolean("SAVE_LOGIN_DATA", false);
            id = appData.getString("ID", "");
        }

        private void hasPermissions(String[] permissionArray) {
            if (permissionArray != null && permissionArray.length > 0) {
                ActivityCompat.requestPermissions(this, permissionArray, REQUEST_PERMISSION);

            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if (requestCode == REQUEST_PERMISSION) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getApplicationContext(), "Please allow all permissions.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                }
            }
        }

        private boolean isAllInputValid() {
            String email = mEditEmail.getText().toString();
            String pw = mEditPassword.getText().toString();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(getApplicationContext(), R.string.msg_please_type_email, Toast.LENGTH_SHORT).show();
                return false;
            } else if (TextUtils.isEmpty(pw)) {
                Toast.makeText(getApplicationContext(), R.string.msg_please_type_pw, Toast.LENGTH_SHORT).show();
                return false;
            } else if (Utils.isEmailInvalid(email)) {
                Toast.makeText(getApplicationContext(), R.string.msg_please_check_email_format, Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }

        private void signIn(final String email, final String password) {
            RetrofitManager.retrofit(getApplicationContext()).create(Client.Api.class)
                    .signIn(new SignInBody(getApplicationContext(), email, password))
                    .enqueue(new ApiCallback<BaseResponse>() {
                        @Override
                        public void onSuccess(BaseResponse response) {
                            if (response.isSuccess()) {
                                saveUserInfo(email, password);

                                checkSession(email);
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

        private void saveUserInfo(String email, String password) {
            Realm realm = Realm.getDefaultInstance();
            User user = new User();
            user.setId(email);
            user.setPassword(password);

            RealmManager.newInstance().saveUser(realm, user);
            realm.close();
        }

        private void checkSession(String email) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(getApplicationContext(), TodosService.class));
            } else {
                startService(new Intent(getApplicationContext(), TodosService.class));
            }

            RetrofitManager.retrofit(this).create(Client.Api.class)
                    .checkSessionInfo(new CheckSessionBody(this, email))
                    .enqueue(new ApiCallback<SipSessionInfoResponse>() {
                        @Override
                        public void onSuccess(SipSessionInfoResponse response) {
                            if (response.isSuccess() && !SipInstance.getInstance(getApplicationContext()).isAccountAvailable()) {
                                BusManager.getInstance().post(new TodosService.Request("Splash",
                                        TodosService.Request.REQUEST_REGISTER_SIP,
                                        response.result));
                            }
                            goToMain();
                        }

                        @Override
                        public void onFail(int error, String msg) {
                            goToMain();
                        }
                    });
        }

        private void goToMain() {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            finish();
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_SIGN_UP && resultCode == RESULT_OK) {
                goToMain();
            }
        }
    }
}

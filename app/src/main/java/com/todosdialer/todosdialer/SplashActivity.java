package com.todosdialer.todosdialer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

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
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import me.everything.providers.android.contacts.Contact;
import me.everything.providers.android.contacts.ContactsProvider;

public class SplashActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION = 12;
    private static final int REQUEST_IGNORING = 123;
    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        mRealm = Realm.getDefaultInstance();

        Utils.loadNativeLibraries(getApplicationContext());

        if (hasPermissions(Utils.checkPermissions(getApplicationContext()))) {
            if (RealmManager.newInstance().loadFriendsSize(mRealm) == 0) {
                new DumpContactTask().execute();
            } else {
                goNext();
            }

        }

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("SplashActivity","savedInstanceState is not null");
            finish();
            return;
        }
    }

    private boolean hasPermissions(String[] permissionArray) {
        if (permissionArray != null && permissionArray.length > 0) {
            ActivityCompat.requestPermissions(this, permissionArray, REQUEST_PERMISSION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    String msg = "Please allow all permission: " + permissions[i];
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }

            if (RealmManager.newInstance().loadFriendsSize(mRealm) == 0) {
                new DumpContactTask().execute();
            } else {
                goNext();
            }
        }
    }

    private void goNext() {
        if (!releaseDozeMode()) {
            User user = RealmManager.newInstance().loadUser(mRealm);

            if (user != null) {
                startService(new Intent(getApplicationContext(), TodosService.class));

                signIn(user.getId(), user.getPassword());
            } else {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(SplashActivity.this, SearchFriendActivity.SignInActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                        finish();
                    }
                }, 1500);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        User user = RealmManager.newInstance().loadUser(mRealm);

        if (user != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(getApplicationContext(), TodosService.class));
            } else {
                startService(new Intent(getApplicationContext(), TodosService.class));
            }

            signIn(user.getId(), user.getPassword());
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SplashActivity.this, SearchFriendActivity.SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    finish();
                }
            }, 1500);
        }
    }

    @SuppressLint("BatteryLife")
    private boolean releaseDozeMode() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                Intent i = new Intent();
                String packageName = getPackageName();
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    Log.e(getClass().getSimpleName(), "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
                    i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    i.setData(Uri.parse("package:" + packageName));
                    startActivityForResult(i, REQUEST_IGNORING);
                    return true;
                }
            }
        }

        return false;
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
                            Log.e("signIn", "Fail to sign in: " + response.message);
                            RealmManager.newInstance().deleteUser(mRealm);
                            restartApp();
                        }
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        Log.e("signIn", "onFail msg: " + msg);
                        RealmManager.newInstance().deleteUser(mRealm);
                        restartApp();
                    }
                });
    }

    private void saveUserInfo(String email, String password) {
        User user = new User();
        user.setId(email);
        user.setPassword(password);

        RealmManager.newInstance().saveUser(mRealm, user);
    }

    private void checkSession(String email) {
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
                        startMainActivity();
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        startMainActivity();
                    }
                });
    }

    private void startMainActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                finish();
            }
        }, 500);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    private void restartApp() {
        finish();

        Intent intent = new Intent(SplashActivity.this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private class DumpContactTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            ContactsProvider contactsProvider = new ContactsProvider(getApplicationContext());
            List<Contact> contacts = contactsProvider.getContacts().getList();

            HashMap<String, String> phoneMap = new HashMap<>();
            ArrayList<Friend> people = new ArrayList<>();
            for (int i = 0; i < contacts.size(); i++) {
                if (TextUtils.isEmpty(phoneMap.get(contacts.get(i).phone))) {
                    people.add(new Friend(contacts.get(i).id,
                            contacts.get(i).displayName,
                            contacts.get(i).phone,
                            contacts.get(i).normilizedPhone,
                            contacts.get(i).uriPhoto));
                }
                phoneMap.put(contacts.get(i).phone, contacts.get(i).displayName);
            }

            Realm realm = Realm.getDefaultInstance();
            RealmManager.newInstance().insertFriends(realm, people);
            realm.close();
            return people.size();
        }

        @Override
        protected void onPostExecute(Integer data) {
            Log.i(getClass().getSimpleName(), "Load all contacts. size is: " + data);

            goNext();
        }
    }
}

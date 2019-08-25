package com.todosdialer.todosdialer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.FindingPwBody;
import com.todosdialer.todosdialer.api.response.BaseResponse;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.util.Utils;

public class FindingPwActivity extends AppCompatActivity {
    private AppCompatEditText mEditEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finding_pw);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        mEditEmail = findViewById(R.id.edit_email);

        findViewById(R.id.btn_finding).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                hideKeyBoard();

                if (isAllInputValid()) {
                    findPw(mEditEmail.getText().toString());
                }
            }
        });

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("FindingPwActivity","savedInstanceState is not null");
            finish();
            return;
        }
    }

    private boolean isAllInputValid() {
        String email = mEditEmail.getText().toString();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), R.string.msg_please_type_email, Toast.LENGTH_SHORT).show();
            return false;
        } else if (Utils.isEmailInvalid(email)) {
            Toast.makeText(getApplicationContext(), R.string.msg_please_check_email_format, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void findPw(String email) {
        RetrofitManager.retrofit(getApplicationContext()).create(Client.Api.class)
                .findPassword(new FindingPwBody(getApplicationContext(), email))
                .enqueue(new ApiCallback<BaseResponse>() {
                    @Override
                    public void onSuccess(BaseResponse response) {
                        if (response.isSuccess()) {
                            Toast.makeText(getApplicationContext(), R.string.msg_success_to_sending_email, Toast.LENGTH_SHORT).show();
                            finish();
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


    private void hideKeyBoard() {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        }
    }
}

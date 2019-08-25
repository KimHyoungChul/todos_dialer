package com.todosdialer.todosdialer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.todosdialer.todosdialer.fragment.AcceptanceFragment;
import com.todosdialer.todosdialer.fragment.SignUpFragment;
import com.todosdialer.todosdialer.fragment.WebViewFragment;

public class SignUpActivity extends AppCompatActivity implements AcceptanceFragment.OnClickListener,
        SignUpFragment.OnSignUpListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
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

        AcceptanceFragment fragment = AcceptanceFragment.newInstance();
        fragment.setOnClickListener(this);
        addFragment(fragment, false);

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("SingUpActivity","savedInstanceState is not null");
            finish();
            return;
        }
    }

    @Override
    public void onNextClicked(String phoneNumber) {
        SignUpFragment fragment = SignUpFragment.newInstance(phoneNumber);
        fragment.setOnSignUpListener(this);
        addFragment(fragment, true);
    }

    @Override
    public void showWebView(String url) {
        addFragment(WebViewFragment.newInstance(url), true);
    }

    @Override
    public void onSignedUp() {
        setResult(RESULT_OK);
        finish();
    }

    private void addFragment(Fragment fragment, boolean isAddToBackStack) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());
        if (isAddToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

}

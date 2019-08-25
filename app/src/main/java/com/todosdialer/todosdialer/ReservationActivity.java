package com.todosdialer.todosdialer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.RegisterOrderBody;
import com.todosdialer.todosdialer.api.response.RegOrderResponse;
import com.todosdialer.todosdialer.fragment.ReservationFormBasicFragment;
import com.todosdialer.todosdialer.fragment.ReservationFormInFragment;
import com.todosdialer.todosdialer.fragment.ReservationFormOutFragment;
import com.todosdialer.todosdialer.manager.GsonManager;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.AppCode;
import com.todosdialer.todosdialer.model.AppCodeGroup;
import com.todosdialer.todosdialer.model.OrderInfo;
import com.todosdialer.todosdialer.model.OrderResult;
import com.todosdialer.todosdialer.model.User;

import io.realm.Realm;

public class ReservationActivity extends AppCompatActivity {
    private AppCodeGroup mSelectedBasicNation;
    private AppCode mSelectedBasicNationTelecom;
    private AppCodeGroup mSelectedOutNation;

    private String mSelectedOutDate;
    private String mSelectedOutTime;
    private AppCode mSelectedOutAirPort;

    private String mSelectedInDate;
    private String mSelectedInTime;
    private AppCode mSelectedInAirPort;

    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initActionBar();

        Realm realm = Realm.getDefaultInstance();
        mUser = RealmManager.newInstance().loadUser(realm);
        realm.close();

        ReservationFormBasicFragment fragment = ReservationFormBasicFragment.newInstance();
        fragment.setOnSelectionListener(new ReservationFormBasicFragment.OnSelectionListener() {
            @Override
            public void onSelectionFinished(AppCodeGroup basicNation, AppCode basicTelecom, AppCodeGroup outNation) {
                mSelectedBasicNation = basicNation;
                mSelectedBasicNationTelecom = basicTelecom;
                mSelectedOutNation = outNation;

                showReservationFormOutFragment();
            }
        });
        addFragment(fragment, false);

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("ReservationActivity","savedInstanceState is not null");
            finish();
            return;
        }
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

       /* findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });*/
    }


    private void showReservationFormOutFragment() {
        ReservationFormOutFragment formOutFragment = ReservationFormOutFragment.newInstance(mSelectedBasicNation);
        formOutFragment.setOnSelectionListener(new ReservationFormOutFragment.OnSelectionListener() {
            @Override
            public void onSelectionFinished(String outDate, String outTime, AppCode outAirport) {
                mSelectedOutDate = outDate;
                mSelectedOutTime = outTime;
                mSelectedOutAirPort = outAirport;

                showReservationFormInFragment();
            }
        });
        addFragment(formOutFragment, true);
    }

    private void showReservationFormInFragment() {
        ReservationFormInFragment formOutFragment = ReservationFormInFragment.newInstance(mSelectedBasicNation);
        formOutFragment.setOnSelectionListener(new ReservationFormInFragment.OnSelectionListener() {
            @Override
            public void onSelectionFinished(String inDate, String inTime, AppCode inAirport) {
                mSelectedInDate = inDate;
                mSelectedInTime = inTime;
                mSelectedInAirPort = inAirport;

                registerReservation();
            }
        });
        addFragment(formOutFragment, true);
    }

    private void addFragment(Fragment fragment, boolean isAddToBackStack) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (isAddToBackStack) {
            fragmentTransaction.setCustomAnimations(R.anim.anim_fragment_enter,
                    R.anim.anim_fragment_exit,
                    R.anim.anim_fragment_enter,
                    R.anim.anim_fragment_exit);
        }

        fragmentTransaction.add(R.id.reservation_fragment_container, fragment, fragment.getClass().getSimpleName());
        if (isAddToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    private void registerReservation() {
        final RegisterOrderBody orderBody = new RegisterOrderBody(getApplicationContext(),
                mUser.getId(),
                mSelectedBasicNation.code,
                mSelectedBasicNationTelecom.code,
                mSelectedOutNation.code,
                mSelectedOutDate,
                mSelectedOutTime,
                mSelectedOutAirPort.code,
                mSelectedInDate,
                mSelectedInTime,
                mSelectedInAirPort.code);
        RetrofitManager.retrofit(getApplicationContext()).create(Client.Api.class)
                .registerOrder(orderBody)
                .enqueue(new ApiCallback<RegOrderResponse>() {
                    @Override
                    public void onSuccess(RegOrderResponse response) {
                        if (response.isSuccess()) {
                            goToResult(response.result.get(0));
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

    private void goToResult(OrderResult result) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.basicTelecomCodeName = mSelectedBasicNationTelecom.codeName;
        orderInfo.outNationCodeName = mSelectedOutNation.codeName;

        orderInfo.outDate = mSelectedOutDate;

        orderInfo.inDate = mSelectedInDate;

        Intent intent = new Intent(ReservationActivity.this, OrderResultActivity.class);
        intent.putExtra(OrderResultActivity.EXTRA_KEY_ORDER_INFO_JSON, GsonManager.getGson().toJson(orderInfo));
        intent.putExtra(OrderResultActivity.EXTRA_KEY_ORDER_RESULT_JSON, GsonManager.getGson().toJson(result));
        startActivity(intent);

        finish();
    }
}

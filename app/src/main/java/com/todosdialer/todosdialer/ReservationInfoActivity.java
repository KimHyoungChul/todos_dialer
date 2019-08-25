package com.todosdialer.todosdialer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.todosdialer.todosdialer.adapter.OrderInfoAdapter;
import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.LoadOrderBody;
import com.todosdialer.todosdialer.api.response.LoadOrderResponse;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.OrderInfo;
import com.todosdialer.todosdialer.model.User;

import java.util.ArrayList;

import io.realm.Realm;

public class ReservationInfoActivity extends AppCompatActivity {
    private RecyclerView mReservationRecyclerView;
    private TextView mReservationEmptyView;
    private ProgressBar mProgressBar;

    private OrderInfoAdapter mAdapter;
    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_info);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initActionBar();

        Realm realm = Realm.getDefaultInstance();
        mUser = RealmManager.newInstance().loadUser(realm);
        realm.close();

        if (mUser == null || TextUtils.isEmpty(mUser.getId())) {
            finish();
            return;
        }

        mReservationRecyclerView = findViewById(R.id.reservation_list);
        mReservationEmptyView = findViewById(R.id.empty_reservation_info_view);
        mProgressBar = findViewById(R.id.progress_bar);

        mReservationRecyclerView.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
        mReservationEmptyView.setVisibility(View.VISIBLE);

        mReservationRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new OrderInfoAdapter();
        mReservationRecyclerView.setAdapter(mAdapter);

        loadOrderInfoList();

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("ReservationofActivity","savedInstanceState is not null");
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

    private void loadOrderInfoList() {
        mProgressBar.setVisibility(View.VISIBLE);
        RetrofitManager.retrofit(getApplicationContext()).create(Client.Api.class)
                .loadOrder(new LoadOrderBody(getApplicationContext(), mUser.getId()))
                .enqueue(new ApiCallback<LoadOrderResponse>() {
                    @Override
                    public void onSuccess(LoadOrderResponse response) {
                        mProgressBar.setVisibility(View.GONE);
                        if (response.isSuccess()) {
                            setInfoList(response.result);
                        } else {
                            Toast.makeText(getApplicationContext(), response.message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        mProgressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void setInfoList(ArrayList<OrderInfo> list) {
        if (list != null && list.size() > 0) {
            mReservationRecyclerView.setVisibility(View.VISIBLE);
            mReservationEmptyView.setVisibility(View.GONE);

            mAdapter.setOrderInfoList(list);
        } else {
            mReservationRecyclerView.setVisibility(View.GONE);
            mReservationEmptyView.setVisibility(View.VISIBLE);
        }
    }
}

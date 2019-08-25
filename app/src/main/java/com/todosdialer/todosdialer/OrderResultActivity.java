package com.todosdialer.todosdialer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.PayResultBody;
import com.todosdialer.todosdialer.api.response.BaseResponse;
import com.todosdialer.todosdialer.manager.GsonManager;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.OrderInfo;
import com.todosdialer.todosdialer.model.OrderResult;
import com.todosdialer.todosdialer.model.User;

import java.text.NumberFormat;

import io.realm.Realm;

public class OrderResultActivity extends AppCompatActivity {
    public static final String EXTRA_KEY_ORDER_RESULT_JSON = "key_order_result_json";
    public static final String EXTRA_KEY_ORDER_INFO_JSON = "key_order_info_json";

    private TextView mTextOrderNumber;
    private TextView mTextBasicNation;
    private TextView mTextBasicNationTelecom;
    private TextView mTextOutNation;

    private TextView mTextInDate;
    private TextView mTextInTime;
    private TextView mTextInAirport;

    private TextView mTextOutDate;
    private TextView mTextOutTime;
    private TextView mTextOutAirport;

    private TextView mTextPrice;
    private Button mBtnOrderStateName;

    private RadioGroup mRadioPayment;

    private OrderResult mOrderResult;
    private OrderInfo mOrderInfo;

    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_result);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initActionBar();

        String resultJson = getIntent().getStringExtra(EXTRA_KEY_ORDER_RESULT_JSON);
        String orderInfoJson = getIntent().getStringExtra(EXTRA_KEY_ORDER_INFO_JSON);
        if (TextUtils.isEmpty(orderInfoJson) || TextUtils.isEmpty(resultJson)) {
            Toast.makeText(getApplicationContext(), "Data is invalid.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Realm realm = Realm.getDefaultInstance();
        mUser = RealmManager.newInstance().loadUser(realm);
        realm.close();

        mOrderResult = GsonManager.getGson().fromJson(resultJson, OrderResult.class);
        mOrderInfo = GsonManager.getGson().fromJson(orderInfoJson, OrderInfo.class);

        mTextOrderNumber = findViewById(R.id.text_order_number);
        mTextBasicNation = findViewById(R.id.text_basic_nation);
        mTextBasicNationTelecom = findViewById(R.id.text_basic_nation_telecom);
        mTextOutNation = findViewById(R.id.text_out_nation);

        mTextInDate = findViewById(R.id.text_in_date);
        mTextInTime = findViewById(R.id.text_in_time);
        mTextInAirport = findViewById(R.id.text_in_airport);

        mTextOutDate = findViewById(R.id.text_out_date);
        mTextOutTime = findViewById(R.id.text_out_time);
        mTextOutAirport = findViewById(R.id.text_out_airport);

        mTextPrice = findViewById(R.id.text_price);
        mBtnOrderStateName = findViewById(R.id.btn_order_state_name);
        mRadioPayment = findViewById(R.id.radio_payment);

        setViewByData();

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("OrderResultActivity","savedInstanceState is not null");
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

    private void setViewByData() {
        mTextOrderNumber.setText(mOrderResult.orderNo);

        mTextBasicNation.setText(mOrderInfo.basicTelecomCodeName);
        mTextBasicNationTelecom.setText(mOrderInfo.basicTelecomCodeName);
        mTextOutNation.setText(mOrderInfo.outNationCodeName);

        mTextInDate.setText(mOrderInfo.inDate);

        mTextOutDate.setText(mOrderInfo.outDate);

        NumberFormat format = NumberFormat.getInstance();
        String priceText = format.format(mOrderResult.orderTotalPrice) + mTextPrice.getContext().getString(R.string.term_currency_unit);
        mTextPrice.setText(priceText);
        mBtnOrderStateName.setText(mOrderResult.orderStateName);
        mBtnOrderStateName.setEnabled(mOrderResult.orderState == OrderInfo.STATE_READY_FOR_PAY);
        mBtnOrderStateName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pay();
            }
        });
    }

    private void pay() {
        switch (mRadioPayment.getCheckedRadioButtonId()) {
            case R.id.radio_credit_card:
                break;
            case R.id.radio_mobile:
                break;
            case R.id.radio_ars:
                break;
        }

        final PayResultBody body = new PayResultBody(getApplicationContext(),
                mUser.getId(),
                mOrderResult.orderNo,
                "payID",
                "card",
                "LGuplus",
                mOrderResult.orderTotalPrice,
                "0000",
                "2018-02-02",
                200);
        RetrofitManager.retrofit(getApplicationContext()).create(Client.Api.class)
                .sendPaymentResult(body)
                .enqueue(new ApiCallback<BaseResponse>() {
                    @Override
                    public void onSuccess(BaseResponse response) {
                        if (response.isSuccess()) {
                            Toast.makeText(getApplicationContext(), response.message, Toast.LENGTH_SHORT).show();

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
}

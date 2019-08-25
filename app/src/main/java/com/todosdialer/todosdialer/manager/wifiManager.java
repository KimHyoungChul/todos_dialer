package com.todosdialer.todosdialer.manager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.fragment.AcceptanceFragment;
import com.todosdialer.todosdialer.fragment.DialFragment;

import java.util.List;

public abstract class wifiManager extends Activity implements AcceptanceFragment.OnClickListener {

    private static final String TAG = "WiFi Scanner";
    //WifiManaer variable
    WifiManager wifiManager;

    TextView textStatus;
    Button BtnScanStart;
    Button BtnScanStop;

    private int scanCount = 0;
    String text = "";
    String result = "";

    private List<ScanResult> mScanResult;

    private  BroadcastReceiver mReceiver  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo mNetworkState = getNetworkInfo();

            if(mNetworkState != null && mNetworkState.isConnected()) {
                //WIFI 네트워크
                if (mNetworkState.getType() == ConnectivityManager.TYPE_WIFI) {
                    final String action = intent.getAction();
                    if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                        getWIFISanResult();
                        wifiManager.startScan();
                    } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                        sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
                    }
                } else if (mNetworkState.getType() == ConnectivityManager.TYPE_MOBILE) {
                    //3G/LTE 네트워크
                } else {
                    //Network 연결 안됨.
                }
            }
        }
    };

    public void getWIFISanResult() {
        mScanResult = wifiManager.getScanResults();
        textStatus.setText("Scan count is \t" + ++scanCount + "times \n");
        textStatus.append("===============================================");
        for(int i = 0 ; i < mScanResult.size(); i++){
            ScanResult result = mScanResult.get(i);
            textStatus.append((i + 1) + "SSID : " + result.SSID.toString() + "\t\t RSSI : " + result.level + "dBm\n");
        }
        textStatus.append("=================================================");
    }

    public void initWIFIScan() {
        scanCount = 0;
        text = "";
        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        wifiManager.startScan();
        Log.d(TAG,"initWIFIScan()");
    }

    public NetworkInfo getNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo;
    }
}

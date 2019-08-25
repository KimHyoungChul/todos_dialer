package com.todosdialer.todosdialer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.todosdialer.todosdialer.manager.RealmManager;

import io.realm.Realm;

public class LogActivity extends AppCompatActivity {
    private TextView mTextLogs;
    private ProgressDialog mProgressDialog;
    private View mBtnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        setContentView(R.layout.activity_log);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextLogs = (TextView) findViewById(R.id.text_logs);
        mBtnDelete = findViewById(R.id.btn_delete);
        mBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DeleteTask().execute();
            }
        });
        mProgressDialog = makeWaitingDialog(this);

        new LoadTask().execute();

        findViewById(R.id.btn_send_to_mail).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("mailto:"));
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, "[inniTT] Logs");
                String data = mTextLogs.getText().toString();
                intent.putExtra(Intent.EXTRA_TEXT, data);
                try {
                    startActivity(Intent.createChooser(intent, "Send email using..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(LogActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("LogActivity","savedInstanceState is not null");
            finish();
            return;
        }
    }


    public static ProgressDialog makeWaitingDialog(Activity activity) {
        ProgressDialog waitingDialog = new ProgressDialog(activity);
        waitingDialog.setMessage(activity.getString(R.string.msg_please_wait));
        waitingDialog.setCanceledOnTouchOutside(false);
        return waitingDialog;
    }


    private class LoadTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
            mBtnDelete.setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            String logs = RealmManager.newInstance().loadLogs(Realm.getDefaultInstance()).toString();
            logs = logs.replace(",", "");
            return logs;
        }

        @Override
        protected void onPostExecute(String logs) {
            super.onPostExecute(logs);
            mTextLogs.setText(logs);
            mBtnDelete.setVisibility(View.VISIBLE);
            mProgressDialog.dismiss();
        }
    }

    private class DeleteTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
            mBtnDelete.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            RealmManager.newInstance().deleteLogs(Realm.getDefaultInstance());
            return null;
        }

        @Override
        protected void onPostExecute(Void logs) {
            super.onPostExecute(logs);
            mTextLogs.setText("");
            mBtnDelete.setEnabled(true);
            mProgressDialog.dismiss();
        }
    }
}

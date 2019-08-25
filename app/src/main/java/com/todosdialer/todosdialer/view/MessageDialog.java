package com.todosdialer.todosdialer.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.todosdialer.todosdialer.R;

public class MessageDialog extends Dialog {

    private String mMessage;

    public MessageDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_message_dialog);

        TextView textBody = findViewById(R.id.text_body);

        if (!TextUtils.isEmpty(mMessage)) {
            textBody.setText(mMessage);
        }

        findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    public void setMessage(String message) {
        mMessage = message;
    }
}

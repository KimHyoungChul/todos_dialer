package com.todosdialer.todosdialer.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.todosdialer.todosdialer.R;

public class DialView extends FrameLayout {
    private TextView mTextNumber;
    private TextView mTextInitSounds;

    public DialView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public DialView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DialView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View rootView = LayoutInflater.from(context).inflate(R.layout.view_dial, this, false);
        mTextNumber = rootView.findViewById(R.id.text_number);
        mTextInitSounds = rootView.findViewById(R.id.text_init_sounds);
        addView(rootView);
    }

    public void setNumberWithInitSound(String number, String initSound) {
        mTextNumber.setText(number);
        mTextInitSounds.setText(initSound);
    }
}

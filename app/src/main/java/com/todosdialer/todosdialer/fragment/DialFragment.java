package com.todosdialer.todosdialer.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.todosdialer.todosdialer.R;

public class DialFragment extends Fragment {
    private OnClickListener mDialClickListener;
    TextView tvDTMFInput;

    public static DialFragment newInstance() {

        Bundle args = new Bundle();

        DialFragment fragment = new DialFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnDialClickListener(OnClickListener listener) {
        mDialClickListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dial, container, false);
        rootView.findViewById(R.id.btn_dial_one).setOnClickListener(new DialListener("1"));
        rootView.findViewById(R.id.btn_dial_two).setOnClickListener(new DialListener("2"));
        rootView.findViewById(R.id.btn_dial_three).setOnClickListener(new DialListener("3"));
        rootView.findViewById(R.id.btn_dial_four).setOnClickListener(new DialListener("4"));
        rootView.findViewById(R.id.btn_dial_five).setOnClickListener(new DialListener("5"));
        rootView.findViewById(R.id.btn_dial_six).setOnClickListener(new DialListener("6"));
        rootView.findViewById(R.id.btn_dial_seven).setOnClickListener(new DialListener("7"));
        rootView.findViewById(R.id.btn_dial_eight).setOnClickListener(new DialListener("8"));
        rootView.findViewById(R.id.btn_dial_nine).setOnClickListener(new DialListener("9"));
        rootView.findViewById(R.id.btn_dial_star).setOnClickListener(new DialListener("*"));
        rootView.findViewById(R.id.btn_dial_zero).setOnClickListener(new DialListener("0"));
        rootView.findViewById(R.id.btn_dial_shop).setOnClickListener(new DialListener("#"));
        rootView.findViewById(R.id.btn_finish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialClickListener != null) {
                    mDialClickListener.onFinishClicked();
                }
            }
        });

        tvDTMFInput = rootView.findViewById(R.id.tv_dtmf_input);

        return rootView;
    }

    private class DialListener implements View.OnClickListener {
        String dial;

        DialListener(String dial) {
            this.dial = dial;
        }

        @Override
        public void onClick(View view) {
            if (mDialClickListener != null) {
                mDialClickListener.onDialClicked(dial);

                tvDTMFInput.setText(tvDTMFInput.getText() + dial);
            }
        }
    }

    public interface OnClickListener {
        void onDialClicked(String dial);

        void onFinishClicked();
    }
}

package com.todosdialer.todosdialer.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.CheckPhoneBody;
import com.todosdialer.todosdialer.api.response.BaseResponse;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.util.Utils;

import java.util.Calendar;
import java.util.Random;

public class AcceptanceFragment extends Fragment {
    private static final String URL_SERVICE = "http://todosdialer.com/cs/terms.txt";
    private static final String URL_PRIVATE = "http://todosdialer.com/cs/privacy.txt";

    private AppCompatCheckBox mCheckAll;
    private AppCompatCheckBox mCheckService;
    private AppCompatCheckBox mCheckPrivate;

    private AppCompatEditText mEditPhone;
    private AppCompatEditText mEditAuthNumber;

    private OnClickListener mListener;

    private String mAuthNumber = "";
    private boolean mIsPhoneChecked = false;

    public static AcceptanceFragment newInstance() {
        Bundle args = new Bundle();
        AcceptanceFragment fragment = new AcceptanceFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_acceptance, container, false);

        mEditPhone = rootView.findViewById(R.id.edit_phone);
        mEditPhone.setText(Utils.getPhoneNumber(getContext(), false));
        mEditPhone.setSelection(mEditPhone.length());

        mEditAuthNumber = rootView.findViewById(R.id.edit_auth_number);
        mCheckAll = rootView.findViewById(R.id.check_all);
        mCheckAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCheckService.setChecked(mCheckAll.isChecked());
                mCheckPrivate.setChecked(mCheckAll.isChecked());
            }
        });
        mCheckService = rootView.findViewById(R.id.check_service);
        mCheckService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCheckAll.setChecked(mCheckService.isChecked() && mCheckPrivate.isChecked());
            }
        });
        mCheckPrivate = rootView.findViewById(R.id.check_private);
        mCheckPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCheckAll.setChecked(mCheckService.isChecked() && mCheckPrivate.isChecked());
            }
        });

        initListeners(rootView);

        return rootView;
    }

    private void initListeners(View rootView) {
        mEditPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mIsPhoneChecked = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        rootView.findViewById(R.id.btn_check_phone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard();

                String phone = mEditPhone.getText().toString();
                if (TextUtils.isEmpty(phone) || phone.length() != 11) {
                    Toast.makeText(getActivity(), R.string.hint_phone, Toast.LENGTH_SHORT).show();
                } else {
                    checkPhone(mEditPhone.getText().toString());
                }
            }
        });

        rootView.findViewById(R.id.btn_send_auth_number).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();

                if (isValidPhoneNumber()) {
                    sendAuthNumber(mEditPhone.getText().toString());
                }
            }
        });

        rootView.findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard();

                if (isAllChecked() && mListener != null) {
                    mListener.onNextClicked(mEditPhone.getText().toString());
                }
            }
        });

        rootView.findViewById(R.id.btn_show_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();

                if (mListener != null) {
                    mListener.showWebView(URL_SERVICE);
                }
            }
        });

        rootView.findViewById(R.id.btn_show_private).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();

                if (mListener != null) {
                    mListener.showWebView(URL_PRIVATE);
                }
            }
        });
    }

    private boolean isValidPhoneNumber() {
        String phone = mEditPhone.getText().toString();
        if (TextUtils.isEmpty(phone) || phone.length() != 11) {
            Toast.makeText(getActivity(), R.string.hint_phone, Toast.LENGTH_SHORT).show();
            return false;
        } else if (!mIsPhoneChecked) {
            Toast.makeText(getActivity(), R.string.msg_please_check_phone, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void checkPhone(String phone) {
        String formattedPhoneNumber = Utils.formattedPhoneNumber(phone);
        RetrofitManager.retrofit(getActivity()).create(Client.Api.class)
                .checkPhoneNumber(new CheckPhoneBody(formattedPhoneNumber))
                .enqueue(new ApiCallback<BaseResponse>() {
                    @Override
                    public void onSuccess(BaseResponse response) {
                        if (response.isSuccess()) {
                            mIsPhoneChecked = true;
                            Toast.makeText(getActivity(), R.string.msg_available_phone, Toast.LENGTH_SHORT).show();
                        } else {
                            mIsPhoneChecked = false;
                            Toast.makeText(getActivity(), response.message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        mIsPhoneChecked = false;
                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendAuthNumber(String phoneNumber) {
        mAuthNumber = makeAuthNumber();
        Log.e(getClass().getSimpleName(), "mAuthNumber: " + mAuthNumber);

        String authText = getString(R.string.msg_auth_number_is) + "\n[" + mAuthNumber + "]\n";
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, authText, null, null);
    }

    private String makeAuthNumber() {
        int max = (int) (Calendar.getInstance().getTimeInMillis() % 2000000);
        int min = max - 100000;

        Random r = new Random();
        long randomNumber = r.nextInt(max - min + 1) + min;
        return String.valueOf(randomNumber);
    }

    private boolean isAllChecked() {
        if (!isValidPhoneNumber()) {
            return false;
        } else if (!mEditAuthNumber.getText().toString().equals(mAuthNumber)) {
            Toast.makeText(getActivity(), R.string.msg_different_auth_number, Toast.LENGTH_SHORT).show();
            return false;
        } else if (!mCheckAll.isChecked()) {
            Toast.makeText(getActivity(), R.string.msg_please_check_all, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void hideKeyboard() {
        if (isAdded() && getActivity() != null && getActivity().getCurrentFocus() != null) {
            InputMethodManager mImm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (mImm != null) {
                mImm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    public interface OnClickListener {
        void onNextClicked(String phoneNumber);

        void showWebView(String url);
    }
}

package com.todosdialer.todosdialer.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.CheckIdBody;
import com.todosdialer.todosdialer.api.body.SignUpBody;
import com.todosdialer.todosdialer.api.response.BaseResponse;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.User;
import com.todosdialer.todosdialer.util.Utils;

import io.realm.Realm;

public class SignUpFragment extends Fragment {
    private ProgressBar mProgressBar;
    private AppCompatEditText mEditEmail;
    private AppCompatEditText mEditName;
    private AppCompatEditText mEditBirthday;

    private AppCompatEditText mEditPassword;
    private AppCompatEditText mEditPwAgain;

    private boolean mIsIdChecked = false;

    private OnSignUpListener mListener;

    private String mPhoneNumber;

    public static SignUpFragment newInstance(String phoneNumber) {
        Bundle args = new Bundle();
        SignUpFragment fragment = new SignUpFragment();
        fragment.mPhoneNumber = phoneNumber;
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnSignUpListener(OnSignUpListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sign_up, container, false);

        mProgressBar = rootView.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);
        mEditEmail = rootView.findViewById(R.id.edit_email);
        mEditName = rootView.findViewById(R.id.edit_name);
        mEditBirthday = rootView.findViewById(R.id.edit_birthday);

        mEditPassword = rootView.findViewById(R.id.edit_password);
        mEditPwAgain = rootView.findViewById(R.id.edit_pw_again);

        rootView.findViewById(R.id.btn_check_id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isEmailValid()) {
                    checkEmail(mEditEmail.getText().toString());
                }
            }
        });

        rootView.findViewById(R.id.btn_sign_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard();

                if (isAllInputValid()) {
                    signUp(mEditEmail.getText().toString(),
                            mEditName.getText().toString(),
                            mEditBirthday.getText().toString(),
                            mPhoneNumber,
                            mEditPassword.getText().toString());
                }
            }
        });

        return rootView;
    }

    private boolean isAllInputValid() {
        String name = mEditName.getText().toString();
        String birthday = mEditBirthday.getText().toString();
        String pw = mEditPassword.getText().toString();
        String pwAgain = mEditPwAgain.getText().toString();

        if (!isEmailValid()) {
            return false;
        } else if (!mIsIdChecked) {
            Toast.makeText(getActivity(), R.string.msg_please_check_email, Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(pw)) {
            Toast.makeText(getActivity(), R.string.msg_please_type_pw, Toast.LENGTH_SHORT).show();
            return false;
        } else if (Utils.isPasswordInvalid(pw)) {
            Toast.makeText(getActivity(), R.string.msg_please_check_pw_format, Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(name)) {
            Toast.makeText(getActivity(), R.string.msg_please_type_name, Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(birthday)) {
            Toast.makeText(getActivity(), R.string.msg_please_type_birthday, Toast.LENGTH_SHORT).show();
            return false;
        } else if (birthday.length() != 8) {
            Toast.makeText(getActivity(), R.string.hint_birthday, Toast.LENGTH_SHORT).show();
            return false;
        } else if (!pw.equals(pwAgain)) {
            Toast.makeText(getActivity(), R.string.msg_please_check_pw, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isEmailValid() {
        String email = mEditEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getActivity(), R.string.msg_please_type_email, Toast.LENGTH_SHORT).show();
            return false;
        } else if (Utils.isEmailInvalid(email)) {
            Toast.makeText(getActivity(), R.string.msg_please_check_email_format, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void signUp(String email, String name, String birthday, String phone, String password) {
        register(email, name, birthday, phone, password);
    }

    private void checkEmail(String email) {
        mProgressBar.setVisibility(View.VISIBLE);
        RetrofitManager.retrofit(getActivity()).create(Client.Api.class)
                .checkEmailAsId(new CheckIdBody(email))
                .enqueue(new ApiCallback<BaseResponse>() {
                    @Override
                    public void onSuccess(BaseResponse response) {
                        if (response.isSuccess()) {
                            mIsIdChecked = true;
                            Toast.makeText(getActivity(), R.string.msg_available_email, Toast.LENGTH_SHORT).show();
                        } else {
                            mIsIdChecked = false;
                            Toast.makeText(getActivity(), response.message, Toast.LENGTH_SHORT).show();
                        }
                        mProgressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                        mProgressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void register(final String email, final String name, final String birthday, final String phone, final String password) {
        mProgressBar.setVisibility(View.VISIBLE);
        String formattedBirthday = Utils.formattedBirthday(birthday);
        String formattedPhoneNumber = Utils.formattedPhoneNumber(phone);

        RetrofitManager.retrofit(getActivity()).create(Client.Api.class)
                .signUp(new SignUpBody(getContext(), email, name, formattedBirthday, formattedPhoneNumber, password))
                .enqueue(new ApiCallback<BaseResponse>() {
                    @Override
                    public void onSuccess(BaseResponse response) {
                        mProgressBar.setVisibility(View.GONE);
                        if (response.isSuccess()) {
                            saveUserInfo(email, name, birthday, phone, password);
                            if (mListener != null) {
                                mListener.onSignedUp();
                            }
                        } else {
                            Toast.makeText(getActivity(), response.message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        mProgressBar.setVisibility(View.GONE);

                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserInfo(String email, String name, String birthday, String phone, String password) {
        User user = new User();
        user.setId(email);
        user.setName(name);
        user.setBirthday(birthday);
        user.setPhone(phone);
        user.setPassword(password);
        Realm realm = Realm.getDefaultInstance();
        RealmManager.newInstance().saveUser(realm, user);
        realm.close();
    }

    private void hideKeyboard() {
        if (isAdded() && getActivity() != null && getActivity().getCurrentFocus() != null) {
            InputMethodManager mImm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (mImm != null) {
                mImm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    public interface OnSignUpListener {
        void onSignedUp();
    }
}

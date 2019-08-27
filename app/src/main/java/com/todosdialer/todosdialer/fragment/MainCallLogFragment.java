package com.todosdialer.todosdialer.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.todosdialer.todosdialer.ChatActivity;
import com.todosdialer.todosdialer.MainActivity;
import com.todosdialer.todosdialer.NewChatActivity;
import com.todosdialer.todosdialer.OutgoingCallActivity;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.SplashActivity;
import com.todosdialer.todosdialer.adapter.CallLogAdapter;
import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.CheckSessionBody;
import com.todosdialer.todosdialer.api.response.SipSessionInfoResponse;
import com.todosdialer.todosdialer.manager.BusManager;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.CallLog;
import com.todosdialer.todosdialer.model.Friend;
import com.todosdialer.todosdialer.model.SipSessionInfo;
import com.todosdialer.todosdialer.model.User;
import com.todosdialer.todosdialer.service.TodosService;
import com.todosdialer.todosdialer.sip.SipInstance;
import com.todosdialer.todosdialer.util.KoreanTextMatcher;
import com.todosdialer.todosdialer.view.MessageDialog;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainCallLogFragment extends Fragment {
    private static final int POSITION_ALL = 0;
    private static final int POSITION_MISSED = 1;

    private TextView mBtnAll;
    private TextView mBtnMissed;
    private View mAllUnderLine;
    private View mMissedUnderLine;

    private RecyclerView mCallLogRecyclerView;
    private TextView mCallEmptyView;

    private CallLogAdapter mAdapter;

    private List<CallLog> mCallLogList;
    private List<CallLog> mMissedCallLogList;

    private int mCurrentPosition;
    private User mUser;

    private TextView mSelectdSearch;
    private String mSelectedKeyword;
    private ImageView mImgCallSearch;

    public static MainCallLogFragment newInstance(List<CallLog> allLogs, List<CallLog> missedLogs) {

        Bundle args = new Bundle();

        MainCallLogFragment fragment = new MainCallLogFragment();
        fragment.setArguments(args);
        fragment.setCallLogList(allLogs);
        fragment.setMissedCallLogList(missedLogs);
        return fragment;
    }


    public void setCallLogList(List<CallLog> logs) {
        mCallLogList = logs == null ? new ArrayList<CallLog>() : logs;

        if (mCurrentPosition == POSITION_ALL) {
            refreshLogList();
        }
    }

    public void setMissedCallLogList(List<CallLog> logs) {
        mMissedCallLogList = logs == null ? new ArrayList<CallLog>() : logs;

        if (mCurrentPosition == POSITION_MISSED) {
            refreshLogList();
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Realm realm = Realm.getDefaultInstance();
        mUser = RealmManager.newInstance().loadUser(realm);
        realm.close();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_call_log, container, false);
        mBtnAll = rootView.findViewById(R.id.btn_all);
        mBtnMissed = rootView.findViewById(R.id.btn_missed);

        mAllUnderLine = rootView.findViewById(R.id.call_log_menu_underline_all);
        mMissedUnderLine = rootView.findViewById(R.id.call_log_menu_underline_missed);

        mCallLogRecyclerView = rootView.findViewById(R.id.call_log_list);
        mCallEmptyView = rootView.findViewById(R.id.call_empty_view);

        mSelectdSearch = rootView.findViewById(R.id.edit_CallSearch);
        mImgCallSearch = rootView.findViewById(R.id.mImg_CallSearch);

        mCallLogRecyclerView.setVisibility(View.INVISIBLE);
        mCallEmptyView.setVisibility(View.VISIBLE);

        mCallLogRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAdapter = new CallLogAdapter(getActivity().getApplicationContext());
        mAdapter.setOnItemClickListener(new CallLogAdapter.OnItemClickListener() {
            @Override
            public void onMessageClicked(String number) {
                sendMessage(number);
            }

            @Override
            public void onCallClicked(String number) {
                call(number);
            }

            @Override
            public void onDeleteClicked(long id) {
                delete(id);
            }
        });

        mCallLogRecyclerView.setAdapter(mAdapter);

        mBtnAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentPosition = POSITION_ALL;
                mSelectdSearch.setText(null);
                changeIndicator();
                refreshLogList();
            }
        });

        mBtnMissed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentPosition = POSITION_MISSED;
                mSelectdSearch.setText(null);
                changeIndicator();
                refreshLogList();
            }
        });

        mSelectdSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                refreshLogList();
            }
        });

        mImgCallSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        mSelectdSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                hideKeyboard();
                return true;
            }
        });

        mCurrentPosition = POSITION_ALL;
        changeIndicator();
        refreshLogList();
        return rootView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mUser == null) {
            if (getActivity() != null) {
                getActivity().finish();

                Intent intent = new Intent(getActivity(), SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } else {
            Realm realm = Realm.getDefaultInstance();
            RealmManager.newInstance().resolveCallLogUncheckedState(realm);
            realm.close();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideKeyboard();

    }

    private void changeIndicator() {
        if (mBtnAll != null && mBtnMissed != null) {
//            mBtnAll.setBackgroundResource(R.drawable.bg_radius_half_l_gray);
//            mBtnMissed.setBackgroundResource(R.drawable.bg_radius_half_r_gray);
            mBtnAll.setSelected(false);
            mBtnMissed.setSelected(false);
            mAllUnderLine.setVisibility(View.INVISIBLE);
            mMissedUnderLine.setVisibility(View.INVISIBLE);

            mBtnAll.setTypeface(null, Typeface.NORMAL);
            mBtnMissed.setTypeface(null, Typeface.NORMAL);

            if (mCurrentPosition == POSITION_ALL) {
                mBtnAll.setSelected(true);
                mAllUnderLine.setVisibility(View.VISIBLE);
                mBtnAll.setTypeface(null, Typeface.BOLD);
            } else {
                mBtnMissed.setSelected(true);
                mMissedUnderLine.setVisibility(View.VISIBLE);
                mBtnMissed.setTypeface(null, Typeface.BOLD);
            }
        }
    }

    private void refreshLogList() {
        if (mCallLogRecyclerView != null && mCallEmptyView != null && mAdapter != null) {
            List<CallLog> list;
            if (mCurrentPosition == POSITION_ALL) {
                list = mCallLogList;
            } else {
                list = mMissedCallLogList;
            }
            //List<CallLog> list = mCurrentPosition == POSITION_ALL ? mCallLogList : mMissedCallLogList;
            if (list.size() == 0) {
                mCallEmptyView.setVisibility(View.VISIBLE);
                mCallLogRecyclerView.setVisibility(View.GONE);
            } else if (mSelectdSearch.getText().toString().length() != 0) {
                mSelectedKeyword = mSelectdSearch.getText().toString().trim();
                mSelectedKeyword.replace("-", "");
                ArrayList<CallLog> SelectedList = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    CallLog checking = list.get(i);
                    if (TextUtils.isDigitsOnly(mSelectedKeyword)) {
                        if (checking.getNumber().replace("-", "").contains(mSelectedKeyword.replace("-", ""))) {
                            SelectedList.add(checking);
                        }
                    } else {
                        if (KoreanTextMatcher.isMatch(checking.getName(), mSelectedKeyword)) {
                            SelectedList.add(checking);
                        }
                    }
                }
                mAdapter.setCallLogList(SelectedList);
            } else {
                mCallEmptyView.setVisibility(View.GONE);
                mAdapter.setCallLogList(list);
                mCallLogRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void sendMessage(String number) {
        Realm realm = Realm.getDefaultInstance();
        if (RealmManager.newInstance().hasChatRoom(realm, number)) {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_KEY_NUMBER, number);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getActivity(), NewChatActivity.class);
            intent.putExtra(NewChatActivity.EXTRA_KEY_NUMBER, number);
            startActivity(intent);
        }
        realm.close();
    }

    private void call(String number) {
        checkSession(number);
    }

    private void checkSession(final String phoneNumber) {
        RetrofitManager.retrofit(getActivity()).create(Client.Api.class)
                .checkSessionInfo(new CheckSessionBody(getContext(), mUser.getId()))
                .enqueue(new ApiCallback<SipSessionInfoResponse>() {
                    @Override
                    public void onSuccess(SipSessionInfoResponse response) {
                        if (response.isSuccess()) {
                            showOutgoingCallActivity(response.result, phoneNumber);
                        } else {
                            showInvalidSessionDialog();
                        }
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void delete(long id) {
        Realm realm = Realm.getDefaultInstance();
        final CallLog calllog = realm.where(CallLog.class).equalTo("id", id).findFirst();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (calllog != null) {
                    calllog.deleteFromRealm();
                }
            }
        });
    }

    private void showOutgoingCallActivity(SipSessionInfo result, String phoneNumber) {
        SipInstance sipInstance = SipInstance.getInstance(getActivity());
        if (sipInstance.isAccountAvailable()) {
            Intent intent = new Intent(getActivity(), OutgoingCallActivity.class);
            intent.putExtra(OutgoingCallActivity.EXTRA_KEY_PHONE_NUMBER, phoneNumber);
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), R.string.msg_please_check_account, Toast.LENGTH_SHORT).show();
            BusManager.getInstance().post(new TodosService.Request("CallLog", TodosService.Request.REQUEST_REGISTER_SIP, result));
        }
    }

    private void showInvalidSessionDialog() {
        if (getActivity() != null) {
            MessageDialog dialog = new MessageDialog(getActivity());
            dialog.setMessage(getString(R.string.msg_invalid_session));
            dialog.show();
        }
    }

    private void hideKeyboard() {
        InputMethodManager mImm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mImm != null) {
            mImm.hideSoftInputFromWindow(mSelectdSearch.getWindowToken(), 0);
        }
    }

    private void refresh() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.detach(this).attach(this).commit();
    }

    public void onResume() {
        super.onResume();
        RealmManager mRealmManager;
        Realm mRealm;
        RealmResults<CallLog> mCallLogResults;
        RealmResults<CallLog> mMissedCallLogResults;

        mRealm = Realm.getDefaultInstance();
        mRealmManager = RealmManager.newInstance();
        mCallLogResults = mRealmManager.loadCallLogs(mRealm);
        mMissedCallLogResults = mRealmManager.loadMissedCallLogs(mRealm);

        mCallLogResults = mRealmManager.loadCallLogs(mRealm);

        new MainActivity().refreshMainCallLogFragment();
    }
}

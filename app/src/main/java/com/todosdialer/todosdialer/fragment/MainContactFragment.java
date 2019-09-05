package com.todosdialer.todosdialer.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.todosdialer.todosdialer.ChatActivity;
import com.todosdialer.todosdialer.NewChatActivity;
import com.todosdialer.todosdialer.OutgoingCallActivity;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.SplashActivity;
import com.todosdialer.todosdialer.adapter.FriendListAdapter;
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

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import me.everything.providers.android.contacts.Contact;
import me.everything.providers.android.contacts.ContactsProvider;

public class MainContactFragment extends Fragment {
    private AppCompatEditText mEditKeyword;
    private List<Friend> mFriends;
    private String mSearchKeyword = "";
    private FriendListAdapter mAdapter;
    private User mUser;
    private Realm mRealm;
    private ImageView mImgSearch;
    private ImageView mImgSearchCancel;
    private TextView textTotalSize;
    private ProgressBar mProgressBar;


    public MainContactFragment() {
    }

    public static MainContactFragment newInstance(List<Friend> friends) {
        Bundle args = new Bundle();
        MainContactFragment fragment = new MainContactFragment();
        fragment.setFriends(friends);
        fragment.setArguments(args);
        return fragment;
    }

    public void setFriends(List<Friend> friends) {
        mFriends = friends == null ? new ArrayList<Friend>() : friends;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRealm = Realm.getDefaultInstance();
        mUser = RealmManager.newInstance().loadUser(mRealm);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_contact, container, false);

        mImgSearch = rootView.findViewById(R.id.mbtn_UserSearch);
        mImgSearchCancel = rootView.findViewById(R.id.mbtn_Search_cancel);
        mEditKeyword = rootView.findViewById(R.id.edit_keyword);
        mProgressBar = rootView.findViewById(R.id.progress_bar);
        textTotalSize = rootView.findViewById(R.id.text_total_size);
        String sizeText = getString(R.string.term_whole) + " " + mFriends.size() + getString(R.string.term_friend_unit);
        textTotalSize.setText(sizeText);

        RecyclerView recyclerView = rootView.findViewById(R.id.friend_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAdapter = new FriendListAdapter(getActivity().getApplicationContext());
        mAdapter.setOnItemClickListener(new FriendListAdapter.OnItemClickListener() {
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
        recyclerView.setAdapter(mAdapter);

        refreshList();

        mEditKeyword.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                if(mEditKeyword.getText().toString().length() > 0) {
                    mImgSearch.setVisibility(View.GONE);
                    mImgSearchCancel.setVisibility(View.VISIBLE);
                } else {
                    mImgSearch.setVisibility(View.VISIBLE);
                    mImgSearchCancel.setVisibility(View.GONE);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearchKeyword = mEditKeyword.getText().toString().trim();

                refreshList();
            }

        });

        mImgSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*mSearchKeyword = mEditKeyword.getText().toString().trim();
                refreshList();*/
                hideKeyboard();
            }
        });

        mEditKeyword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                hideKeyboard();
                return true;
            }
        });

        mImgSearchCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*mSearchKeyword = mEditKeyword.getText().toString().trim();
                refreshList();*/
                hideKeyboard();
                mEditKeyword.setText("");
            }
        });

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
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideKeyboard();
        mRealm.close();

    }

    @Override
    public void onResume() {
        super.onResume();

//        mProgressBar.setVisibility(View.VISIBLE);
//        try {
////            RealmManager.newInstance().deleteAllFriends(mRealm);
//            new DumpContactTask().execute();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    private class DumpContactTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            ContactsProvider contactsProvider = new ContactsProvider(getActivity().getApplicationContext());
            List<Contact> contacts = contactsProvider.getContacts().getList();

            HashMap<String, String> phoneMap = new HashMap<>();
            ArrayList<Friend> people = new ArrayList<>();
            for (int i = 0; i < contacts.size(); i++) {
                if (TextUtils.isEmpty(phoneMap.get(contacts.get(i).phone))) {
                    people.add(new Friend(contacts.get(i).id,
                            contacts.get(i).displayName,
                            contacts.get(i).phone,
                            contacts.get(i).normilizedPhone,
                            contacts.get(i).uriPhoto));
                }
                phoneMap.put(contacts.get(i).phone, contacts.get(i).displayName);
            }

//            RealmManager.newInstance().deleteAllFriends(mRealm);
            RealmManager.newInstance().insertFriends(mRealm, people);
            mRealm.close();
            return people.size();
        }

        @Override
        protected void onPostExecute(Integer data) {
            Log.i(getClass().getSimpleName(), "Load all contacts. size is: " + data);

            mFriends = RealmManager.newInstance().loadFriends(mRealm);
            mAdapter.setFriendList(mFriends);

            textTotalSize.setText("총 " + mFriends.size() + "명");

            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void sendMessage(String number) {
        if (RealmManager.newInstance().hasChatRoom(mRealm, number)) {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_KEY_NUMBER, number);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getActivity(), NewChatActivity.class);
            intent.putExtra(NewChatActivity.EXTRA_KEY_NUMBER, number);
            startActivity(intent);
        }
    }

    private void call(String phoneNumber) {
        checkSession(phoneNumber);
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
            BusManager.getInstance().post(new TodosService.Request("Contact", TodosService.Request.REQUEST_REGISTER_SIP, result));
        }
    }

    private void showInvalidSessionDialog() {
        if (getActivity() != null) {
            MessageDialog dialog = new MessageDialog(getActivity());
            dialog.setMessage(getString(R.string.msg_invalid_session));
            dialog.show();
        }
    }

    private void refreshList() {
        ArrayList<Friend> refreshList = new ArrayList<>();
        for (int i = 0; i < mFriends.size(); i++) {
            Friend checking = mFriends.get(i);

            if (TextUtils.isDigitsOnly(mSearchKeyword)) {
                if (checking.getNumber().contains(mSearchKeyword)) {
                    refreshList.add(checking);
                }
            } else {
                if (TextUtils.isEmpty(mSearchKeyword) || KoreanTextMatcher.isMatch(checking.getName(), mSearchKeyword)) {
                    refreshList.add(checking);
                }
            }
        }
        mAdapter.setFriendList(refreshList);

        if (mEditKeyword.getText().toString().equals("")) {
            textTotalSize.setText("총 " + refreshList.size() + "명");
        } else {
            textTotalSize.setText("검색결과 " + refreshList.size() + "명");
        }
    }

    private void hideKeyboard() {
        InputMethodManager mImm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mImm != null) {
            mImm.hideSoftInputFromWindow(mEditKeyword.getWindowToken(), 0);
        }
    }

}

package com.todosdialer.todosdialer;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.todosdialer.todosdialer.adapter.MessageListAdapter;
import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.CheckSessionBody;
import com.todosdialer.todosdialer.api.body.SendingMessageBody;
import com.todosdialer.todosdialer.api.response.BaseResponse;
import com.todosdialer.todosdialer.api.response.SipSessionInfoResponse;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.ChatRoom;
import com.todosdialer.todosdialer.model.Friend;
import com.todosdialer.todosdialer.model.Message;
import com.todosdialer.todosdialer.model.User;
import com.todosdialer.todosdialer.util.Utils;
import com.todosdialer.todosdialer.view.MessageDialog;

import java.util.ArrayList;
import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_KEY_NUMBER = "ChatActivity.key_number";
    private static final int MAX_EN_LENGTH = 160;
    private static final int MAX_KO_LENGTH = 80;

    private RecyclerView mMessageRecyclerView;
    private ProgressBar mProgressBar;
    private AppCompatEditText mEditMessage;

    private MessageListAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private ChatRoom mChatRoom;
    private static Friend mFriend;

    private Realm mRealm;
    private RealmManager mRealmManager;
    private boolean mIsScrolling = false;

    private RealmResults<Message> mMessageResults;

    private RealmChangeListener<Realm> mRealmListener = new RealmChangeListener<Realm>() {
        @Override
        public void onChange(@NonNull Realm realm) {
            refresh();
        }
    };

    public static Friend currentChatFriend() {
        return mFriend;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRealm = Realm.getDefaultInstance();
        mRealmManager = RealmManager.newInstance();
        if (!isChatRoomValid()) {
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        initActionBarListener();

        mProgressBar = findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);
        TextView textFriendsName = findViewById(R.id.text_f_name);
        if(Pattern.matches("^[0-9]+$", mFriend.getName())){
            String tmptextFriendsName = mFriend.getName();
            String area = "";
            String state = "";
            String serial = "";
            if(tmptextFriendsName.length() == 9){
                area = tmptextFriendsName.substring(0,2);
                state = tmptextFriendsName.substring(2,5);
                serial = tmptextFriendsName.substring(5,9);
                tmptextFriendsName = area + "-" + state + "-" + serial;
            }else if(tmptextFriendsName.length() == 10){
                if(tmptextFriendsName.startsWith("02")){
                    area = tmptextFriendsName.substring(0,2);
                    state = tmptextFriendsName.substring(2,6);
                    serial = tmptextFriendsName.substring(6,10);
                    tmptextFriendsName = area + "-" + state + "-" + serial;
                }else{
                    area = tmptextFriendsName.substring(0,3);
                    state = tmptextFriendsName.substring(3,6);
                    serial = tmptextFriendsName.substring(6,10);
                    tmptextFriendsName = area + "-" + state + "-" + serial;
                }
            } else if (tmptextFriendsName.length() == 11) {
                area = tmptextFriendsName.substring(0,3);
                state = tmptextFriendsName.substring(3,7);
                serial = tmptextFriendsName.substring(7,11);
                tmptextFriendsName = area + "-" + state + "-" + serial;
            }
            textFriendsName.setText(tmptextFriendsName);
        }else {
            textFriendsName.setText(mFriend.getName());
        }

        mEditMessage = findViewById(R.id.edit_message);
        mMessageRecyclerView = findViewById(R.id.list_message);

        mLayoutManager = new LinearLayoutManager(this);
        mMessageRecyclerView.setLayoutManager(mLayoutManager);
        mMessageRecyclerView.setNestedScrollingEnabled(false);

        mAdapter = new MessageListAdapter();
        mAdapter.setOnInItemClickListener(new MessageListAdapter.OnInItemClickListener() {
            @Override
            public void onMessageClicked(Message message) {
                resendMessage(message);
            }
        });
        mMessageRecyclerView.setAdapter(mAdapter);

        mMessageResults = mRealmManager.loadMessageList(mRealm, mChatRoom.getPhoneNumber());

        mEditMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(mEditMessage.getText().toString().matches(".*[ㄱ-ㅎ 가-힣].*")) {

                    mEditMessage.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_KO_LENGTH)});
                    if (mEditMessage.getText().toString().length() >= MAX_KO_LENGTH) {
                        Toast.makeText(getApplicationContext(), getString(R.string.no_more_then_ko), Toast.LENGTH_SHORT).show();
                    }
                }else {
                    //Log.d("TAG", "onTextChanged: " + mEditMessage.getText().toString().length() );

                    mEditMessage.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_EN_LENGTH)});
                    if (mEditMessage.getText().toString().length() >= MAX_EN_LENGTH) {
                        //Log.d("TAG", "onTextChanged: 160자 이상(chatActivity)");
                        Toast.makeText(getApplicationContext(), getString(R.string.no_more_then_en), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        refresh();

        initListeners();

        mRealm.addChangeListener(mRealmListener);

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("ChatActivity","savedInstanceState is not null");
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRealmManager.clearChatRoomUnreadCount(mRealm, mChatRoom.getPhoneNumber());
    }

    private boolean isChatRoomValid() {
        String phoneNumber = getIntent().getStringExtra(EXTRA_KEY_NUMBER);
        if (TextUtils.isEmpty(phoneNumber)) {
            String errorMsg = "[ChatActivity] Phone number is not valid! Can not calling.";
            RealmManager.newInstance().writeLog(errorMsg);
            return false;
        }


        mChatRoom = mRealmManager.findChatRoom(mRealm, phoneNumber);
        if (mChatRoom == null) {
            Toast.makeText(getApplicationContext(), "Phone number is not valid", Toast.LENGTH_SHORT).show();
            String errorMsg = "[ChatActivity] Can not find chat rqoom of which phone is: " + phoneNumber;
            RealmManager.newInstance().writeLog(errorMsg);
            return false;
        }

        mFriend = new Friend(mChatRoom.getFid(),
                mChatRoom.getName(),
                mChatRoom.getPhoneNumber(),
                mChatRoom.getPhoneNumber(),
                mChatRoom.getUriPhoto());

        return true;
    }

    private void initActionBarListener() {
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        findViewById(R.id.btn_remove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRealmManager.deleteChatRoomWithMessage(mRealm, mChatRoom.getPhoneNumber());

                finish();
            }
        });
    }

    private void initListeners() {
        mMessageRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    mMessageRecyclerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scrollToBottom();
                        }
                    }, 250);
                }
            }
        });

        findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                if (isAllValidInput()) {
                    String message = mEditMessage.getText().toString();
                    mEditMessage.setText("");
                    sendMessage(mFriend.getNumber(), message);
                }
            }
        });
    }

    private boolean isAllValidInput() {
        if (mEditMessage.getText().toString().length() > 0) {
                if(mEditMessage.getText().toString().matches(".*[ㄱ-ㅎ가-힣]+.*")){
                    if (mEditMessage.getText().toString().length() <= MAX_KO_LENGTH) {
                        return true;
                    }else {
                        Toast.makeText(getApplicationContext(), getString(R.string.msg_komessage_is_too_long), Toast.LENGTH_LONG).show();
                        return false;
                    }
                }else {
                    if (mEditMessage.getText().toString().length() <= MAX_EN_LENGTH) {
                        return true;
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.msg_enmessage_is_too_long), Toast.LENGTH_LONG).show();
                        return  false;
                    }
                }
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.msg_there_are_no_message_content), Toast.LENGTH_LONG).show();
            return false;
        }
    }
    //재전송 기능
    private void resendMessage(Message message) {
        if (message != null &&
                message.getInputState() == Message.INPUT_STATE_IN &&
                message.getSendState() == Message.SEND_STATE_FAIL) {
            mRealmManager.deleteMessage(mRealm, message);

            sendMessage(message.getPhoneNumber(), message.getBody());
        }
    }
    //전송 기능
    private void sendMessage(String phoneNumber, String body) {
        mProgressBar.setVisibility(View.VISIBLE);
        User user = RealmManager.newInstance().loadUser(mRealm);
        checkSession(phoneNumber, body, user.getId());
    }

    private void checkSession(final String phoneNumber, final String body, String userEmail) {
        RetrofitManager.retrofit(getApplicationContext()).create(Client.Api.class)
                .checkSessionInfo(new CheckSessionBody(getApplicationContext(), userEmail))
                .enqueue(new ApiCallback<SipSessionInfoResponse>() {
                    @Override
                    public void onSuccess(SipSessionInfoResponse response) {
                        if (response.isSuccess()) {
                            sendToServer(response.result.getSipID(), response.result.getSipPW(), response.result.getSmsSVRURL(), phoneNumber, body);
                        } else {
                            mProgressBar.setVisibility(View.GONE);
                            showInvalidSessionDialog();
                        }
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        mProgressBar.setVisibility(View.GONE);
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showInvalidSessionDialog() {
        MessageDialog dialog = new MessageDialog(this);
        dialog.setMessage(getString(R.string.msg_invalid_session));
        dialog.show();
    }

    private void sendToServer(String sipId, String sipPw, String url, final String phoneNumber, final String body) {
        String formattedSipId = sipId.replace(" ", "").replace("-", "");
        RetrofitManager.messagingRetrofit(this, url).create(Client.MessageApi.class)
                .sendMessage(formattedSipId, sipPw, new SendingMessageBody(phoneNumber, body))
                .enqueue(new ApiCallback<BaseResponse>() {
                    @Override
                    public void onSuccess(BaseResponse response) {
                        updateDbAndSend(phoneNumber, body, response.code == 0 ? Message.SEND_STATE_SUCCESS : Message.SEND_STATE_FAIL);
                        mProgressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        mProgressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void updateDbAndSend(String phoneNumber, String body, int sendState) {
        Message message = mRealmManager.insertMessage(mRealm,
                mFriend,
                body,
                Message.INPUT_STATE_IN,
                Message.READ_STATE_READ,
                sendState);
        mRealmManager.createOrUpdateChatRoom(mRealm, message);


        Log.e(getClass().getSimpleName(), "PhoneNumber: " + phoneNumber);
        Log.e(getClass().getSimpleName(), "PhoneNumber: " + mFriend.getNumber());
        Log.e(getClass().getSimpleName(), "Fid: " + mFriend.getPid());
        Log.e(getClass().getSimpleName(), "Name: " + mFriend.getName());

        Log.e(getClass().getSimpleName(), "message PhoneNumber: " + message.getPhoneNumber());
        Log.e(getClass().getSimpleName(), "message Fid: " + message.getFid());
        Log.e(getClass().getSimpleName(), "message Name: " + message.getName());

    }

    private void refresh() {
        ArrayList<Message> messageList = new ArrayList<>();
        for (Message room : mMessageResults) {
            messageList.add(mRealm.copyFromRealm(room));
        }

        if (mAdapter != null) {
            mAdapter.setMessageList(messageList);
        }

        scrollToBottom();
    }

    private void scrollToBottom() {
        if (!mIsScrolling) {
            mIsScrolling = true;
            mLayoutManager.scrollToPosition(mAdapter.getItemCount() - 1);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mIsScrolling = false;
                }
            }, 100);
        }
    }

    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && mEditMessage != null) {
            imm.hideSoftInputFromWindow(mEditMessage.getWindowToken(), 0);

            mEditMessage.setFocusable(false);
            mEditMessage.setFocusableInTouchMode(false);
            mEditMessage.setFocusable(true);
            mEditMessage.setFocusableInTouchMode(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideKeyBoard();

        mFriend = null;
        mRealm.removeChangeListener(mRealmListener);
        mRealm.close();
    }
}

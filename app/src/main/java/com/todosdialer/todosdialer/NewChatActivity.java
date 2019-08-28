package com.todosdialer.todosdialer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
//import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
//import android.util.Log;
//import android.view.KeyEvent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.CheckSessionBody;
import com.todosdialer.todosdialer.api.body.SendingMessageBody;
import com.todosdialer.todosdialer.api.response.BaseResponse;
import com.todosdialer.todosdialer.api.response.SipSessionInfoResponse;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.Friend;
import com.todosdialer.todosdialer.model.Message;
import com.todosdialer.todosdialer.model.User;
import com.todosdialer.todosdialer.util.KoreanTextMatcher;
import com.todosdialer.todosdialer.view.MessageDialog;

import java.util.ArrayList;
import java.util.List;
//import java.util.regex.Pattern;

import io.realm.Realm;

public class NewChatActivity extends AppCompatActivity {
    public static final String EXTRA_KEY_NUMBER = "com.todosdialer.todosdialer.key_number";
    public static final String EXTRA_KEY_NAME = "com.todosdialer.todosdialer.key_name";

    private static final int MAX_KO_LENGTH = 80;
    private static final int MAX_EN_LENGTH = 160;

    private ProgressBar mProgressBar;
    private AutoCompleteTextView mEditPhoneNumber;
    private AppCompatEditText mEditMessage;
    private ImageButton mBtnSend;

    private List<Friend> mWholeFriends = new ArrayList<>();
    private List<Friend> mFilteredFriends = new ArrayList<>();

    private String mSelectedPhoneNumber;

    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        mProgressBar = findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);
        mEditPhoneNumber = findViewById(R.id.edit_keyword);
        mEditMessage = findViewById(R.id.edit_message);
        mBtnSend = findViewById(R.id.btn_send);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();

                if (isAllValidInput()) {
                    mBtnSend.setEnabled(false);
                    sendMessage(TextUtils.isEmpty(mSelectedPhoneNumber) ?
                            mEditPhoneNumber.getText().toString().replace("-","") :
                            mSelectedPhoneNumber, mEditMessage.getText().toString());
                }
            }
        });

        mEditPhoneNumber.setAdapter(new AutoComAdapter(this, R.layout.view_autocomplete_item, mFilteredFriends));
        mEditPhoneNumber.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Friend c = mFilteredFriends.get(arg2);
                mSelectedPhoneNumber = c.getNumber();
                mEditPhoneNumber.setText(TextUtils.isEmpty(c.getName()) ? c.getNumber() : c.getName());
                mEditPhoneNumber.setSelection(mEditPhoneNumber.getText().toString().length());
                mEditPhoneNumber.setTag(c);
            }
        });

       mEditPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(mEditPhoneNumber.getText().toString().length() > 1){

                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().matches("^[0-9\\-]*$")) {
                    if(s.length() > 3 && s.length() <= 8) {
                        if(Character.isDigit(s.charAt(3))) {
                            s.insert(3, "-");
                        }
                    }
                    if(s.length() > 8 ) {
                        if(Character.isDigit(s.charAt(3))) {
                            s.insert(3, "-");
                        }
                        if(Character.isDigit(s.charAt(8))) {
                            s.insert(8, "-");
                        }
                    }
                }
            }
        });

        mEditMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(mEditMessage.getText().toString().trim().matches(".*[ㄱ-ㅎ 가-힣]+.*")) {
                    mEditMessage.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_KO_LENGTH)});
                    if(mEditMessage.getText().toString().trim().length() >= MAX_KO_LENGTH){
                        Toast.makeText(getApplicationContext(), getString(R.string.no_more_then_ko), Toast.LENGTH_SHORT).show();
                    }
                }else{
                    mEditMessage.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_EN_LENGTH)});
                    if(mEditMessage.getText().toString().trim().length() >= MAX_EN_LENGTH) {
                        Toast.makeText(getApplicationContext(), getString(R.string.no_more_then_en), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        initData();

        //home 버튼을 이용해 앱을 나갔다가 다시 실행할때 죽는 문제 해결
        if (savedInstanceState != null) {
            Log.d("NewChatActivity","savedInstanceState is not null");
            finish();
            return;
        }
    }

    private void initData() {
        String phoneNumber = getIntent().getStringExtra(EXTRA_KEY_NUMBER);
        String phoneName = getIntent().getStringExtra(EXTRA_KEY_NAME);

        mSelectedPhoneNumber = phoneNumber;

        if (!TextUtils.isEmpty(phoneName)) {
            mEditPhoneNumber.setText(phoneName);
        } else if (!TextUtils.isEmpty(phoneNumber)) {
            mEditPhoneNumber.setText(phoneNumber);
        }

        mRealm = Realm.getDefaultInstance();
        mWholeFriends = RealmManager.newInstance().loadFriendsAsArrayList(mRealm);
        mFilteredFriends.addAll(mWholeFriends);
    }

    private boolean isAllValidInput() {
        if (!TextUtils.isEmpty(mSelectedPhoneNumber)) {
            if (mEditMessage.getText().toString().length() > 0) {
                if(mEditMessage.getText().toString().matches(".*[ㄱ-ㅎ 가-힣]+.*")) {
                    if (mEditMessage.getText().toString().trim().length() <= MAX_KO_LENGTH) {
                        return true;
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.msg_komessage_is_too_long), Toast.LENGTH_LONG).show();
                        return false;
                    }
                }else{
                    if(mEditMessage.getText().toString().trim().length() <= MAX_EN_LENGTH){
                        return true;
                    }else{
                        Toast.makeText(getApplicationContext(), getString(R.string.msg_enmessage_is_too_long), Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.msg_there_are_no_message_content), Toast.LENGTH_LONG).show();
                return false;
            }
        } else {
            if (mEditPhoneNumber.getText().length() > 0) {
                if(isNumber(mEditPhoneNumber.getText().toString().replace("-",""))){
                    return true;
                }else{
                    Toast.makeText(getApplicationContext(), getString(R.string.msg_there_is_no_members), Toast.LENGTH_LONG).show();
                    return false;
                }
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.msg_there_is_no_numbers), Toast.LENGTH_LONG).show();
                return false;
            }
        }
    }

    private boolean isNumber(String str){
        try{
            int tmpn = Integer.parseInt(str);
            return true;
        }catch(Exception e){
            return false;
        }
    }

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
                            mBtnSend.setEnabled(true);

                            showInvalidSessionDialog();
                        }
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        mProgressBar.setVisibility(View.GONE);
                        mBtnSend.setEnabled(true);
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
                        mProgressBar.setVisibility(View.GONE);
                        mBtnSend.setEnabled(true);
                        updateDbAndSend(phoneNumber, body, response.code == 0 ? Message.SEND_STATE_SUCCESS : Message.SEND_STATE_FAIL);
                        startChatActivity(phoneNumber);
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        mProgressBar.setVisibility(View.GONE);
                        mBtnSend.setEnabled(true);
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void updateDbAndSend(String phoneNumber, String body, int sendState) {
        RealmManager realmManager = RealmManager.newInstance();
        phoneNumber.replace("-","");
        Friend friend = realmManager.findFriend(mRealm, phoneNumber);
        if (friend == null) {
            friend = new Friend();
            friend.setName(phoneNumber);
            friend.setNumber(phoneNumber);
        }

        Message message = realmManager.insertMessage(mRealm,
                friend,
                body,
                Message.INPUT_STATE_IN,
                Message.READ_STATE_READ,
                sendState);
        realmManager.createOrUpdateChatRoom(mRealm, message);
    }

    private void startChatActivity(String phoneNumber) {
        finish();

        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_KEY_NUMBER, phoneNumber);
        startActivity(intent);
    }

    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && mEditMessage != null && mEditPhoneNumber != null) {
            imm.hideSoftInputFromWindow(mEditMessage.getWindowToken(), 0);
            imm.hideSoftInputFromWindow(mEditPhoneNumber.getWindowToken(), 0);

            mEditPhoneNumber.setFocusable(false);
            mEditPhoneNumber.setFocusableInTouchMode(false);
            mEditPhoneNumber.setFocusable(true);
            mEditPhoneNumber.setFocusableInTouchMode(true);

            mEditMessage.setFocusable(false);
            mEditMessage.setFocusableInTouchMode(false);
            mEditMessage.setFocusable(true);
            mEditMessage.setFocusableInTouchMode(true);
        }
    }

    @Override
    protected void onDestroy() {
        mRealm.close();
        super.onDestroy();
    }


    private class AutoComAdapter extends ArrayAdapter<Friend> {
        private List<Friend> items;

        AutoComAdapter(Context context, int ResourceId, List<Friend> item) {
            super(context, ResourceId, item);

            items = item;
        }

        @NonNull
        @Override
        public View getView(int position, View corver, @NonNull ViewGroup parent) {
            Friend con = items.get(position);

            if (corver == null) {
                corver = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_autocomplete_item, parent, false);
                if (con != null) {
                    TextView name = corver.findViewById(R.id.text_f_name);
                    TextView number = corver.findViewById(R.id.text_f_number);

                    name.setText(con.getName());
                    number.setText(con.getNumber());
                }

            }
            return corver;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @NonNull
        public Filter getFilter() {
            return new Filter() {

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    Friend friend = (Friend) resultValue;
                    return friend.getName() + " " + friend.getNumber();
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    items = (ArrayList<Friend>) results.values;
                    mFilteredFriends = items;
                    if (results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }

                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    ArrayList<Friend> resultPrimaryFilter = new ArrayList<>();
                    ArrayList<Friend> resultFilterByNumber = new ArrayList<>();
                    if (constraint != null) {
                        for (Friend c : mWholeFriends) {
                            if (KoreanTextMatcher.isMatch(c.getName(), String.valueOf(constraint))) {
                                resultPrimaryFilter.add(c);
                            } else if (c.getNumber().replace("-", "").contains(constraint.toString().replace("-",""))) {
                                resultFilterByNumber.add(c);
                            }
                        }
                    }
                    resultPrimaryFilter.addAll(resultFilterByNumber);

                    filterResults.count = resultPrimaryFilter.size();
                    filterResults.values = resultPrimaryFilter;

                    return filterResults;
                }
            };
        }
    }

    private void setActionbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            // Get the ActionBar here to configure the way it behaves.
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
//            actionBar.setHomeAsUpIndicator(R.drawable.arrow_left);
//            actionBar.setHomeButtonEnabled(true);

            TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
            toolbarTitle.setText(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

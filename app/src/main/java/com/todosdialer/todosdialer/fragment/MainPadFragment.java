package com.todosdialer.todosdialer.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.todosdialer.todosdialer.ChatActivity;
import com.todosdialer.todosdialer.NewChatActivity;
import com.todosdialer.todosdialer.OutgoingCallActivity;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.SearchFriendActivity;
import com.todosdialer.todosdialer.SplashActivity;
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
import com.todosdialer.todosdialer.view.ContactView;
import com.todosdialer.todosdialer.view.DialView;
import com.todosdialer.todosdialer.view.MessageDialog;
import com.todosdialer.todosdialer.worker.ToneWorker;

import org.eclipse.paho.client.mqttv3.util.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;

public class MainPadFragment extends Fragment {
    private static final int REQUEST_ACTIVITY = 983;
    private static final int REQUEST_SEARCH_FRIENDS = 123;

    private static final String DIAL_0 = "0";
    private static final String DIAL_1 = "1";
    private static final String DIAL_2 = "2";
    private static final String DIAL_3 = "3";
    private static final String DIAL_4 = "4";
    private static final String DIAL_5 = "5";
    private static final String DIAL_6 = "6";
    private static final String DIAL_7 = "7";
    private static final String DIAL_8 = "8";
    private static final String DIAL_9 = "9";
    private static final String DIAL_STAR = "*";
    private static final String DIAL_SHOP = "#";

    private ImageView mImgAdd;
    private ImageView mImgSms;
    private ImageView mImgSearch;
    private ImageView mImgBackspace;
    private AppCompatEditText mEditNumber;
    private ContactView mContactView;
    private ImageButton mImgBtnMore;

    private List<Friend> mFriends;

    private HashMap<String, String> mInitSoundsMap;
    private User mUser;
    private CallLog mLastCallLog;
    private boolean mIsProcessing = false;
    private ToneWorker mToneWorker;

    public static MainPadFragment newInstance(List<Friend> friends, CallLog lastCallLog) {

        Bundle args = new Bundle();

        MainPadFragment fragment = new MainPadFragment();
        fragment.setFriends(friends);
        fragment.setLastCallLog(lastCallLog);
        fragment.setArguments(args);
        return fragment;
    }

    public void setFriends(List<Friend> friends) {
        mFriends = friends == null ? new ArrayList<Friend>() : friends;
    }

    public void setLastCallLog(CallLog callLog) {
        mLastCallLog = callLog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Realm realm = Realm.getDefaultInstance();
        mUser = RealmManager.newInstance().loadUser(realm);
        realm.close();
        mToneWorker = new ToneWorker(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_pad, container, false);
        mEditNumber = rootView.findViewById(R.id.edit_phone_number);
        mEditNumber.setInputType(InputType.TYPE_NULL);

        mImgAdd = rootView.findViewById(R.id.btn_add_contact);
        mImgSearch = rootView.findViewById(R.id.btn_search);
        mImgBackspace = rootView.findViewById(R.id.btn_backspace);
        mContactView = rootView.findViewById(R.id.contact_search);
        mImgBtnMore = rootView.findViewById(R.id.btn_see_more);
        mContactView.setButtonVisible(false);
        mImgSms = rootView.findViewById(R.id.btn_sms);

        mImgBackspace.setEnabled(false);
        mImgSms.setEnabled(false);

        makeInitSoundsMap();

        initBtnListener();

        initDial((DialView) rootView.findViewById(R.id.btn_dial_one), DIAL_1, new String[]{"ㄱ", "ㅋ"});
        initDial((DialView) rootView.findViewById(R.id.btn_dial_two), DIAL_2, new String[]{"ㄴ"});
        initDial((DialView) rootView.findViewById(R.id.btn_dial_three), DIAL_3, new String[]{"ㄷ", "ㅌ"});
        initDial((DialView) rootView.findViewById(R.id.btn_dial_four), DIAL_4, new String[]{"ㄹ"});
        initDial((DialView) rootView.findViewById(R.id.btn_dial_five), DIAL_5, new String[]{"ㅁ"});
        initDial((DialView) rootView.findViewById(R.id.btn_dial_six), DIAL_6, new String[]{"ㅂ", "ㅍ"});
        initDial((DialView) rootView.findViewById(R.id.btn_dial_seven), DIAL_7, new String[]{"ㅅ"});
        initDial((DialView) rootView.findViewById(R.id.btn_dial_eight), DIAL_8, new String[]{"ㅇ"});
        initDial((DialView) rootView.findViewById(R.id.btn_dial_nine), DIAL_9, new String[]{"ㅈ", "ㅊ"});
        initDial((DialView) rootView.findViewById(R.id.btn_dial_star), DIAL_STAR, new String[]{});
        initDial((DialView) rootView.findViewById(R.id.btn_dial_zero), DIAL_0, new String[]{"ㅎ"});
        initDial((DialView) rootView.findViewById(R.id.btn_dial_shop), DIAL_SHOP, new String[]{});

        mContactView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                call(mContactView.getPhoneNumber());
            }
        });

        rootView.findViewById(R.id.btn_call_to_someone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = mEditNumber.getText().toString().replace("-","");
                if (phoneNumber.length() > 0) {
                    call(phoneNumber);
                } else if (mLastCallLog != null && !TextUtils.isEmpty(mLastCallLog.getNumber())) {
                    mEditNumber.setText(mLastCallLog.getNumber());
                    mEditNumber.setSelection(mEditNumber.length());
                }
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
        } else {
            mToneWorker.start();
        }
    }

    private void makeInitSoundsMap() {
        mInitSoundsMap = new HashMap<>();
        mInitSoundsMap.put("1", "ㄱ");
        mInitSoundsMap.put("11", "ㅋ");
        mInitSoundsMap.put("2", "ㄴ");
        mInitSoundsMap.put("3", "ㄷ");
        mInitSoundsMap.put("33", "ㅌ");
        mInitSoundsMap.put("4", "ㄹ");
        mInitSoundsMap.put("5", "ㅁ");
        mInitSoundsMap.put("6", "ㅂ");
        mInitSoundsMap.put("66", "ㅍ");
        mInitSoundsMap.put("7", "ㅅ");
        mInitSoundsMap.put("8", "ㅇ");
        mInitSoundsMap.put("9", "ㅈ");
        mInitSoundsMap.put("99", "ㅊ");
        mInitSoundsMap.put("0", "ㅎ");
        mInitSoundsMap.put("*", "");
        mInitSoundsMap.put("#", "");
    }

    private void initDial(DialView dialView, String dial, String[] initSounds) {
        StringBuilder initSound = new StringBuilder();
        for (String sound : initSounds) {
            initSound.append(sound);
        }
        dialView.setNumberWithInitSound(dial, initSound.toString());
        dialView.setOnClickListener(new DialListener(dial));
    }

    private void initBtnListener() {
        mEditNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                /*Log.d("TAB","마지막 문자 : " + mEditNumber.getText().toString().charAt(mEditNumber.getText().length() - 1));*/
                s = s.toString().replace("-","");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setUiByText(mEditNumber.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().matches("^[0-9\\-]*$")) {
                    if(s.toString().startsWith("01")) { /*핸드폰 번호 '-' 표기*/
                        if (s.length() > 3) {
                            if (Character.isDigit(s.charAt(3))) {
                                s.insert(3, "-");
                            }
                        }
                        if (s.length() > 8) {
                            if (Character.isDigit(s.charAt(8))) {
                                s.insert(8, "-");
                            }
                        }

                    } else if (s.toString().startsWith("02")) { /*일반전화 지역번호 서울일 경우 */

                        if (s.length() > 2) {
                            if (Character.isDigit(s.charAt(2))) {
                                s.insert(2,"-");
                            }
                        }
                        if (s.length() > 6 && s.length() < 12) {
                            /* 지울때 -의 위치 변경함 국번 4-> 3*/
                            if (s.length() == 11 && !Character.isDigit(s.charAt(7))) {
                                s.delete(7,8);
                            }
                            if (Character.isDigit(s.charAt(6))) {
                                s.insert(6, "-");
                            }
                        }
                        /*입력할때 - 위치 변경 국번 3 -> 4*/
                        if (s.length() == 12) {
                            if(!Character.isDigit(s.charAt(6))) {
                                String tmp1 = s.toString().substring(0, 6);
                                String tmp2 = s.toString().substring(7, 8);
                                String tmp3 = s.toString().substring(8, s.length());

                                mEditNumber.setText(tmp1 + tmp2 + "-" + tmp3);
                            }

                        }
                        if (s.length() > 12){
                            if(Character.isDigit(s.charAt(7))){
                                s.insert(7,"-");
                            }
                        }
                    } else { /*일반전화번호 - 서울 제외한 타지역*/
                            if (s.length() > 3) {
                                if (Character.isDigit(s.charAt(3))) {
                                    s.insert(3, "-");
                                }
                            }
                            if (s.length() > 7 && s.length() < 13) {
                                if (s.length() == 12 && !Character.isDigit(s.charAt(8))) {
                                    s.delete(8,9);
                                }
                                if (Character.isDigit(s.charAt(7))) {
                                    s.insert(7, "-");
                                }

                            }
                            if (s.length() == 13) {
                                if (!Character.isDigit(s.charAt(7))) {
                                    String tmp1 = s.toString().substring(0, 7);
                                    String tmp2 = s.toString().substring(8, 9);
                                    String tmp3 = s.toString().substring(9, s.length());

                                    mEditNumber.setText(tmp1 + tmp2 + "-" + tmp3);
                                }
                            }
                            if (s.length() > 13) {
                                if (Character.isDigit(s.charAt(8))) {
                                    s.insert(8, "-");
                                }
                            }
                    }
                }
            }
        });

        mImgAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToContact(mEditNumber.getText().toString());
            }
        });

        mImgBackspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeText();
            }
        });

        mImgBackspace.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mEditNumber.setText("");
                return true;
            }
        });

        mImgSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = mEditNumber.getText().toString();
                sendMessage(number);
            }
        });

        mImgBtnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchFriendsActivity();
            }
        });
    }

    private void removeText() {
        String number = mEditNumber.getText().toString();
        int cursorPos = mEditNumber.getSelectionEnd();
        int selection = number.length();
        Log.e("removeText","cursorPos : " + cursorPos);
        Log.e("removeText","selection : " + selection);
        Log.e("removeText","number : " + number);
        if (number.length() > 0) {
            if (cursorPos +2 >= number.length()) {
                number = number.substring(0, number.length() - 1);
                selection = number.length();
            } else if (cursorPos > 0) {
                String subString = number.substring(cursorPos);
                number = number.substring(0, cursorPos - 1) + subString;
                selection = cursorPos - 1;
            } else {
                selection = cursorPos;
            }
        }
        mEditNumber.setText(number);
        mEditNumber.setSelection(selection);
    }

    private void addToContact(String number) {
        Intent intent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, Uri.parse("tel:" + number));
        intent.putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, true);
        startActivityForResult(intent, REQUEST_ACTIVITY);
    }

    private void setUiByText(String numberText) {
        mContactView.setVisibility(View.INVISIBLE);
        mImgBtnMore.setVisibility(View.INVISIBLE);

        if (TextUtils.isEmpty(numberText)) {
//            mImgBackspace.setVisibility(View.INVISIBLE);
            mImgBackspace.setEnabled(false);
            mImgAdd.setVisibility(View.INVISIBLE);
            mImgSearch.setVisibility(View.VISIBLE);
            mImgSms.setEnabled(false);
        } else {
//            mImgBackspace.setVisibility(View.VISIBLE);
            mImgBackspace.setEnabled(true);
            mImgAdd.setVisibility(View.VISIBLE);
//            mImgSearch.setVisibility(View.INVISIBLE);
            mImgSms.setEnabled(true);
        }

        if (mFriends != null) {

            /* 초성 검색 후 대표번호 가져오기 */
            String initSounds = convertNumberToInitSounds(numberText);
            if (!TextUtils.isEmpty(numberText)) {
                for (int i = 0; i < mFriends.size(); i++) {
                    Friend checking = mFriends.get(i);
                    if (KoreanTextMatcher.isMatch(checking.getName(), "^" + initSounds)) {
                        mContactView.setFriend(checking);
                        mContactView.setVisibility(View.VISIBLE);
                        mImgBtnMore.setVisibility(View.VISIBLE);
                        return;
                    }
                }
                /*전화번호 검색 후 번호 가져오기 - 전화번호 검색은 번호4자리 입력시부터 검색한다.*/
                if(numberText.length() > 3){
                    if(numberText.matches("^[0-9\\-]*$")) {
                        for (int i = 0; i < mFriends.size(); i++) {
                            Friend checking = mFriends.get(i);
                            if (checking.getNumber().replace("-", "").contains(numberText.replace("-", ""))) {
                                mContactView.setFriend(checking);
                                mContactView.setVisibility(View.VISIBLE);
                                mImgBtnMore.setVisibility(View.VISIBLE);
                                return;
                            }
                        }
                    }
                }
                /*첫 음절에서 초성 검색에서 검색 안될 경우 이름 안에 초성 검색이 들어있는지를 검색한다.*/
                for (int i = 0; i < mFriends.size(); i++) {
                    Friend checking = mFriends.get(i);
                    if (KoreanTextMatcher.isMatch(checking.getName(), initSounds)) {
                        mContactView.setFriend(checking);
                        mContactView.setVisibility(View.VISIBLE);
                        mImgBtnMore.setVisibility(View.VISIBLE);
                        return;
                    }
                }
            }
        }
    }

    private String convertNumberToInitSounds(String numberText) {
        StringBuilder initSounds = new StringBuilder();
        if (!TextUtils.isEmpty(numberText)) {
            for (int i = 0; i  < numberText.length(); i++) {
                if(numberText.equals("-")){
                    continue;
                }
                initSounds.append(mInitSoundsMap.get(numberText.substring(i, i + 1)));
            }
        }
        return initSounds.toString();
    }

    private void call(String phoneNumber) {
        if (!mIsProcessing) {
            mIsProcessing = true;
            /*내가 건 전화번호에 대한 정보를 표기한다*/
            for (int i = 0; i < mFriends.size(); i++) {
                Friend checking = mFriends.get(i);
                if (checking.getNumber().replace("-","").contains(phoneNumber.replace("-",""))) {
                    mContactView.setFriend(checking);
                    mContactView.setVisibility(View.VISIBLE);
                    mImgBtnMore.setVisibility(View.VISIBLE);
                }
            }
            checkSession(phoneNumber);
        } else {
            Toast.makeText(getContext(), R.string.msg_procession_to_register_sip, Toast.LENGTH_SHORT).show();
        }
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
                    mIsProcessing = false;
                    showInvalidSessionDialog();
                }
            }

            @Override
            public void onFail(int error, String msg) {
                mIsProcessing = false;
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOutgoingCallActivity(SipSessionInfo result, String phoneNumber) {
        SipInstance sipInstance = SipInstance.getInstance(getActivity());
        if (sipInstance.isAccountAvailable()) {
            Intent intent = new Intent(getActivity(), OutgoingCallActivity.class);
            intent.putExtra(OutgoingCallActivity.EXTRA_KEY_PHONE_NUMBER, phoneNumber);
            startActivity(intent);

            mIsProcessing = false;
        } else {
            mIsProcessing = false;
            BusManager.getInstance().post(new TodosService.Request("MainPad", TodosService.Request.REQUEST_REGISTER_SIP, result));
        }
    }

    private void showInvalidSessionDialog() {
        if (getActivity() != null) {
            MessageDialog dialog = new MessageDialog(getActivity());
            dialog.setMessage(getString(R.string.msg_invalid_session));
            dialog.show();
        }
    }

    private void showSearchFriendsActivity() {
        String editText = mEditNumber.getText().toString().replace("-","");
        //editText.replace("-","");
        String initSounds = convertNumberToInitSounds(editText);
        //initSounds.replace(null,"");

        ArrayList<String> keywords = new ArrayList<>();
        if(editText.length() < 4){
            keywords.add(initSounds);
        }else {
            keywords.add(editText);
            keywords.add(initSounds);
        }
        Log.d("TAG","editText : " + editText);
        Log.d("TAG","initSound : " + initSounds);
        Intent intent = new Intent(getActivity(), SearchFriendActivity.class);
        Log.d("TAG","KEYWORDS : " + SearchFriendActivity.EXTRA_KEY_KEYWORDS + ":" + keywords);
        intent.putExtra(SearchFriendActivity.EXTRA_KEY_KEYWORDS, keywords);
        Log.d("TAG","SEARCH_FRIENDS : " + REQUEST_SEARCH_FRIENDS);
        startActivityForResult(intent, REQUEST_SEARCH_FRIENDS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ACTIVITY) {
            mEditNumber.setText("");
        } else if (requestCode == REQUEST_SEARCH_FRIENDS && data != null) {
            String number = data.getStringExtra(SearchFriendActivity.DATA_KEY_SEARCH_RESULT);
            if (!TextUtils.isEmpty(number)) {
                mEditNumber.setText(number.replace("-", ""));

                call(number);
            }
        }
    }

    private void generateTone(String dial) {
        if (mToneWorker != null) {
            mToneWorker.addDialer(dial);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mToneWorker != null) {
            mToneWorker.release();
        }
    }

    /* 다이얼 버튼을 누른 경우 */
    /* 커서 위치도 확인한다*/
    private class DialListener implements View.OnClickListener {
        String dial;

        DialListener(String dial) {
            this.dial = dial;
        }

        @Override
        public void onClick(View view) {
            /*핸드폰 번호 및 주소록 조회시에 커서 위치*/
            String original = mEditNumber.getText().toString();
            if (20 >= original.length() + 1) {
                String number = original + dial;
                int cursorPos = mEditNumber.getSelectionStart();
                int selection = number.length();
                if (original.length() > 0) {
                    if (cursorPos+2 >= original.length()) {
                        selection = number.length();
                    } else if (cursorPos > 0) {
                        if(cursorPos < 4) {
                            String subString = original.substring(cursorPos);
                            number = original.substring(0, cursorPos) + dial;
                            selection = cursorPos + 1;
                        }else if( cursorPos > 3 && cursorPos < 8){
                            String subString = original.substring(cursorPos+1);
                            number = original.substring(0, cursorPos+1) + dial;
                            selection = cursorPos + 2;
                        }else if( cursorPos > 7){
                            String subString = original.substring(cursorPos);
                            number = original.substring(0, cursorPos+2) + dial;
                            selection = cursorPos + 3;
                        }
                    } else {
                        number = "";
                        selection = number.length();
                    }
                }

                mEditNumber.setText(number);
                mEditNumber.setSelection(selection);
            }
            generateTone(dial);
        }
    }

    private void sendMessage(String number) {
        Realm mRealm = Realm.getDefaultInstance();
        if (RealmManager.newInstance().hasChatRoom(mRealm, number)) {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_KEY_NUMBER, number);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getActivity(), NewChatActivity.class);
            intent.putExtra(NewChatActivity.EXTRA_KEY_NUMBER, number);
            startActivity(intent);
        }
        mRealm.close();
    }

}

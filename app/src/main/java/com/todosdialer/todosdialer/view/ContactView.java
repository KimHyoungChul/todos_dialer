package com.todosdialer.todosdialer.view;


import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.model.Friend;

public class ContactView extends FrameLayout {
    private TextView mTextName;
    private TextView mTextNumber;
    private ImageView mImgPhoto;

    private ImageView mImgMessage;
    private ImageView mImgCall;
    private View mViewBetween;
    private View mContainer;
    private OnContactClickListener mListener;

    public ContactView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ContactView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ContactView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.view_contact, this, false);
        mTextName = itemView.findViewById(R.id.text_person_name);
        mTextNumber = itemView.findViewById(R.id.text_person_number);
        mImgPhoto = itemView.findViewById(R.id.img_person_photo);

        mImgMessage = itemView.findViewById(R.id.btn_message);
        mImgCall = itemView.findViewById(R.id.btn_call);
        mViewBetween = itemView.findViewById(R.id.view_between);

        mImgMessage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onMessageClicked(mTextNumber.getText().toString());
                }
            }
        });

        mImgCall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onCallClicked(mTextNumber.getText().toString());
                }
            }
        });
        mContainer = itemView.findViewById(R.id.container_person);
        mContainer.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mListener != null) {
                            mListener.onTouched(mTextNumber.getText().toString());
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        ContactView.this.performClick();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        addView(itemView);
    }

    public void setFriend(Friend friend) {
        setName(friend.getName());
        setNumber(friend.getNumber());
        setPhoto(friend.getUriPhoto());
    }

    public void setButtonVisible(boolean isVisible) {
        if (isVisible) {
            mImgMessage.setVisibility(VISIBLE);
            mImgCall.setVisibility(VISIBLE);
            mViewBetween.setVisibility(VISIBLE);
        } else {
            mImgMessage.setVisibility(GONE);
            mImgCall.setVisibility(GONE);
            mViewBetween.setVisibility(GONE);
        }
    }

    public void setName(String name) {
        mTextName.setText(name);
    }

    public void setNumber(String number) {
        String tmpNumber = "";
        String area = "";
        String state = "";
        String serial = "";

        tmpNumber = number;
        if (tmpNumber.length() == 8) {
            if(tmpNumber.startsWith("1")){
                state = tmpNumber.substring(0, 4);
                serial = tmpNumber.substring(4, 8);
                tmpNumber = state + "-" + serial;
            }
        }
        if(tmpNumber.length() == 9){
            if(tmpNumber.startsWith("02")) {
                area = tmpNumber.substring(0, 2);
                state = tmpNumber.substring(2, 5);
                serial = tmpNumber.substring(5, 9);
                tmpNumber = area + "-" + state + "-" + serial;
            }
        }else if(tmpNumber.length() == 10){
            if(tmpNumber.startsWith("02")){
                area = tmpNumber.substring(0,2);
                state = tmpNumber.substring(2,6);
                serial = tmpNumber.substring(6,10);
                tmpNumber = area + "-" + state + "-" + serial;
            }else{
                area = tmpNumber.substring(0,3);
                state = tmpNumber.substring(3,6);
                serial = tmpNumber.substring(6,10);
                tmpNumber = area + "-" + state + "-" + serial;
            }
        } else if (tmpNumber.length() == 11) {
            area = tmpNumber.substring(0,3);
            state = tmpNumber.substring(3,7);
            serial = tmpNumber.substring(7,11);
            tmpNumber = area + "-" + state + "-" + serial;
        }
        mTextNumber.setText(tmpNumber);
        //mTextNumber.setText(number);
    }

    public void setPhoto(String photoUri) {
        if (!TextUtils.isEmpty(photoUri)) {
            Glide.with(mImgPhoto.getContext())
                    .load(Uri.parse(photoUri))
                    .apply(RequestOptions.centerCropTransform())
                    .into(mImgPhoto);
        } else {
            mImgPhoto.setImageResource(R.drawable.ic_account_circle_48dp);
        }
    }

    public void setOnContactClickListener(OnContactClickListener listener) {
        mListener = listener;
    }

    public String getPhoneNumber() {
        return mTextNumber.getText().toString();
    }

    public interface OnContactClickListener {
        void onTouched(String number);

        void onMessageClicked(String number);

        void onCallClicked(String number);
    }
}
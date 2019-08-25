package com.todosdialer.todosdialer.adapter;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.model.CallLog;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.ViewHolder> {
    private static final String FORMAT_TIME = "yyyy/MM/dd a hh:mm";
    private List<CallLog> mCallLogList = new ArrayList<>();
    private OnItemClickListener mListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public void setCallLogList(List<CallLog> callLogs) {
        mCallLogList.clear();
        notifyDataSetChanged();

        if (callLogs != null) {
            for (int i = 0; i < callLogs.size(); i++) {
                mCallLogList.add(callLogs.get(i));
                notifyItemChanged(mCallLogList.size());
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_call_log, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setCallLog(mCallLogList.get(position));
    }

    @Override
    public int getItemCount() {
        return mCallLogList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView mImgStateIn;
        private ImageView mImgStateMissed;
        private ImageView mImgStateOut;

        private ImageView mImgPhoto;
        private TextView mTextName;
        private TextView mTextNumber;

        private TextView mTextTime;
        private TextView mTextDuration;

        private ImageView mImgMessage;
        private ImageView mImgCall;
        private ImageView mbtnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            mImgStateIn = itemView.findViewById(R.id.img_state_in);
            mImgStateMissed = itemView.findViewById(R.id.img_state_missed);
            mImgStateOut = itemView.findViewById(R.id.img_state_out);

            mTextName = itemView.findViewById(R.id.text_person_name);
            mTextNumber = itemView.findViewById(R.id.text_person_number);
            mImgPhoto = itemView.findViewById(R.id.img_person_photo);

            mTextTime = itemView.findViewById(R.id.text_time);
            mTextDuration = itemView.findViewById(R.id.text_duration);

            mImgMessage = itemView.findViewById(R.id.btn_message);
            mImgCall = itemView.findViewById(R.id.btn_call);
            mbtnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void setCallLog(final CallLog callLog) {
            String tmpNumber = "";
            String area = "";
            String state = "";
            String serial = "";

            mImgStateIn.setVisibility(View.GONE);
            mImgStateMissed.setVisibility(View.GONE);
            mImgStateOut.setVisibility(View.GONE);

            switch (callLog.getState()) {
                case CallLog.STATE_INCOMING:
                    mImgStateIn.setVisibility(View.VISIBLE);
                    break;
                case CallLog.STATE_MISS:
                    mImgStateMissed.setVisibility(View.VISIBLE);
                    break;
                case CallLog.STATE_OUTGOING:
                    mImgStateOut.setVisibility(View.VISIBLE);
                    break;
                default:
                    mImgStateMissed.setVisibility(View.VISIBLE);
                    break;
            }
            mTextName.setText(callLog.getName());
            tmpNumber = callLog.getNumber();
            if (tmpNumber.length() == 8) {
                if(tmpNumber.startsWith("1")){
                    state = tmpNumber.substring(0, 4);
                    serial = tmpNumber.substring(4, 8);
                    tmpNumber = state + "-" + serial;
                }
            }else if(tmpNumber.length() == 9){
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
            //                                                                                                                                                                                                                                                                                                                                                          mTextNumber.setText(callLog.getNumber());

            if (!TextUtils.isEmpty(callLog.getUriPhoto())) {
                Glide.with(mImgPhoto.getContext())
                        .load(Uri.parse(callLog.getUriPhoto()))
                        .apply(RequestOptions.centerCropTransform())
                        .into(mImgPhoto);
            } else {
                mImgPhoto.setImageResource(R.drawable.ic_account_circle_white_24dp);
            }

            mTextTime.setText(Utils.convertToLocal(callLog.getCreatedAt(), FORMAT_TIME));
            setDuration(callLog.getDuration());

            mImgMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onMessageClicked(callLog.getNumber());
                    }
                }
            });

            mImgCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onCallClicked(callLog.getNumber());
                    }
                }
            });

            mbtnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mListener != null){
                        mListener.onDeleteClicked(callLog.getId());
                    }
                }
            });
        }

        private void setDuration(long time) {
            time = time / 1000;
            long min = time / 60;
            long sec = time % 60;
            long hour = min / 60;

            String strTime = String.format(Locale.getDefault(), "%02d : %02d : %02d", hour, min, sec);
            mTextDuration.setText(strTime);
        }
    }

    public interface OnItemClickListener {
        void onMessageClicked(String number);

        void onCallClicked(String number);

        void onDeleteClicked(long id);
    }
}

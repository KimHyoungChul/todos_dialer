package com.todosdialer.todosdialer.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.model.CallLog;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.ItemViewHolder> {

    //    private static final String FORMAT_TIME = "yyyy/MM/dd a hh:mm";
    private static final String FORMAT_TIME = "yyyy.MM.dd E요일";
    private static final String FORMAT_CALL_DATE = "yyyy.MM.dd";
    private static final String FORMAT_CALL_TIME = "a hh:mm";
    private List<CallLog> mCallLogList = new ArrayList<>();
    private CallLogAdapter.OnItemClickListener mListener;

//    private String curDay;

    Context mContext;


    // adapter에 들어갈 list 입니다.
    private Context context;
    // Item의 클릭 상태를 저장할 array 객체
    private SparseBooleanArray selectedItems = new SparseBooleanArray();
    // 직전에 클릭됐던 Item의 position
    private int prePosition = -1;

    public CallLogAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void setOnItemClickListener(CallLogAdapter.OnItemClickListener listener) {
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
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // LayoutInflater를 이용하여 전 단계에서 만들었던 item.xml을 inflate 시킵니다.
        // return 인자는 ViewHolder 입니다.
        context = parent.getContext();
        try {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_call_log, parent, false);
            return new ItemViewHolder(view);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        // Item을 하나, 하나 보여주는(bind 되는) 함수입니다.
        holder.setCallLog(mCallLogList.get(position));
        holder.onBind(mCallLogList.get(position), position);
    }

    @Override
    public int getItemCount() {
        // RecyclerView의 총 개수 입니다.
        return mCallLogList.size();
    }

    // RecyclerView의 핵심인 ViewHolder 입니다.
    // 여기서 subView를 setting 해줍니다.
    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

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

        private LinearLayout layout_day;
        private TextView callLogDays;

        private LinearLayout layoutUtility;
        private LinearLayout linearItem;

        private int position;

        ItemViewHolder(View itemView) {
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

            layout_day = itemView.findViewById(R.id.layout_day);
            callLogDays = itemView.findViewById(R.id.call_log_days);
        }

        void onBind(CallLog mCallLogList, int position) {
            this.position = position;

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

            layout_day = itemView.findViewById(R.id.layout_day);
            callLogDays = itemView.findViewById(R.id.call_log_days);

            layoutUtility = itemView.findViewById(R.id.layout_utility);
            linearItem = itemView.findViewById(R.id.linearItem);

            changeVisibility(selectedItems.get(position));

            layoutUtility.setOnClickListener(this);
            linearItem.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.linearItem:
                case R.id.layout_utility:
                    if (selectedItems.get(position)) {
                        // 펼쳐진 Item을 클릭 시
                        selectedItems.delete(position);
                    } else {
                        // 직전의 클릭됐던 Item의 클릭상태를 지움
                        selectedItems.delete(prePosition);
                        // 클릭한 Item의 position을 저장
                        selectedItems.put(position, true);
                    }
                    // 해당 포지션의 변화를 알림
                    if (prePosition != -1) notifyItemChanged(prePosition);
                    notifyItemChanged(position);
                    // 클릭된 position 저장
                    prePosition = position;
                    break;


//                case R.id.imageView:
//                    Toast.makeText(context, data.getTitle() + " 이미지 입니다.", Toast.LENGTH_SHORT).show();
//                    break;
            }
        }

        /**
         * 클릭된 Item의 상태 변경
         *
         * @param isExpanded Item을 펼칠 것인지 여부
         */
        private void changeVisibility(final boolean isExpanded) {
            // height 값을 dp로 지정해서 넣고싶으면 아래 소스를 이용
            int dpValue = 60;
            float d = context.getResources().getDisplayMetrics().density;
            int height = (int) (dpValue * d);

            // ValueAnimator.ofInt(int... values)는 View가 변할 값을 지정, 인자는 int 배열
            ValueAnimator va = isExpanded ? ValueAnimator.ofInt(0, height) : ValueAnimator.ofInt(height, 0);
            // Animation이 실행되는 시간, n/1000초
            va.setDuration(400);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    // value는 height 값
                    int value = (int) animation.getAnimatedValue();
                    // imageView의 높이 변경
                    layoutUtility.getLayoutParams().height = value;
                    layoutUtility.requestLayout();
                    // imageView가 실제로 사라지게하는 부분
                    layoutUtility.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                }
            });
            // Animation start
            va.start();
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
                if (tmpNumber.startsWith("1")) {
                    state = tmpNumber.substring(0, 4);
                    serial = tmpNumber.substring(4, 8);
                    tmpNumber = state + "-" + serial;
                }
            } else if (tmpNumber.length() == 9) {
                if (tmpNumber.startsWith("02")) {
                    area = tmpNumber.substring(0, 2);
                    state = tmpNumber.substring(2, 5);
                    serial = tmpNumber.substring(5, 9);
                    tmpNumber = area + "-" + state + "-" + serial;
                }
            } else if (tmpNumber.length() == 10) {
                if (tmpNumber.startsWith("02")) {
                    area = tmpNumber.substring(0, 2);
                    state = tmpNumber.substring(2, 6);
                    serial = tmpNumber.substring(6, 10);
                    tmpNumber = area + "-" + state + "-" + serial;
                } else {
                    area = tmpNumber.substring(0, 3);
                    state = tmpNumber.substring(3, 6);
                    serial = tmpNumber.substring(6, 10);
                    tmpNumber = area + "-" + state + "-" + serial;
                }
            } else if (tmpNumber.length() == 11) {
                area = tmpNumber.substring(0, 3);
                state = tmpNumber.substring(3, 7);
                serial = tmpNumber.substring(7, 11);
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

//            curDay = Utils.convertToLocal(callLog.getCreatedAt(), FORMAT_TIME);

//            SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences(mContext);

//            String prevDay = mPref.getString("prevDay", null);

//                if ((prevDay == null) || !(prevDay.equals(curDay))) {
//                    layout_day.setVisibility(View.VISIBLE);
//                    callLogDays.setText(curDay);
//
//                    SharedPreferences.Editor editor = mPref.edit();
//                    editor.putString("prevDay", curDay);
//                    editor.commit();
//                } else {
//                    layout_day.setVisibility(View.GONE);
//                }
            try {
                String date = Utils.convertToLocal(callLog.getCreatedAt(), FORMAT_CALL_DATE);
                String time = Utils.convertToLocal(callLog.getCreatedAt(),FORMAT_CALL_TIME);
                mTextTime.setText((Html.fromHtml(date + "<br />" + time)));
            } catch (Exception e) {
                e.printStackTrace();
            }
//                setDuration(callLog.getDuration());

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

                        selectedItems.delete(position);
                        notifyDataSetChanged();
                    }
                }
            });

            mbtnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onDeleteClicked(callLog.getId());

                        selectedItems.clear();

                        mCallLogList.remove(getAdapterPosition());

                        notifyItemRemoved(getAdapterPosition());
                        notifyItemRangeChanged(getAdapterPosition(), mCallLogList.size());

                    }
                }
            });
        }

        private void setDuration(long time) {
            time = time / 1000;
            long min = time / 60;
            long sec = time % 60;
            long hour = min / 60;

            String strTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", hour, min, sec);
            mTextDuration.setText(strTime);
        }
    }

    public interface OnItemClickListener {
        void onMessageClicked(String number);

        void onCallClicked(String number);

        void onDeleteClicked(long id);
    }

}

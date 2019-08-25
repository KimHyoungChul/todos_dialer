package com.todosdialer.todosdialer.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.model.OrderInfo;

import java.text.NumberFormat;
import java.util.ArrayList;

public class OrderInfoAdapter extends RecyclerView.Adapter<OrderInfoAdapter.ViewHolder> {
    private ArrayList<OrderInfo> mOrderInfoList = new ArrayList<>();
    private OnOrderPaymentClickListener mOnPaymentClickListener;

    public void setOrderInfoList(ArrayList<OrderInfo> list) {
        mOrderInfoList.clear();
        notifyDataSetChanged();

        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                mOrderInfoList.add(list.get(i));
                notifyItemInserted(mOrderInfoList.size());
            }
        }
    }

    public void setOnOrderInfoClickListener(OnOrderPaymentClickListener listener) {
        this.mOnPaymentClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_order_info, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setOrderInfo(mOrderInfoList.get(position));
    }

    @Override
    public int getItemCount() {
        return mOrderInfoList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextOrderNumber; //토도스 번호(핸드폰 번호)
        private TextView mTextBasicNationTelecom; // Usim 개통사
        private TextView mTextOutNation; // 해외 이용국가

        private TextView mTextInDate; // 시작 일자
        private TextView mTextOutDate; // 종료 일자

        private TextView mTextUsimPhone; // Usim 번호 1
        private TextView mTextUsimPhone2; // Usim 번호 2
        private TextView mTextUsimMethod;

        private TextView mTextDayCount; // 사용 기간중 남의 기간의 일자
        private TextView mTextPrice; // 가격
        private TextView mBtnOrderStateName; //예약자 이름

        ViewHolder(View itemView) {
            super(itemView);
            mTextOrderNumber = itemView.findViewById(R.id.text_order_number);
            mTextBasicNationTelecom = itemView.findViewById(R.id.text_basic_nation_telecom);
            mTextOutNation = itemView.findViewById(R.id.text_out_nation);

            mTextInDate = itemView.findViewById(R.id.text_in_date);
            mTextOutDate = itemView.findViewById(R.id.text_out_date);

            mTextUsimPhone = itemView.findViewById(R.id.text_usim_phone);
            mTextUsimPhone2 = itemView.findViewById(R.id.text_usim_phone_2);
            mTextUsimMethod = itemView.findViewById(R.id.text_usim_method);

            mTextDayCount = itemView.findViewById(R.id.text_day_count);
            mTextPrice = itemView.findViewById(R.id.text_price);
            mBtnOrderStateName = itemView.findViewById(R.id.btn_order_state_name);
        }

        private void setOrderInfo(final OrderInfo orderInfo) {
            mTextOrderNumber.setText(orderInfo.orderNo);
            mTextBasicNationTelecom.setText(orderInfo.basicTelecomCodeName);
            mTextOutNation.setText(orderInfo.outNationCodeName);

            mTextInDate.setText(orderInfo.inDate);
            mTextOutDate.setText(orderInfo.outDate);

            mTextUsimPhone.setText(orderInfo.usimPhone);
            mTextUsimPhone2.setText(orderInfo.usimPhone2);
            mTextUsimMethod.setText(orderInfo.usimMethodName);

            NumberFormat format = NumberFormat.getInstance();
            String dayCountText = format.format(orderInfo.orderDayCount) + mTextDayCount.getContext().getString(R.string.term_day_unit);
            mTextDayCount.setText(dayCountText);

            String priceText = format.format(orderInfo.orderTotalPrice) + mTextPrice.getContext().getString(R.string.term_currency_unit);
            mTextPrice.setText(priceText);
            mBtnOrderStateName.setText(orderInfo.orderStateName);
            mBtnOrderStateName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnPaymentClickListener != null) {
                        mOnPaymentClickListener.onClicked(orderInfo);
                    }
                }
            });
        }
    }

    public interface OnOrderPaymentClickListener {
        void onClicked(OrderInfo orderInfo);
    }
}

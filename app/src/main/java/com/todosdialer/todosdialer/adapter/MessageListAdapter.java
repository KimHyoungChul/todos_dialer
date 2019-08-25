package com.todosdialer.todosdialer.adapter;


import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.model.Message;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;

public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.ViewHolder> {
    private static final String FORMAT_HEADER_DATE = "yyyy. MM. dd.";
    private static final String FORMAT_DATE = "aa hh:mm";

    private ArrayList<Message> mMessageList = new ArrayList<>();
    private OnInItemClickListener mOnInItemClickListener;

    public void setOnInItemClickListener(OnInItemClickListener listener) {
        mOnInItemClickListener = listener;
    }

    public void setMessageList(ArrayList<Message> messageList) {
        mMessageList.clear();
        notifyDataSetChanged();

        if (messageList != null) {
            for (int i = 0; i < messageList.size(); i++) {
                mMessageList.add(messageList.get(i));
                notifyItemInserted(mMessageList.size());
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_chat_message, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setMessage(mMessageList.get(position));
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout header;
        TextView headerText;
        TextView time;
        TextView body;

        TextView rec_time;
        TextView rec_body;

        LinearLayout send = null;
        LinearLayout receive = null;

        ViewHolder(View v) {
            super(v);

            header = v.findViewById(R.id.header);
            headerText = v.findViewById(R.id.sms_header_tv);

            time = v.findViewById(R.id.send_check_tv);
            body = v.findViewById(R.id.send_body_tv);
            rec_body = v.findViewById(R.id.rec_body_tv);
            rec_time = v.findViewById(R.id.rec_check_time);

            send = v.findViewById(R.id.right);
            receive = v.findViewById(R.id.left);
        }

        public void setMessage(final Message sms) {
            if (getLayoutPosition() > 0 && sms.dateCompareTo(mMessageList.get(getLayoutPosition() - 1), FORMAT_HEADER_DATE)) {
                header.setVisibility(View.GONE);
            } else {
                header.setVisibility(View.VISIBLE);
                headerText.setText(Utils.convertToLocal(sms.getCreatedAt(), FORMAT_HEADER_DATE));
            }

            if (sms.getInputState() == Message.INPUT_STATE_IN) {
                send.setVisibility(View.VISIBLE);
                receive.setVisibility(View.GONE);
            } else {
                send.setVisibility(View.GONE);
                receive.setVisibility(View.VISIBLE);
            }

            if (sms.getInputState() == Message.INPUT_STATE_IN) {
                body.setTag(sms);
                body.setText(sms.getBody());
                body.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnInItemClickListener != null) {
                            mOnInItemClickListener.onMessageClicked(sms);
                        }
                    }
                });

                time.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnInItemClickListener != null) {
                            mOnInItemClickListener.onMessageClicked(sms);
                        }
                    }
                });

                setVisibilityByState(sms);
            } else {
                rec_time.setText(Utils.convertToLocal(sms.getCreatedAt(), FORMAT_DATE));
                rec_body.setText(sms.getBody());
                rec_body.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnInItemClickListener != null) {
                            mOnInItemClickListener.onMessageClicked(sms);
                        }
                    }
                });
            }
        }

        private void setVisibilityByState(final Message sms) {
            switch (sms.getSendState()) {
                case Message.SEND_STATE_NOT_SHOW:
                    time.setVisibility(View.GONE);
                    break;

                case Message.SEND_STATE_UNKNOWN:
                    time.setVisibility(View.GONE);
                    break;

                case Message.SEND_STATE_SUCCESS:
                    time.setVisibility(View.VISIBLE);
                    time.setText(Utils.convertToLocal(sms.getCreatedAt(), FORMAT_DATE));
                    break;

                case Message.SEND_STATE_FAIL:
                    time.setVisibility(View.VISIBLE);
                    time.setText(time.getContext().getString(R.string.term_fail));
                    break;

                default:
                    time.setVisibility(View.GONE);
                    break;
            }
        }
    }

    public interface OnInItemClickListener {
        void onMessageClicked(Message message);
    }
}

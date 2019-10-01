package com.todosdialer.todosdialer.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.model.ChatRoom;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatRoomListAdapter extends RecyclerView.Adapter<ChatRoomListAdapter.ViewHolder> {
    private static final String FORMAT_DATE = "yyyy. MM. dd.";
    private List<ChatRoom> mChatRoomList = new ArrayList<>();
    private OnItemClickListener mListener;

    Context mContext;

    public ChatRoomListAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public void setChatRoomList(List<ChatRoom> chatRooms) {
        mChatRoomList.clear();
        notifyDataSetChanged();

        if (chatRooms != null) {
            for (int i = 0; i < chatRooms.size(); i++) {
                mChatRoomList.add(chatRooms.get(i));
                notifyItemChanged(mChatRoomList.size());
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_chat_room, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setChatRoom(mChatRoomList.get(position));
    }

    @Override
    public int getItemCount() {
        return mChatRoomList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View container;
        CircleImageView imgPhoto;
        TextView textPhoneNumber;
        TextView textRecentMessage;
        TextView textRecentMsgTime;
        TextView textUnreadCount;

        ViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container_chat_room);
            imgPhoto = itemView.findViewById(R.id.img_photo);
            textPhoneNumber = itemView.findViewById(R.id.text_phone_number);
            textRecentMessage = itemView.findViewById(R.id.text_recent_message);
            textRecentMsgTime = itemView.findViewById(R.id.text_recent_msg_time);
            textUnreadCount = itemView.findViewById(R.id.text_unread_count);
        }

        void setChatRoom(final ChatRoom chatRoom) {
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onRoomClicked(chatRoom);
                    }
                }
            });

            if (!TextUtils.isEmpty(chatRoom.getUriPhoto())) {
                Glide.with(imgPhoto.getContext())
                        .load(Uri.parse(chatRoom.getUriPhoto()))
                        .apply(RequestOptions.centerCropTransform())
                        .into(imgPhoto);
            } else {
                imgPhoto.setImageResource(R.drawable.ic_account_circle_white_24dp);
            }

            String tmpNumber = chatRoom.getPhoneNumber();

            String name = TextUtils.isEmpty(chatRoom.getName()) ? chatRoom.getPhoneNumber() : chatRoom.getName();
            if(Pattern.matches("^[0-9]+$", name)){
                String tmpName = name;
                String area = "";
                String state = "";
                String serial = "";

                if(tmpName.length() == 9){
                    area = tmpName.substring(0,2);
                    state = tmpName.substring(2,5);
                    serial = tmpName.substring(5,9);
                    tmpName = area + "-" + state + "-" + serial;
                }else if(tmpName.length() == 10){
                    if(tmpName.startsWith("02")){
                        area = tmpName.substring(0,2);
                        state = tmpName.substring(2,6);
                        serial = tmpName.substring(6,10);
                        tmpName = area + "-" + state + "-" + serial;
                    }else{
                        area = tmpName.substring(0,3);
                        state = tmpName.substring(3,6);
                        serial = tmpName.substring(6,10);
                        tmpName = area + "-" + state + "-" + serial;
                    }
                } else if (tmpName.length() == 11) {
                    area = tmpName.substring(0,3);
                    state = tmpName.substring(3,7);
                    serial = tmpName.substring(7,11);
                    tmpName = area + "-" + state + "-" + serial;
                }
                textPhoneNumber.setText(tmpName);

            }else {
                textPhoneNumber.setText(name);
            }


            textRecentMessage.setText(chatRoom.getBody());
            if (chatRoom.getUpdatedAt() != 0) {
                textRecentMsgTime.setText(Utils.convertToLocal(chatRoom.getUpdatedAt(), FORMAT_DATE));
                textRecentMsgTime.setVisibility(View.VISIBLE);
            } else {
                textRecentMsgTime.setVisibility(View.GONE);
            }

            setUnreadCountText(chatRoom.getUnreadCount());
        }

        private void setUnreadCountText(int uncheckedSize) {
            if (textUnreadCount != null) {
                if (uncheckedSize == 0) {
                    textUnreadCount.setVisibility(View.GONE);
                } else if (uncheckedSize > 99) {
                    textUnreadCount.setVisibility(View.VISIBLE);
                    String sizeText = "99+";
                    textUnreadCount.setText(sizeText);
                } else {
                    textUnreadCount.setVisibility(View.VISIBLE);
                    textUnreadCount.setText(String.valueOf(uncheckedSize));
                }
            }
        }
    }


    public interface OnItemClickListener {
        void onRoomClicked(ChatRoom chatRoom);
    }
}

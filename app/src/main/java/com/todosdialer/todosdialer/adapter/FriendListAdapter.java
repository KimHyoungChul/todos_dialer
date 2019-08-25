package com.todosdialer.todosdialer.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.model.Friend;
import com.todosdialer.todosdialer.view.ContactView;

import java.util.ArrayList;
import java.util.List;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {
    private List<Friend> mFriendList = new ArrayList<>();
    private OnFriendClickListener mListener;

    private boolean mIsButtonVisible = true;

    public FriendListAdapter(boolean isBtnVisible) {
        mIsButtonVisible = isBtnVisible;
    }

    public void setOnFriendClickListener(OnFriendClickListener listener) {
        mListener = listener;
    }

    public void setFriendList(List<Friend> friends) {
        mFriendList.clear();
        notifyDataSetChanged();

        if (friends != null) {
            for (int i = 0; i < friends.size(); i++) {
                mFriendList.add(friends.get(i));
                notifyItemChanged(mFriendList.size());
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_friend, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setFriend(mFriendList.get(position));
    }

    @Override
    public int getItemCount() {
        return mFriendList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ContactView contactView;

        ViewHolder(View itemView) {
            super(itemView);
            contactView = itemView.findViewById(R.id.friend_view);
        }

        void setFriend(final Friend friend) {
            contactView.setButtonVisible(mIsButtonVisible);
            contactView.setFriend(friend);
            contactView.setOnContactClickListener(new ContactView.OnContactClickListener() {
                @Override
                public void onTouched(String number) {
                    if (mListener != null) {
                        mListener.onCellTouched(number);
                    }
                }

                @Override
                public void onMessageClicked(String number) {
                    if (mListener != null) {
                        mListener.onMessageClicked(number);
                    }
                }

                @Override
                public void onCallClicked(String number) {
                    if (mListener != null) {
                        mListener.onCallClicked(number);
                    }
                }
            });
            contactView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onCellClicked(friend.getNumber());
                    }
                }
            });
        }
    }

    public interface OnFriendClickListener {
        void onCellTouched(String number);

        void onCellClicked(String number);

        void onMessageClicked(String number);

        void onCallClicked(String number);
    }
}

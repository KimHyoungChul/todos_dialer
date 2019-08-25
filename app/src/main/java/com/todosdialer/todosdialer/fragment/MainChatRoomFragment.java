package com.todosdialer.todosdialer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.todosdialer.todosdialer.ChatActivity;
import com.todosdialer.todosdialer.NewChatActivity;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.adapter.ChatRoomListAdapter;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.model.ChatRoom;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class MainChatRoomFragment extends Fragment {

    private Realm mRealm;
    private RealmResults<ChatRoom> mChatRoomResults;

    private RealmChangeListener<Realm> mRealmListener = new RealmChangeListener<Realm>() {
        @Override
        public void onChange(@NonNull Realm realm) {
            refresh();
        }
    };

    private ChatRoomListAdapter mAdapter;

    public static MainChatRoomFragment newInstance() {
        Bundle args = new Bundle();
        MainChatRoomFragment fragment = new MainChatRoomFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_chat_room, container, false);

        RecyclerView recyclerView = rootView.findViewById(R.id.chat_room_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAdapter = new ChatRoomListAdapter();
        mAdapter.setOnItemClickListener(new ChatRoomListAdapter.OnItemClickListener() {
            @Override
            public void onRoomClicked(ChatRoom chatRoom) {
                showChatActivity(chatRoom);
            }
        });
        recyclerView.setAdapter(mAdapter);

        rootView.findViewById(R.id.btn_new_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewChatActivity();
            }
        });


        mRealm = Realm.getDefaultInstance();
        mChatRoomResults = RealmManager.newInstance().loadChatRoomList(mRealm);

        refresh();

        mRealm.addChangeListener(mRealmListener);

        return rootView;
    }

    private void refresh() {
        ArrayList<ChatRoom> chatRooms = new ArrayList<>();
        for (ChatRoom room : mChatRoomResults) {
            chatRooms.add(mRealm.copyFromRealm(room));
        }

        if (mAdapter != null) {
            mAdapter.setChatRoomList(chatRooms);
        }
    }

    @Override
    public void onDestroyView() {
        mRealm.removeChangeListener(mRealmListener);
        mRealm.close();
        super.onDestroyView();
    }

    private void showChatActivity(ChatRoom chatRoom) {
        if (chatRoom != null) {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_KEY_NUMBER, chatRoom.getPhoneNumber());
            startActivity(intent);
        }
    }

    private void showNewChatActivity() {
        startActivity(new Intent(getActivity(), NewChatActivity.class));
    }
}

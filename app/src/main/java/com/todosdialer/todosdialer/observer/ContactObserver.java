package com.todosdialer.todosdialer.observer;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.model.CallLog;
import com.todosdialer.todosdialer.model.ChatRoom;
import com.todosdialer.todosdialer.model.Friend;
import com.todosdialer.todosdialer.model.Message;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import me.everything.providers.android.contacts.Contact;
import me.everything.providers.android.contacts.ContactsProvider;

public class ContactObserver extends ContentObserver {
    private Context mContext;
    private boolean mIsRegistered = false;

    public ContactObserver(Context context, Handler handler) {
        super(handler);
        mContext = context;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return true;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);

        if (!mIsRegistered) {
            mIsRegistered = true;

            new SyncFriendsTask().execute();
        }
    }

    private class SyncFriendsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(getClass().getSimpleName(), "Running SyncFriendsTask..");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Realm realm = Realm.getDefaultInstance();
            RealmManager realmManager = RealmManager.newInstance();

            ContactsProvider contactsProvider = new ContactsProvider(mContext);
            List<Contact> contacts = contactsProvider.getContacts().getList();

            RealmResults<CallLog> callList = realmManager.loadCallLogs(realm);
            for (int i = 0; i < callList.size(); i++) {
                CallLog callLog = callList.get(i);
                if (callLog != null) {
                    Friend person = Utils.findFriend(contacts, callLog.getNumber());
                    if (person != null) {
                        realm.beginTransaction();
                        callLog.setName(person.getName());
                        callLog.setPid(person.getPid());
                        callLog.setNumber(person.getNumber());
                        realm.commitTransaction();
                    }
                }
            }

            RealmResults<Message> smsList = realmManager.loadAllMessageList(realm);
            for (int i = 0; i < smsList.size(); i++) {
                Message message = smsList.get(i);
                if (message != null) {
                    Friend person = Utils.findFriend(contacts, message.getPhoneNumber());
                    if (person != null) {
                        realm.beginTransaction();
                        message.setName(person.getName());
                        message.setFid(person.getPid());
                        message.setPhoneNumber(person.getNumber());
                        realm.commitTransaction();
                    }
                }
            }

            RealmResults<ChatRoom> chatRoomList = realmManager.loadChatRoomList(realm);
            for (int i = 0; i < chatRoomList.size(); i++) {
                ChatRoom chatRoom = chatRoomList.get(i);
                if (chatRoom != null) {
                    Friend person = Utils.findFriend(contacts, chatRoom.getPhoneNumber());
                    if (person != null) {
                        realm.beginTransaction();
                        chatRoom.setName(person.getName());
                        chatRoom.setFid(person.getPid());
                        realm.commitTransaction();
                    }
                }
            }

            ArrayList<Friend> newFriends = new ArrayList<>();
            for (int i = 0; i < contacts.size(); i++) {
                if (realmManager.findFriend(realm, contacts.get(i).phone) == null) {
                    newFriends.add(new Friend(contacts.get(i).id,
                            contacts.get(i).displayName,
                            contacts.get(i).phone,
                            contacts.get(i).normilizedPhone,
                            contacts.get(i).uriPhoto));
                }
            }

            realmManager.insertFriends(realm, newFriends);
            realm.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.i(getClass().getSimpleName(), "Done SyncFriendsTask..");
            mIsRegistered = false;
        }
    }
}
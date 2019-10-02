package com.todosdialer.todosdialer.manager;

import android.support.annotation.NonNull;
import android.util.Log;

import com.todosdialer.todosdialer.model.CallLog;
import com.todosdialer.todosdialer.model.ChatRoom;
import com.todosdialer.todosdialer.model.Friend;
import com.todosdialer.todosdialer.model.LogData;
import com.todosdialer.todosdialer.model.Message;
import com.todosdialer.todosdialer.model.User;
import com.todosdialer.todosdialer.service.TodosService;
import com.todosdialer.todosdialer.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class RealmManager {

    public static RealmManager newInstance() {
        return new RealmManager();
    }

    public void writeLog(final String log) {
        if (TodosService.hasToShowLog) {
            try {
                Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(@NonNull Realm realm) {
                        LogData logData = realm.createObject(LogData.class);
                        logData.setLog(log);
                        logData.setTime(Calendar.getInstance().getTimeInMillis());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<String> loadLogs(Realm realm) {
        ArrayList<String> logs = new ArrayList<>();
        RealmResults<LogData> result = realm.where(LogData.class).findAll();
        for (int i = 0; i < result.size(); i++) {
            String log = result.get(i).getFormattedTime() + "    " + result.get(i).getLog() + "\n\n";
            logs.add(log);
        }
        return logs;
    }

    public void deleteLogs(Realm realm) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.delete(LogData.class);
            }
        });
    }

    public User loadUser(Realm realm) {
        User user = realm.where(User.class).findFirst();
        return user == null ? null : realm.copyFromRealm(user);
    }

    public User loadUserWithDefault(Realm realm) {
        User user = realm.where(User.class).findFirst();
        return user == null ? new User() : realm.copyFromRealm(user);
    }

    public void saveUser(Realm realm, final User user) {
        realm.beginTransaction();
        realm.copyToRealm(user);
        realm.commitTransaction();
    }

    public void deleteUser(Realm realm) {
        realm.beginTransaction();
        realm.delete(User.class);
        realm.commitTransaction();
    }

    public void insertFriends(Realm realm, ArrayList<Friend> friends) {
        if (friends != null && friends.size() > 0) {
            realm.beginTransaction();
            realm.insertOrUpdate(friends);
            realm.commitTransaction();
        }
    }

    public int loadFriendsSize(Realm realm) {
        return realm.where(Friend.class).findAll().size();
    }

    public RealmResults<Friend> loadFriends(Realm realm) {
        RealmResults<Friend> friends = realm.where(Friend.class).findAll();
        return friends.sort("name", Sort.ASCENDING);
    }

    public List<Friend> loadFriendsAsArrayList(Realm realm) {
        RealmResults<Friend> friends = realm.where(Friend.class).findAll();
        return realm.copyFromRealm(friends.sort("name", Sort.ASCENDING));
    }

    public RealmResults<CallLog> loadCallLogs(Realm realm) {
        RealmResults<CallLog> calls = realm.where(CallLog.class).findAll();
        return calls.sort("createdAt", Sort.DESCENDING);
    }

    public RealmResults<CallLog> loadMissedCallLogs(Realm realm) {
        RealmResults<CallLog> calls = realm.where(CallLog.class)
                .equalTo("state", CallLog.STATE_MISS)
                .findAll();
        return calls.sort("createdAt", Sort.DESCENDING);
    }

    public int countUncheckedCallLogs(RealmResults<CallLog> logs) {
        return logs.where()
                .equalTo("isChecked", false)
                .findAll().size();
    }

    public long countAllUnreadMessageCount(Realm realm) {
        Number count = realm.where(ChatRoom.class).sum("unreadCount");
        return count != null ? count.longValue() : 0;
    }

    public RealmResults<ChatRoom> loadChatRoomList(Realm realm) {
        RealmResults<ChatRoom> cha = realm.where(ChatRoom.class).findAll();
        return cha.sort("updatedAt", Sort.DESCENDING);
    }

    public boolean hasChatRoom(Realm realm, String phoneNumber) {
        return findChatRoom(realm, phoneNumber) != null;
    }

    public ChatRoom findChatRoom(Realm realm, String phoneNumber) {
        ChatRoom chatRoom = realm.where(ChatRoom.class).equalTo("phoneNumber", phoneNumber).findFirst();
        return chatRoom != null ? realm.copyFromRealm(chatRoom) : null;
    }

    public Friend findFriendWithDefault(Realm realm, String phoneNumber) {
        Friend friend = findFriend(realm, phoneNumber);
        if (friend == null) {
            friend = new Friend();
            friend.setName(phoneNumber);
            friend.setNumber(phoneNumber);
        }
        return friend;
    }

    public Friend findFriend(Realm realm, String phoneNumber) {
        String anotherPhoneNumber = phoneNumber;
        if (anotherPhoneNumber.contains("-")) {
            anotherPhoneNumber = anotherPhoneNumber.replace("-", "");
        } else {
            anotherPhoneNumber = Utils.formattedPhoneNumber(phoneNumber);
        }
        Log.i(getClass().getSimpleName(), "phoneNumber: " + phoneNumber);
        Log.i(getClass().getSimpleName(), "anotherPhoneNumber: " + anotherPhoneNumber);
        Friend friend = realm.where(Friend.class).equalTo("number", phoneNumber).findFirst();
        Friend another = realm.where(Friend.class).equalTo("number", anotherPhoneNumber).findFirst();

        if (friend != null) {
            return realm.copyFromRealm(friend);
        } else {
            return another != null ? realm.copyFromRealm(another) : null;
        }
    }


    public void insertCallLog(Realm realm, String userID, Friend friend, long callDuration, int state, long createdAt) {
        insertCallLog(realm, userID, friend, callDuration, state, createdAt, true);
    }

    public CallLog insertCallLog(Realm realm, String userID, Friend friend, long callDuration, int state, long createdAt, boolean isChecked) {
        long newId = makeNextId(realm, CallLog.class);
        CallLog callLog = new CallLog();
        callLog.setId(newId);
        callLog.setUserID(userID);
        callLog.setNumber(friend.getNumber());
        callLog.setState(state);
        callLog.setDuration(callDuration);
        callLog.setCreatedAt(createdAt);

        callLog.setPid(friend.getPid());
        callLog.setName(friend.getName());
        callLog.setUriPhoto(friend.getUriPhoto());
        callLog.setIsChecked(isChecked);

        realm.beginTransaction();
        realm.copyToRealm(callLog);
        realm.commitTransaction();
        return callLog;
    }


    public Message insertMessage(Realm realm, String userID, Friend friend, String body, int inputState, int readState, int sendState) {
        long newId = makeNextId(realm, Message.class);
        Message message = new Message();
        message.setUserID(userID);
        message.setId(newId);
        message.setBody(body);
        message.setPhoneNumber(friend.getNumber());
        message.setCreatedAt(Calendar.getInstance().getTimeInMillis());
        message.setInputState(inputState);
        message.setReadState(readState);
        message.setSendState(sendState);
        message.setFid(friend.getPid());
        message.setName(friend.getName());
        message.setUriPhoto(friend.getUriPhoto());

        realm.beginTransaction();
        realm.copyToRealm(message);
        realm.commitTransaction();

        return message;
    }

    private long makeNextId(Realm realm, Class clazz) {
        Number number = realm.where(clazz).max("id");
        return number == null ? 1 : number.longValue() + 1;
    }

    public void createOrUpdateChatRoom(Realm realm, Message message) {
        ChatRoom chatRoom = findChatRoom(realm, message.getPhoneNumber());
        if (chatRoom == null) { //create
            createOrUpdateChatRoom(realm, message,
                    message.getInputState() == Message.INPUT_STATE_IN ?
                            0 :
                            1);
        } else { //update
            createOrUpdateChatRoom(realm, message,
                    message.getInputState() == Message.INPUT_STATE_IN ?
                            chatRoom.getUnreadCount() :
                            chatRoom.getUnreadCount() + 1);
        }
    }

    public void clearChatRoomUnreadCount(Realm realm, String phoneNumber) {
        ChatRoom chatRoom = realm.where(ChatRoom.class).equalTo("phoneNumber", phoneNumber).findFirst();
        if (chatRoom != null) {
            realm.beginTransaction();
            chatRoom.setUnreadCount(0);
            realm.commitTransaction();
        }
    }

    public void createOrUpdateChatRoom(Realm realm, Message message, int unreadCount) {
        if (message == null) {
            return;
        }

        ChatRoom chatRoom = findChatRoom(realm, message.getPhoneNumber());
        if (chatRoom == null) { //create
            chatRoom = new ChatRoom();
            chatRoom.setUserID(message.getUserID());
            chatRoom.setPhoneNumber(message.getPhoneNumber());
            chatRoom.setName(message.getName());
            chatRoom.setFid(message.getFid());
            chatRoom.setUriPhoto(message.getUriPhoto());
            chatRoom.setBody(message.getBody());
            chatRoom.setUpdatedAt(Calendar.getInstance().getTimeInMillis());
            chatRoom.setInputState(message.getInputState());
            chatRoom.setReadState(message.getInputState() == Message.INPUT_STATE_IN ?
                    Message.READ_STATE_READ :
                    Message.READ_STATE_UNREAD);
            chatRoom.setSendState(message.getSendState());
            chatRoom.setUnreadCount(unreadCount);
        } else { //update
            chatRoom.setUserID(message.getUserID());
            chatRoom.setPhoneNumber(message.getPhoneNumber());
            chatRoom.setName(message.getName());
            chatRoom.setFid(message.getFid());
            chatRoom.setUriPhoto(message.getUriPhoto());
            chatRoom.setBody(message.getBody());
            chatRoom.setInputState(message.getInputState());
            chatRoom.setReadState(message.getInputState() == Message.INPUT_STATE_IN ?
                    Message.READ_STATE_READ :
                    Message.READ_STATE_UNREAD);
            chatRoom.setSendState(message.getSendState());
            chatRoom.setUnreadCount(unreadCount);

            chatRoom.setUpdatedAt(Calendar.getInstance().getTimeInMillis());
        }

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(chatRoom);
        realm.commitTransaction();
    }

    public RealmResults<Message> loadMessageList(Realm realm, String phoneNumber) {
        return realm.where(Message.class).equalTo("phoneNumber", phoneNumber).findAll();
    }

    public RealmResults<Message> loadAllMessageList(Realm realm) {
        return realm.where(Message.class).findAll();
    }

    public void resolveCallLogUncheckedState(Realm realm) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(@NonNull Realm realm) {
                RealmResults<CallLog> callLogs = realm.where(CallLog.class)
                        .equalTo("state", CallLog.STATE_MISS)
                        .findAll();
                for (CallLog callLog : callLogs) {
                    callLog.setIsChecked(true);
                }
            }
        });
    }

    public void deleteChatRoomWithMessage(Realm realm, String phoneNumber) {
        realm.beginTransaction();
        realm.where(ChatRoom.class).equalTo("phoneNumber", phoneNumber).findAll().deleteAllFromRealm();
        realm.where(Message.class).equalTo("phoneNumber", phoneNumber).findAll().deleteAllFromRealm();
        realm.commitTransaction();
    }

    public void deleteMessage(Realm realm, Message message) {
        realm.beginTransaction();
        realm.where(Message.class).equalTo("id", message.getId()).findAll().deleteAllFromRealm();
        realm.commitTransaction();
    }

    public static int makeNewId(Class clazz) {
        return Realm.getDefaultInstance().where(clazz).findAll().size() + 1;
    }

    public void deleteAllFriends(Realm realm) {
        realm.beginTransaction();
        realm.delete(Friend.class);
        realm.commitTransaction();
    }
}
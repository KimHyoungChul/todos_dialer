package com.todosdialer.todosdialer.model;

import com.todosdialer.todosdialer.util.Utils;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class Message extends RealmObject {
    public static final int SMS_NOTI_ID = 39;

    public static final int INPUT_STATE_IN = 1;
    public static final int INPUT_STATE_OUT = 0;

    public static final int READ_STATE_READ = 1;
    public static final int READ_STATE_UNREAD = 0;

    public static final int SEND_STATE_SUCCESS = 1;
    public static final int SEND_STATE_UNKNOWN = 0;
    public static final int SEND_STATE_FAIL = 2;
    public static final int SEND_STATE_NOT_SHOW = 3;

    @PrimaryKey
    private long id;
    @Index
    private String phoneNumber;
    private String body;
    private long createdAt;
    private int readState;
    private int sendState;
    private int inputState;

    private long fid;
    private String name;
    private String uriPhoto;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setReadState(int readState) {
        this.readState = readState;
    }

    public int getReadState() {
        return readState;
    }

    public void setSendState(int sendState) {
        this.sendState = sendState;
    }

    public int getSendState() {
        return sendState;
    }

    public void setInputState(int inputState) {
        this.inputState = inputState;
    }

    public int getInputState() {
        return inputState;
    }

    public void setFid(long fid) {
        this.fid = fid;
    }

    public long getFid() {
        return fid;
    }

    public void setUriPhoto(String uriPhoto) {
        this.uriPhoto = uriPhoto;
    }

    public String getUriPhoto() {
        return uriPhoto;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean dateCompareTo(Message other, String dateFormat) {
        if (other != null) {

            String before = Utils.convertToLocal(getCreatedAt(), dateFormat);
            String otherTime = Utils.convertToLocal(other.getCreatedAt(), dateFormat);

            if (before.equals(otherTime))
                return true;
        }

        return false;
    }
}

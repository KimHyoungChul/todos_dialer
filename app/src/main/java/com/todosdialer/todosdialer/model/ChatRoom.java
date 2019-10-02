package com.todosdialer.todosdialer.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ChatRoom extends RealmObject {
    @PrimaryKey
    private String phoneNumber;
    private String userID;
    private String name;
    private long fid = 0;
    private String uriPhoto;

    private String body;
    private long updatedAt;
    private int readState;
    private int sendState = 0;
    private int inputState = 0;

    private int unreadCount = 0;

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserID() {
        return userID;
    }

    public void setPhoneNumber(String phoneNumber) {
        phoneNumber.replace("-","");
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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

    public void setBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setReadState(int readState) {
        this.readState = readState;
    }

    public int getReadState() {
        return readState;
    }

    public void setInputState(int inputState) {
        this.inputState = inputState;
    }

    public int getInputState() {
        return inputState;
    }

    public void setSendState(int sendState) {
        this.sendState = sendState;
    }

    public int getSendState() {
        return sendState;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}

package com.todosdialer.todosdialer.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class CallLog extends RealmObject {
    public static final int STATE_OUTGOING = 1;
    public static final int STATE_INCOMING = 2;
    public static final int STATE_MISS = 3;


    @PrimaryKey
    private long id;
    private String number;
    private int state;
    private long duration;
    private long createdAt;

    private long pid;
    private String name;
    private String uriPhoto;
    private boolean isChecked;

    public CallLog() {
    }

    public CallLog(long id, String name, String number) {
        this.pid = id;
        this.name = name;
        this.number = number;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long getPid() {
        return pid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setUriPhoto(String uriPhoto) {
        this.uriPhoto = uriPhoto;
    }

    public String getUriPhoto() {
        return uriPhoto;
    }

    public void setIsChecked(boolean checked) {
        isChecked = checked;
    }

    public boolean getIsChecked() {
        return isChecked;
    }
}

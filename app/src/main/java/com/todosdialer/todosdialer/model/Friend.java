package com.todosdialer.todosdialer.model;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class Friend extends RealmObject {
    @PrimaryKey
    private long pid;
    private String name;

    @Index
    private String number;

    private String normilizedPhone;
    private String uriPhoto;

    public Friend() {
    }

    public Friend(long id, String name, String number, String normilizedPhone, String uriPhoto) {
        this.pid = id;

        this.name = name;
        number.replace("-","");
        this.number = number;
        this.normilizedPhone = normilizedPhone;
        this.uriPhoto = uriPhoto;
    }

    public void setNormilizedPhone(String normilizedPhone) {
        this.normilizedPhone = normilizedPhone;
    }

    public String getNormilizedPhone() {
        return normilizedPhone;
    }

    public void setNumber(String number) {
        number.replace("-","");
        this.number = number;
    }

    public String getNumber() {
        return number;
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
}

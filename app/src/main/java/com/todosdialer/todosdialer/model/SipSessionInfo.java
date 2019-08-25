package com.todosdialer.todosdialer.model;

import io.realm.RealmObject;

public class SipSessionInfo extends RealmObject {
    private String sipID;
    private String sipPW;
    private String sipSVRIP;
    private String sipSVRPort;
    private String endDate;
    private String dueDay;
    private String smsSVRURL;

    public void setSipID(String sipID) {
        this.sipID = sipID;
    }

    public String getSipID() {
        return sipID;
    }

    public void setSipPW(String sipPW) {
        this.sipPW = sipPW;
    }

    public String getSipPW() {
        return sipPW;
    }

    public void setSipSVRIP(String sipSVRIP) {
        this.sipSVRIP = sipSVRIP;
    }

    public String getSipSVRIP() {
        return sipSVRIP;
    }

    public void setSipSVRPort(String sipSVRPort) {
        this.sipSVRPort = sipSVRPort;
    }

    public String getSipSVRPort() {
        return sipSVRPort;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setDueDay(String dueDay) {
        this.dueDay = dueDay;
    }

    public String getDueDay() {
        return dueDay;
    }

    public String getSmsSVRURL() {
        return smsSVRURL;
    }
}

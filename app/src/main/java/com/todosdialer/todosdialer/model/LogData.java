package com.todosdialer.todosdialer.model;

import com.todosdialer.todosdialer.util.Utils;

import io.realm.RealmObject;

public class LogData extends RealmObject {
    private String log;
    private Long time;

    public void setLog(String log) {
        this.log = log;
    }

    public String getLog() {
        return log;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Long getTime() {
        return time;
    }

    public String getFormattedTime() {
        return Utils.convertToLocal(time, "yyyy/MM/dd a hh:mm:ss");
    }
}
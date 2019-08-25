package com.todosdialer.todosdialer.api.response;

public class BaseResponse {
    private static final int SUCCESS_CODE = 0;

    public int code;
    public String message;

    public boolean isSuccess() {
        return code == SUCCESS_CODE;
    }
}

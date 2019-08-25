package com.todosdialer.todosdialer.api.response;

import com.todosdialer.todosdialer.model.MemberInfo;

public class AppMemberInfoResponse extends BaseResponse {
    public MemberInfo result;

    public MemberInfo result() {
        return result == null ? new MemberInfo() : result;
    }
}

package com.todosdialer.todosdialer.api.body;

import java.util.HashMap;

public class SendingMessageBody extends HashMap<String, String> {
    public SendingMessageBody(String to, String message) {
        put("to", to.replace(" ", "").replace("-", ""));
        put("message", message);
    }
}

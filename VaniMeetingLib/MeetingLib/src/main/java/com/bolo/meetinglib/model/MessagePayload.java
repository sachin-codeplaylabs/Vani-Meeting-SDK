package com.bolo.meetinglib.model;

public class MessagePayload {


    public String message;
    public String to;
    public String sender;

    public MessagePayload(String message, String to) {
        this.message = message;
        this.to = to;
    }


}

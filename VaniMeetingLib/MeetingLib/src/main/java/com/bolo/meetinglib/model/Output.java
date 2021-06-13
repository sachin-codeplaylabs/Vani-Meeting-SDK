package com.bolo.meetinglib.model;

import org.json.JSONObject;

import java.util.Map;

public class Output {
    public  String  message;
    public  boolean  isSuccess;
    public  String  status;
    public Map<String , String> data;

    public Output(String message, boolean isSuccess, String status,  Map<String , String> data) {
        this.message = message;
        this.isSuccess = isSuccess;
        this.status = status;
        this.data = data;
    }
}

package com.bolo.meetinglib.model;

import android.view.SurfaceView;


import org.json.JSONObject;
import org.webrtc.SurfaceViewRenderer;

import java.util.HashMap;
import java.util.Map;

public class MeetingStartRequest {



    public  enum  CameraDirection{
        CAMERA_DIRECTION_FRONT,
        CAMERA_DIRECTION_BACK
    }//Sachin

    public int videoCaptureWidth = 300;
    public int videoCaptureHeight = 300;
    public String userId;
    public String roomId;
    public boolean defaultWhiteboardEditEnable = true;
    public String cameraDevice; //Sachin
    public String audioInDevice; //Sachin
    public int numberOfUsers = 2;
    public boolean isRecordingRequired = false;
    public boolean isAdmin = false;
    public String appId = "";
    public Map<String,String> userData = new HashMap<String,String>();
    public JSONObject apiData = new JSONObject();
    public boolean isMobileApp = true;//Sachin
    public int screenSharingHeight = 640;
    public int screenSharingWidth = 320;
    public CameraDirection defaultCameraDirection = CameraDirection.CAMERA_DIRECTION_FRONT;

    public MeetingStartRequest(String userId, String roomId, String appId) {
        this.userId = userId;
        this.roomId = roomId;
        this.appId = appId;
    }

    public boolean shouldUseSFU(){
        return  (this.numberOfUsers > 3 );

    }
}

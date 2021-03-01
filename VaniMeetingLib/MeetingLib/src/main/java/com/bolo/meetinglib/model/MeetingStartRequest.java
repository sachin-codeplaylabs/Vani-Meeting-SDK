package com.bolo.meetinglib.model;

import android.view.SurfaceView;


import org.webrtc.SurfaceViewRenderer;

public class MeetingStartRequest {



    public  enum  CameraDirection{
        CAMERA_DIRECTION_FRONT,
        CAMERA_DIRECTION_BACK
    }
    private String userId;
    private String roomId;
    private boolean canSendVideo;
    private boolean canSendAudio;
    private boolean defaultVideoEnable = true;
    private boolean defaultMicEnable = true;
    private CameraDirection deafultCameraDirection = CameraDirection.CAMERA_DIRECTION_FRONT;
    private int videoCaptureWidth = 480;
    private int videoCaptureHeight = 640;
    private boolean localRenderingRequired = true;

    public MeetingStartRequest(String userId, String roomId, boolean canSendVideo, boolean canSendAudio) {
        this.userId = userId;
        this.roomId = roomId;
        this.canSendVideo = canSendVideo;
        this.canSendAudio = canSendAudio;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public boolean isCanSendVideo() {
        return canSendVideo;
    }

    public void setCanSendVideo(boolean canSendVideo) {
        this.canSendVideo = canSendVideo;
    }

    public boolean isCanSendAudio() {
        return canSendAudio;
    }

    public void setCanSendAudio(boolean canSendAudio) {
        this.canSendAudio = canSendAudio;
    }

    public boolean isDefaultVideoEnable() {
        return defaultVideoEnable;
    }

    public void setDefaultVideoEnable(boolean defaultVideoEnable) {
        this.defaultVideoEnable = defaultVideoEnable;
    }

    public boolean isDefaultMicEnable() {
        return defaultMicEnable;
    }

    public void setDefaultMicEnable(boolean defaultMicEnable) {
        this.defaultMicEnable = defaultMicEnable;
    }

    public boolean isLocalRenderingRequired() {
        return localRenderingRequired;
    }

    public void setLocalRenderingRequired(boolean localRenderingRequired) {
        this.localRenderingRequired = localRenderingRequired;
    }


    public CameraDirection getDeafultCameraDirection() {
        return deafultCameraDirection;
    }

    public void setDeafultCameraDirection(CameraDirection deafultCameraDirection) {
        this.deafultCameraDirection = deafultCameraDirection;
    }

    public int getVideoCaptureWidth() {
        return videoCaptureWidth;
    }

    public void setVideoCaptureWidth(int videoCaptureWidth) {
        this.videoCaptureWidth = videoCaptureWidth;
    }

    public int getVideoCaptureHeight() {
        return videoCaptureHeight;
    }

    public void setVideoCaptureHeight(int videoCaptureHeight) {
        this.videoCaptureHeight = videoCaptureHeight;
    }



}

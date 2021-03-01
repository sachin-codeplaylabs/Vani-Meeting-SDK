package com.bolo.meetinglib;

import android.view.SurfaceView;

import com.bolo.meetinglib.model.MessagePayload;

import org.webrtc.CameraEnumerator;
import org.webrtc.SurfaceViewRenderer;

public interface MeetingCallBack {

    public enum Error{
        Error_Socket_Failure,
        ERROR_Camera_Permission_Not_Present,
        ERROR_Audio_Permission_Not_Present,
    };
    void onNewUserJoined(String userId);
    void onUserLeft(String userId);
    void onNewChatMessageReceived(MessagePayload messagePayload);
    void onError(Error error);
    void onConnected();
    void onLocalVideoSurfaceView(SurfaceView  surfaceViewRenderer);
    void onRemoteVideoSurfaceView(SurfaceView  surfaceViewRenderer,String userId);
    void onWhiteboardUrl(String  whiteboardUrl);
    void onVideoSurfaceViewDestroyed(SurfaceView  surfaceViewRenderer,  boolean isLocalVideo);

}

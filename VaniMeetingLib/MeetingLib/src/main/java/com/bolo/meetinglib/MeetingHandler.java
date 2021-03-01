package com.bolo.meetinglib;

import android.content.Context;

import com.bolo.meetinglib.model.MeetingStartRequest;
import com.bolo.meetinglib.model.MessagePayload;
import com.bolo.meetinglib.socketHandler.SocketAndRtc;

import org.webrtc.SurfaceViewRenderer;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.client.SocketOptionBuilder;
import io.socket.engineio.client.transports.WebSocket;

public class MeetingHandler {

    private static MeetingHandler meetingHandler;
    private SocketAndRtc socketAndRtc;
    private SurfaceViewRenderer surfaceViewRenderer;

    public static MeetingHandler getInstance(){
        if (meetingHandler == null){
            meetingHandler = new MeetingHandler();
        }
        return meetingHandler;
    }

    public void initWithUserIdAndRoomId(Context context, MeetingStartRequest meetingStartRequest, MeetingCallBack meetingCallBack){
        socketAndRtc = new SocketAndRtc(context,meetingStartRequest,meetingCallBack);
    }

    public void connect(){
        if (socketAndRtc != null){
            socketAndRtc.connectSocket();
        }

    }
    public void muteSelf(){

        if (socketAndRtc != null){
            socketAndRtc.muteSelf();
        }
    }

    public void unMuteSelf(){
        if (socketAndRtc != null){
            socketAndRtc.unMuteSelf();
        }
    }

    public void cameraOff(){
        if (socketAndRtc != null){
            socketAndRtc.pauseCamera();
        }
    }

    public void resumeCamera(){
        if (socketAndRtc != null){
            socketAndRtc.resumeCamera();
        }
    }

    public boolean sendMessage(MessagePayload messagePayload){
        if (socketAndRtc != null){
            socketAndRtc.sendChatMessage(messagePayload);
        }
        return false;
    }

    public void switchCamera(){
        if (socketAndRtc != null){
            socketAndRtc.switchCamera();
        }
    }

    public void fetchWhiteboard(){
        if (socketAndRtc != null){
            socketAndRtc.fetchWhiteboard();
        }
    }
    public void destory(){
        if (socketAndRtc != null) {
            socketAndRtc.destory();
            socketAndRtc = null;
        }
        meetingHandler = null;




    }


}

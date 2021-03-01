package com.bolo.meetinglib.socketHandler;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

public class ProxyVideoSink implements VideoSink {
    private VideoSink mTarget;
    @Override
    synchronized public void onFrame(VideoFrame frame) {
        if (mTarget == null) {
            return;
        }
        mTarget.onFrame(frame);
    }
    public synchronized void setTarget(VideoSink target) {
        this.mTarget = target;
    }
}
package com.bolo.meetinglib;

import android.view.SurfaceView;

import com.bolo.meetinglib.model.MessagePayload;

import org.webrtc.CameraEnumerator;
import org.webrtc.SurfaceViewRenderer;

public interface EventEmitterHandler {

   public void onEvent(String type , Object data);
}

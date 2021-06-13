package com.bolo.meetinglib.model;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;

import com.bolo.meetinglib.socketHandler.ProxyVideoSink;

import org.json.JSONObject;
import org.webrtc.CandidatePairChangeEvent;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Peer {

    public String id;
    public PeerConnection rTCPeerConnection;
    public boolean isNegotiating = true;
    public boolean isOfferByThis = true;
    public Participant participant  = null;
    public List<Map<String ,Object>> rtpSendres = new ArrayList();//Sachin
    public MediaStream mediaStream = new MediaStream(1);//Sachin
    public String kindOfStreamAdded = null;
    public String kindOfStreamRecived = null;
    public boolean canRestartIce = true;
    public boolean willSendOfferOnRestart = false;
    public boolean forRestartIce = false;
    public Peer(String id, PeerConnection rTCPeerConnection) {
        this.id = id;
        this.rTCPeerConnection = rTCPeerConnection;
    }
}


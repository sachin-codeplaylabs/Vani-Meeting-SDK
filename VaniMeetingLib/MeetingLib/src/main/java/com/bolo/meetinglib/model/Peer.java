package com.bolo.meetinglib.model;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;

import com.bolo.meetinglib.socketHandler.ProxyVideoSink;

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
import java.util.List;

public class Peer implements PeerConnection.Observer, SdpObserver {
    private String userName;
    private String id;
    private PeerConnection pc;
    private OnPeerConnectionResponse onPeerConnectionResponse;
    private boolean isOfferCreatedByPeer = true;
    private boolean isRenegotiating = false;
    private ProxyVideoSink videoSink;
    private SurfaceViewRenderer surfaceViewRenderer;
    private String status = "new";


    public  Peer(String id){
        super();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PeerConnection getPc() {
        return pc;
    }

    public void setPc(PeerConnection pc) {
        this.pc = pc;
    }

    public OnPeerConnectionResponse getOnPeerConnectionResponse() {
        return onPeerConnectionResponse;
    }

    public void setOnPeerConnectionResponse(OnPeerConnectionResponse onPeerConnectionResponse) {
        this.onPeerConnectionResponse = onPeerConnectionResponse;
    }


    public void setPeerConnection(PeerConnectionFactory peerFactory,    PeerConnection.RTCConfiguration rtcConfig){
        this.setPc(peerFactory.createPeerConnection(rtcConfig,this));
    }

    public static Peer peerById(List<Peer> peers , String id, boolean shouldAddIfNotExist,OnPeerConnectionResponse onPeerConnectionResponse){

        for (Peer peer : peers){
            if (peer.getId().equals(id)){
                return peer;
            }
        }
        if (shouldAddIfNotExist){
            Peer peer = new Peer(id);
            peer.setOnPeerConnectionResponse(onPeerConnectionResponse);
            peers.add(peer);

            return peer;
        }
        return null;
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
//        Log.e("Sachib","onSignalingChange");

    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        if (onPeerConnectionResponse != null){
            onPeerConnectionResponse.onIceConnectionChange(this,iceConnectionState);
        }
//        Log.e("Sachib","onIceConnectionChange");

    }

    @Override
    public void onStandardizedIceConnectionChange(PeerConnection.IceConnectionState newState) {
//        Log.e("Sachib","onStandardizedIceConnectionChange");

    }

    @Override
    public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
//        Log.e("Sachib","onConnectionChange : "  + newState.toString());
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
//        Log.e("Sachib","onIceConnectionReceivingChange");

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
//        Log.e("Sachib","onIceGatheringChange : " +iceGatheringState.toString());

    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        if (onPeerConnectionResponse != null){
            onPeerConnectionResponse.onIceCandidate(this,iceCandidate);
        }
//        Log.e("Sachib","onIceCandidate");

    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
//        Log.e("Sachib","onIceCandidatesRemoved");

    }

    @Override
    public void onSelectedCandidatePairChanged(CandidatePairChangeEvent event) {
//        Log.e("Sachib","onSelectedCandidatePairChanged");

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.e("Sachib","onAddStream");
        if (onPeerConnectionResponse != null){
            onPeerConnectionResponse.onTrack(this,mediaStream);
        }
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
//        Log.e("Sachib","onRemoveStream");

    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
//        Log.e("Sachib","onDataChannel");

    }

    @Override
    public void onRenegotiationNeeded() {
//        Log.e("VaniMeeting","onRenegotiationNeeded");
        if (onPeerConnectionResponse != null){
            onPeerConnectionResponse.onRenegotiationNeeded(this);
        }
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        Log.e("VaniMeeting","onAddTrack");

    }

    @Override
    public void onTrack(RtpTransceiver transceiver) {
        Log.e("VaniMeeting","onTrack");
//
//        if (onPeerConnectionResponse != null){
//            onPeerConnectionResponse.onTrack(this,transceiver);
//        }
    }


    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        if (onPeerConnectionResponse != null){
            onPeerConnectionResponse.onSuccesfullyCreated(this,sessionDescription);
        }
    }

    @Override
    public void onSetSuccess() {
        Log.e("onSetSuccess","onSetSuccess");

    }

    @Override
    public void onCreateFailure(String s) {
//        Log.e("onCreateFailure","onCreateFailure");
        if (onPeerConnectionResponse != null){
            onPeerConnectionResponse.onFailed(s);
        }
    }

    @Override
    public void onSetFailure(String s) {
//        Log.e("onSetFailure","onSetFailure");
        if (onPeerConnectionResponse != null){
            onPeerConnectionResponse.onFailed(s);
        }
    }

    public boolean isOfferCreatedByPeer() {
        return isOfferCreatedByPeer;
    }

    public void setOfferCreatedByPeer(boolean offerCreatedByPeer) {
        isOfferCreatedByPeer = offerCreatedByPeer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isRenegotiating() {
        return isRenegotiating;
    }

    public void setRenegotiating(boolean renegotiating) {
        isRenegotiating = renegotiating;
    }

    public ProxyVideoSink getVideoSink() {
        return videoSink;
    }

    public void setVideoSink(ProxyVideoSink videoSink) {
        this.videoSink = videoSink;
    }

    public SurfaceViewRenderer getSurfaceViewRenderer() {
        return surfaceViewRenderer;
    }

    public void setSurfaceViewRenderer(SurfaceViewRenderer surfaceViewRenderer) {
        this.surfaceViewRenderer = surfaceViewRenderer;
    }


    public void destory(){
        Handler mainHandler = new Handler(Looper.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (pc != null) {
                        pc.close();
                        pc = null;
                    }
                }
                catch (Exception e){

                }

                try{
                    if (videoSink != null){
                        videoSink.setTarget(null);
                    }
                }
                catch (Exception e){

                }
                try{
                    if (surfaceViewRenderer != null){
                        if (onPeerConnectionResponse != null){
                            onPeerConnectionResponse.onSurfaceRenderRemoving(Peer.this,surfaceViewRenderer);
                        }
                        surfaceViewRenderer.release();
                        surfaceViewRenderer = null;
                    }
                }
                catch (Exception e){

                }
                onPeerConnectionResponse = null;

            } // This is your code
        };
        mainHandler.post(myRunnable);

    }


}


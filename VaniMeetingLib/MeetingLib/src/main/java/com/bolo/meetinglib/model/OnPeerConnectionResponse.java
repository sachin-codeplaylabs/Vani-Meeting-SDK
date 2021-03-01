package com.bolo.meetinglib.model;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpTransceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

public interface OnPeerConnectionResponse {

    void onSuccesfullyCreated(Peer peer, SessionDescription sessionDescription);
    void onFailed(String error);
    void onIceCandidate(Peer peer, IceCandidate iceCandidate);
    void onRenegotiationNeeded(Peer peer);
    void onIceConnectionChange(Peer peer, PeerConnection.IceConnectionState iceConnectionState);
    void onTrack(Peer peer, MediaStream mediaStream);
    void onSurfaceRenderRemoving(Peer peer, SurfaceViewRenderer surfaceViewRenderer);

}


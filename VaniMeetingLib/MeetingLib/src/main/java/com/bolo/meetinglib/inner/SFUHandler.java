package com.bolo.meetinglib.inner;

import android.content.Context;

import com.bolo.meetinglib.model.Participant;
import com.bolo.meetinglib.model.Track;

import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;

public class SFUHandler extends BaseWebrtcSFU {


    public SFUHandler(Context context){
        super(context);
    }
    @Override
    public void optimizeStream(JSONObject data) {

    }

    @Override
    public void onAudioMuteUnmute(boolean isMuted) {

    }

    @Override
    public void onVideoPauseResume(boolean isPause) {

    }

    @Override
    public void pauseComsumer(Track track) {

    }

    @Override
    public void resumeComsumer(Track track) {

    }

    @Override
    public void startScreenshare(MediaStream screenshareStream) {

    }

    @Override
    public void updateAudioStream(MediaStream localStream) {

    }

    @Override
    public void updateVideoStream(MediaStream localStream) {

    }

    @Override
    public void handleHandshake(JSONObject data, MediaStream localStream) {

    }

    @Override
    public void onReconnect(MediaStream localStream) {

    }

    @Override
    public void onScreenShareStopped() {
        super.onScreenShareStopped();

    }

    @Override
    public void destory() {
        super.destory();
    }
    public MediaStreamTrack getLocalMediaTrack(String type){
        return null;
    }

    public void onRestartIceCandidate(JSONObject data) {

    }

    public void onSpeakerChanged(JSONObject data) {
    }
    public void  onProduceSyncDone(JSONObject data) {

    }
    public void  onConsumeTransportCreated(JSONObject data) {

    }
    public void  onServerConsumer(JSONObject data) {
    }

    public void onSendTransport(JSONObject data) {
    }

    public void onRouterRtpCapabilities(JSONObject data) {
    }
    public void  onNewProducer(JSONObject data) {
    }
    public void  onTransportConnectDone(JSONObject data) {
    }
    public void init(MediaStream localStream){

    }
    public void onParticipantUpdated(){

    }
    public void onUseLeft(Participant participant){

    }

    public void onTrackEnded(Track track){

    }

}

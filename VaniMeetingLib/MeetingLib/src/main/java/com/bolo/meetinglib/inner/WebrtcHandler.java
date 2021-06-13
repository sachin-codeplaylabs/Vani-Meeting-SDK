package com.bolo.meetinglib.inner;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.bolo.meetinglib.MeetingHandler;
import com.bolo.meetinglib.constant.Config;
import com.bolo.meetinglib.model.Participant;
import com.bolo.meetinglib.model.Peer;
import com.bolo.meetinglib.model.Track;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.HashMap;
import java.util.Map;

public class WebrtcHandler  extends BaseWebrtcSFU{

    Map<String , Peer> peerDatabase = new HashMap<>();
    MediaStream localStream = null;
    MediaStream screenshareStream = null;
    private MediaConstraints mPeerConnConstraints;


    public WebrtcHandler(Context context){
        super(context);
        getPeerFactory();
    }
    @Override
    public void handleHandshake(JSONObject data, MediaStream localStream) {
        try {
            this.localStream = localStream;
            checkAndDisconnectPeer(data.getString("userId"));
            startWebrtcOffer(data);
        }
        catch (Exception e){
            logEvent(e.toString(),true);
        }
    }
    @Override
    public void updateVideoStream(MediaStream localStream) {
        this.localStream = localStream;
        onUpdateVideoStream();
    }
    @Override
    public void updateAudioStream(MediaStream localStream) {
        this.localStream = localStream;
        updateAudioStream();
    }


    @Override
    public void destory() {

        for (Map.Entry<String, Peer> peerDataBaseEntry : peerDatabase.entrySet()) {
            disconnectPeer(peerDataBaseEntry.getValue());

        }
        super.destory();
    }



    @Override
    public void onReconnect(MediaStream localStream) {
        logEvent("onReconnect "  , false);
        this.localStream = localStream;

        for (Map.Entry<String, Peer> peerDataBaseEntry : peerDatabase.entrySet()) {
            Peer peer = (peerDataBaseEntry.getValue());
            if(peer.rTCPeerConnection.iceConnectionState() == PeerConnection.IceConnectionState.FAILED ||
                    peer.rTCPeerConnection.iceConnectionState() == PeerConnection.IceConnectionState.DISCONNECTED ){
                peer.isOfferByThis  = true;
                peer.isNegotiating = false;
                peer.forRestartIce = true;
                mPeerConnConstraints = null;
                createOffer(peer);
//                peer.rTCPeerConnection.restartIce();//Sachin
            }


        }

    }

    public void reconnectedWithoutPing(MediaStream localStream ){
        this.localStream = localStream;
        for (Map.Entry<String, Peer> peerDataBaseEntry : peerDatabase.entrySet()) {
            Peer peer = (peerDataBaseEntry.getValue());
            if(peer.rTCPeerConnection.iceConnectionState() == PeerConnection.IceConnectionState.FAILED ||
                    peer.rTCPeerConnection.iceConnectionState() == PeerConnection.IceConnectionState.DISCONNECTED ){
                if(peer.participant != null){
                    try{
                        JSONObject messageJson = new JSONObject();
                        messageJson.put("to" , peer.participant.userId);
                        messageJson.put("type" , "iceRestartPing");

                        JSONObject message = new JSONObject();
                        message.put("userId" , MeetingHandler.getInstance().getMeetingStartRequestModel().userId);
                        messageJson.put("message" , message);

                        MeetingHandler.getInstance().sendMessageToSocket(messageJson);

                    }
                    catch (Exception e){
                        logEvent(e.toString(),true);

                    }

                }

            }


        }
    }

    public void onIceRestartPing(JSONObject data ,MediaStream localStream){
        try {
            logEvent("onIceRestartPing", false);
            logEvent(data.toString(), false);
            String senderId = data.getString("userId");

            if (peerDatabase.containsKey(senderId)) {
                Peer peer = peerDatabase.get(senderId);
                if (peer.rTCPeerConnection.iceConnectionState() == PeerConnection.IceConnectionState.FAILED ||
                        peer.rTCPeerConnection.iceConnectionState() == PeerConnection.IceConnectionState.DISCONNECTED ) {
                    if (peer.willSendOfferOnRestart) {
                        peer.isOfferByThis = true;
                        peer.isNegotiating = false;
                        peer.forRestartIce  =  true;
                        mPeerConnConstraints = null;
                        createOffer(peer);
//                        peer.rTCPeerConnection.restartIce();//Sachin
                    } else {

                        JSONObject messageJson = new JSONObject();
                        messageJson.put("to" , peer.participant.userId);
                        messageJson.put("type" , "iceRestartPong");

                        JSONObject message = new JSONObject();
                        message.put("userId" , MeetingHandler.getInstance().getMeetingStartRequestModel().userId);
                        messageJson.put("message" , message);

                        MeetingHandler.getInstance().sendMessageToSocket(messageJson);

                    }
                } else {
                    if (peer.participant !=  null) {
                        JSONObject messageJson = new JSONObject();
                        messageJson.put("to" , peer.participant.userId);
                        messageJson.put("type" , "iceRestartPong");

                        JSONObject message = new JSONObject();
                        message.put("userId" , MeetingHandler.getInstance().getMeetingStartRequestModel().userId);
                        messageJson.put("message" , message);

                        MeetingHandler.getInstance().sendMessageToSocket(messageJson);

                    }
                }
            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    public void onIceRestartPong(JSONObject data ){
        try {
            logEvent("iceRestartPong", false);
            String senderId = data.getString("userId");


            if (peerDatabase.containsKey(senderId)) {
                Peer peer = peerDatabase.get(senderId);
                if (peer.rTCPeerConnection.iceConnectionState() == PeerConnection.IceConnectionState.FAILED ||
                        peer.rTCPeerConnection.iceConnectionState() == PeerConnection.IceConnectionState.DISCONNECTED) {
                    if (peer.willSendOfferOnRestart) {
                        peer.isOfferByThis = true;
                        peer.isNegotiating = false;
                        peer.forRestartIce = true;
                        mPeerConnConstraints = null;
                        createOffer(peer);

//                        peer.rTCPeerConnection.restartIce();//Sachin
                        logEvent("restartIce called", false);
                    }
                }
            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    @Override
    public void onScreenShareStopped() {
        screenShareStopped();
        super.onScreenShareStopped();

    }

    public void onPeerScreenShareEnded(JSONObject data) {
        peerScreenShareEnded(data);
    }

    public void onOffer(JSONObject data ,MediaStream localStream){
        this.localStream = localStream;
        onNewOfferRecived(data);
    }

    public void onAnswer(JSONObject data ,MediaStream localStream){
        this.localStream = localStream;
        onAnswerRecived(data);

    }


    public void onIceCandidateRecieved(JSONObject data ){
        onIceCandidate(data);

    }
    public void disconnectPeer(JSONObject data ){
        try {
            if (data.has("message") && data.getJSONObject("message").has("participant") && data.getJSONObject("message").getJSONObject("participant").has("userId")) {
                String partnerId = data.getJSONObject("message").getJSONObject("participant").getString("userId");
                checkAndDisconnectPeer(partnerId);

            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    @Override
    public void startScreenshare(MediaStream screenshareStream) {
        this.screenshareStream = screenshareStream;
        sendSSToOtherParty();
    }

    private void onIceCandidate(JSONObject data){
        try {
            Peer peer = checkAndAddPeer(data, false);
            if (peer != null) {
                JSONObject iceCandidateOfOtherParty = data.getJSONObject("message");
                String sdpMid = iceCandidateOfOtherParty.getString("sdpMid");
                int sdpMLineIndex = iceCandidateOfOtherParty.getInt("sdpMLineIndex");
                String sdp = iceCandidateOfOtherParty.getString("sdp");

                peer.rTCPeerConnection.addIceCandidate(new IceCandidate(sdpMid,sdpMLineIndex,sdp));

            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

        private void onAnswerRecived(JSONObject data){
        try {

            Peer peer = checkAndAddPeer(data, true);
            peer.kindOfStreamRecived = data.getString("streamKind");

            try {
                String  remoteSdpDescription = data.getString("message");
                String  type = data.getString("type");
                SessionDescription sdp = new SessionDescription(SessionDescription.Type.fromCanonicalForm(type),remoteSdpDescription);
                peer.rTCPeerConnection.setRemoteDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {

                    }

                    @Override
                    public void onSetSuccess() {

                    }

                    @Override
                    public void onCreateFailure(String s) {

                    }

                    @Override
                    public void onSetFailure(String s) {

                    }
                }, sdp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void  onNewOfferRecived(JSONObject data){
        try {
            Peer peer = checkAndAddPeer(data, true);
            peer.isOfferByThis = false;

            peer.kindOfStreamRecived = data.getString("streamKind");
            String remoteSdpDescription = data.getString("message");
            String type = data.getString("type");
            SessionDescription sdp = new SessionDescription(SessionDescription.Type.fromCanonicalForm(type), remoteSdpDescription);

            peer.rTCPeerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {

                }

                @Override
                public void onSetSuccess() {

                }

                @Override
                public void onCreateFailure(String s) {

                }

                @Override
                public void onSetFailure(String s) {

                }
            }, sdp);
            peer.rTCPeerConnection.createAnswer(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {

                    peer.rTCPeerConnection.setLocalDescription(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {

                        }

                        @Override
                        public void onSetSuccess() {

                        }

                        @Override
                        public void onCreateFailure(String s) {
                            logEvent(s, true);

                        }

                        @Override
                        public void onSetFailure(String s) {
                            logEvent(s, true);

                        }
                    }, sessionDescription);

                    if(peer.kindOfStreamAdded == null){
                        peer.kindOfStreamAdded = "audio-video";
                    }
                    try {
                        JSONObject messageJson = new JSONObject();
                        messageJson.put("to", peer.participant.userId);
                        messageJson.put("type", sessionDescription.type.canonicalForm());
                        messageJson.put("message", sessionDescription.description);
                        messageJson.put("streamKind", peer.kindOfStreamAdded);
                        MeetingHandler.getInstance().sendMessageToSocket(messageJson);
                    }
                    catch (Exception e){
                        logEvent(e.toString(),true);

                    }
                }

                @Override
                public void onSetSuccess() {

                }

                @Override
                public void onCreateFailure(String s) {
                    logEvent(s, true);

                }

                @Override
                public void onSetFailure(String s) {
                    logEvent(s, true);

                }
            }, getmPeerConnConstraints(peer));
        }
        catch (Exception e) {
            logEvent(e.toString(),true);

        }
    }

    private void startWebrtcOffer(JSONObject data){
        Peer peer = checkAndAddPeer(data,true);
        peer.willSendOfferOnRestart = true;
        createOffer(peer);
    }

    private void createOffer(Peer peer){
        logEvent("createOffer",false);


        boolean shouldEnableTransceiverForAudio = true;
        boolean shouldEnableTransceiverForVideo = true;
        if(localStream != null && localStream.videoTracks.size() > 0){
            shouldEnableTransceiverForVideo = false;
        }
        if(localStream != null && localStream.audioTracks.size() > 0){
            shouldEnableTransceiverForAudio = false;
        }
        try{
            if(shouldEnableTransceiverForAudio ){
                RtpTransceiver.RtpTransceiverInit init = new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY);
                peer.rTCPeerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO , init);


            }
            if(shouldEnableTransceiverForVideo){
                RtpTransceiver.RtpTransceiverInit init = new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY);
                peer.rTCPeerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO , init);
            }
        }
        catch(Exception e){

        }
        peer.isOfferByThis = true ;
        peer.isNegotiating = true;

        peer.rTCPeerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peer.rTCPeerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {


                    }

                    @Override
                    public void onSetSuccess() {

                    }

                    @Override
                    public void onCreateFailure(String s) {

                    }

                    @Override
                    public void onSetFailure(String s) {

                    }
                },sessionDescription);

                if(peer.kindOfStreamAdded == null){
                    peer.kindOfStreamAdded = "audio-video";
                }
                try {
                    JSONObject messageJson = new JSONObject();
                    messageJson.put("to", peer.participant.userId);
                    messageJson.put("type", sessionDescription.type.canonicalForm());
                    messageJson.put("message", sessionDescription.description);
                    messageJson.put("streamKind", peer.kindOfStreamAdded);
                    MeetingHandler.getInstance().sendMessageToSocket(messageJson);
                    peer.kindOfStreamAdded = null;

                }
                catch (Exception e){
                    logEvent(e.toString(),true);

                }
            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        },getmPeerConnConstraints(peer));

    }



    private Peer checkAndAddPeer(JSONObject data,boolean canAddNew){
        try {
            String senderId = data.getString("userId");
            Peer peer = peerDatabase.get(senderId);
            if (peer == null && canAddNew == true) {

                PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(Config.getIceServers(MeetingHandler.getInstance().getMeetingStartRequestModel().appId));
                rtcConfiguration.iceTransportsType = PeerConnection.IceTransportsType.ALL;
                rtcConfiguration.iceCandidatePoolSize = 2;
                rtcConfiguration.bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT;
                rtcConfiguration.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

                peer = new Peer(senderId, null);
                peer.rTCPeerConnection =  addCallBackForPeer(peer,rtcConfiguration);

                if (localStream != null) {

                    if (localStream.videoTracks != null) {
                        for (MediaStreamTrack track : localStream.videoTracks) {
                            peer.kindOfStreamAdded = "audio-video";
                            RtpSender rtpSender = peer.rTCPeerConnection.addTrack(track);
                            Map<String, Object> peerRtpSender = new HashMap<>();
                            peerRtpSender.put("rtpSender", rtpSender);
                            peerRtpSender.put("kind", track.kind());
                            peer.rtpSendres.add(peerRtpSender);
                        }
                    }
                    if (localStream.audioTracks != null) {
                        for (MediaStreamTrack track : localStream.audioTracks) {
                            peer.kindOfStreamAdded = "audio-video";
                            RtpSender rtpSender = peer.rTCPeerConnection.addTrack(track);
                            Map<String, Object> peerRtpSender = new HashMap<>();
                            peerRtpSender.put("rtpSender", rtpSender);
                            peerRtpSender.put("kind", track.kind());
                            peer.rtpSendres.add(peerRtpSender);
                        }
                    }
                    peerDatabase.put(senderId, peer);

                }
                if (peer.participant == null) {
                    peer.participant = Participant.participantByUserId(MeetingHandler.getInstance().getParticipantFromLocal(), senderId);
                }

                if (peer.participant == null && data.has("participant")) {

                    JSONObject participant = data.getJSONObject("participant");
                    ObjectMapper objectMapper = new ObjectMapper();
                    Participant newParticipant = objectMapper.readValue(participant.toString(), Participant.class);
                    Participant.addParticipantIfNotExist(MeetingHandler.getInstance().getParticipantFromLocal(), newParticipant);
                    peer.participant = newParticipant;
                }
            }
            return peer;
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
        return null;
    }

    private void setMaxBitRateForRtpSender(Peer peer){
        try {
            if (peer != null && peer.rTCPeerConnection != null && peer.rTCPeerConnection.getSenders() != null) {

                for (RtpSender sender : peer.rTCPeerConnection.getSenders()) {
                    if (sender != null && sender.track() != null && sender.track().kind().equalsIgnoreCase("video")) {
                        RtpParameters params = sender.getParameters();
                        if (params != null && params.encodings != null) {
                            for (RtpParameters.Encoding e : params.encodings) {
                                e.maxBitrateBps = Config.maxBitRateConfig(MeetingHandler.getInstance().getMeetingStartRequestModel().appId);
                            }
                            sender.setParameters(params);
                        }
                    }
                }

            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }

    }

    private PeerConnection addCallBackForPeer(Peer peer, PeerConnection.RTCConfiguration rtcConfiguration){

        return  getPeerFactory().createPeerConnection(rtcConfiguration, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                if(peer.rTCPeerConnection.signalingState() == PeerConnection.SignalingState.STABLE){
                    setMaxBitRateForRtpSender(peer);
                }
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                logEvent(("pc.iceConnectionState " + iceConnectionState.toString()) , false);

                if(iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED ||
                        iceConnectionState ==PeerConnection.IceConnectionState.FAILED){
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(peer.canRestartIce){
                                if(peer.rTCPeerConnection.iceConnectionState() == PeerConnection.IceConnectionState.FAILED ||
                                        peer.rTCPeerConnection.iceConnectionState() == PeerConnection.IceConnectionState.DISCONNECTED){
                                    MeetingHandler.getInstance().onIceCandidateDisconnected();
                                }
                            }

                        }
                    }, 300);


                }
                if(iceConnectionState == PeerConnection.IceConnectionState.COMPLETED){
                    onPeerConnected(peer);
                    setMaxBitRateForRtpSender(peer);
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {

            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {


            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                if(iceCandidate != null){
                    try {
                        JSONObject iceCandidateJson = new JSONObject();
                        iceCandidateJson.put("sdp", iceCandidate.sdp);
                        iceCandidateJson.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                        iceCandidateJson.put("sdpMid", iceCandidate.sdpMid);

                        JSONObject messageJson = new JSONObject();
                        messageJson.put("to", peer.participant.userId);
                        messageJson.put("type", "iceCandidate");
                        messageJson.put("message", iceCandidateJson);
                        MeetingHandler.getInstance().sendMessageToSocket(messageJson);
                    }
                    catch (Exception e){
                        logEvent(e.toString(),true);

                    }

                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                logEvent("onaddstream 1111" , false);
                if(mediaStream != null){
                    MediaStream incomingMediaStream = mediaStream;
                    if(incomingMediaStream.audioTracks != null){
                        for (MediaStreamTrack streamTrack : incomingMediaStream.audioTracks){
                            onNewTrack(streamTrack,peer,incomingMediaStream);
                        }
                    }
                    if(incomingMediaStream.videoTracks != null){
                        for (MediaStreamTrack streamTrack : incomingMediaStream.videoTracks){
                            onNewTrack(streamTrack,peer,incomingMediaStream);
                        }
                    }
                }
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {

            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {

            }

            @Override
            public void onRenegotiationNeeded() {
                logEvent("onnegotiationneeded",false);
                if (peer.rTCPeerConnection.signalingState() != PeerConnection.SignalingState.STABLE) return;

                if(peer.isNegotiating || peer.isOfferByThis == false){
                    return;
                }
                peer.isNegotiating = true;
                createOffer(peer);
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                logEvent("ontrack 1111" , false);

                if(rtpReceiver != null && rtpReceiver.track() != null){
                    MediaStream stream  = null;
                    if (mediaStreams != null && mediaStreams.length >0){
                        stream = mediaStreams[0];
                    }
                    else{
                        stream = getPeerFactory().createLocalMediaStream(peer.id);
                    }
                    onNewTrack(rtpReceiver.track(),peer,stream);
                }

            }
        });

    }


    private void onNewTrack ( MediaStreamTrack track,Peer peer,MediaStream mediaStreams){



        if(track != null){
            boolean shouldRefersh = false;
            String videoType = track.kind() ;
            if(peer.kindOfStreamRecived != null && peer.kindOfStreamRecived.equalsIgnoreCase("SS") && track.kind() .equalsIgnoreCase( "video")){
                videoType = "SS";
            }

            Track trackObj = Track.getTrackOfUserId(MeetingHandler.getInstance().getAllTracks(),track.kind(),peer.participant.userId,videoType);
            String videoKind = "audio";

            if(peer.kindOfStreamRecived != null && peer.kindOfStreamRecived.equalsIgnoreCase( "SS") && track.kind().equalsIgnoreCase( "video")){
                videoKind = peer.kindOfStreamRecived;
            }
            else if(track.kind().equalsIgnoreCase( "video")){
                videoKind = "video";
            }


            if(trackObj == null){
                trackObj = new Track(peer.participant.userId,track,false,videoKind );
            }
            else{
                trackObj.updateTrackData(track.id(),track);
                shouldRefersh = true;
            }
            trackObj.participant = peer.participant;
            if(peer.kindOfStreamRecived != null && peer.kindOfStreamRecived.equalsIgnoreCase( "SS") && trackObj.kind.equalsIgnoreCase( "video")){
                trackObj.videoType = peer.kindOfStreamRecived;
                trackObj.cameraType = "back";
            }
            else if(trackObj.kind.equalsIgnoreCase( "video")){
                trackObj.videoType = "video";
                trackObj.cameraType = "front";

            }

            if(shouldRefersh){
                MeetingHandler.getInstance().emitToSource("refershTrack",trackObj);
            }
            else{
                MeetingHandler.getInstance().addTrack(trackObj);
                MeetingHandler.getInstance().emitToSource("onTrack",trackObj);

            }
        }
    }



    private  void  onPeerConnected(Peer peer){
        logEvent("screenshareStream check",false);
        if(screenshareStream != null){
            boolean isSSStreamFound = false;
            for(Map rtpSendre : peer.rtpSendres){
                logEvent("screenshareStream " + rtpSendre.get("kind") , false);


                if( ((String)rtpSendre.get("kind")).equalsIgnoreCase( "SS")){
                    isSSStreamFound = true;
                    break;
                }

            }

            if(!isSSStreamFound){
                logEvent("SS Sending",false);
                sendSSToPeer(peer);
            }

        }
        else{
            logEvent("screenshareStream not found",false);
        }
    }

    private void sendSSToOtherParty(){
        if(screenshareStream != null && screenshareStream.videoTracks.size() > 0){
            for (Map.Entry<String, Peer> peerDataBaseEntry : peerDatabase.entrySet()) {
                sendSSToPeer(peerDataBaseEntry.getValue());

            }
        }
    }

    private void sendSSToPeer(Peer peer) {
        logEvent("sendSSToPeer",false);

        Map rtpSenderMap = getRtpStrem(peer,"SS");
        boolean shouldAdd = true;

        if(rtpSenderMap != null){
            RtpSender rtpSender = (RtpSender) rtpSenderMap.get("rtpSender");
            if(rtpSender != null){
                try {
                    rtpSender.setTrack(screenshareStream.videoTracks.get(0), true);

                    try{
                        JSONObject messageJson = new JSONObject();
                        messageJson.put("to" ,peer.participant.userId );
                        messageJson.put("type" ,"trackReplaced" );
                        messageJson.put("streamType" ,"SS" );
                        MeetingHandler.getInstance().sendMessageToSocket(messageJson);

                    }
                    catch (Exception e){
                        logEvent(e.toString(),true);

                    }

                    shouldAdd = false;
                }
                catch (Exception e){

                }

            }
        }


        if(shouldAdd){
            peer.isNegotiating = false;
            peer.isOfferByThis = true;
            peer.kindOfStreamAdded = "SS";
            RtpSender newRtpSender = null;
            newRtpSender = peer.rTCPeerConnection.addTrack(screenshareStream.videoTracks.get(0));
            logEvent("SS",false);

            Map<String, Object> peerRtpSender = new HashMap<>();
            peerRtpSender.put("rtpSender", newRtpSender);
            peerRtpSender.put("kind", "SS");
            peer.rtpSendres.add(peerRtpSender);
        }
    }


    private void peerScreenShareEnded(JSONObject data) {
        try {
            String userId = data.getString("userId");

            Track screenShareTrack = null;
            for (Track track : MeetingHandler.getInstance().getAllTracks()){
                if (track.participant != null && track.participant.userId.equalsIgnoreCase(userId) && track.videoType.equalsIgnoreCase("SS")) {
                    screenShareTrack = track;
                    break;
                }
            }
            if (screenShareTrack !=  null) {
                MeetingHandler.getInstance().emitToSource("onTrackEnded", screenShareTrack);
                Track.checkAndRemoveTrack(MeetingHandler.getInstance().getAllTracks(), screenShareTrack.trackId);
            }

        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }


    private void screenShareStopped(){
        if(screenshareStream != null  ){
            for (Map.Entry<String, Peer> peerDataBaseEntry : peerDatabase.entrySet()) {
                Peer peer = peerDataBaseEntry.getValue();
                for(int index = 0 ; index < peer.rtpSendres.size() ; index ++) {
                    Map rtpSendre = peer.rtpSendres.get(index);
                    if(((String)rtpSendre.get("kind")).equalsIgnoreCase( "SS")) {
                        peer.isNegotiating = false;
                        peer.isOfferByThis = true;
//                        for(RtpTransceiver transceiver : peer.rTCPeerConnection.getTransceivers()){
//                            if(transceiver.getSender() != null && transceiver.getSender().track() != null && transceiver.getSender().track().id().equalsIgnoreCase(((RtpSender)rtpSendre.get("rtpSender")).track().id() )){
//                                try {
//                                    transceiver.stop();
//                                }
//                                catch (Exception e){
//
//                                }
//                            }
//                        }
                        peer.rtpSendres.remove(index);
                        break;
                    }
                }
            }

        }
        screenshareStream = null;
    }


    private void checkAndDisconnectPeer(String partnerId){
        if(peerDatabase.containsKey(partnerId)){
            Peer peer = peerDatabase.get(partnerId);
            disconnectPeer(peer);
        }
    }

    private void disconnectPeer(Peer peer){
        if(peer == null){ return;}
        // meetingHandler.emitToSource("onUserLeft",peer.participant)
        if(peer.rTCPeerConnection != null){
            peer.canRestartIce = false;
            peer.rTCPeerConnection.close();
        }
        peerDatabase.remove(peer.id);
    }


    private void onUpdateVideoStream() {
        if(localStream != null && localStream.videoTracks.size() > 0  ){

            for (Map.Entry<String, Peer> peerDataBaseEntry : peerDatabase.entrySet()) {
                Peer peer = peerDataBaseEntry.getValue();
                Map rtpSenderMap = getRtpStrem(peer,"video");
                RtpSender rtpSender = (RtpSender) rtpSenderMap.get("rtpSender");
                boolean shouldAdd = true;
                if(rtpSender != null){
                    logEvent("Vani updateVideoStream  found",false);
                    try {
                        rtpSender.setTrack(localStream.videoTracks.get(0), true);
                        shouldAdd = false;

                    }
                    catch (Exception e){

                    }
                }
                if(shouldAdd){
                    logEvent("Vani updateVideoStream not found",false);

                    peer.isOfferByThis = true;
                    peer.isNegotiating = false;
                    peer.rTCPeerConnection.addTrack(localStream.videoTracks.get(0));
                    try{
                        Map<String, Object> peerRtpSender = new HashMap<>();
                        peerRtpSender.put("rtpSender", rtpSender);
                        peerRtpSender.put("kind", "video");
                        peer.rtpSendres.add(peerRtpSender);
                    }
                    catch (Exception e){
                        logEvent(e.toString(),true);

                    }


                }
            }
        }
    }


    private void  updateAudioStream() {
        if(localStream != null && localStream.audioTracks.size() > 0  ){

            for (Map.Entry<String, Peer> peerDataBaseEntry : peerDatabase.entrySet()) {
                Peer peer = peerDataBaseEntry.getValue();
                Map rtpSenderMap = getRtpStrem(peer,"audio");
                RtpSender rtpSender = (RtpSender) rtpSenderMap.get("rtpSender");
                boolean shouldAddTrack = true;
                if(rtpSender != null){
                    logEvent("Vani updateVideoStream  found",false);
                    try {
                        rtpSender.setTrack(localStream.audioTracks.get(0), true);
                        shouldAddTrack = false;

                    }
                    catch (Exception e){
                        logEvent(e.toString(),true);

                    }
                }
                if(shouldAddTrack){
                    logEvent("Vani updateVideoStream not found",false);

                    peer.isOfferByThis = true;
                    peer.isNegotiating = false;
                    peer.rTCPeerConnection.addTrack(localStream.audioTracks.get(0));
                    try{
                        Map<String, Object> peerRtpSender = new HashMap<>();
                        peerRtpSender.put("rtpSender", rtpSender);
                        peerRtpSender.put("kind", "audio");
                        peer.rtpSendres.add(peerRtpSender);
                    }
                    catch (Exception e){
                        logEvent(e.toString(),true);

                    }


                }
            }



        }

    }

    private Map getRtpStrem(Peer peer,String videoType){
        for(Map rtpSendre : peer.rtpSendres){
            if(((String)rtpSendre.get("kind")).equalsIgnoreCase(videoType)){
                return rtpSendre;
            }
        }
        return null;
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



    private MediaConstraints getmPeerConnConstraints(Peer peer){
        if (mPeerConnConstraints == null){
            mPeerConnConstraints = new MediaConstraints();
            mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));
            mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
            mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("voiceActivityDetection", "true"));
            if(peer.forRestartIce){
                mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("IceRestart", "true"));
                peer.forRestartIce = false;
            }

        }
        return mPeerConnConstraints;
    }











}

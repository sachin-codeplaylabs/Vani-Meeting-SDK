package com.bolo.meetinglib.socketHandler;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.bolo.meetinglib.MeetingCallBack;
import com.bolo.meetinglib.constant.Constant;
import com.bolo.meetinglib.constant.Utility;
import com.bolo.meetinglib.model.MeetingStartRequest;
import com.bolo.meetinglib.model.MessagePayload;
import com.bolo.meetinglib.model.OnPeerConnectionResponse;
import com.bolo.meetinglib.model.Peer;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Capturer;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Capturer;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.CapturerObserver;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.HardwareVideoDecoderFactory;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.client.SocketOptionBuilder;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.EngineIOException;
import io.socket.engineio.client.transports.WebSocket;

public class SocketAndRtc implements OnPeerConnectionResponse {
    private final GoogleAnalytics sAnalytics;
    private static Tracker sTracker;
    private Socket mSocket;
    private String userId,roomId;
    private MeetingStartRequest meetingStartRequest;
    private List<String> listnerEvents = new ArrayList<>();
    private List<Peer> peers = new ArrayList<>();
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private MediaConstraints mPeerConnConstraints = new MediaConstraints();
    public MediaStream mLocalMediaStream;
    private  EglBase rootEglBase = EglBase.create();

    private Context context;

    MeetingCallBack meetingCallBack;
    private PeerConnectionFactory peerFactory;
    private VideoSource mVideoSource;
    private CameraVideoCapturer cameraVideoCapturer;
    private boolean isConnectCalled = false;
    private ProxyVideoSink localVideoSink;
    private SurfaceViewRenderer localSurfaceViewRenderer;
    private long startTime = 0;

    public SocketAndRtc(Context context, MeetingStartRequest meetingStartRequest, MeetingCallBack meetingCallBack) {
        this.userId = meetingStartRequest.getUserId();
        this.roomId = meetingStartRequest.getRoomId();
        this.meetingCallBack = meetingCallBack;
        this.meetingStartRequest = meetingStartRequest;
        this.context = context;
        sAnalytics = GoogleAnalytics.getInstance(context);

    }

    synchronized public Tracker getDefaultTracker() {
        // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
        if (sTracker == null) {
            sTracker = sAnalytics.newTracker("UA-190458994-3");
            sTracker.enableExceptionReporting(true);
            sTracker.enableAutoActivityTracking(true);

        }

        return sTracker;
    }
    public  void  connectSocket(){
        if(initWebRtc(context) == false){
            return;
        }

        try {
            if (mSocket != null){
                return;
            }

            IO.Options option = SocketOptionBuilder.builder().build();
            option.forceNew = true;
            option.transports = new String[]{WebSocket.NAME};

            mSocket = IO.socket(Constant.host,option);
            mSocket.on(Socket.EVENT_CONNECT,onConnect);
            mSocket.on(Socket.EVENT_CONNECT_ERROR,onConnectionError);
            mSocket.on(Socket.EVENT_DISCONNECT,onConnectionDisconnect);
            mSocket.connect();

            Utility.logEventNew("Call","Connect",getDefaultTracker());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destory(){
        cleanUpAllConnection();
        cleanLocalVideo();
        if (mSocket != null){
            mSocket.off();
            mSocket.close();
            mSocket.disconnect();
            mSocket  = null;
        }

        if (startTime > 100){
            long callTimeInMilli = Calendar.getInstance().getTimeInMillis() - startTime;
            callTimeInMilli = callTimeInMilli / 1000;
            Utility.logEventNew("Call","Time:" + callTimeInMilli + " Sec", getDefaultTracker());
        }
    }

    private void cleanUpAllConnection() {
        for (Peer peer : peers) {
            peer.destory();
        }
        peers.clear();
    }

    private void cleanLocalVideo(){
        try{

            if(localVideoSink != null){
                localVideoSink.setTarget(null);
                localVideoSink = null;
            }
            if (localSurfaceViewRenderer != null){
                if (meetingCallBack != null){
                    meetingCallBack.onVideoSurfaceViewDestroyed(localSurfaceViewRenderer,true);
                }
                localSurfaceViewRenderer.release();
                localSurfaceViewRenderer = null;
            }


        }
        catch (Exception e){

        }
    }


    private void addExtraListener(String event, Emitter.Listener listener){
        if (mSocket != null && mSocket.isActive()){
            if(mSocket.hasListeners(event)){
                return;
            }

            mSocket.on(event,listener);
            listnerEvents.add(event);
        }

    }

    private void socketSubscribeToTopic(){

        if (mSocket != null && mSocket.isActive() ){

            addExtraListener("offer", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onNewOfferRecieved((JSONObject) args[0]);

                }
            });
            addExtraListener("chat", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onChatMessageReceived((JSONObject) args[0]);

                }
            });

            addExtraListener("whiteboard", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onWhiteboardUrlRecieved((JSONObject) args[0]);

                }
            });

            addExtraListener("answer", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onAnswer((JSONObject) args[0]);

                }
            });

//            addExtraListener("userLeft", new Emitter.Listener() {
//                @Override
//                public void call(Object... args) {
////                    onPartnerLeft((JSONObject) args[0]);
//
//                }
//            });


            addExtraListener("iceCandidate", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onIceCandidate((JSONObject) args[0]);

                }
            });


            addExtraListener("newJoinee", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onNewUserJoined((JSONObject) args[0]);

                }
            });
            addExtraListener("handshake", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onHandshakeMsg((JSONObject) args[0]);

                }
            });

            addExtraListener("setupDone", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onSetUpDone((JSONObject) args[0]);

                }
            });
            try {
                JSONObject userIdInformation = new JSONObject();
                userIdInformation.put("userId",userId);
                mSocket.emit("userId", userIdInformation);


                JSONObject hostNotificationRoom = new JSONObject();
                hostNotificationRoom.put("type","subscribeNotification");
                hostNotificationRoom.put("message",userId);
                hostNotificationRoom.put("id",userId);

                mSocket.emit("joinRoom", hostNotificationRoom);

                JSONObject roomForAllClient = new JSONObject();
                roomForAllClient.put("type","newJoinee");
                roomForAllClient.put("message",roomId);
                roomForAllClient.put("id",userId);

                mSocket.emit("joinRoom", roomForAllClient);
                askIfSetupDone();
            }
            catch (Exception e){

            }
        }
    }


    private void askIfSetupDone(){
        try{
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject checkIfSetupDone = new JSONObject();
                        checkIfSetupDone.put("type", "setupDone");
                        checkIfSetupDone.put("message", "Is Setup Done");
                        checkIfSetupDone.put("to", userId);
                        mSocket.emit("setupDone", checkIfSetupDone);

                    }
                    catch (Exception e){

                    }
                }
            }, 150);

        }
        catch (Exception e){

        }
    }

    private void onSetUpDone(JSONObject jsonObject){
        if (meetingCallBack != null && isConnectCalled == false){
            Handler mainHandler = new Handler(Looper.getMainLooper());

            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    isConnectCalled = true;
                    meetingCallBack.onConnected();
                    startTime = Calendar.getInstance().getTimeInMillis();
                }
            };
            mainHandler.post(myRunnable);
        }
    }
    private void onWhiteboardUrlRecieved(JSONObject jsonObject){
        try{
            String whiteboardUrl = jsonObject.getString("meesage");
            Handler mainHandler = new Handler(Looper.getMainLooper());

            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    if (meetingCallBack != null){
                        meetingCallBack.onWhiteboardUrl(whiteboardUrl);
                    }
                }
            };
            mainHandler.post(myRunnable);

        }
        catch (Exception e){

        }

    }
    private void onNewUserJoined(JSONObject jsonObject){
        Log.e("onNewUserJoined",jsonObject.toString());
        try{
            JSONObject handShakeJson = new JSONObject();
            handShakeJson.put("meesage","Welcome");
            handShakeJson.put("type","handshake");
            handShakeJson.put("to",jsonObject.get("id"));
            sendMessage(handShakeJson);
        }
        catch (Exception e){

        }

    }
    private void onHandshakeMsg(JSONObject jsonObject){
        Log.e("onHandshakeMsg",jsonObject.toString());
        startWebrtcOffer(jsonObject);

    }

    private void startWebrtcOffer(JSONObject jsonObject){
        try {

            String otherPartyUserId = jsonObject.getString("id");
            Peer peer = Peer.peerById(peers, otherPartyUserId, true, this);
            if (peer.getStatus().equals("new")) {
                peer.setOfferCreatedByPeer(false);
                PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(getIceServers());
//                rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
                peer.setPeerConnection(getPeerFactory(),rtcConfig);
//                peer.getPc().addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO);
//                peer.getPc().addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO);
//                if (mLocalMediaStream != null){
//                    for (VideoTrack  videoTrack : mLocalMediaStream.videoTracks){
//                        peer.getPc().addTrack(videoTrack);
//                    }
//                    for (AudioTrack  audioTrack : mLocalMediaStream.audioTracks){
//                        peer.getPc().addTrack(audioTrack);
//                    }
//                }
                if (mLocalMediaStream != null){
                    peer.getPc().addStream(mLocalMediaStream);
                }

                peer.getPc().createOffer(peer, getmPeerConnConstraints());
                peer.setStatus("Offer Created");

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void onNewOfferRecieved(JSONObject offerObject){

        try {
            String otherPartyUserId = offerObject.getString("id");
            Peer peer = Peer.peerById(peers,otherPartyUserId,true,this);
            PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(getIceServers());
//            rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
            peer.setPeerConnection(getPeerFactory(),rtcConfig);
//            peer.getPc().addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO);
//            peer.getPc().addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO);
//            if (mLocalMediaStream != null){
//               for (VideoTrack  videoTrack : mLocalMediaStream.videoTracks){
//                   peer.getPc().addTrack(videoTrack);
//               }
//                for (AudioTrack  audioTrack : mLocalMediaStream.audioTracks){
//                    peer.getPc().addTrack(audioTrack);
//                }
//            }
            if (mLocalMediaStream != null){
                peer.getPc().addStream(mLocalMediaStream);
            }
            setRemoteDescription(peer,offerObject);
            peer.getPc().createAnswer(peer,getmPeerConnConstraints());
            peer.setStatus("Answer Sent");


        } catch (JSONException e) {
            e.printStackTrace();
        }



    }

    private void  onAnswer(JSONObject answerObject){
        try {
            String otherPartyUserId = answerObject.getString("id");
            Peer peer = Peer.peerById(peers,otherPartyUserId,false,this);
            if (peer != null) {
                setRemoteDescription(peer,answerObject);

            }
        }
        catch (Exception e){
        }
    }

    private void setRemoteDescription(Peer peer, JSONObject remoteDescription){
        try {
            String  description = remoteDescription.getString("message");
            String  type = remoteDescription.getString("type");
            SessionDescription sdp = new SessionDescription(SessionDescription.Type.fromCanonicalForm(type),description);
            peer.getPc().setRemoteDescription(peer, sdp);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void onIceCandidate(JSONObject iceCandiateJson){
        try{
            String otherPartyUserId = iceCandiateJson.getString("id");
            Peer peer = Peer.peerById(peers,otherPartyUserId,false,this);
            if (peer != null){
                try {
                    JSONObject iceCandidateDetails = iceCandiateJson.getJSONObject("message");
                    String sdp = iceCandidateDetails.getString("sdp");
                    int sdpMLineIndex = iceCandidateDetails.getInt("sdpMLineIndex");
                    String sdpMid = iceCandidateDetails.getString("sdpMid");
                    peer.getPc().addIceCandidate(new IceCandidate(sdpMid, sdpMLineIndex, sdp));
                    Log.e("onIceCandidate rec","set");

                }
                catch (Exception e){

                }
            }
        }
        catch (Exception e){

        }
    }
    private void onChatMessageReceived(JSONObject chatJsonObject) {
        try {
            String to = chatJsonObject.getString("to");
            if (to.equals(roomId)) {
                to = "all";
            }
            MessagePayload messagePayload = new MessagePayload(chatJsonObject.getString("message"),to);
            messagePayload.setSender(chatJsonObject.getString("id"));
            if (meetingCallBack != null) {
                Handler mainHandler = new Handler(Looper.getMainLooper());

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        meetingCallBack.onNewChatMessageReceived(messagePayload);
                    }
                };
                mainHandler.post(myRunnable);
            }
        }
        catch (Exception e) {
        }
    }

    private void sendSessionDescription(Peer peer, SessionDescription sessionDescription){
        try {
                JSONObject offerObject = new JSONObject();
            offerObject.put("to",peer.getId());
            offerObject.put("id",userId);
            offerObject.put("type",sessionDescription.type.canonicalForm());
            offerObject.put("message",sessionDescription.description);
            sendMessage(offerObject);

//                Log.e("offer",offerObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public boolean sendChatMessage(MessagePayload messagePayload) {
        if (mSocket != null && mSocket.isActive()) {
            try {
                JSONObject chatMsg = new JSONObject();
                chatMsg.put("message",messagePayload.getMessage());
                if (messagePayload.getTo().equals("all")) {
                    chatMsg.put("to",roomId);
                }
                else {
                    chatMsg.put("to",messagePayload.getTo());
                }
                chatMsg.put("id",mSocket.id());
                chatMsg.put("type","chat");
                sendMessage(chatMsg);
                return true;
            } catch (JSONException e) {
                return false;
            }
        }
        return false;
    }

    public void muteSelf() {

        if (mLocalMediaStream != null) {
            if(mLocalMediaStream.audioTracks != null && mLocalMediaStream.audioTracks.isEmpty() == false) {
                AudioTrack audioTrack = mLocalMediaStream.audioTracks.get(0);
                audioTrack.setEnabled(false);
            }
        }
    }

    public void unMuteSelf() {
        if (mLocalMediaStream != null) {
            if(mLocalMediaStream.audioTracks != null && mLocalMediaStream.audioTracks.isEmpty() == false) {
                AudioTrack audioTrack = mLocalMediaStream.audioTracks.get(0);
                audioTrack.setEnabled(true);
            }
        }
    }
    public void pauseCamera() {
        if (mLocalMediaStream != null) {
            if(mLocalMediaStream.videoTracks != null && mLocalMediaStream.videoTracks.isEmpty() == false) {
                VideoTrack videoTrack = mLocalMediaStream.videoTracks.get(0);
                videoTrack.setEnabled(false);
            }
        }
    }
    public void resumeCamera() {
        if (mLocalMediaStream != null) {
            if(mLocalMediaStream.videoTracks != null && mLocalMediaStream.videoTracks.isEmpty() == false) {
                VideoTrack videoTrack = mLocalMediaStream.videoTracks.get(0);
                videoTrack.setEnabled(true);
            }
        }
    }



    public void switchCamera(){
        cameraVideoCapturer.switchCamera(null);
    }
    public void fetchWhiteboard(){
        if (mSocket != null && mSocket.isActive()){
            try{
                JSONObject whiteboardJson = new JSONObject();
                whiteboardJson.put("type","whiteboard");
                whiteboardJson.put("meesage",roomId);
                whiteboardJson.put("to",userId);
                mSocket.emit("whiteboard",whiteboardJson);
            }
            catch (Exception e){

            }
        }
    }

    private void sendMessage(JSONObject jsonObject){
        if (mSocket == null || mSocket.isActive() == false){return;}

        try {
            mSocket.emit("message",jsonObject);
            Log.e("message",jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private VideoTrack createVideoTrack(Context context) {

        CameraEnumerator cameraEnumerator;
        if (useCamera2()){
            cameraEnumerator = new Camera2Enumerator(context);
//            cameraEnumerator = new Camera1Enumerator(false);

        }
        else{
            cameraEnumerator = new Camera1Enumerator(false);
        }

         cameraVideoCapturer = createVideoCapturer(cameraEnumerator,meetingStartRequest.getDeafultCameraDirection());
        boolean isScreencast = cameraVideoCapturer.isScreencast();
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
        mVideoSource = getPeerFactory().createVideoSource(isScreencast);
        cameraVideoCapturer.initialize(surfaceTextureHelper,context, mVideoSource.getCapturerObserver());
        cameraVideoCapturer.startCapture(meetingStartRequest.getVideoCaptureWidth(),meetingStartRequest.getVideoCaptureHeight(),30);
        VideoTrack localVideoTrack = getPeerFactory().createVideoTrack("ARDAMSv0", mVideoSource);
        localVideoTrack.setEnabled(meetingStartRequest.isDefaultVideoEnable());
        if (meetingStartRequest.isLocalRenderingRequired() ){

            localSurfaceViewRenderer = new SurfaceViewRenderer(context);
            localSurfaceViewRenderer.init(rootEglBase.getEglBaseContext(), null);
            localSurfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            localSurfaceViewRenderer.setMirror(true);
            localSurfaceViewRenderer.setEnableHardwareScaler(false);
            localSurfaceViewRenderer.setZOrderMediaOverlay(true);

            localVideoSink = new ProxyVideoSink();

            localVideoSink.setTarget(localSurfaceViewRenderer);
            ((VideoTrack) localVideoTrack).addSink(localVideoSink);

            if (meetingCallBack != null){
                meetingCallBack.onLocalVideoSurfaceView(localSurfaceViewRenderer);
            }
        }

        return localVideoTrack;
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(context);
    }

    private  CameraVideoCapturer createVideoCapturer( CameraEnumerator enumerator,
                                                               MeetingStartRequest.CameraDirection direction)
    {
        String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if ((direction == MeetingStartRequest.CameraDirection.CAMERA_DIRECTION_FRONT && enumerator.isFrontFacing(deviceName)) ||
                    (direction == MeetingStartRequest.CameraDirection.CAMERA_DIRECTION_BACK  && enumerator.isBackFacing(deviceName)))
            {
                return enumerator.createCapturer(deviceName, null);
            }
        }

        return null;
    }
    public boolean initWebRtc(Context context){

        if (meetingStartRequest.isCanSendAudio()){
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                meetingCallBack.onError(MeetingCallBack.Error.ERROR_Audio_Permission_Not_Present);
                return false;
            }
        }
        if(meetingStartRequest.isCanSendVideo()){
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                meetingCallBack.onError(MeetingCallBack.Error.ERROR_Camera_Permission_Not_Present);
                return false;
            }
        }
        mLocalMediaStream = getPeerFactory().createLocalMediaStream("ARDAMS");

        if (meetingStartRequest.isCanSendAudio()) {
            AudioSource audioSource = getPeerFactory().createAudioSource(new MediaConstraints());
            AudioTrack audioTrack = getPeerFactory().createAudioTrack("ARDAMSa0", audioSource);
            audioTrack.setEnabled(meetingStartRequest.isDefaultMicEnable());
            mLocalMediaStream.addTrack(audioTrack);


        }
        if (meetingStartRequest.isCanSendVideo()){
            mLocalMediaStream.addTrack( createVideoTrack(context));
        }

        return true;
    }

    private LinkedList<PeerConnection.IceServer> getIceServers(){
        if (iceServers == null || iceServers.isEmpty()){
            iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
            iceServers.add(new PeerConnection.IceServer("stun:stundoubtconnect.vaniassistant.com:3478","sachin","Hhands@12345"));
            iceServers.add(new PeerConnection.IceServer("turn:turndoubtconnect.vaniassistant.com:3478","sachin","Hhands@12345"));
        }
        return iceServers;
    }

    private MediaConstraints getmPeerConnConstraints(){
        if (mPeerConnConstraints == null){
            mPeerConnConstraints = new MediaConstraints();
            mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));
            mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
            mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("voiceActivityDetection", "true"));

        }
        return mPeerConnConstraints;
    }

    private PeerConnectionFactory getPeerFactory(){
        if (peerFactory == null){
            initPeerConnectionFactory();
        }
        return peerFactory;
    }

    private void initPeerConnectionFactory(){

        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions()
        );

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.networkIgnoreMask = 0;



        VideoDecoderFactory videoDecoderFactory = new HardwareVideoDecoderFactory(EglBase.create().getEglBaseContext());

        peerFactory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(context)
                        .setUseHardwareAcousticEchoCanceler(false).setUseHardwareAcousticEchoCanceler(false).createAudioDeviceModule())
                .setVideoEncoderFactory( new DefaultVideoEncoderFactory(
                        rootEglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, false))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext()))
                .setOptions(options)

                .createPeerConnectionFactory();


    }
    Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            if (mSocket != null && mSocket.isActive()){

                listnerEvents = new ArrayList<>();
                Log.e("Socket id",mSocket.id());

                socketSubscribeToTopic();

                Utility.logEventNew("Call","Connect Success",getDefaultTracker());


            }
        }
    };

    Emitter.Listener onConnectionDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

        }
    };

    Emitter.Listener onConnectionError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e("error",args[0].toString());
            try {
                Utility.logEventNew("Call","Connect Error",getDefaultTracker());
                EngineIOException engineIOException  = (EngineIOException) args[0];
                if (meetingCallBack != null && peers.isEmpty() && isConnectCalled == false){
                    Handler mainHandler = new Handler(Looper.getMainLooper());

                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            meetingCallBack.onError(MeetingCallBack.Error.Error_Socket_Failure);
                        }
                    };
                    mainHandler.post(myRunnable);
                }

                if (mSocket != null) {
                    for (String event : listnerEvents) {
                        mSocket.off(event);
                    }
                    listnerEvents = new ArrayList<>();
                }
            }
            catch (Exception e){

            }
            //mSocket.off();
        }
    };


    private void onUserLeft(Peer peer){
        String userId = peer.getId();
        peer.destory();
        peers.remove(peer);

        Handler mainHandler = new Handler(Looper.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if (meetingCallBack != null) {
                    meetingCallBack.onUserLeft(userId);
                }
            } // This is your code
        };
        mainHandler.post(myRunnable);

    }
    @Override
    public void onSuccesfullyCreated(Peer peer, SessionDescription sessionDescription) {
        sendSessionDescription(peer,sessionDescription);
        peer.setStatus("SessionDescription Sent");
        peer.getPc().setLocalDescription(peer,sessionDescription);

    }

    @Override
    public void onFailed(String error) {

    }

    @Override
    public void onIceCandidate(Peer peer, IceCandidate iceCandidate) {

        try {
            JSONObject iceCandidateMessage = new JSONObject();
            iceCandidateMessage.put("to",peer.getId());
            iceCandidateMessage.put("type","iceCandidate");

            JSONObject  iceCandidateInnerJson = new JSONObject();
            iceCandidateInnerJson.put("sdp",iceCandidate.sdp);
            iceCandidateInnerJson.put("sdpMLineIndex",iceCandidate.sdpMLineIndex);
            iceCandidateInnerJson.put("sdpMid",iceCandidate.sdpMid);
            iceCandidateMessage.put("message",iceCandidateInnerJson);

            sendMessage(iceCandidateMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRenegotiationNeeded(Peer peer) {
        if (peer.isOfferCreatedByPeer() == false && peer.isRenegotiating() == false && peer.getStatus().equals("new") == false){
            peer.setRenegotiating(true);
            peer.getPc().createOffer(peer, getmPeerConnConstraints());
            peer.setStatus("Offer Created");
        }

    }

    @Override
    public void onIceConnectionChange(Peer peer, PeerConnection.IceConnectionState iceConnectionState) {
        Log.e("onIceConnectionChange",iceConnectionState.toString());

        if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED){
           onUserLeft(peer);
        }
        else if(iceConnectionState == PeerConnection.IceConnectionState.CONNECTED){
            peer.setRenegotiating(false);
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    if (meetingCallBack != null) {
                        meetingCallBack.onNewUserJoined(peer.getId());

                    }
                    Utility.logEventNew("Peer","Count: " + peers.size(), getDefaultTracker());
                }
            };
            mainHandler.post(myRunnable);

        }

    }

    @Override
    public void onTrack(Peer peer, MediaStream mediaStream) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                for(VideoTrack track : mediaStream.videoTracks) {
                    SurfaceViewRenderer surfaceViewRenderer = new SurfaceViewRenderer(context);
                    surfaceViewRenderer.init(rootEglBase.getEglBaseContext(), null);
                    surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                    surfaceViewRenderer.setMirror(true);
                    surfaceViewRenderer.setEnableHardwareScaler(false);
                    surfaceViewRenderer.setZOrderMediaOverlay(true);
                    peer.setSurfaceViewRenderer(surfaceViewRenderer);
                    peer.setVideoSink(new ProxyVideoSink());
                    peer.getVideoSink().setTarget(surfaceViewRenderer);
                    ((VideoTrack) track).addSink(peer.getVideoSink());
                    if (meetingCallBack != null) {
                        meetingCallBack.onRemoteVideoSurfaceView(surfaceViewRenderer, peer.getId());
                    }

                }
            }
        };
        mainHandler.post(myRunnable);
    }

    @Override
    public void onSurfaceRenderRemoving(Peer peer, SurfaceViewRenderer surfaceViewRenderer) {
        if (meetingCallBack != null){
            meetingCallBack.onVideoSurfaceViewDestroyed(surfaceViewRenderer,false);
        }
    }

}


package com.bolo.meetinglib;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bolo.meetinglib.constant.Config;
import com.bolo.meetinglib.constant.EglUtils;
import com.bolo.meetinglib.inner.BaseWebrtcSFU;
import com.bolo.meetinglib.inner.HandlerDelegate;
import com.bolo.meetinglib.inner.SFUHandler;
import com.bolo.meetinglib.inner.WebrtcHandler;
import com.bolo.meetinglib.model.MeetingStartRequest;
import com.bolo.meetinglib.model.MessagePayload;
import com.bolo.meetinglib.model.Output;
import com.bolo.meetinglib.model.Participant;
import com.bolo.meetinglib.model.Peer;
import com.bolo.meetinglib.model.Track;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoTrack;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


import static com.bolo.meetinglib.constant.Config.wssUrl;


public class MeetingHandler implements HandlerDelegate {

    private Context context;
    private MeetingStartRequest meetingStartRequest = null;
    private WebSocketClient wss = null;
    private MediaStream localStream = null;
    private boolean isSetUpDone = false;
    private List<JSONObject> handShakeDatas = new ArrayList<>();
    private List<JSONObject> newJoniees = new ArrayList<>();
    private boolean isStartupSetupCalled = false;
    private List<Peer> peers = new ArrayList<>();
    private boolean isPermissionGiven = false;
    private MediaStream screenshareStream = null;
    private String currentVideoDeviceId = null, currentAudioDeviceId = null;
    private int setUpTry = 0;
    private boolean isStartAndSetupWithServerCalled = false;
    List<Participant> allParticipant = new ArrayList<>();
    List<Track> allTracks = new ArrayList<>();
    private String connection = "new";

    private boolean isFetchAudioInProgress = false;
    private boolean isFetchVideoInProgress = false;
    OutputCallBack muteUmutePromiseResolver = null;
    OutputCallBack videoPauseResumePromiseResolver = null;
    private boolean isEnded = false;
    private BaseWebrtcSFU webrtcSFUHandller = null;
    private Timer internetReachbilityTimeout = null;
    private Timer socketCheckTimeout = null;
    private boolean isReachable = true;
    private Map<String,List<EventEmitterHandler>> events = new HashMap<>();
    int CAPTURE_PERMISSION_REQUEST_CODE = 1922;

    static MeetingHandler  instance;
    private Intent mMediaProjectionPermissionResultData;

    public  static  MeetingHandler getInstance(){
        if(instance == null){
            instance = new MeetingHandler();
        }
        return instance;
    }

    public boolean isPermissionApproved(){
        return  isPermissionGiven;
    }
// To Optimize Stream if Speaker is not speaking
    public void optimizeStream(JSONObject data){
        if(meetingStartRequest == null){
            return;
        }
        getHandler().optimizeStream(data);
    }
// Start Screenshare require Foreground Service
    public void startScreenShare(Intent foregroundServiceIntent, Activity activity){//Sachin
        if (foregroundServiceIntent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(foregroundServiceIntent);
            }
        }
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) activity.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);

    }
    private VideoCapturer createScreenCapturer() {

        if (mMediaProjectionPermissionResultData != null) {

            return   new ScreenCapturerAndroid(
                    mMediaProjectionPermissionResultData, new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    mMediaProjectionPermissionResultData = null;
                    stopScreenSharing();
                }
            });
        }
        return  null;
    }

    // Stop Screenshare
    public void stopScreenSharing(){
        if (screenshareStream != null && screenshareStream.videoTracks.size() > 0) {

            onScreensharingStoped();
//            try {
//                screenshareStream.videoTracks.get(0).dispose();
//            }
//            catch (Exception e){
//
//            }
        }
    }
    // Get Meeting Start Time
    public void getMeetingStartTime(){
        try {
            if (isWebScoketConnected()) {
                JSONObject meetingStartTimeObject = new JSONObject();
                meetingStartTimeObject.put("type", "getMeetingStartTime");
                meetingStartTimeObject.put("to", meetingStartRequest.userId);
                sendSocketMessage("getMeetingStartTime", meetingStartTimeObject);
            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }
    // Init SDK
    public void init(Context context){
        this.context = context;
        createEmptySelfStreamIfNotPresent();
    }
    //Check Socket Connection
    public void checkSocket(){
        isEnded = false;
        logEvent("vani checkSocket",false);
        connect(false);
    }

    //Switch Camera
    public boolean switchCamera(){
        Track track = Track.getLocalTrack(allTracks,"video" , "video");
        if(track != null && track.track != null){
            getHandler().switchCamera();
            return true;
        }
        return false;
    }

    public MeetingStartRequest meetingStartRequestObject(String roomId,String  userId,String  appId){
        if(roomId.equals(userId)){
            Log.e("Vani_Meeting" , "Room Id and User Id could not be same");
            return  null;
        }
        meetingStartRequest = new MeetingStartRequest( userId , roomId, appId);
        return meetingStartRequest;
    }
    public void switchToWhiteboard(){
        try {
            if (isWebScoketConnected()) {
                JSONObject whiteboardJson = new JSONObject();
                whiteboardJson.put("type", "switchWhiteboard");
                whiteboardJson.put("to", meetingStartRequest.roomId);
                sendMessage(whiteboardJson);
            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }

    }
    public MessagePayload newMessageObject(String to, String message){
        MessagePayload messagePayload = new MessagePayload(message, to);
        messagePayload.sender = meetingStartRequest.userId;
        return messagePayload;
    }
    public void fetchWhiteboard(){
        try {
            if (isWebScoketConnected()) {
                JSONObject whiteboardJson = new JSONObject();
                whiteboardJson.put("type", "whiteboard");
                whiteboardJson.put("message", meetingStartRequest.roomId);
                whiteboardJson.put("to", meetingStartRequest.roomId);
                sendMessage(whiteboardJson);
            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    public boolean isAudioMuted(){
        Track track = Track.getLocalTrack(allTracks, "audio", "audio");
        if (track != null && track.track != null) {
            return !track.track.enabled() || (track.track.state() == MediaStreamTrack.State.ENDED);
        }
        if (track.participant != null) {
            return !track.participant.isAudioEnable;
        }
        return true;
    }


    public void pauseStreamWithoutStopping(String stremType,String  userId){
        if (meetingStartRequest.userId.equals(userId)) {
            String kind = "video";
            if(stremType .equalsIgnoreCase( "audio")){
                kind = "audio";
            }
            Track track = Track.getLocalTrack(allTracks, kind, stremType);
            if(track != null && track.track != null){
                track.track.setEnabled( false);
                try {
                    if (isWebScoketConnected()) {
                        JSONObject data = new JSONObject();
                        data.put("userId",  meetingStartRequest.userId);
                        data.put("type", stremType);
                        data.put("status", "pause");
                        JSONObject audioPause = new JSONObject();
                        audioPause.put("message" , data);
                        sendSocketMessage("audioVideoPauseResume" , audioPause);
                    }
                }
                catch (Exception e){
                    logEvent(e.toString(),true);

                }
            }


        }
    }
    public void resumeStreamWithoutAdding(String stremType,String  userId){
        if (meetingStartRequest.userId .equalsIgnoreCase( userId)) {
            String kind = "video";
            if(stremType .equalsIgnoreCase( "audio")){
                kind = "audio";
            }
            Track track = Track.getLocalTrack(allTracks, kind, stremType);
            if(track != null && track.track != null){
                track.track.setEnabled(true);
                try {
                    JSONObject data = new JSONObject();
                    data.put("userId" ,meetingStartRequest.userId);
                    data.put("type" ,stremType);
                    data.put("status" ,"resume");

                    JSONObject audioPause = new JSONObject();
                    audioPause.put("message" ,data);
                    audioPause.put("type" ,"audioVideoPauseResume");
                    sendSocketMessage("audioVideoPauseResume", audioPause);
                }
                catch (Exception e){
                    logEvent(e.toString(),true);

                }
            }


        }
    }


    public void muteUser(String  userId,OutputCallBack outputCallBack){

        if (meetingStartRequest.userId .equalsIgnoreCase( userId) ){
            if (isFetchAudioInProgress) {
                outputCallBack.onCompletion(new Output("Already in Progess", false, "Busy", new HashMap<>()));
                return;
            }
            Track track = Track.getLocalTrack(allTracks, "audio", "audio");
            if (track != null) {

                if (track.participant != null) {
                    track.participant.isAudioEnable = false;
                }
                if (track.track != null) {
                    track.track.setEnabled(false);
                    try {
                        track.track.dispose();
                    }
                    catch (Exception e){

                    }
                    try {
                        localStream.removeTrack((AudioTrack) track.track);
                    }
                    catch (Exception e){

                    }
                }
                track.track = null;
            }
            try {
                if (isWebScoketConnected()) {
                    JSONObject data = new JSONObject();
                    data.put("userId",  meetingStartRequest.userId);
                    data.put("type", "audio");
                    data.put("status", "pause");
                    JSONObject audioPause = new JSONObject();
                    audioPause.put("message" , data);
                    sendSocketMessage("audioVideoPauseResume" , audioPause);
                }
            }
            catch (Exception e){
                logEvent(e.toString(),true);

            }

            if (meetingStartRequest != null && meetingStartRequest.shouldUseSFU()) {
                getHandler().onAudioMuteUnmute(true);
            }
            Participant participant = getSelfParticipant();
            if (participant != null) {
                participant.isAudioEnable = false;
            }
            currentAudioDeviceId = null;
            outputCallBack.onCompletion(new Output("Success", true, "Success", new HashMap<>()));
            return;
        }
        else {
            if (meetingStartRequest.isAdmin) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("admin",meetingStartRequest.userId);
                    data.put("user",userId);
                    JSONObject audioPause = new JSONObject();
                    audioPause.put("message" , data);
                    audioPause.put("type" , "audioBlock");
                    audioPause.put("to" , meetingStartRequest.roomId);
                    sendMessage(  audioPause);
                }
                catch (Exception e){
                    logEvent(e.toString(),true);

                }

                Participant participant = Participant.participantByUserId(allParticipant, userId);
                if (participant != null) {
                    participant.isAudioBlockedByAdmin = true;
                }

                outputCallBack.onCompletion( new Output("Success", true, "Success", new HashMap<>()));
                return;
            }
            else {
                outputCallBack.onCompletion( new Output("Permission Not Allowed", false, "Permission_Failure",new HashMap<>()));
                return;
            }
        }

    }

    public void startLocalStream(boolean isVideoRequired ,boolean isAudioRequired ){
        initProcess(isAudioRequired, isVideoRequired);
    }

    public void unmute(String userId , OutputCallBack outputCallBack){
        if (meetingStartRequest.userId .equalsIgnoreCase( userId)) {

            if (isFetchAudioInProgress) {
                outputCallBack.onCompletion(new Output("Already in Progess", false, "Busy", new HashMap<>()));
                return;
            }
            Participant participant = getSelfParticipant();
            if (participant == null) {
                outputCallBack.onCompletion(new Output("Permission Not Allowed", false, "Permission_Failure",new HashMap<>()));
                return;
            }
            if (participant.isAudioBlockedByAdmin) {
                outputCallBack.onCompletion(new Output("Permission Not Allowed", false, "Permission_Failure", new HashMap<>()));
                return;
            }
            else {
                muteUmutePromiseResolver = outputCallBack;
                if (meetingStartRequest != null) {
                    initProcess(true, false);
                }
                return;
            }
        }
        else {
            if (meetingStartRequest.isAdmin) {
                try{
                    JSONObject data = new JSONObject();
                    data.put("admin" , meetingStartRequest.userId);
                    data.put("user" , userId);
                    JSONObject audioUnblock = new JSONObject();
                    audioUnblock.put("message" , data);
                    audioUnblock.put("type" , "audioUnblock");
                    audioUnblock.put("to" , meetingStartRequest.roomId);
                    sendMessage(audioUnblock);

                }
                catch (Exception e)
                {
                    logEvent(e.toString(),true);

                }

                Participant participant = Participant.participantByUserId(allParticipant, userId);
                if (participant != null) {
                    participant.isAudioBlockedByAdmin = false;
                }
                outputCallBack.onCompletion(new Output("Success", true, "Success", new HashMap<>()));
                return;
            }
            else {
                outputCallBack.onCompletion(new Output("Permission Not Allowed", false, "Permission_Failure", new HashMap<>()));
                return;
            }
        }
    }


    public Output blockMessaging(String userId ){
        if (meetingStartRequest.isAdmin) {
            try{
                JSONObject data = new JSONObject();
                data.put("admin" , meetingStartRequest.userId);
                data.put("user" , userId);
                JSONObject messageBlock = new JSONObject();
                messageBlock.put("message" , data);
                messageBlock.put("type" , "messageBlock");
                messageBlock.put("to" , meetingStartRequest.roomId);
                sendMessage(messageBlock);

            }
            catch (Exception e)
            {
                logEvent(e.toString(),true);

            }
            Participant participant = Participant.participantByUserId(allParticipant, userId);
            if (participant != null) {
                participant.isMessageBlockedByAdmin = true;
            }

            return new Output("Success", true, "Success", new HashMap<>());

        }
        else {
            return new Output("Permission Not Allowed", false, "Permission_Failure", new HashMap<>());

        }
    }

    public Output unblockMessaging(String userId ) {
        if (meetingStartRequest.isAdmin) {

            try{
                JSONObject data = new JSONObject();
                data.put("admin" , meetingStartRequest.userId);
                data.put("user" , userId);
                JSONObject messageUnblock = new JSONObject();
                messageUnblock.put("message" , data);
                messageUnblock.put("type" , "messageUnblock");
                messageUnblock.put("to" , meetingStartRequest.roomId);
                sendMessage(messageUnblock);

            }
            catch (Exception e)
            {
                logEvent(e.toString(),true);

            }

            Participant participant = Participant.participantByUserId(allParticipant, userId);
            if (participant != null) {
                participant.isMessageBlockedByAdmin = false;
            }
            return new Output("Success", true, "Success", new HashMap<>());

        }
        else {
            return new Output("Permission Not Allowed", false, "Permission_Failure", new HashMap<>());

        }
    }

    public Output blockWhiteboard(String userId ) {
        if (meetingStartRequest.isAdmin) {

            try{
                JSONObject data = new JSONObject();
                data.put("admin" , meetingStartRequest.userId);
                data.put("user" , userId);
                JSONObject whiteboardBlock = new JSONObject();
                whiteboardBlock.put("message" , data);
                whiteboardBlock.put("type" , "whiteboardBlock");
                whiteboardBlock.put("to" , meetingStartRequest.roomId);
                sendSocketMessage("whiteboardBlock", whiteboardBlock);

            }
            catch (Exception e)
            {
                logEvent(e.toString(),true);

            }

            Participant participant = Participant.participantByUserId(allParticipant, userId);
            if (participant != null) {
                participant.isWhiteboardBlockedByAdmin = true;
            }

            return new Output("Success", true, "Success", new HashMap<>());

        }
        else {
            return new Output("Permission Not Allowed", false, "Permission_Failure", new HashMap<>());

        }
    }

    public Output unblockWhiteboard(String userId ) {
        if (meetingStartRequest.isAdmin) {

            try{
                JSONObject data = new JSONObject();
                data.put("admin" , meetingStartRequest.userId);
                data.put("user" , userId);
                JSONObject whiteboardUnblock = new JSONObject();
                whiteboardUnblock.put("message" , data);
                whiteboardUnblock.put("to" , meetingStartRequest.roomId);
                sendSocketMessage("whiteboardUnblock", whiteboardUnblock);

            }
            catch (Exception e)
            {                    logEvent(e.toString(),true);


            }

            Participant participant = Participant.participantByUserId(allParticipant, userId);
            if (participant != null) {
                participant.isWhiteboardBlockedByAdmin = false;
            }
            return new Output("Success", true, "Success", new HashMap<>());

        }
        else {
            return new Output("Permission Not Allowed", false, "Permission_Failure", new HashMap<>());

        }
    }

    public  boolean isVideoPaused(){
        Track track = Track.getLocalTrack(allTracks, "video", "video");
        if (track != null && track.track != null) {
            return !track.track.enabled() || (track.track.state() == MediaStreamTrack.State.ENDED);
        }
        if (track.participant != null) {
            return !track.participant.isVideoEnable;
        }
        return true;
    }


    public void pauseCamera(String userId , OutputCallBack outputCallBack){
        if (meetingStartRequest.userId .equalsIgnoreCase( userId)) {
            if (isFetchVideoInProgress) {

                outputCallBack.onCompletion( new Output("Already in Progess", false, "Busy", new HashMap<>()));
                return;

            }


            Track track = Track.getLocalTrack(allTracks, "video", "video");
            if (track != null) {
                // track.track.enabled = false;
                if (track.track != null) {
                    getHandler().stopCameraCapture();
                    track.track.setEnabled(false);
                    try {
                        track.track.dispose();
                    }
                    catch (Exception e){

                    }
                    try{
                    localStream.removeTrack((VideoTrack) track.track);
                    }
                    catch (Exception e){

                    }
                }
                if (track.participant != null) {
                    track.participant.isVideoEnable = false;
                }


            }
            Participant participant = getSelfParticipant();
            if (participant != null) {
                participant.isVideoEnable = false;
            }
            try{
                JSONObject data = new JSONObject();
                data.put("userId" , meetingStartRequest.userId);
                data.put("status" , "pause");
                data.put("type" , "video");

                JSONObject videoPause = new JSONObject();
                videoPause.put("message" , data);
                videoPause.put("type" , "audioVideoPauseResume");
                sendSocketMessage("audioVideoPauseResume", videoPause);

            }
            catch (Exception e)
            {
                logEvent(e.toString(),true);

            }

            if (meetingStartRequest != null ) {
                getHandler().onVideoPauseResume(true);
            }

            track.track = null;
            currentVideoDeviceId = null;
            outputCallBack.onCompletion( new Output("Success", true, "Success", new HashMap<>()));
            return;
        }
        else {
            if (meetingStartRequest.isAdmin) {
                try{
                    JSONObject data = new JSONObject();
                    data.put("admin" , meetingStartRequest.userId);
                    data.put("user" , userId);

                    JSONObject videoBlock = new JSONObject();
                    videoBlock.put("message" , data);
                    videoBlock.put("type" , "videoBlock");
                    videoBlock.put("to" , meetingStartRequest.roomId);
                    sendMessage( videoBlock);
                }
                catch (Exception e)
                {
                    logEvent(e.toString(),true);

                }
                Participant participant = Participant.participantByUserId(allParticipant, userId);
                if (participant != null) {
                    participant.isVideoBlockedByAdmin = true;
                }

                outputCallBack.onCompletion( new Output("Success", true, "Success", new HashMap<>()));
                return;
            }
            else {
                outputCallBack.onCompletion( new Output("Permission Not Allowed", false, "Permission_Failure", new HashMap<>()));
                return;
            }
        }
    }

    public void resumeCamera(String userId , OutputCallBack outputCallBack){


        if (meetingStartRequest.userId .equalsIgnoreCase( userId) ){
            if (isFetchVideoInProgress) {
                outputCallBack.onCompletion( new Output("Already in Progess", false, "Busy", new HashMap<>()));
                return;
            }
            Participant participant = Participant.participantByUserId(allParticipant, userId);
            if (participant == null || participant.isVideoBlockedByAdmin) {
                outputCallBack.onCompletion( new Output("Permission Not Allowed", false, "Permission_Failure", new HashMap<>()));
                return;

            }

            if (meetingStartRequest != null) {
                videoPauseResumePromiseResolver = outputCallBack;
                initProcess(false, true);
            }
            else{
                outputCallBack.onCompletion( new Output("Permission Not Allowed", false, "Permission_Failure", new HashMap<>()));
                return;
            }
        }
        else {
            if (meetingStartRequest.isAdmin) {
                try{
                    JSONObject data = new JSONObject();
                    data.put("admin" , meetingStartRequest.userId);
                    data.put("user" , userId);

                    JSONObject videoBlock = new JSONObject();
                    videoBlock.put("message" , data);
                    videoBlock.put("type" , "videoUnblock");
                    videoBlock.put("to" , meetingStartRequest.roomId);
                    sendMessage( videoBlock);
                }
                catch (Exception e)
                {
                    logEvent(e.toString(),true);

                }
                Participant participant = Participant.participantByUserId(allParticipant, userId);
                if (participant != null) {
                    participant.isVideoBlockedByAdmin = false;
                }
                outputCallBack.onCompletion(  new Output("Success", true, "Success", new HashMap<>()));
                return;

            }
            else {
                outputCallBack.onCompletion(  new Output("Permission Not Allowed", false, "Permission_Failure", new HashMap<>()));
                return;

            }
        }

    }

    public Participant participantByUserId(String userId){
        return Participant.participantByUserId(allParticipant, userId);
    }


    public List<Track> tracksByUserId(String userId){
        return Track.getAllTrackOfUserId(allTracks, userId);
    }


    public void  startMeeting(){
        if(isStartAndSetupWithServerCalled){
            return;
        }
        logEvent("startMeeting",false);
        try{
            JSONObject data = new JSONObject();
            data.put("user" , meetingStartRequest.userId);

            JSONObject startMeetingCalled = new JSONObject();
            startMeetingCalled.put("message" , data);
            startMeetingCalled.put("type" , "startMeetingCalled");
            startMeetingCalled.put("to" , meetingStartRequest.roomId);
            sendSocketMessage( "startMeetingCalled",startMeetingCalled);
        }
        catch (Exception e)
        {
            logEvent(e.toString(),true);

        }

        Participant participant = Participant.participantByUserId(allParticipant, meetingStartRequest.userId);
        if (participant != null) {
            logEvent("startMeeting participant",false);
            participant.isStartMeetingCalled = true;
        }
        isStartAndSetupWithServerCalled = true;
        startAndSetupWithServer();
        getAllPausedTracks();
    }

    public void getLocalStream(){
        sendLocalStreamRequest(true);
    }

    public void endAndDestory(DestoryCallBack destoryCallBack){
        onEndAndDestory(destoryCallBack);
    }

    public Output sendMessage(MessagePayload messagePayload) {
        Participant participant = Participant.participantByUserId(allParticipant, messagePayload.sender);
        if (participant == null || participant.isMessageBlockedByAdmin) {
            return new Output("Permission Not Allowed", false, "Permission_Failure", new HashMap<>());
        } else {
            String to = messagePayload.to;
            if (to .equalsIgnoreCase( "all") ){
                to = meetingStartRequest.roomId;
            }

            try{
                JSONObject messageObj = new JSONObject();
                messageObj.put("message" , messagePayload.message);
                messageObj.put("type" , "chat");
                messageObj.put("to" , to);
                sendMessage( messageObj);
            }
            catch (Exception e)
            {
                logEvent(e.toString(),true);

            }

            return new Output("Success", true, "Success", new HashMap<>());
        }
    }


    public void getParticipants(){
        getAllParticipants();
    }

    public void pauseTrackOutOfScreen(Track track){
        if(meetingStartRequest != null){
            getHandler().pauseComsumer(track);
        }
    }

    public void resumeTrackInScreen(Track track){
        if(meetingStartRequest != null){
            getHandler().resumeComsumer(track);
        }
    }




    ////////////////////////////////////////
    //*********HandlerDelegate***********//
    //////////////////////////////////////

    public void onIceCandidateDisconnected(){
        if (internetReachbilityTimeout != null) {
            return;
        }
        internetReachbilityTimeout = new Timer();
        internetReachbilityTimeout.schedule(new TimerTask() {
            @Override
            public void run() {
                MeetingHandler.this.checkIfInternetReachable(0);
            }
        }, 200);
    }

    public void sendMessageToSocket(JSONObject msgToSend){
        sendMessage(msgToSend);
    }

    public MeetingStartRequest getMeetingStartRequestModel(){
        return meetingStartRequest;
    }


    public void sendSFUMessageToSocket(JSONObject msgToSend){
        sendSFUMessage(msgToSend);
    }

    public void emitToSource(String  type  , Object emitData ){
        emitMessageToSource(type, emitData);
    }

    public List<Participant> getParticipantFromLocal(){
        return allParticipant;
    }

    public List<Track> getAllTracks(){
        return allTracks;
    }

    public boolean addTrack(Track track){
        return Track.checkAndAddTrack(allTracks, track);
    }

    ////////////////////////////////////////
    //*********HandlerDelegate Over***********//
    //////////////////////////////////////



    public void onScreenShareStream(MediaStream stream){
        screenshareStream = stream;
        if (stream != null && stream.videoTracks.size() > 0) {
            if (meetingStartRequest == null) {
                return;
            }
            getHandler().startScreenshare(screenshareStream);

            Track trackObject = Track.getLocalTrack(allTracks, "video", "SS");
            boolean shouldRefersh = false;
            if (trackObject == null) {
                trackObject = new Track(getSelfParticipant().userId, stream.videoTracks.get(0), true,"SS");
                shouldRefersh = false;
            }
            else {
                trackObject.updateTrackData(stream.videoTracks.get(0).id(), stream.videoTracks.get(0));
                shouldRefersh = true;
            }
            trackObject.videoType = "SS";
            trackObject.participant = getSelfParticipant();
            if (shouldRefersh) {
                emitMessageToSource("refershTrack", trackObject);
            }
            else {
                Track.checkAndAddTrack(allTracks, trackObject);
                emitMessageToSource("onTrack", trackObject);
            }
            emitMessageToSource("screenshareStream", trackObject);
        }
    }


    private void initProcess(boolean isAudioRequired, boolean isVideoRequired) {
        if (isAudioRequired == false &&
                isVideoRequired == false) {
            isFetchVideoInProgress = false;
            isFetchAudioInProgress = false;
            return;
        }

        MediaStream currentStream = getHandler().getPeerFactory().createLocalMediaStream("localStream");
        if (isAudioRequired){
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                try {
                    JSONObject permissionIssue = new JSONObject();
                    permissionIssue.put("error", "permissionError");
                    permissionIssue.put("isForAudio", true);
                    permissionIssue.put("isForVideo", false);
                    permissionIssue.put("isForSS", false);

                    emitMessageToSource("permissionError", permissionIssue);
                    isAudioRequired = false;
                }
                catch (Exception e){

                }

            }
            else{
                Track oldLocalTrack = Track.getLocalTrack(allTracks, "audio", "audio");
                if (oldLocalTrack !=  null) {
                    if (oldLocalTrack.track !=  null) {
                        try {
                            oldLocalTrack.track.dispose();
                        }
                        catch (Exception e){

                        }
                    }
                }
                currentStream.addTrack((AudioTrack) getHandler().getLocalMediaTrack("audio"));
            }


        }
        if (isVideoRequired){
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                try {
                    JSONObject permissionIssue = new JSONObject();
                    permissionIssue.put("error", "permissionError");
                    permissionIssue.put("isForAudio", true);
                    permissionIssue.put("isForVideo", false);
                    permissionIssue.put("isForSS", false);

                    emitMessageToSource("permissionError", permissionIssue);
                    isVideoRequired = false;
                }
                catch (Exception e){

                }

            }
            else{
                Track oldLocalTrack = Track.getLocalTrack(allTracks, "video", "video");
                if (oldLocalTrack !=  null) {
                    if (oldLocalTrack.track !=  null) {
                        try {
                            oldLocalTrack.track.dispose();
                        }
                        catch (Exception e){

                        }
                    }
                }

                currentStream.addTrack((VideoTrack) getHandler().getLocalMediaTrack("video"));
            }


        }


        onStreamGot(currentStream, isAudioRequired, isVideoRequired);




    }

    private void createEmptySelfStreamIfNotPresent() {
        Track videoTrack = Track.getLocalTrack(allTracks, "video", "video");
        Participant selfParticipant = getSelfParticipant();
        if (videoTrack == null) {
            videoTrack = new Track(selfParticipant.userId , null, true,"video");
            videoTrack.kind = "video";
            videoTrack.videoType = "video";
            videoTrack.participant = selfParticipant;
            videoTrack.participant.isVideoEnable = false;
            allTracks.add(videoTrack);
        }
        Track audioTrack = Track.getLocalTrack(allTracks, "audio", "audio");
        if (audioTrack == null) {
            audioTrack = new Track(selfParticipant.userId, null, true,"audio");
            audioTrack.kind = "audio";
            audioTrack.videoType = "audio";
            audioTrack.participant = selfParticipant;
            audioTrack.participant.isAudioEnable = false;
            allTracks.add(audioTrack);
        }

    }



    private void onStreamError(Exception error,boolean isAudioRequired,boolean isVideoRequired) {
        isFetchVideoInProgress = false;
        isFetchAudioInProgress = false;
        logEvent(error.getLocalizedMessage(),true);
        try {
            JSONObject permissionError = new JSONObject();
            permissionError.put("error" ,error);
            permissionError.put("isForAudio" ,isAudioRequired);
            permissionError.put("isForVideo" ,isVideoRequired);
            permissionError.put("isForSS" ,false);
            emitMessageToSource("permissionError", permissionError);
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
        if(isAudioRequired && muteUmutePromiseResolver != null){
            muteUmutePromiseResolver.onCompletion(new Output("Permission Denied", false, "permission_error", new HashMap<>()));
            muteUmutePromiseResolver = null;
        }
        else if(isVideoRequired && videoPauseResumePromiseResolver != null){
            videoPauseResumePromiseResolver.onCompletion(new Output("Permission Denied", false, "permission_error", new HashMap<>()));
            videoPauseResumePromiseResolver = null;
        }
    }

    private void removeAndStreamInLocalStream(MediaStreamTrack newMediaStreamTrack) {
        if (localStream == null) {
            localStream = getHandler().getPeerFactory().createLocalMediaStream("localStream");
        }
        if (newMediaStreamTrack.kind() .equalsIgnoreCase( "video") ){
            if (localStream.videoTracks.size() > 0) {
                try{
                localStream.removeTrack(localStream.videoTracks.get(0));
                }
                catch (Exception e){

                }
            }
            localStream.addTrack((VideoTrack) newMediaStreamTrack);

        }
        if (newMediaStreamTrack.kind() .equalsIgnoreCase( "audio") ){
            if (localStream.audioTracks.size() > 0) {
                try{
                localStream.removeTrack(localStream.audioTracks.get(0));
                }
                catch (Exception e){

                }
            }
            localStream.addTrack((AudioTrack) newMediaStreamTrack);
        }
    }


    private void updateLocalTrackInAllTracks(MediaStreamTrack newTrack) {

        Track oldTrack = Track.getLocalTrack(allTracks, newTrack.kind(), newTrack.kind());
        oldTrack.updateTrackData(newTrack.id(), newTrack);

        if (newTrack.kind() .equalsIgnoreCase( "audio")) {

            if(isStartAndSetupWithServerCalled){
                getHandler().updateAudioStream(localStream);
            }
//
        }

        if (newTrack.kind() .equalsIgnoreCase( "video") ){
            if(isStartAndSetupWithServerCalled){
                getHandler().updateVideoStream(localStream);
            }



        }

        emitMessageToSource("refershTrack", oldTrack);

    }


    private void onStreamGot(MediaStream stream,boolean isForAudio,boolean isForVideo) {
        if (stream == null || (isForAudio == false && isForVideo == false)) {
            isFetchVideoInProgress = false;
            isFetchAudioInProgress = false;
            return;
        }
        if (localStream == null) {
            localStream = getHandler().getPeerFactory().createLocalMediaStream("localStream");

        }

        if (isForAudio && stream.audioTracks.size() > 0) {
            removeAndStreamInLocalStream(stream.audioTracks.get(0));
            updateLocalTrackInAllTracks(stream.audioTracks.get(0));
            getSelfParticipant().isAudioEnable = true;

            try {
                JSONObject data = new JSONObject();
                data.put("userId" ,meetingStartRequest.userId);
                data.put("type" ,"audio");
                data.put("status" ,"resume");

                JSONObject audioPause = new JSONObject();
                audioPause.put("message" ,data);
                audioPause.put("type" ,"audioVideoPauseResume");
                sendSocketMessage("audioVideoPauseResume", audioPause);
            }
            catch (Exception e){
                logEvent(e.toString(),true);

            }

            if(muteUmutePromiseResolver != null){
                muteUmutePromiseResolver.onCompletion(new Output("Success", true, "Success", new HashMap<>()));
                muteUmutePromiseResolver = null;
            }

        }
        if (isForVideo && stream.videoTracks.size() > 0) {
            removeAndStreamInLocalStream(stream.videoTracks.get(0));
            updateLocalTrackInAllTracks(stream.videoTracks.get(0));
            getSelfParticipant().isVideoEnable = true;

            try {
                JSONObject data = new JSONObject();
                data.put("userId" ,meetingStartRequest.userId);
                data.put("type" ,"video");
                data.put("status" ,"resume");

                JSONObject audioPause = new JSONObject();
                audioPause.put("message" ,data);
                audioPause.put("type" ,"audioVideoPauseResume");
                sendSocketMessage("audioVideoPauseResume", audioPause);
            }
            catch (Exception e){
                logEvent(e.toString(),true);

            }

            if( videoPauseResumePromiseResolver != null){
                videoPauseResumePromiseResolver.onCompletion(new Output("Success", true, "Success", new HashMap<>()));
                videoPauseResumePromiseResolver = null;
            }
        }

        isFetchVideoInProgress = false;
        isFetchAudioInProgress = false;
        try {
            JSONObject permissionApproved = new JSONObject();
            permissionApproved.put("isForAudio" ,isForAudio);
            permissionApproved.put("isForVideo" ,isForVideo);
            emitMessageToSource("'permissionApproved'", permissionApproved);
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }


    private void sendLocalStreamRequest(boolean forcefully) {
        emitMessageToSource("onTrack", Track.getLocalTrack(allTracks, "video", "video"));
        emitMessageToSource("onTrack", Track.getLocalTrack(allTracks, "audio", "audio"));
    }



    private void connect(boolean isForceFully) {
        if(isEnded == true){
            return;
        }
        if (wss == null  || (isForceFully && isWebScoketConnected() == false )) {
            logEvent("reconnect ----" , false);


            configSelfParticipant();
            createEmptySelfStreamIfNotPresent();

            String url = wssUrl(meetingStartRequest.appId);
            logEvent(url , false);

                try {
                    wss = new WebSocketClient(new URI(url + connection)) {
                        @Override
                        public void onOpen(ServerHandshake handshakedata) {
                            logEvent("connected ----" , false);
                            isSetUpDone = false;
                            socketCheckTimeout = null;
                            onSocketConnected();
                        }

                        @Override
                        public void onMessage(String message) {
                            MeetingHandler.this.onMessage(message);
                        }

                        @Override
                        public void onClose(int code, String reason, boolean remote) {
                            logEvent("WebSocket is closed now.",false);
                            logEvent(reason + " " + code,false);
                            if(socketCheckTimeout == null){
                                tryToReconectSocket();
                                logEvent("socketCheckTimeout.",false);
                            }

                        }

                        @Override
                        public void onError(Exception ex) {
                            logEvent("WebSocket error observed:",true);
                            logEvent(ex.toString(),true);
                            emitMessageToSource("onSocketError", ex);

                        }
                    };
                    wss.connect();
                }
                catch (Exception e){
                    logEvent(e.toString(),true);
                }
        }
        else if(isForceFully == false && isWebScoketConnected()){
            emitMessageToSource("onConnected", new HashMap<>());

        }

    }


    private void tryToReconectSocket(){
        if(isWebScoketConnected() == false && isEnded == false){
            wss = null;
            connection = "reconnect";
            connect(true);
            socketCheckTimeout = new Timer();
            socketCheckTimeout.schedule(new TimerTask() {
                @Override
                public void run() {
                    MeetingHandler.this.tryToReconectSocket();
                }
            }, 1500);
        }
        else{
            socketCheckTimeout = null;
        }
    }


    private void configSelfParticipant() {
        Participant selfParticipant = getSelfParticipant();
        selfParticipant.isWhiteboardBlockedByAdmin = !meetingStartRequest.defaultWhiteboardEditEnable;

    }


    private void onMessage(String message) {
        try {


            JSONObject messagejson = new JSONObject(message);

            if (messagejson.has("type")){
                String type = messagejson.getString("type");
                if (type .equalsIgnoreCase( "ping") == false ) {
                    logEvent(message , false);
                }
                JSONObject data = messagejson.getJSONObject("data");

                if (type .equalsIgnoreCase( "setupDone") ){
                    onSetupDone(data);

                } else if (type .equalsIgnoreCase( "chat") ){
                    onChatMessageReceived(data);
                } else if (type .equalsIgnoreCase( "offer") ){
                    ((WebrtcHandler)getHandler()).onOffer(data, localStream);
                } else if (type .equalsIgnoreCase( "iceRestartPing")) {
                    ((WebrtcHandler)getHandler()).onIceRestartPing(data, localStream);
                } else if (type .equalsIgnoreCase( "iceRestartPong") ){
                    ((WebrtcHandler)getHandler()).onIceRestartPong(data);
                } else if (type .equalsIgnoreCase( "whiteboard") ){
                    onWhiteboardUrlRecieved(data);
                } else if (type .equalsIgnoreCase( "answer") ){
                    ((WebrtcHandler)getHandler()).onAnswer(data, localStream);
                } else if (type .equalsIgnoreCase( "audioVideoStatusUpdated") ){
                    onAudioVideoStatusUpdated(data);
                } else if (type .equalsIgnoreCase( "videoBlock") ){
                    onVideoBlockCalled(data);
                } else if (type .equalsIgnoreCase( "videoUnblock") ){
                    onVideoUnblockCalled(data);
                } else if (type .equalsIgnoreCase( "audioBlock") ){
                    onAudioBlockCalled(data);
                } else if (type .equalsIgnoreCase( "audioUnblock") ){
                    onAudioUnblockCalled(data);
                } else if (type .equalsIgnoreCase( "messageBlock") ){
                    onMessageBlockCalled(data);
                } else if (type .equalsIgnoreCase(  "messageUnblock") ){
                    onMessageUnblockCalled(data);
                } else if (type .equalsIgnoreCase(  "whiteboardBlock") ){
                    onWhiteboardBlockCalled(data);
                } else if (type .equalsIgnoreCase(  "whiteboardUnblock") ){
                    onWhiteboardUnblockCalled(data);
                } else if (type .equalsIgnoreCase(  "startMeetingCalled") ){
                    onStartMeetingCalled(data);
                } else if (type .equalsIgnoreCase(  "switchWhiteboard") ){
                    emitMessageToSource("switchToWhiteboard",new HashMap<>());
                } else if (type .equalsIgnoreCase(  "onPeerScreenShareEnded") ){
                    onPeerScreenShareEnded(data);
                } else if (type .equalsIgnoreCase(  "participants") ){
                    onParticipants(data);
                } else if (type .equalsIgnoreCase( "onStreamStatusUpdated") ){
                    onStreamStatusUpdated(data);
                } else if (type .equalsIgnoreCase( "iceCandidate")) {
                    ((WebrtcHandler)getHandler()).onIceCandidateRecieved(data);
                } else if (type .equalsIgnoreCase( "ping") ){
                    sendSocketMessage("pong", data);
                } else if (type .equalsIgnoreCase( "handshake") ){
                    if (meetingStartRequest.shouldUseSFU()) {
                        return;
                    }
                    if (isStartupSetupCalled) {
                        getHandler().handleHandshake(data, localStream);
                    } else {
                        handShakeDatas.add(data);
                    }
                } else if (type .equalsIgnoreCase( "newJoinee") ){
                    if (isStartupSetupCalled) {
                        onNewPartnerAdded(data, true);
                    } else {
                        newJoniees.add(data);
                    }

                }
                else if (type .equalsIgnoreCase("rejoined") ){
                    onRejoined(data);

                }
                else if (type .equalsIgnoreCase( "userLeft") ){
                    onPartnerLeft(data);
                } else if (type .equalsIgnoreCase(  "onRouterRtpCapabilities") ){
                    onRouterRtpCapabilities(data);
                } else if (type .equalsIgnoreCase(  "onServerConsumer") ){
                    onServerConsumer(data);
                } else if (type .equalsIgnoreCase(  "onNewProducer") ){
                    onNewProducer(data);
                } else if (type .equalsIgnoreCase(  "onSendTransport") ){
                    onSendTransport(data);
                } else if (type .equalsIgnoreCase(  "transportConnectDone") ){
                    onTransportConnectDone(data);
                } else if (type .equalsIgnoreCase(  "produceSyncDone") ){
                    onProduceSyncDone(data);
                } else if (type .equalsIgnoreCase(  "onConsumeTransport")) {
                    onConsumeTransportCreated(data);
                } else if (type .equalsIgnoreCase(  "onSpeakerChanged") ){
                    onSpeakerChanged(data);
                } else if (type .equalsIgnoreCase(  "onRestartIceCandidate") ){
                    onRestartIceCandidate(data);
                } else if (type .equalsIgnoreCase(  "onTrackEnded") ){
                    onTrackEnded(data);
                } else if (type .equalsIgnoreCase(  "meetingStartTime") ){
                    onMeetingStartTime(data);
                } else if (type .equalsIgnoreCase( "trackReplaced")) {
                    onTrackReplaced(data);
                }

            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void onTrackReplaced(JSONObject data){
        try {
            logEvent("onTrackReplaced", false);
            logEvent(data.toString(), false);

            String senderUserId = data.getString("userId");
            String streamType = data.getString("streamType");
            String videoType = streamType;
            if (streamType.equalsIgnoreCase("SS")) {
                videoType = "video";
            }

            Track track = Track.getTrackOfUserId(allTracks, videoType, senderUserId, streamType);
            if (track != null) {
                emitMessageToSource("refershTrack", track);
            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }

    }

    private void socketSubscribeToTopic() {
        try {
            if (isWebScoketConnected() == false) {
                return;
            }
            JSONObject config = new JSONObject();
            config.put("participant" , getSelfParticipant().toJsonObject());
            config.put("appId" , meetingStartRequest.appId);
            config.put("apiData" , meetingStartRequest.apiData);
            config.put("sfuRequired" , meetingStartRequest.shouldUseSFU());

            sendSocketMessage("config", config);


            JSONObject hostNotificationRoom = new JSONObject();
            hostNotificationRoom.put("roomId" , meetingStartRequest.userId);
            hostNotificationRoom.put("id" , meetingStartRequest.userId);

            sendSocketMessage("joinRoom", hostNotificationRoom);

            JSONObject roomForAllClient = new JSONObject();
            roomForAllClient.put("roomId" , meetingStartRequest.roomId);
            roomForAllClient.put("id" , meetingStartRequest.userId);

            sendSocketMessage("joinRoom", roomForAllClient);

            setUpTry = 0;
            askIfSetupDone();

        }
        catch (Exception e){
            logEvent(e.toString(),true);
        }
    }


    private Participant getSelfParticipant() {
        Participant participant = Participant.participantByUserId(allParticipant, meetingStartRequest.userId);
        if (participant == null) {
            participant = new Participant(meetingStartRequest.userId, meetingStartRequest.roomId, meetingStartRequest.isAdmin, meetingStartRequest.userData);
            participant.isWhiteboardBlockedByAdmin = !meetingStartRequest.defaultWhiteboardEditEnable;
            allParticipant.add(participant);
        }
        return participant;
    }

    private void onRejoined(JSONObject data){
        try {
            JSONObject newParticipant = data.getJSONObject("message").getJSONObject("participant");

            if (isStartupSetupCalled) {
                if (Participant.isParticipantPresent(allParticipant, newParticipant.getString("userId")) ==  false) {
                    onNewPartnerAdded(data, true);
                } else {
                    if (meetingStartRequest.shouldUseSFU() ==  false) {
                        Peer peer = ((WebrtcHandler)getHandler()).peerByUserId(newParticipant.getString("userId"));
                        if (peer ==  null) {
                            JSONObject handShake = new JSONObject();
                            handShake.put("message" , "Welcome");
                            handShake.put("type" , "handshake");
                            handShake.put("to" ,newParticipant.getString("userId"));
                            sendMessage(handShake);
                        }
                    }
                }
            } else {
                for (JSONObject newJoniee : newJoniees) {
                    JSONObject oldJoinee = newJoniee.getJSONObject("message").getJSONObject("participant");
                    if (oldJoinee.getString("userId").equalsIgnoreCase( newParticipant.getString("userId"))) {
                        return;
                    }
                }
                newJoniees.add(data);
            }
        }
        catch (Exception e){

        }
    }


    private void onNewPartnerAdded(JSONObject data, boolean shouldInfrom) {
        try {
            if (meetingStartRequest == null) {
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JSONObject participant = data.getJSONObject("message").getJSONObject("participant");
            Participant newParticipant = objectMapper.readValue(participant.toString(),Participant.class);
            if (Participant.addParticipantIfNotExist(allParticipant, newParticipant)) {
                if (shouldInfrom) {
                    emitMessageToSource("onUserJoined", newParticipant);

                }
                logEvent("onNewPartnerAdded", false);
                handleOnParticipantJoined(newParticipant);

                if (meetingStartRequest.shouldUseSFU()) {
                    ((SFUHandler)getHandler()).onParticipantUpdated();
                } else {


                    JSONObject participantJSon = new JSONObject();
                    participantJSon.put("participant" , participant);

                    JSONObject messageJson = new JSONObject();
                    messageJson.put("message" , participantJSon);
                    ((WebrtcHandler)getHandler()).disconnectPeer(messageJson);


                    JSONObject handShake = new JSONObject();
                    handShake.put("message" , "Welcome");
                    handShake.put("type" , "handshake");
                    handShake.put("to" ,newParticipant.userId);
                    Participant selfParticipant = getSelfParticipant();
                    String selfParticipantString = objectMapper.writeValueAsString(selfParticipant);

                    handShake.put("participant" , new JSONObject(selfParticipantString));
                    sendMessage(handShake);
                }
            }
        }
        catch (Exception e){

            logEvent(e.toString(),true);
        }
    }

    private void handleOnParticipantJoined(Participant participant) {

        if (participant.isStartMeetingCalled) {
            Track track = Track.getTrackOfUserId(allTracks, "video", participant.userId, "video");
            if (track == null) {

                track = new Track(participant.userId , null, false,"video");
                track.kind = "video";
                track.videoType = "video";
                track.participant = participant;
                allTracks.add(track);
                emitMessageToSource("onTrack", track);

            }
            Track trackAudio = Track.getTrackOfUserId(allTracks, "audio", participant.userId, "audio");
            if (trackAudio == null) {

                trackAudio = new Track(participant.userId , null, false,"audio");
                trackAudio.kind = "audio";
                trackAudio.videoType = "audio";
                trackAudio.participant = participant;
                allTracks.add(trackAudio);
                emitMessageToSource("onTrack", trackAudio);

            }
        }
    }

    private void getAllPausedTracks() {
        for (Participant participant : allParticipant) {
            handleOnParticipantJoined(participant);
        }
    }


    private void onPartnerLeft(JSONObject data) {//Sachin
        // console.log(data)
        try {
            if (meetingStartRequest == null) {
                return;
            }
            String participantId = data.getJSONObject("message").getJSONObject("participant").getString("userId");
            Participant participantToBeRemove = Participant.participantByUserId(allParticipant,participantId);
            Participant.removeParticipantWithId(allParticipant, participantId);
            int index = 0;
            while (index < allTracks.size()) {
                Track track = allTracks.get(index);
                if (track.participant.userId .equalsIgnoreCase( participantId)) {
                    allTracks.remove(index);
                    emitMessageToSource("onTrackEnded", track);
                } else {
                    index++;
                }
            }
            if (meetingStartRequest.shouldUseSFU()) {
                ((SFUHandler)getHandler()).onUseLeft(participantToBeRemove);
                ((SFUHandler)getHandler()).onParticipantUpdated();
            } else {
                ((WebrtcHandler)getHandler()).disconnectPeer(data);
            }

            emitMessageToSource("onUserLeft",participantToBeRemove);
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }


    private void getAllParticipants() {
        try {
            JSONObject getParticipant = new JSONObject();
            getParticipant.put("type" , "getParticipant");
            getParticipant.put("message" , "");
            sendSocketMessage("getParticipant", getParticipant);
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }


    private void onSetupDone(JSONObject data) {
        if (meetingStartRequest == null) {
            return;
        }
        try {
            if (isSetUpDone == false) {
                logEvent(data.toString(), false);

                isSetUpDone = true;
                if (isStartAndSetupWithServerCalled) {
                    boolean isReconnect = data.getJSONObject("message").getBoolean("isReconnection");
                    if (isReconnect) {
                        logEvent("onReconnect Meeting", false);
                        getHandler().onReconnect(localStream);
                        emitMessageToSource("connectionBack", new HashMap<>());
                    } else {
                        startAndSetupWithServer();
                    }
                    return;
                } else {
                    Participant participant = getSelfParticipant();
                    participant.isWhiteboardBlockedByAdmin = !meetingStartRequest.defaultWhiteboardEditEnable;
                    emitMessageToSource("onConnected", new HashMap<>());

                }
            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void startAndSetupWithServer() {
        if (meetingStartRequest == null) {
            return;
        }
        if (meetingStartRequest.shouldUseSFU()) {
            ((SFUHandler)getHandler()).init(localStream);
        }
        else {
            for (JSONObject handShakeData : handShakeDatas) {
                ((WebrtcHandler)getHandler()).handleHandshake(handShakeData, localStream);
            }
            handShakeDatas = new ArrayList<>();
        }
        for (JSONObject newJoniee : newJoniees) {
            onNewPartnerAdded(newJoniee, false);
        }
        newJoniees = new ArrayList<>();
        isStartupSetupCalled = true;
    }

    private void onMeetingStartTime(JSONObject data) {
        try {
            emitMessageToSource("meetingStartTime", data.getLong("time"));
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }


    private void onParticipants( JSONObject data) {
        if (meetingStartRequest == null) {
            return;
        }
        try {
            List<Participant> copyOfParticipant = new ArrayList<Participant>(allParticipant);

            for (Participant participant : copyOfParticipant) {

                boolean isParticipantStillPrensent = false;
                HashMap<String,Object> result =
                new ObjectMapper().readValue(data.getJSONObject("message").toString(), HashMap.class);
                for (Map.Entry<String, Object> peerDataBaseEntry : result.entrySet()) {
                    if ((participant.userId + " ") .equalsIgnoreCase (peerDataBaseEntry.getKey() + " ")) {
                        isParticipantStillPrensent = true;
                        break;
                    }
                }


                if (isParticipantStillPrensent == false) {

                    JSONObject participantData = new JSONObject();
                    participantData.put("participant" , participant);

                    JSONObject partnerLeft = new JSONObject();
                    partnerLeft.put("message" , participantData);

                    onPartnerLeft(partnerLeft);
                }
            }

            allParticipant = new ArrayList<>();
            HashMap<String,Object> result =
                    new ObjectMapper().readValue(data.getJSONObject("message").toString(), HashMap.class);
            ObjectMapper objectMapper = new ObjectMapper();
            for (Map.Entry<String, Object> participantDataEntry : result.entrySet()) {
                String participantJson = new JSONObject((Map) participantDataEntry.getValue()).toString();
                Participant newParticipant = objectMapper.readValue(participantJson,Participant.class);
                allParticipant.add(newParticipant);

            }
            if (meetingStartRequest.shouldUseSFU()) {
                ((SFUHandler)getHandler()).onParticipantUpdated();
            }

            emitMessageToSource("participants", allParticipant);


            for (Track track : allTracks) {
                for (Participant participant : allParticipant) {
                    if (track.participant.userId.equalsIgnoreCase( participant.userId) ){
                        track.participant = participant;
                        break;
                    }
                }
            }

        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }

    }


    private  void  onStreamStatusUpdated(JSONObject data) {//Sachin May be not used
//        JSONObject _streamData = data["message"];
//        String streamId = _streamData["producerId"];
//        boolean isPresent = _streamData["isStremPresent"];
//        Track track = Track.getTrackById(allTracks, streamId);
//        if (track != null && track.isStremPresent != isPresent) {
//            track.isStremPresent = isPresent;
//            var shouldInfrom = true;
//            if (isPresent === false) {
//                if (track.kind == "audio" && (track.participant.isAudioEnable === false || track.participant.isAudioBlockedByAdmin === true)) {
//                    shouldInfrom = false;
//                }
//                else if (track.kind === "video" && (track.participant.isVideoEnable === false || track.participant.isVideoBlockedByAdmin === true)) {
//                    shouldInfrom = false;
//                }
//            }
//
//            if (shouldInfrom) {
//                emitMessageToSource("streamStatusUpdated", track);
//            }
//
//        }
    }


    private void onWhiteboardUrlRecieved(JSONObject data) {
        try {
            String whiteboardUrl = data.getString("message");
            emitMessageToSource("onWhiteboardUrl", whiteboardUrl);
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void onAudioVideoStatusUpdated(JSONObject _data) {
        try {
            JSONObject givenParticipant = _data.getJSONObject("message").getJSONObject("participant");

            Participant participant = Participant.participantByUserId(allParticipant, givenParticipant.getString("userId"));
            participant.isVideoEnable = givenParticipant.getBoolean("isVideoEnable");
            participant.isAudioEnable = givenParticipant.getBoolean("isAudioEnable");
            emitMessageToSource("audioVideoStatusUpdated", participant);
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }



    private void onChatMessageReceived(JSONObject data) {
        try {
            String to = data.getString("to");
            if (to .equalsIgnoreCase( meetingStartRequest.roomId)) {
                to = "all";
            }

            MessagePayload messagePayload = new MessagePayload(data.getString("message") , to);
            messagePayload.sender = data.getString("userId");
            emitMessageToSource("onNewChatMessageReceived", messagePayload);
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void askIfSetupDone() {
        if (isSetUpDone || setUpTry > 10) {
            return;
        }
        if(meetingStartRequest == null){
            return;
        }
        try{
            JSONObject checkIfSetupDone = new JSONObject();
            checkIfSetupDone.put("to" , meetingStartRequest.userId);
            sendSocketMessage("setupDone", checkIfSetupDone);
            logEvent("vani setupDone asked", false);
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isSetUpDone || setUpTry > 10) {
                        return;
                    }
                    askIfSetupDone();
                    setUpTry = setUpTry + 1;
                }
            }, 200);
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void onSocketConnected() {
        socketSubscribeToTopic();
    }

    private void sendMessage(JSONObject msgToSend) {
        sendSocketMessage("message", msgToSend);
    }

    private void sendSFUMessage(JSONObject msgToSend) {
        sendSocketMessage("sfuMessage", msgToSend);
    }

    private void sendSocketMessage(String type, JSONObject payload) {
        try {
            if (isWebScoketConnected()) {
                if(type.equalsIgnoreCase("pong") == false){
                    logEvent(payload.toString(),false);
                }
                JSONObject updatedPayload  =  new JSONObject();
                updatedPayload.put("type" , type);
                updatedPayload.put("data" , payload);
                wss.send(updatedPayload.toString());
            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void emitMessageToSource(String type, Object msgData) {
        if(events.containsKey(type)){
            List<EventEmitterHandler> eventEmitterHandlers = events.get(type);

            for(EventEmitterHandler eventEmitterHandler : eventEmitterHandlers){

                Handler mainHandler = new Handler(context.getMainLooper());

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            eventEmitterHandler.onEvent(type, msgData);
                        }
                        catch (Exception e){

                        }

                    } // This is your code
                };
                mainHandler.post(myRunnable);
            }
        }
    }

    private void onRouterRtpCapabilities(JSONObject data) {
        if(meetingStartRequest.shouldUseSFU()){
            ((SFUHandler)getHandler()).onRouterRtpCapabilities(data);
        }
    }
    private void onSendTransport(JSONObject data) {
        if(meetingStartRequest.shouldUseSFU()){
            ((SFUHandler)getHandler()).onSendTransport(data);
        }
    }

    private void  onServerConsumer(JSONObject data) {
        if(meetingStartRequest.shouldUseSFU()){
            ((SFUHandler)getHandler()).onServerConsumer(data);
        }
    }
    private void   onProduceSyncDone(JSONObject data) {
        if(meetingStartRequest.shouldUseSFU()){
            ((SFUHandler)getHandler()).onProduceSyncDone(data);
        }
    }

    private void  onConsumeTransportCreated(JSONObject data) {
        if(meetingStartRequest.shouldUseSFU()){
            ((SFUHandler)getHandler()).onConsumeTransportCreated(data);
        }
    }

    private void onSpeakerChanged(JSONObject data) {
        try {
            String speakerId = data.getJSONObject("message").getString("speakerUserId");
            Participant participant = Participant.participantByUserId(allParticipant, speakerId);
            JSONObject output = new JSONObject();
            output.put("speakerUserId" , speakerId);
            output.put("participant" , participant);
            emitMessageToSource("onSpeakerChanged", output);
            if(meetingStartRequest.shouldUseSFU()){
                ((SFUHandler)getHandler()).onSpeakerChanged(data);
            }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void  onRestartIceCandidate(JSONObject data) {
        if(meetingStartRequest.shouldUseSFU()){
            ((SFUHandler)getHandler()).onRestartIceCandidate(data);
        }
    }

    private void onTrackEnded(JSONObject data) {
        try {

            if (meetingStartRequest == null) {
                return;
            }
            if (data.has("message") && data.getJSONObject("message").has("producerId")) {
                Track track = Track.trackByProducerId(allTracks,  data.getJSONObject("message").getString("producerId"));
                if (track != null) {
                    emitMessageToSource("onTrackEnded", track);
                }

                if (meetingStartRequest.shouldUseSFU()) {
                    ((SFUHandler)getHandler()).onTrackEnded(track);
                }

                Track.checkAndRemoveTrack(allTracks, track.trackId);
            }
        }
        catch (Exception e){

        }
    }
    private void  onTransportConnectDone(JSONObject data) {
        if(meetingStartRequest.shouldUseSFU()){
            ((SFUHandler)getHandler()).onTransportConnectDone(data);
        }
    }

    private void  onNewProducer(JSONObject data) {
        if(meetingStartRequest.shouldUseSFU()){
            ((SFUHandler)getHandler()).onNewProducer(data);
        }
    }
    private void  onScreensharingStoped() {
        try {
            if (meetingStartRequest == null) {
                return;
            }

            getHandler().onScreenShareStopped();

            JSONObject screenShareEnded = new JSONObject();
            screenShareEnded.put("type" , "onPeerScreenShareEnded");
            screenShareEnded.put("to" , meetingStartRequest.roomId);
            screenShareEnded.put("userId" , meetingStartRequest.userId);
            sendMessage(screenShareEnded);
            screenshareStream = null;

            Track trackObj = Track.getLocalTrack(allTracks, "video", "SS");
            if (trackObj !=  null) {
                Track.checkAndRemoveTrack(allTracks, trackObj.trackId);
                emitMessageToSource("onTrackEnded", trackObj);
            }
            emitMessageToSource("screenShareEnded", trackObj);
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }

    }

    private void onPeerScreenShareEnded(JSONObject data) {
        if (meetingStartRequest == null) {
            return;
        }
        if (!meetingStartRequest.shouldUseSFU()) {
            ((WebrtcHandler)getHandler()).onPeerScreenShareEnded(data);
        }
    }

    private void onVideoBlockCalled(JSONObject data) {
        try {
            String userId = data.getJSONObject("message").getString("user");
            Participant participant = Participant.participantByUserId(allParticipant, userId);
            if (participant != null) {
                participant.isVideoBlockedByAdmin = true;
                participant.isVideoEnable = false;
            }
            if (meetingStartRequest.userId .equalsIgnoreCase(  userId) ){

                String adminId = data.getJSONObject("message").getString("admin");
                Participant admin = Participant.participantByUserId(allParticipant, adminId);
                emitMessageToSource("videoBlocked", admin);

            }
            emitMessageToSource("audioVideoStatusUpdated", participant);
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void  onVideoUnblockCalled(JSONObject data) {
        try {
            String userId = data.getJSONObject("message").getString("user");
            Participant participant = Participant.participantByUserId(allParticipant, userId);
            if (participant !=  null) {
                participant.isVideoBlockedByAdmin = false;

            }
            if (meetingStartRequest.userId .equalsIgnoreCase(  userId) ){
                String adminId = data.getJSONObject("message").getString("admin");
                Participant admin = Participant.participantByUserId(allParticipant, adminId);
                emitMessageToSource("videoUnblocked", admin);

            }

        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void   onAudioBlockCalled(JSONObject data) {
        try {

            String userId = data.getJSONObject("message").getString("user");
            Participant participant = Participant.participantByUserId(allParticipant, userId);
        if (participant != null) {
            participant.isAudioBlockedByAdmin = true;
            participant.isAudioEnable = false;

        }
        if (meetingStartRequest.userId .equalsIgnoreCase( userId)) {
            String adminId = data.getJSONObject("message").getString("admin");
            Participant admin = Participant.participantByUserId(allParticipant, adminId);
            emitMessageToSource("audioBlocked", admin);

        }
        emitMessageToSource("audioVideoStatusUpdated", participant);

        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void    onAudioUnblockCalled(JSONObject data) {
        try {

            String userId = data.getJSONObject("message").getString("user");
            Participant participant = Participant.participantByUserId(allParticipant, userId);
        if (participant != null) {
            participant.isAudioBlockedByAdmin = false;

        }
        if (meetingStartRequest.userId .equalsIgnoreCase( userId) ){
            String adminId = data.getJSONObject("message").getString("admin");
            Participant admin = Participant.participantByUserId(allParticipant, adminId);
            emitMessageToSource("audioUnblocked", admin);

        }

        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }


    private void     onMessageBlockCalled(JSONObject data) {
        try {

            String userId = data.getJSONObject("message").getString("user");
            Participant participant = Participant.participantByUserId(allParticipant, userId);
        if (participant != null) {
            participant.isMessageBlockedByAdmin = true;
        }
        if (meetingStartRequest.userId .equalsIgnoreCase( userId) ){
            String adminId = data.getJSONObject("message").getString("admin");
            Participant admin = Participant.participantByUserId(allParticipant, adminId);
            emitMessageToSource("messageBlocked", admin);

        }

        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void      onMessageUnblockCalled(JSONObject data) {
        try{
        String userId = data.getJSONObject("message").getString("user");
            Participant participant = Participant.participantByUserId(allParticipant, userId);
        if (participant != null) {
            participant.isMessageBlockedByAdmin = false;

        }
        if (meetingStartRequest.userId .equalsIgnoreCase( userId)) {
            String adminId = data.getJSONObject("message").getString("admin");
            Participant admin = Participant.participantByUserId(allParticipant, adminId);
            emitMessageToSource("messageUnblocked", admin);

        }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void       onWhiteboardBlockCalled(JSONObject data) {
        try{

            String userId = data.getJSONObject("message").getString("user");
            Participant participant = Participant.participantByUserId(allParticipant, userId);
        if (participant != null) {
            participant.isWhiteboardBlockedByAdmin = true;
        }
        if (meetingStartRequest.userId .equalsIgnoreCase( userId) ){
            String adminId = data.getJSONObject("message").getString("admin");
            Participant admin = Participant.participantByUserId(allParticipant, adminId);
            emitMessageToSource("whiteboardBlocked", admin);

        }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void       onStartMeetingCalled(JSONObject data) {
        try{

            String userId = data.getJSONObject("message").getString("user");
            Participant participant = Participant.participantByUserId(allParticipant, userId);
        if( participant != null){

            this.logEvent("onStartMeetingCalled",false);
            participant.isStartMeetingCalled = true;
            handleOnParticipantJoined(participant);

        }
        }
        catch (Exception e){
            logEvent(e.toString(),true);

        }
    }

    private void     onWhiteboardUnblockCalled(JSONObject data) {
        try{

            String userId = data.getJSONObject("message").getString("user");
            Participant participant = Participant.participantByUserId(allParticipant, userId);
        if (participant != null) {
            participant.isWhiteboardBlockedByAdmin = false;

        }
        if (meetingStartRequest.userId .equalsIgnoreCase( userId)) {
            String adminId = data.getJSONObject("message").getString("admin");
            Participant admin = Participant.participantByUserId(allParticipant, adminId);
            emitMessageToSource("whiteboardUnblocked", admin);

        }
    }
        catch (Exception e){
            logEvent(e.toString(),true);

    }
    }

    private boolean isWebScoketConnected() {
        if (wss != null && wss.isOpen()) {
            return true;
        }
        return false;
    }



    private void checkIfInternetReachable(int count) {
        if(meetingStartRequest == null){
            onEndAndDestory(null);
            return;
        }
        if (count == 2) {
            infromSourceOnNetworkIssue("notReachable");
        }
        isReachable = false;
        logEvent("checkIfInternetReachable",true);
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = Config.urlToCheckInternetPresent(meetingStartRequest.appId);

        final int countFinal = count;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        onApiResponded(countFinal);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {


                logEvent(error.getLocalizedMessage(),true);
                if (countFinal > 35) {
                    infromSourceOnNetworkIssue("reconectionTimeout");
                    onEndAndDestory(null);
                    return;
                }
                internetReachbilityTimeout = new Timer();
                internetReachbilityTimeout.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        checkIfInternetReachable((countFinal + 1));
                    }
                }, 1000);
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    private void onApiResponded(int count) {

        logEvent("onApiResponded",true);


        if (isReachable == false) {
            isReachable = true;
            internetReachbilityTimeout = null;
            if(meetingStartRequest.shouldUseSFU() == false && count < 2 && isWebScoketConnected()){
                ((WebrtcHandler)getHandler()).reconnectedWithoutPing(localStream);
                return;
            }
            if (wss != null) {
                infromSourceOnNetworkIssue("reconecting");
                wss.close(3005);
                wss = null;
                connection = "reconnect";
                connect(true);
            }
        }
    }
    private void infromSourceOnNetworkIssue(String message) {
        emitMessageToSource(message, new HashMap<>());
    }
    private void onEndAndDestory(DestoryCallBack destoryCallBack) {
        isEnded = true;

        new Thread(() -> {
            {
                logEvent("onEndAndDestory",false);

                try{
                    emitMessageToSource("onEnded",null);
                }
                catch (Exception e){

                }

                events = new HashMap<>();
                {

                    try {
                        if (meetingStartRequest != null) {
                            JSONObject data = new JSONObject();
                            data.put("userId" , meetingStartRequest.userId);
                            sendSocketMessage("selfLeft", data);
                        }
                    }
                    catch (Exception e){
                        logEvent(e.toString(),true);

                    }


                    EglUtils.cleanUp();

                    try {
                        getHandler().destory();
                    }

                    catch (Exception e) {
                        logEvent(e.toString(),true);

                    }
                    if (wss != null) {
                        wss.close();
                        wss = null;
                    }

                    for(Track track : allTracks){
                        if(track.track != null){
                            try{
                                track.track.dispose();
                            }
                            catch (Exception e){

                            }
                        }
                    }


                    if(screenshareStream != null) {
                        logEvent("screenshareStream",false);
                        try {
                            screenshareStream.dispose();
                        }
                        catch (Exception e){

                        }
                    }
                    screenshareStream = null;


                    if(localStream != null) {
                        logEvent("localStream",false);
                        try {
                            localStream.dispose();
                        }
                        catch (Exception e){

                        }
                    }
                    localStream = null;
                    mMediaProjectionPermissionResultData = null;


                    handShakeDatas = new ArrayList<>();
                    peers = new ArrayList<>();
                    newJoniees = new ArrayList<>();
                    allParticipant = new ArrayList<>();
                    allTracks = new ArrayList<>();
                    meetingStartRequest = null;
                    localStream = null;
                    isSetUpDone = false;

                    isStartupSetupCalled = false;
                    peers = new ArrayList<>();
                    isPermissionGiven = false;
                    screenshareStream = null;
                    currentVideoDeviceId = null;
                    currentAudioDeviceId = null;
                    setUpTry = 0;
                    isStartAndSetupWithServerCalled = false;
                    connection = "new";
                    internetReachbilityTimeout = null;
                    muteUmutePromiseResolver = null;
                    videoPauseResumePromiseResolver = null;
                    socketCheckTimeout = null;
                    isFetchAudioInProgress = false;
                    isFetchVideoInProgress = false;
                    webrtcSFUHandller = null;
                    instance = null;
                    if(destoryCallBack != null){
                        Handler mainHandler = new Handler(Looper.getMainLooper());

                        Runnable myRunnable = new Runnable() {
                            @Override
                            public void run() {
                                destoryCallBack.onEnded();
                            }


                        };
                        mainHandler.post(myRunnable);
                    }
                }
            }

        }).start();
//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                logEvent("onEndAndDestory",false);
//
//                try{
//                    emitMessageToSource("onEnded",null);
//                }
//                catch (Exception e){
//
//                }
//
//                events = new HashMap<>();
//                {
//
//                    try {
//                        if (meetingStartRequest != null) {
//                            JSONObject data = new JSONObject();
//                            data.put("userId" , meetingStartRequest.userId);
//                            sendSocketMessage("selfLeft", data);
//                        }
//                    }
//                    catch (Exception e){
//                        logEvent(e.toString(),true);
//
//                    }
//
//
//                    EglUtils.cleanUp();
//
//                    try {
//                        getHandler().destory();
//                    }
//
//                    catch (Exception e) {
//                        logEvent(e.toString(),true);
//
//                    }
//                    if (wss != null) {
//                        wss.close();
//                        wss = null;
//                    }
//
//                    for(Track track : allTracks){
//                        if(track.track != null){
//                            try{
//                                track.track.dispose();
//                            }
//                            catch (Exception e){
//
//                            }
//                        }
//                    }
//                    if(localStream != null) {
//                        try {
//                            localStream.dispose();
//                        }
//                        catch (Exception e){
//
//                        }
//                    }
//                    localStream = null;
//
//                    if(screenshareStream != null) {
//                        try {
//                            screenshareStream.dispose();
//                        }
//                        catch (Exception e){
//
//                        }
//                    }
//                    screenshareStream = null;
//                    mMediaProjectionPermissionResultData = null;
//
//
//                    handShakeDatas = new ArrayList<>();
//                    peers = new ArrayList<>();
//                    newJoniees = new ArrayList<>();
//                    allParticipant = new ArrayList<>();
//                    allTracks = new ArrayList<>();
//                    meetingStartRequest = null;
//                    localStream = null;
//                    isSetUpDone = false;
//
//                    isStartupSetupCalled = false;
//                    peers = new ArrayList<>();
//                    isPermissionGiven = false;
//                    screenshareStream = null;
//                    currentVideoDeviceId = null;
//                    currentAudioDeviceId = null;
//                    setUpTry = 0;
//                    isStartAndSetupWithServerCalled = false;
//                    connection = "new";
//                    internetReachbilityTimeout = null;
//                    muteUmutePromiseResolver = null;
//                    videoPauseResumePromiseResolver = null;
//                    socketCheckTimeout = null;
//                    isFetchAudioInProgress = false;
//                    isFetchVideoInProgress = false;
//                    webrtcSFUHandller = null;
//                    instance = null;
//                    if(destoryCallBack != null){
//                        Handler mainHandler = new Handler(Looper.getMainLooper());
//
//                        Runnable myRunnable = new Runnable() {
//                            @Override
//                            public void run() {
//                                destoryCallBack.onEnded();
//                            }
//
//
//                        };
//                        mainHandler.post(myRunnable);
//                    }
//                }
//            }
//        });

    }
    private BaseWebrtcSFU getHandler(){
        if(webrtcSFUHandller == null){
            if(meetingStartRequest != null){
                    if(meetingStartRequest.shouldUseSFU()){
                        webrtcSFUHandller = new SFUHandler(context);
                    }
                    else{
                        webrtcSFUHandller = new WebrtcHandler(context);
                    }
                webrtcSFUHandller.setHandlerDelegate(MeetingHandler.this);
            }
        }
        return webrtcSFUHandller;
    }

    public void logEvent(String msg, boolean isError){
        if (isError) {
            Log.e("VaniMeeting", msg);
        }
        else{
            Log.d("VaniMeeting", msg);

        }

    }
    public void addEventListner(String event, EventEmitterHandler eventListener){
        addEventListner(event,eventListener,false);
    }

    public void addEventListner(String event, EventEmitterHandler eventListener, boolean shouldOverride){
        List<EventEmitterHandler> eventListeners = new ArrayList<>();
        if (shouldOverride == false && events.containsKey(event)){
            eventListeners = events.get(event);
        }
        eventListeners.add(eventListener);
        events.put(event ,eventListeners );
    }

    public void  removeEventListner(String event, EventEmitterHandler eventListener){
        List<EventEmitterHandler> eventListeners = null;
        if (events.containsKey(event)){
            eventListeners = events.get(event);
        }
        if(eventListeners != null){
            for(int index = 0 ; index < eventListeners.size() ; index ++){
                EventEmitterHandler eventEmitterHandler = eventListeners.get(index);
                if(eventEmitterHandler == eventListener ){
                    eventListeners.remove(index);
                }
            }
        }
    }
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == CAPTURE_PERMISSION_REQUEST_CODE){
            if (resultCode != Activity.RESULT_OK){
                try {
                    JSONObject permissionIssue = new JSONObject();
                    permissionIssue.put("error", "permissionError");
                    permissionIssue.put("isForAudio", false);
                    permissionIssue.put("isForVideo", false);
                    permissionIssue.put("isForSS", true);

                    emitMessageToSource("permissionError", permissionIssue);
                }
                catch (Exception e){

                }
            }
            else {
                mMediaProjectionPermissionResultData = data;
                VideoTrack screenShareVideoTrack = getHandler().createScreenShareVideoTrack(createScreenCapturer());
                if( screenShareVideoTrack != null ){
                    MediaStream ssMediaStream = getHandler().getPeerFactory().createLocalMediaStream("screenshare");
                    ssMediaStream.addTrack(screenShareVideoTrack);
                    onScreenShareStream(ssMediaStream);
                }

            }
        }
    }



  public   interface OutputCallBack{
        void onCompletion(Output output);
    }
    public   interface DestoryCallBack{
        void onEnded();
    }
}

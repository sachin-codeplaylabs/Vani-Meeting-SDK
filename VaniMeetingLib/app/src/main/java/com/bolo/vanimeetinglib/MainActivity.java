package com.bolo.vanimeetinglib;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.bolo.meetinglib.EventEmitterHandler;
import com.bolo.meetinglib.MeetingHandler;
import com.bolo.meetinglib.VaniMeetingVideoView;
import com.bolo.meetinglib.constant.EglUtils;
import com.bolo.meetinglib.inner.WebrtcHandler;
import com.bolo.meetinglib.model.MeetingStartRequest;
import com.bolo.meetinglib.model.MessagePayload;
import com.bolo.meetinglib.model.Output;
import com.bolo.meetinglib.model.Participant;
import com.bolo.meetinglib.model.Track;
import com.bolo.meetinglib.socketHandler.ProxyVideoSink;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;



import java.util.List;

public class MainActivity extends AppCompatActivity  {

    private LinearLayout localVideoView,remoteVideoView;
    boolean isMuted,isPaused;
//    private EglBase rootEglBase = EglBase.create();
    private Track track;
//    private SurfaceViewRenderer surfaceViewRenderer;

        private VaniMeetingVideoView  vaniMeetingVideoView;
    private ProxyVideoSink sink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);








        vaniMeetingVideoView =   findViewById(R.id.meetingVideoView);
        EditText roomEditText = findViewById(R.id.roomId);
        EditText userIdEditText = findViewById(R.id.userId);
        Switch videoRequired = findViewById(R.id.cameraRequired);
        Switch audioRequired = findViewById(R.id.audioRequired);
        localVideoView  = findViewById(R.id.localSteamSurfaceView);
        remoteVideoView  = findViewById(R.id.remoteSteamSurfaceView);






        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (roomEditText.getText().toString().length() > 0 && userIdEditText.getText().toString() .length() > 0) {
                    String roomId = roomEditText.getText().toString();
                    String userId = userIdEditText.getText().toString();


                        MeetingStartRequest meetingStartRequest= MeetingHandler.getInstance().meetingStartRequestObject(roomId,userId,"testing");
                    try {
                        meetingStartRequest.userData.put("name","sachin");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    startConnection();



                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {


                        }
                    }, 100);

                }
                else {
                    Toast.makeText(MainActivity.this,"Room id and User Id cant be empty",Toast.LENGTH_LONG).show();
                }
            }
        });
        findViewById(R.id.switchCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                MeetingHandler.getInstance().switchCamera();
                Intent in = new Intent(MainActivity.this, MyService.class);

                MeetingHandler.getInstance().startScreenShare(in,MainActivity.this);
            }
        });

        findViewById(R.id.sendMsg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MeetingHandler.getInstance().stopScreenSharing();
//                MessagePayload messagePayload = MeetingHandler.getInstance().newMessageObject("all","Hi");
//                MeetingHandler.getInstance().sendMessage(messagePayload);
            }
        });

        findViewById(R.id.end).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MeetingHandler.getInstance().endAndDestory(null);
//                MeetingHandler.getInstance().destory();
            }
        });

        findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPaused){
                    isPaused = false;

                    MeetingHandler.getInstance().resumeStreamWithoutAdding("video",MeetingHandler.getInstance().getMeetingStartRequestModel().userId);
                }
                else{
                    isPaused = true;

                    MeetingHandler.getInstance().pauseStreamWithoutStopping("video",MeetingHandler.getInstance().getMeetingStartRequestModel().userId);
                }
            }
        });
        findViewById(R.id.mute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isMuted){
                    MeetingHandler.getInstance().resumeStreamWithoutAdding("audio",MeetingHandler.getInstance().getMeetingStartRequestModel().userId);
                    isMuted = false;
                }
                else{
                    MeetingHandler.getInstance().pauseStreamWithoutStopping("audio",MeetingHandler.getInstance().getMeetingStartRequestModel().userId);
                    isMuted = true;
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        askPermission();

    }

    @Override
    protected void onStop() {
        super.onStop();
//        MeetingHandler.getInstance().destory();
    }


    private void startConnection(){
//        MeetingHandler.getInstance().initWithUserIdAndRoomId(this,meetingStartRequest,this);

        MeetingHandler.getInstance().addEventListner("onConnected", new EventEmitterHandler() {
            @Override
            public void onEvent(String type,Object listner) {
                Log.e("onConnected","onConnected");
                MeetingHandler.getInstance().startLocalStream(false,true);

                MeetingHandler.getInstance().startMeeting();



            }
        });

        MeetingHandler.getInstance().addEventListner("onTrack", new EventEmitterHandler() {
            @Override
            public void onEvent(String type,Object data) {
                Track track = (Track) data;

                Log.e("Sachin","onTrack "+track.trackId);
                if(track.kind.equalsIgnoreCase("video") && track.isLocalTrack == false  ){
                    Log.e("Sachin","refershTrack "+track.trackId);

                    vaniMeetingVideoView.setVideoTrack((Track) data);

                }

            }
        });
        MeetingHandler.getInstance().addEventListner("refershTrack", new EventEmitterHandler() {
            @Override
            public void onEvent(String type,Object data) {
                Track track = (Track) data;
                if(track.kind.equalsIgnoreCase("video") && track.isLocalTrack == false ){
                    Log.e("Sachin","refershTrack "+track.trackId);

                    vaniMeetingVideoView.setVideoTrack((Track) data);

                }

            }
        });

        MeetingHandler.getInstance().addEventListner("onUserJoined", new EventEmitterHandler() {
            @Override
            public void onEvent(String type,Object data) {
                Participant participant = (Participant)data;
                Log.e("Sachin","onUserJoined " + participant.userId);

            }
        });

        MeetingHandler.getInstance().init(MainActivity.this);
        MeetingHandler.getInstance().startLocalStream(true,true);
        MeetingHandler.getInstance().checkSocket();

    }

//    private void renderVideo(Track track){
//        if ( MainActivity.this.track  != null || track.track == null){
//            return;
//        }
//
//        MainActivity.this.track = track;
//
//        EglBase.Context eglBaseContext = EglUtils.getRootEglBaseContext();
//
//
//
////        SurfaceViewRenderer localView = findViewById(R.id.localView);
////        localView.setMirror(true);
////        localView.init(eglBaseContext, null);
//
//
//
//        sink = new ProxyVideoSink();
////        sink.setTarget(localView);
//
//        ((VideoTrack)track.track).addSink(sink);
//Log.e("Sachin","Done");
//
//    }

    private void askPermission(){
        Dexter.withContext(getApplicationContext())
                .withPermissions(Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {

                    }
                }).check();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MeetingHandler.getInstance().onActivityResult(requestCode,resultCode,data);
    }
}
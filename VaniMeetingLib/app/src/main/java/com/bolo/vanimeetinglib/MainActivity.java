package com.bolo.vanimeetinglib;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.bolo.meetinglib.MeetingCallBack;
import com.bolo.meetinglib.MeetingHandler;
import com.bolo.meetinglib.model.MeetingStartRequest;
import com.bolo.meetinglib.model.MessagePayload;
import com.bolo.meetinglib.socketHandler.SocketAndRtc;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;



import java.util.List;

public class MainActivity extends AppCompatActivity implements MeetingCallBack {

    private LinearLayout localVideoView,remoteVideoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                    boolean isAudioRequired= audioRequired.isChecked();
                    boolean isVideoRequired = videoRequired.isChecked();
                    MeetingStartRequest meetingStartRequest= new MeetingStartRequest(userId,roomId,isVideoRequired,isAudioRequired);
                    startConnection(meetingStartRequest);

                }
                else {
                    Toast.makeText(MainActivity.this,"Room id and User Id cant be empty",Toast.LENGTH_LONG).show();
                }
            }
        });
        findViewById(R.id.switchCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MeetingHandler.getInstance().switchCamera();
            }
        });

        findViewById(R.id.sendMsg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MessagePayload messagePayload = new MessagePayload("Hi","all");
                MeetingHandler.getInstance().sendMessage(messagePayload);
            }
        });

        findViewById(R.id.end).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MeetingHandler.getInstance().destory();
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
        MeetingHandler.getInstance().destory();
    }

    private void startConnection(MeetingStartRequest meetingStartRequest){
        MeetingHandler.getInstance().initWithUserIdAndRoomId(this,meetingStartRequest,this);
        MeetingHandler.getInstance().connect();

    }


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
    public void onNewUserJoined(String userId) {
        Toast.makeText(this,"User id Joined : " + userId , Toast.LENGTH_LONG).show();Toast.makeText(this,"User id Joined : " + userId , Toast.LENGTH_LONG).show();
        AudioManager audioManager =  (AudioManager) this.getSystemService(AUDIO_SERVICE);

        AudioManager mAudioManager = audioManager;
        mAudioManager.setSpeakerphoneOn(true);
    }

    @Override
    public void onUserLeft(String userId) {
        Toast.makeText(this,"User id Left : " + userId , Toast.LENGTH_LONG).show();Toast.makeText(this,"User id Joined : " + userId , Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNewChatMessageReceived(MessagePayload messagePayload) {
        Toast.makeText(this,"New Message From " + messagePayload.getSender() +" Message : " +messagePayload.getMessage() , Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(Error error) {

    }

    @Override
    public void onConnected() {

        MeetingHandler.getInstance().fetchWhiteboard();
    }

    @Override
    public void onLocalVideoSurfaceView(SurfaceView surfaceView) {
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(300, 300);
        surfaceView.setLayoutParams(params);
        localVideoView.addView(surfaceView);

    }

    @Override
    public void onRemoteVideoSurfaceView(SurfaceView surfaceView, String userId) {
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(300, 300);
        surfaceView.setLayoutParams(params);
        remoteVideoView.addView(surfaceView);
    }

    @Override
    public void onWhiteboardUrl(String whiteboardUrl) {
        Log.e("Whiteboard",whiteboardUrl);
    }

    @Override
    public void onVideoSurfaceViewDestroyed(SurfaceView surfaceViewRenderer, boolean isLocalVideo) {
            ViewGroup viewParent = (ViewGroup) surfaceViewRenderer.getParent();
            if (viewParent != null) {
                viewParent.removeView(surfaceViewRenderer);
            }


    }


}